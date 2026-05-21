package com.untitled.service;

import com.untitled.dto.ClubCreateRequest;
import com.untitled.dto.ClubResponse;
import com.untitled.dto.ClubUpdateRequest;
import com.untitled.mapper.ActivityMapper;
import com.untitled.mapper.ActivitySlotMapper;
import com.untitled.mapper.ClubMapper;
import com.untitled.model.Club;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ClubService {
    private static final Pattern HTTP_URL_PATTERN = Pattern.compile("(?i)https?://[^\\s\"'<>]+");
    private static final Pattern DOUYIN_SHORT_URL_PATTERN = Pattern.compile("(?i)^https?://v\\.douyin\\.com/[A-Za-z0-9_-]+/?");
    private static final int SHORT_URL_MAX_REDIRECTS = 6;
    private final ClubMapper clubMapper;
    private final ActivityMapper activityMapper;
    private final ActivitySlotMapper activitySlotMapper;

    public ClubService(ClubMapper clubMapper, ActivityMapper activityMapper, ActivitySlotMapper activitySlotMapper) {
        this.clubMapper = clubMapper;
        this.activityMapper = activityMapper;
        this.activitySlotMapper = activitySlotMapper;
    }

    public List<ClubResponse> list() {
        return clubMapper.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<ClubResponse> get(long id) {
        Club club = clubMapper.findById(id);
        if (club == null) {
            return Optional.empty();
        }
        return Optional.of(toResponse(club));
    }

    public ClubResponse create(ClubCreateRequest request) { // 作用：新增俱乐部信息；方法：构造俱乐部对象并保存到数据库
        Club club = new Club(); // 作用：创建俱乐部实体对象；方法：实例化 Club 作为新增数据载体
        applyRequest(club, request.getName(), request.getLocation(), request.getAddress(),
                request.getPhone(), request.getOpenTime(), request.getDouyinUrl(), request.getTags(), request.getCover(), request.getLicenseImage(), request.getGallery()); // 作用：写入俱乐部字段；方法：复用 applyRequest 统一处理门店基础信息
        clubMapper.insert(club); // 作用：保存俱乐部记录；方法：调用 Mapper 执行插入操作
        return toResponse(clubMapper.findById(club.getId())); // 作用：返回新增后的俱乐部详情；方法：重新查询最新记录并转换为响应对象
    }

    public Optional<ClubResponse> update(long id, ClubUpdateRequest request) { // 作用：修改俱乐部信息；方法：查询原记录后更新字段并保存
        Club club = clubMapper.findById(id); // 作用：查询待修改的俱乐部；方法：根据主键读取现有门店数据
        if (club == null) {
            return Optional.empty();
        }
        applyRequest(club, request.getName(), request.getLocation(), request.getAddress(),
                request.getPhone(), request.getOpenTime(), request.getDouyinUrl(), request.getTags(), request.getCover(), request.getLicenseImage(), request.getGallery()); // 作用：写入修改后的字段；方法：复用 applyRequest 统一覆盖更新内容
        clubMapper.update(club); // 作用：更新俱乐部记录；方法：调用 Mapper 执行数据库更新
        return Optional.of(toResponse(clubMapper.findById(id))); // 作用：返回修改后的俱乐部详情；方法：重新查询最新数据并转换为响应对象
    }

    @Transactional
    public boolean delete(long id) {
        Club club = clubMapper.findById(id);
        if (club == null) {
            return false;
        }
        List<Long> activityIds = activityMapper.findIdsByClubId(id);
        if (activityIds != null && !activityIds.isEmpty()) {
            activitySlotMapper.deleteByActivityIds(activityIds);
            activityMapper.deleteByClubId(id);
        }
        return clubMapper.delete(id) > 0;
    }

    private void applyRequest(Club club, String name, String location, String address, String phone, String openTime,
                              String douyinUrl,
                              List<String> tags, String cover, String licenseImage, List<String> gallery) {
        String cleanCover = normalizeMedia(cover);
        String cleanLicenseImage = normalizeMedia(licenseImage);
        List<String> cleanGallery = normalizeMediaList(gallery).stream()
                .filter(url -> !url.equals(cleanCover))
                .filter(url -> !url.equals(cleanLicenseImage))
                .collect(Collectors.toList());
        club.setName(name);
        club.setLocation(location);
        club.setAddress(address);
        club.setPhone(phone);
        club.setOpenTime(openTime);
        club.setDouyinUrl(normalizeDouyinUrlForStore(douyinUrl));
        club.setTags(join(tags));
        club.setCover(cleanCover);
        club.setLicenseImage(cleanLicenseImage);
        club.setGallery(join(cleanGallery));
    }

    private ClubResponse toResponse(Club club) {
        ClubResponse response = new ClubResponse();
        response.setId(club.getId());
        response.setName(club.getName());
        response.setLocation(club.getLocation());
        response.setAddress(club.getAddress());
        response.setPhone(club.getPhone());
        response.setOpenTime(club.getOpenTime());
        response.setDouyinUrl(normalizeDouyinUrl(club.getDouyinUrl()));
        response.setTags(split(club.getTags()));
        response.setCover(club.getCover());
        response.setLicenseImage(club.getLicenseImage());
        response.setGallery(split(club.getGallery()));
        return response;
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(v -> v != null && !v.trim().isEmpty())
                .collect(Collectors.joining(","));
    }

    private List<String> split(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toList());
    }

    private String normalizeMedia(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeDouyinUrlForStore(String value) {
        String normalized = normalizeDouyinUrl(value);
        if (normalized.isEmpty()) {
            return "";
        }
        String candidate = normalized;
        if (DOUYIN_SHORT_URL_PATTERN.matcher(normalized).find()) {
            candidate = normalizeDouyinUrl(expandShortUrl(normalized));
        }
        candidate = stripQueryAndFragment(candidate);
        if (candidate.length() > 255) {
            if (normalized.length() <= 255) {
                return normalized;
            }
            return normalized.substring(0, 255);
        }
        return candidate;
    }

    private String normalizeDouyinUrl(String value) {
        String text = normalizeMedia(value);
        if (text.isEmpty()) {
            return "";
        }
        Matcher matcher = HTTP_URL_PATTERN.matcher(text);
        String candidate = matcher.find() ? matcher.group() : text;
        candidate = trimTrailingUrlChars(candidate);
        if (!candidate.matches("(?i)^https?://.*")) {
            candidate = "https://" + candidate;
        }
        Matcher shortMatcher = DOUYIN_SHORT_URL_PATTERN.matcher(candidate);
        if (shortMatcher.find()) {
            return shortMatcher.group();
        }
        return candidate.replaceAll("\\s+", "");
    }

    private String expandShortUrl(String shortUrl) {
        String current = shortUrl;
        for (int i = 0; i < SHORT_URL_MAX_REDIRECTS; i++) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(current).openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile Safari"
                );
                int code = conn.getResponseCode();
                if (code >= 300 && code < 400) {
                    String location = conn.getHeaderField("Location");
                    if (location == null || location.trim().isEmpty()) {
                        break;
                    }
                    current = resolveLocation(current, location.trim());
                    continue;
                }
                break;
            } catch (IOException ex) {
                break;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        return trimTrailingUrlChars(current).replaceAll("\\s+", "");
    }

    private String resolveLocation(String base, String location) {
        try {
            return new URL(new URL(base), location).toString();
        } catch (Exception ex) {
            return location;
        }
    }

    private String stripQueryAndFragment(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        int q = value.indexOf('?');
        int h = value.indexOf('#');
        int cut = -1;
        if (q >= 0 && h >= 0) {
            cut = Math.min(q, h);
        } else if (q >= 0) {
            cut = q;
        } else if (h >= 0) {
            cut = h;
        }
        return cut >= 0 ? value.substring(0, cut) : value;
    }

    private String trimTrailingUrlChars(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String trimChars = ")]}>.,;!?";
        int end = value.length();
        while (end > 0 && trimChars.indexOf(value.charAt(end - 1)) >= 0) {
            end--;
        }
        return value.substring(0, end);
    }

    private List<String> normalizeMediaList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(
                values.stream()
                        .map(this::normalizeMedia)
                        .filter(v -> !v.isEmpty())
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        );
    }
}
