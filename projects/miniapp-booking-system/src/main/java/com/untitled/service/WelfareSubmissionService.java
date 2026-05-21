package com.untitled.service;

import com.untitled.dto.WelfareSubmissionCreateRequest;
import com.untitled.dto.WelfareSubmissionResponse;
import com.untitled.dto.WelfareSubmissionReviewRequest;
import com.untitled.mapper.BountyTaskMapper;
import com.untitled.mapper.UserMapper;
import com.untitled.mapper.WelfareSubmissionMapper;
import com.untitled.model.BountyTask;
import com.untitled.model.CommissionRecord;
import com.untitled.model.User;
import com.untitled.model.WelfareSubmission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WelfareSubmissionService {
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    private static final String DEFAULT_OPERATOR = "admin";

    private final WelfareSubmissionMapper welfareSubmissionMapper;
    private final UserMapper userMapper;
    private final BountyTaskMapper bountyTaskMapper;
    private final BountyTaskService bountyTaskService;
    private final StorageService storageService;
    private final CommissionService commissionService;
    private final Random random = new Random();

    public WelfareSubmissionService(WelfareSubmissionMapper welfareSubmissionMapper,
                                    UserMapper userMapper,
                                    BountyTaskMapper bountyTaskMapper,
                                    BountyTaskService bountyTaskService,
                                    StorageService storageService,
                                    CommissionService commissionService) {
        this.welfareSubmissionMapper = welfareSubmissionMapper;
        this.userMapper = userMapper;
        this.bountyTaskMapper = bountyTaskMapper;
        this.bountyTaskService = bountyTaskService;
        this.storageService = storageService;
        this.commissionService = commissionService;
    }

    @Transactional
    public WelfareSubmissionResponse create(long userId, WelfareSubmissionCreateRequest request) {
        cleanupExpiredImagesQuietly();
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        String screenshotUrl = trimToNull(request.getScreenshotUrl());
        if (screenshotUrl == null) {
            throw new IllegalArgumentException("请先上传好评截图");
        }
        Long submissionId = request.getSubmissionId() != null && request.getSubmissionId() > 0
                ? request.getSubmissionId()
                : null;
        Long bountyTaskId = request.getBountyTaskId() != null && request.getBountyTaskId() > 0
                ? request.getBountyTaskId()
                : null;
        WelfareSubmission existingSubmission = null;
        if (submissionId != null) {
            existingSubmission = welfareSubmissionMapper.findByIdForUpdate(submissionId);
            if (existingSubmission == null || existingSubmission.getUserId() == null
                    || existingSubmission.getUserId() != userId) {
                throw new IllegalArgumentException("悬赏提交记录不存在");
            }
            if (!STATUS_REJECTED.equals(existingSubmission.getStatus())) {
                throw new IllegalArgumentException("当前记录无法重新提交");
            }
            if (bountyTaskId == null && existingSubmission.getBountyTaskId() != null
                    && existingSubmission.getBountyTaskId() > 0) {
                bountyTaskId = existingSubmission.getBountyTaskId();
            }
            if (bountyTaskId != null && existingSubmission.getBountyTaskId() != null
                    && existingSubmission.getBountyTaskId() > 0
                    && !bountyTaskId.equals(existingSubmission.getBountyTaskId())) {
                throw new IllegalArgumentException("重新提交的悬赏任务不匹配");
            }
        }
        BountyTask bountyTask = null;
        if (bountyTaskId != null) {
            bountyTask = bountyTaskMapper.findById(bountyTaskId);
            if (bountyTask == null || !BountyTaskService.STATUS_ACTIVE.equals(bountyTask.getStatus())) {
                throw new IllegalArgumentException("悬赏任务不存在或已下架");
            }
        }
        WelfareSubmission submission = existingSubmission != null ? existingSubmission : new WelfareSubmission();
        if (existingSubmission == null) {
            submission.setSubmissionNo(generateSubmissionNo());
            submission.setUserId(userId);
        }
        submission.setBountyTaskId(bountyTaskId);
        submission.setBountyLocation(bountyTask != null ? trimToEmpty(bountyTask.getLocation()) : submission.getBountyLocation());
        submission.setPlatformName(bountyTask != null ? trimToEmpty(bountyTask.getLocation()) : trimToEmpty(request.getPlatformName()));
        submission.setReviewText(trimToNull(request.getReviewText()));
        String previousScreenshotUrl = trimToNull(existingSubmission != null ? existingSubmission.getScreenshotUrl() : null);
        submission.setScreenshotUrl(screenshotUrl);
        submission.setStatus(STATUS_PENDING);
        submission.setRewardAmount(BigDecimal.ZERO);
        submission.setReviewNote(null);
        submission.setReviewedBy(null);
        submission.setReviewedAt(null);
        submission.setRewardRecordId(null);
        submission.setRewardedAt(null);
        submission.setDeleteImageAt(null);
        submission.setImageDeleted(0);
        if (existingSubmission == null) {
            welfareSubmissionMapper.insert(submission);
        } else {
            welfareSubmissionMapper.update(submission);
            deleteStaleScreenshotQuietly(previousScreenshotUrl, screenshotUrl);
        }
        if (bountyTaskId != null) {
            bountyTaskService.markSubmitted(bountyTaskId, userId, submission.getId());
        }
        return toResponse(welfareSubmissionMapper.findById(submission.getId()));
    }

    public List<WelfareSubmissionResponse> listByUser(long userId) {
        cleanupExpiredImagesQuietly();
        List<WelfareSubmission> list = welfareSubmissionMapper.findByUserId(userId);
        List<WelfareSubmissionResponse> result = new ArrayList<WelfareSubmissionResponse>();
        Map<Long, User> userMap = buildUserMap(list);
        for (WelfareSubmission item : list) {
            result.add(toResponse(item, userMap));
        }
        return result;
    }

    public List<WelfareSubmissionResponse> listForAdmin(Long userId, String status) {
        cleanupExpiredImagesQuietly();
        List<WelfareSubmission> list = welfareSubmissionMapper.findAllForAdmin(userId, trimToNull(status));
        List<WelfareSubmissionResponse> result = new ArrayList<WelfareSubmissionResponse>();
        Map<Long, User> userMap = buildUserMap(list);
        Map<Long, Integer> attemptCountMap = buildAttemptCountMap(list);
        for (WelfareSubmission item : list) {
            result.add(toResponse(item, userMap, attemptCountMap));
        }
        return result;
    }

    @Transactional
    public WelfareSubmissionResponse review(long id, WelfareSubmissionReviewRequest request, String operatorName) {
        cleanupExpiredImagesQuietly();
        WelfareSubmission submission = welfareSubmissionMapper.findByIdForUpdate(id);
        if (submission == null) {
            throw new IllegalArgumentException("悬赏任务记录不存在");
        }
        String targetStatus = trimToNull(request != null ? request.getStatus() : null);
        if (targetStatus == null) {
            throw new IllegalArgumentException("审核状态不能为空");
        }
        if (!STATUS_PENDING.equals(targetStatus) && !STATUS_APPROVED.equals(targetStatus) && !STATUS_REJECTED.equals(targetStatus)) {
            throw new IllegalArgumentException("不支持的审核状态");
        }
        if (STATUS_APPROVED.equals(submission.getStatus())) {
            throw new IllegalArgumentException("该记录已审核通过，无需重复处理");
        }

        BigDecimal rewardAmount = money(request != null ? request.getRewardAmount() : null);
        if (STATUS_APPROVED.equals(targetStatus)) {
            BountyTask bountyTask = submission.getBountyTaskId() != null && submission.getBountyTaskId() > 0
                    ? bountyTaskMapper.findById(submission.getBountyTaskId())
                    : null;
            if (rewardAmount.signum() <= 0) {
                rewardAmount = money(submission.getRewardAmount());
            }
            if (rewardAmount.signum() <= 0) {
                throw new IllegalArgumentException("审核通过时请填写奖励佣金");
            }
            if (bountyTask != null) {
                BigDecimal commissionMin = money(bountyTask.getCommissionMin());
                BigDecimal commissionMax = money(bountyTask.getCommissionMax());
                if (rewardAmount.compareTo(commissionMin) < 0 || rewardAmount.compareTo(commissionMax) > 0) {
                    throw new IllegalArgumentException(
                            "奖励佣金需在悬赏区间 " + commissionMin.toPlainString() + "-" + commissionMax.toPlainString() + " 元之间"
                    );
                }
            }
            if (submission.getRewardRecordId() == null || submission.getRewardRecordId() <= 0) {
                CommissionRecord record = commissionService.grantWelfareCommission(
                        submission.getUserId(),
                        submission.getId(),
                        submission.getSubmissionNo(),
                        rewardAmount,
                        submission.getPlatformName()
                );
                submission.setRewardRecordId(record.getId());
                submission.setRewardedAt(LocalDateTime.now());
            }
            submission.setRewardAmount(rewardAmount);
            submission.setDeleteImageAt(LocalDateTime.now().plusHours(2));
        }
        String reviewNote = trimToNull(request != null ? request.getReviewNote() : null);
        if (STATUS_REJECTED.equals(targetStatus)) {
            if (reviewNote == null) {
                throw new IllegalArgumentException("驳回时请填写驳回原因");
            }
            submission.setRewardAmount(BigDecimal.ZERO);
            submission.setDeleteImageAt(null);
        }

        submission.setStatus(targetStatus);
        submission.setReviewNote(reviewNote);
        submission.setReviewedBy(trimToNull(operatorName) != null ? trimToNull(operatorName) : DEFAULT_OPERATOR);
        submission.setReviewedAt(LocalDateTime.now());
        submission.setImageDeleted(submission.getImageDeleted() != null ? submission.getImageDeleted() : 0);
        welfareSubmissionMapper.update(submission);
        if (submission.getBountyTaskId() != null && submission.getBountyTaskId() > 0) {
            bountyTaskService.markReviewed(submission.getBountyTaskId(), submission.getUserId(), targetStatus, submission.getId());
        }
        return toResponse(welfareSubmissionMapper.findById(id));
    }

    @Transactional
    public int cleanupExpiredImages() {
        List<WelfareSubmission> list = welfareSubmissionMapper.findExpiredImages(LocalDateTime.now());
        int count = 0;
        for (WelfareSubmission item : list) {
            if (item == null || item.getId() == null) {
                continue;
            }
            String screenshotUrl = trimToNull(item.getScreenshotUrl());
            if (screenshotUrl != null) {
                String[] urls = screenshotUrl.split(",");
                for (String url : urls) {
                    String trimmed = trimToNull(url);
                    if (trimmed != null) {
                        try {
                            storageService.delete(trimmed);
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
            item.setScreenshotUrl(null);
            item.setImageDeleted(1);
            welfareSubmissionMapper.update(item);
            count += 1;
        }
        return count;
    }

    private void cleanupExpiredImagesQuietly() {
        try {
            cleanupExpiredImages();
        } catch (Exception ignored) {
            // Ignore opportunistic cleanup failures.
        }
    }

    private void deleteStaleScreenshotQuietly(String previousScreenshotUrl, String currentScreenshotUrl) {
        if (previousScreenshotUrl == null || previousScreenshotUrl.equals(currentScreenshotUrl)) {
            return;
        }
        String[] previousUrls = previousScreenshotUrl.split(",");
        String[] currentUrls = currentScreenshotUrl != null ? currentScreenshotUrl.split(",") : new String[0];
        Set<String> currentSet = new HashSet<String>();
        for (String url : currentUrls) {
            currentSet.add(trimToNull(url));
        }
        for (String url : previousUrls) {
            String trimmed = trimToNull(url);
            if (trimmed != null && !currentSet.contains(trimmed)) {
                try {
                    storageService.delete(trimmed);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private WelfareSubmissionResponse toResponse(WelfareSubmission submission) {
        User user = submission != null && submission.getUserId() != null
                ? userMapper.findById(submission.getUserId())
                : null;
        return toResponse(submission, user);
    }

    private WelfareSubmissionResponse toResponse(WelfareSubmission submission, Map<Long, User> userMap) {
        return toResponse(submission, userMap, null);
    }

    private WelfareSubmissionResponse toResponse(WelfareSubmission submission,
                                                 Map<Long, User> userMap,
                                                 Map<Long, Integer> attemptCountMap) {
        User user = submission != null && submission.getUserId() != null
                ? userMap.get(submission.getUserId())
                : null;
        Integer attempt = submission != null && submission.getId() != null && attemptCountMap != null
                ? attemptCountMap.get(submission.getId())
                : null;
        return toResponse(submission, user, attempt);
    }

    private WelfareSubmissionResponse toResponse(WelfareSubmission submission, User user) {
        return toResponse(submission, user, null);
    }

    private WelfareSubmissionResponse toResponse(WelfareSubmission submission, User user, Integer submissionAttempt) {
        WelfareSubmissionResponse response = new WelfareSubmissionResponse();
        response.setId(submission.getId());
        response.setSubmissionNo(submission.getSubmissionNo());
        response.setUserId(submission.getUserId());
        response.setUserPhone(user != null ? trimToNull(user.getPhone()) : null);
        response.setUserNickname(user != null ? trimToNull(user.getNickname()) : null);
        response.setBountyTaskId(submission.getBountyTaskId());
        String bountyLocation = trimToNull(submission.getBountyLocation());
        response.setBountyLocation(bountyLocation);
        Integer normalizedAttempt = submissionAttempt != null && submissionAttempt > 0 ? submissionAttempt : null;
        response.setSubmissionAttempt(normalizedAttempt);
        response.setBountyLocationAttemptText(formatBountyLocationAttempt(bountyLocation, normalizedAttempt));
        response.setPlatformName(submission.getPlatformName());
        response.setReviewText(submission.getReviewText());
        response.setScreenshotUrl(submission.getScreenshotUrl());
        response.setStatus(submission.getStatus());
        response.setStatusText(statusText(submission.getStatus()));
        response.setRewardAmount(money(submission.getRewardAmount()));
        response.setReviewNote(submission.getReviewNote());
        response.setReviewedBy(submission.getReviewedBy());
        response.setReviewedAt(submission.getReviewedAt());
        response.setRewardRecordId(submission.getRewardRecordId());
        response.setRewardedAt(submission.getRewardedAt());
        response.setDeleteImageAt(submission.getDeleteImageAt());
        response.setImageDeleted(submission.getImageDeleted() != null && submission.getImageDeleted() == 1);
        response.setCreatedAt(submission.getCreatedAt());
        response.setUpdatedAt(submission.getUpdatedAt());
        return response;
    }

    private Map<Long, Integer> buildAttemptCountMap(List<WelfareSubmission> list) {
        if (list == null || list.isEmpty()) {
            return new HashMap<Long, Integer>();
        }
        List<Long> ids = list.stream()
                .map(WelfareSubmission::getId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return new HashMap<Long, Integer>();
        }
        Map<Long, Integer> result = new HashMap<Long, Integer>();
        for (Map<String, Object> row : welfareSubmissionMapper.findAttemptCountsByIds(ids)) {
            if (row == null) {
                continue;
            }
            Long submissionId = toLong(row.get("submissionId"));
            Integer submissionAttempt = toInt(row.get("submissionAttempt"));
            if (submissionId != null && submissionAttempt != null && submissionAttempt > 0) {
                result.put(submissionId, submissionAttempt);
            }
        }
        return result;
    }

    private String formatBountyLocationAttempt(String bountyLocation, Integer submissionAttempt) {
        String normalizedLocation = trimToNull(bountyLocation);
        if (normalizedLocation == null) {
            return null;
        }
        if (submissionAttempt == null || submissionAttempt <= 0) {
            return normalizedLocation;
        }
        return normalizedLocation + "（" + submissionAttempt + "）";
    }

    private Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<Long, User> buildUserMap(List<WelfareSubmission> submissions) {
        List<Long> userIds = submissions.stream()
                .map(WelfareSubmission::getUserId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return userMapper.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    private String statusText(String status) {
        if (STATUS_PENDING.equals(status)) {
            return "待审核";
        }
        if (STATUS_APPROVED.equals(status)) {
            return "已奖励";
        }
        if (STATUS_REJECTED.equals(status)) {
            return "未通过";
        }
        return status;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String trimToEmpty(String value) {
        String text = trimToNull(value);
        return text != null ? text : "";
    }

    private String generateSubmissionNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int suffix = 100 + random.nextInt(900);
        return "FL" + time + suffix;
    }
}
