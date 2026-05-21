package com.untitled.service;

import com.untitled.config.UploadProperties;
import com.untitled.dto.StorageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalStorageService {
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp4", "mov", "m4v", "webm", "avi", "mkv"
    ));
    private final UploadProperties uploadProperties;

    public LocalStorageService(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    public StorageResult upload(MultipartFile file) throws IOException {
        return upload(file, null);
    }

    public StorageResult upload(MultipartFile file, Integer maxVideoSeconds) throws IOException {
        String original = file.getOriginalFilename();
        String safeName = "";
        if (StringUtils.hasText(original)) {
            safeName = Paths.get(original).getFileName().toString();
            safeName = safeName.replaceAll("[^a-zA-Z0-9._-]", "_");
        }
        if (!StringUtils.hasText(safeName)) {
            safeName = UUID.randomUUID().toString().replace("-", "");
        }
        String extension = getExtension(safeName);
        String baseName = safeName;
        int dotIndex = safeName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = safeName.substring(0, dotIndex);
        }
        if (!StringUtils.hasText(baseName)) {
            baseName = "upload";
        }
        String shortBase = baseName.length() > 24 ? baseName.substring(0, 24) : baseName;
        String unique = UUID.randomUUID().toString().replace("-", "");
        String filename = shortBase + "_" + unique + (StringUtils.hasText(extension) ? "." + extension : "");
        Path dir = uploadProperties.resolvePath();
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        boolean shouldTrimVideo = maxVideoSeconds != null && maxVideoSeconds > 0 && isVideoFile(file, safeName);
        if (!shouldTrimVideo) {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new StorageResult("/uploads/" + filename, filename);
        }
        Path tmpInput = Files.createTempFile("banner-video-src-", getExtensionWithDot(safeName));
        try {
            Files.copy(file.getInputStream(), tmpInput, StandardCopyOption.REPLACE_EXISTING);
            trimVideo(tmpInput, target, maxVideoSeconds);
        } finally {
            Files.deleteIfExists(tmpInput);
        }
        return new StorageResult("/uploads/" + filename, filename);
    }

    private boolean isVideoFile(MultipartFile file, String fileName) {
        String extension = getExtension(fileName);
        if (StringUtils.hasText(extension) && VIDEO_EXTENSIONS.contains(extension.toLowerCase())) {
            return true;
        }
        String contentType = file.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("video/");
    }

    private void trimVideo(Path input, Path output, int maxSeconds) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", input.toString(),
                "-t", String.valueOf(maxSeconds),
                "-c:v", "libx264",
                "-profile:v", "main",
                "-level", "4.0",
                "-preset", "fast",
                "-vf", "scale='if(gte(iw,ih),if(gte(ih,1080),iw,-2),if(gte(iw,1080),iw,1080))':'if(gte(iw,ih),if(gte(ih,1080),ih,1080),if(gte(iw,1080),ih,-2))'",
                "-r", "24",
                "-crf", "19",
                "-maxrate", "3500k",
                "-bufsize", "7000k",
                "-g", "48",
                "-pix_fmt", "yuv420p",
                "-movflags", "+faststart",
                "-an",
                output.toString()
        );
        builder.redirectErrorStream(true);
        Process process;
        String ffmpegOutput = "";
        try {
            process = builder.start();
            try (InputStream stream = process.getInputStream()) {
                ffmpegOutput = new String(readAllBytes(stream), StandardCharsets.UTF_8);
            }
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IllegalStateException("视频裁剪失败，请检查 ffmpeg 配置。详情: " + summarizeLog(ffmpegOutput));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("服务器未安装 ffmpeg，无法自动剪切视频到 5 秒。", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("视频裁剪被中断，请重试。", ex);
        }
    }

    private byte[] readAllBytes(InputStream stream) throws IOException {
        byte[] buffer = new byte[4096];
        int len;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        while ((len = stream.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        return out.toByteArray();
    }

    private String summarizeLog(String log) {
        if (!StringUtils.hasText(log)) {
            return "";
        }
        String cleaned = log.replace("\r", " ").replace("\n", " ").trim();
        if (cleaned.length() <= 180) {
            return cleaned;
        }
        return cleaned.substring(0, 180) + "...";
    }

    private String getExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1);
    }

    private String getExtensionWithDot(String fileName) {
        String ext = getExtension(fileName);
        if (!StringUtils.hasText(ext)) {
            return ".tmp";
        }
        return "." + ext;
    }

    public boolean delete(String urlOrName) throws IOException {
        if (!StringUtils.hasText(urlOrName)) {
            return false;
        }
        String cleaned = urlOrName.trim();
        int queryIndex = cleaned.indexOf('?');
        if (queryIndex >= 0) {
            cleaned = cleaned.substring(0, queryIndex);
        }
        int hashIndex = cleaned.indexOf('#');
        if (hashIndex >= 0) {
            cleaned = cleaned.substring(0, hashIndex);
        }
        String marker = "/uploads/";
        String name;
        int markerIndex = cleaned.lastIndexOf(marker);
        if (markerIndex >= 0) {
            name = cleaned.substring(markerIndex + marker.length());
        } else {
            int slashIndex = Math.max(cleaned.lastIndexOf('/'), cleaned.lastIndexOf('\\'));
            name = slashIndex >= 0 ? cleaned.substring(slashIndex + 1) : cleaned;
        }
        if (!StringUtils.hasText(name)) {
            return false;
        }
        String safeName = Paths.get(name).getFileName().toString();
        if (!StringUtils.hasText(safeName)) {
            return false;
        }
        Path dir = uploadProperties.resolvePath();
        Path target = dir.resolve(safeName).normalize();
        if (!target.startsWith(dir)) {
            return false;
        }
        return Files.deleteIfExists(target);
    }
}
