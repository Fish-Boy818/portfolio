package com.untitled.service;

import com.untitled.config.StorageProperties;
import com.untitled.config.UploadProperties;
import com.untitled.dto.UploadCleanupResult;
import com.untitled.dto.UploadMissingItem;
import com.untitled.dto.UploadMissingResult;
import com.untitled.mapper.ActivityMapper;
import com.untitled.mapper.BannerMapper;
import com.untitled.mapper.ClubMapper;
import com.untitled.mapper.WelfareSubmissionMapper;
import com.untitled.model.Activity;
import com.untitled.model.Banner;
import com.untitled.model.Club;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class UploadCleanupService {
    private final UploadProperties uploadProperties;
    private final StorageProperties storageProperties;
    private final ActivityMapper activityMapper;
    private final ClubMapper clubMapper;
    private final BannerMapper bannerMapper;
    private final WelfareSubmissionMapper welfareSubmissionMapper;
    private final StorageService storageService;

    public UploadCleanupService(UploadProperties uploadProperties,
                                StorageProperties storageProperties,
                                ActivityMapper activityMapper,
                                ClubMapper clubMapper,
                                BannerMapper bannerMapper,
                                WelfareSubmissionMapper welfareSubmissionMapper,
                                StorageService storageService) {
        this.uploadProperties = uploadProperties;
        this.storageProperties = storageProperties;
        this.activityMapper = activityMapper;
        this.clubMapper = clubMapper;
        this.bannerMapper = bannerMapper;
        this.welfareSubmissionMapper = welfareSubmissionMapper;
        this.storageService = storageService;
    }

    public UploadCleanupResult cleanup(boolean dryRun) throws IOException {
        UploadCleanupResult result = new UploadCleanupResult();
        result.setDryRun(dryRun);
        if (!"local".equalsIgnoreCase(storageProperties.getType())) {
            result.setSkipped(true);
            result.setMessage("当前存储不是本地，已跳过清理");
            return result;
        }

        Path dir = uploadProperties.resolvePath();
        if (!Files.exists(dir)) {
            result.setTotalFiles(0);
            return result;
        }

        Set<String> used = collectUsedFileNames();
        List<String> deleted = new ArrayList<>();
        List<String> kept = new ArrayList<>();

        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                String name = path.getFileName().toString();
                if (".gitignore".equalsIgnoreCase(name)) {
                    kept.add(name);
                    return;
                }
                if (used.contains(name)) {
                    kept.add(name);
                    return;
                }
                if (!dryRun) {
                    try {
                        storageService.delete(name);
                    } catch (Exception ignored) {
                        return;
                    }
                }
                deleted.add(name);
            });
        }

        result.setDeletedFiles(deleted);
        result.setKeptFiles(kept);
        result.setTotalFiles(deleted.size() + kept.size());
        result.setUsedFiles(kept.size());
        result.setUnusedFiles(deleted.size());
        return result;
    }

    public UploadMissingResult detectMissing() throws IOException {
        UploadMissingResult result = new UploadMissingResult();
        if (!"local".equalsIgnoreCase(storageProperties.getType())) {
            result.setSkipped(true);
            result.setMessage("当前存储不是本地，已跳过缺图检测");
            return result;
        }
        Path dir = uploadProperties.resolvePath();
        Set<String> existingFiles = listLocalFileNames(dir);
        List<UploadMissingItem> items = new ArrayList<>();
        int totalReferences = 0;

        List<Club> clubs = clubMapper.findAll();
        if (clubs == null) {
            clubs = new ArrayList<>();
        }
        Map<Long, String> clubNameMap = new HashMap<>();
        for (Club club : clubs) {
            if (club != null && club.getId() != null) {
                clubNameMap.put(club.getId(), resolveName(club.getName(), "俱乐部#" + club.getId()));
            }
        }

        for (Club club : clubs) {
            if (club == null) {
                continue;
            }
            String clubName = resolveName(club.getName(), "俱乐部#" + club.getId());
            totalReferences += collectMissing(items, existingFiles, "club", club.getId(), clubName, null,
                    "cover", "封面图", club.getCover());
            totalReferences += collectMissing(items, existingFiles, "club", club.getId(), clubName, null,
                    "licenseImage", "营业执照", club.getLicenseImage());
            totalReferences += collectMissingList(items, existingFiles, "club", club.getId(), clubName, null,
                    "gallery", "图集图", club.getGallery());
        }

        List<Activity> activities = activityMapper.findAll(null, null, null);
        if (activities == null) {
            activities = new ArrayList<>();
        }
        for (Activity activity : activities) {
            if (activity == null) {
                continue;
            }
            String clubName = "";
            if (activity.getClubId() != null) {
                clubName = resolveName(clubNameMap.get(activity.getClubId()), "俱乐部#" + activity.getClubId());
            }
            String activityName = resolveName(activity.getTitle(), "商品#" + activity.getId());
            totalReferences += collectMissing(items, existingFiles, "activity", activity.getId(), activityName, clubName,
                    "cover", "封面图", activity.getCover());
            totalReferences += collectMissingList(items, existingFiles, "activity", activity.getId(), activityName, clubName,
                    "gallery", "图集图", activity.getGallery());
            totalReferences += collectMissingList(items, existingFiles, "activity", activity.getId(), activityName, clubName,
                    "detailImages", "详情图", activity.getDetailImages());
        }

        List<Banner> banners = bannerMapper.findAll();
        if (banners == null) {
            banners = new ArrayList<>();
        }
        for (Banner banner : banners) {
            if (banner == null) {
                continue;
            }
            String bannerName = resolveName(banner.getTitle(), "轮播图#" + banner.getId());
            totalReferences += collectMissing(items, existingFiles, "banner", banner.getId(), bannerName, null,
                    "imageUrl", "轮播图", banner.getImageUrl());
        }

        result.setTotalReferences(totalReferences);
        result.setMissingCount(items.size());
        result.setItems(items);
        return result;
    }

    private Set<String> collectUsedFileNames() {
        Set<String> used = new HashSet<>();
        List<Map<String, Object>> activityRefs = activityMapper.findAllImageRefs();
        for (Map<String, Object> row : activityRefs) {
            addName(used, row.get("cover"));
            addList(used, row.get("gallery"));
            addList(used, row.get("detailImages"));
        }
        List<Map<String, Object>> clubRefs = clubMapper.findAllImageRefs();
        for (Map<String, Object> row : clubRefs) {
            addName(used, row.get("cover"));
            addName(used, row.get("licenseImage"));
            addList(used, row.get("gallery"));
        }
        List<String> bannerUrls = bannerMapper.findAllImageUrls();
        for (String url : bannerUrls) {
            addName(used, url);
        }
        List<String> welfareUrls = welfareSubmissionMapper.findAllImageUrls();
        for (String url : welfareUrls) {
            addName(used, url);
        }
        return used;
    }

    private Set<String> listLocalFileNames(Path dir) throws IOException {
        Set<String> files = new HashSet<>();
        if (!Files.exists(dir)) {
            return files;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile).forEach(path -> files.add(path.getFileName().toString()));
        }
        return files;
    }

    private int collectMissingList(List<UploadMissingItem> items,
                                   Set<String> existingFiles,
                                   String entityType,
                                   Long entityId,
                                   String entityName,
                                   String clubName,
                                   String fieldKey,
                                   String fieldLabel,
                                   String value) {
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        int references = 0;
        String[] parts = value.split(",");
        for (String part : parts) {
            references += collectMissing(items, existingFiles, entityType, entityId, entityName, clubName,
                    fieldKey, fieldLabel, part);
        }
        return references;
    }

    private int collectMissing(List<UploadMissingItem> items,
                               Set<String> existingFiles,
                               String entityType,
                               Long entityId,
                               String entityName,
                               String clubName,
                               String fieldKey,
                               String fieldLabel,
                               String reference) {
        if (!StringUtils.hasText(reference)) {
            return 0;
        }
        String raw = reference.trim();
        if (!isLocalUploadReference(raw)) {
            return 0;
        }
        String fileName = extractName(raw);
        if (!StringUtils.hasText(fileName)) {
            return 0;
        }
        if (existingFiles.contains(fileName)) {
            return 1;
        }

        UploadMissingItem item = new UploadMissingItem();
        item.setEntityType(entityType);
        item.setEntityId(entityId);
        item.setEntityName(entityName);
        item.setClubName(clubName);
        item.setFieldKey(fieldKey);
        item.setFieldLabel(fieldLabel);
        item.setFileName(fileName);
        item.setReference(raw);
        items.add(item);
        return 1;
    }

    private boolean isLocalUploadReference(String reference) {
        if (!StringUtils.hasText(reference)) {
            return false;
        }
        String raw = reference.trim();
        String lower = raw.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return lower.contains("/uploads/");
        }
        if (lower.startsWith("/uploads/") || lower.startsWith("uploads/") || lower.contains("\\uploads\\")) {
            return true;
        }
        return !(raw.contains("/") || raw.contains("\\"));
    }

    private String resolveName(String value, String fallback) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return fallback;
    }

    private void addList(Set<String> used, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return;
        }
        String[] parts = text.split(",");
        for (String part : parts) {
            addName(used, part);
        }
    }

    private void addName(Set<String> used, Object value) {
        if (value == null) {
            return;
        }
        String name = extractName(String.valueOf(value));
        if (StringUtils.hasText(name)) {
            used.add(name);
        }
    }

    private String extractName(String urlOrName) {
        if (!StringUtils.hasText(urlOrName)) {
            return "";
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
            return "";
        }
        return Paths.get(name).getFileName().toString();
    }
}
