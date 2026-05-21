package com.untitled.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.untitled.dto.BountyTaskCreateRequest;
import com.untitled.dto.BountyTaskResponse;
import com.untitled.dto.BountyTaskStepItem;
import com.untitled.dto.BountyTaskUpdateRequest;
import com.untitled.mapper.BountyClaimMapper;
import com.untitled.mapper.BountyTaskMapper;
import com.untitled.mapper.WelfareSubmissionMapper;
import com.untitled.model.BountyClaim;
import com.untitled.model.BountyClaimStat;
import com.untitled.model.BountyTask;
import com.untitled.model.WelfareSubmission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BountyTaskService {
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";

    public static final String CLAIM_ACCEPTED = "accepted";
    public static final String CLAIM_SUBMITTED = "submitted";
    public static final String CLAIM_APPROVED = "approved";
    public static final String CLAIM_REJECTED = "rejected";

    private final BountyTaskMapper bountyTaskMapper;
    private final BountyClaimMapper bountyClaimMapper;
    private final WelfareSubmissionMapper welfareSubmissionMapper;
    private final ObjectMapper objectMapper;

    public BountyTaskService(BountyTaskMapper bountyTaskMapper,
                             BountyClaimMapper bountyClaimMapper,
                             WelfareSubmissionMapper welfareSubmissionMapper,
                             ObjectMapper objectMapper) {
        this.bountyTaskMapper = bountyTaskMapper;
        this.bountyClaimMapper = bountyClaimMapper;
        this.welfareSubmissionMapper = welfareSubmissionMapper;
        this.objectMapper = objectMapper;
    }

    public List<BountyTaskResponse> listAllForAdmin() {
        return toResponses(bountyTaskMapper.findAll(null), null, true);
    }

    public List<BountyTaskResponse> listActive(Long userId) {
        return toResponses(bountyTaskMapper.findAll(STATUS_ACTIVE), userId, false);
    }

    public List<BountyTaskResponse> listMine(long userId) {
        List<BountyClaim> claims = bountyClaimMapper.findByUserId(userId);
        if (claims == null || claims.isEmpty()) {
            return new ArrayList<BountyTaskResponse>();
        }

        List<Long> taskIds = claims.stream()
                .map(BountyClaim::getBountyTaskId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (taskIds.isEmpty()) {
            return new ArrayList<BountyTaskResponse>();
        }

        Map<Long, BountyTask> taskMap = bountyTaskMapper.findByIds(taskIds).stream()
                .collect(Collectors.toMap(BountyTask::getId, task -> task));

        Map<Long, BountyClaim> claimMap = claims.stream()
                .filter(claim -> claim != null && claim.getBountyTaskId() != null && claim.getBountyTaskId() > 0)
                .collect(Collectors.toMap(BountyClaim::getBountyTaskId, claim -> claim, (left, right) -> left));
        List<WelfareSubmission> submissions = welfareSubmissionMapper.findAllByUserIdAndTaskIds(userId, taskIds);

        List<BountyTaskResponse> responses = new ArrayList<BountyTaskResponse>();
        Set<Long> taskIdsWithSubmission = new HashSet<Long>();
        for (WelfareSubmission submission : submissions) {
            if (submission == null || submission.getBountyTaskId() == null || submission.getBountyTaskId() <= 0) {
                continue;
            }
            Long taskId = submission.getBountyTaskId();
            BountyTask task = taskMap.get(taskId);
            if (task == null) {
                continue;
            }
            BountyClaim claim = claimMap.get(taskId);
            BountyTaskResponse response = toResponse(task, claim, null, false);
            hydrateRecordState(response, claim, submission);
            applySubmissionRecordState(response, submission);
            responses.add(response);
            taskIdsWithSubmission.add(taskId);
        }

        for (BountyClaim claim : claims) {
            if (claim == null || claim.getBountyTaskId() == null || taskIdsWithSubmission.contains(claim.getBountyTaskId())) {
                continue;
            }
            BountyTask task = taskMap.get(claim.getBountyTaskId());
            if (task == null) {
                continue;
            }
            BountyTaskResponse response = toResponse(task, claim, null, false);
            hydrateRecordState(response, claim, null);
            responses.add(response);
        }
        responses.sort(Comparator.comparing(BountyTaskResponse::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BountyTaskResponse::getLatestSubmissionCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BountyTaskResponse::getLatestSubmissionId, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BountyTaskResponse::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        return responses;
    }

    public Optional<BountyTaskResponse> get(long id, boolean admin, Long userId) {
        BountyTask task = bountyTaskMapper.findById(id);
        if (task == null) {
            return Optional.empty();
        }
        if (!admin && !STATUS_ACTIVE.equals(task.getStatus())) {
            return Optional.empty();
        }
        Map<Long, BountyClaim> claimMap = buildClaimMap(userId, Collections.singletonList(task.getId()));
        Map<Long, BountyTaskStats> statsMap = admin
                ? buildStatsMap(Collections.singletonList(task.getId()))
                : Collections.emptyMap();
        return Optional.of(toResponse(task, claimMap.get(task.getId()), statsMap.get(task.getId()), admin));
    }

    @Transactional
    public Optional<BountyTaskResponse> accept(long taskId, long userId) {
        BountyTask task = bountyTaskMapper.findById(taskId);
        if (task == null || !STATUS_ACTIVE.equals(task.getStatus())) {
            return Optional.empty();
        }
        BountyClaim claim = ensureClaim(taskId, userId);
        if (claim.getAcceptedAt() == null) {
            claim.setAcceptedAt(LocalDateTime.now());
        }
        if (!StringUtils.hasText(claim.getStatus())) {
            claim.setStatus(CLAIM_ACCEPTED);
        }
        bountyClaimMapper.update(claim);
        return get(taskId, false, userId);
    }

    @Transactional
    public BountyTaskResponse create(BountyTaskCreateRequest request) {
        BountyTask task = new BountyTask();
        apply(task, request);
        bountyTaskMapper.insert(task);
        return get(task.getId(), true, null).orElse(null);
    }

    @Transactional
    public Optional<BountyTaskResponse> update(long id, BountyTaskUpdateRequest request) {
        BountyTask task = bountyTaskMapper.findById(id);
        if (task == null) {
            return Optional.empty();
        }
        apply(task, request);
        bountyTaskMapper.update(task);
        return get(id, true, null);
    }

    public boolean delete(long id) {
        return bountyTaskMapper.delete(id) > 0;
    }

    @Transactional
    public BountyClaim ensureClaim(long taskId, long userId) {
        BountyClaim claim = bountyClaimMapper.findByUserIdAndTaskId(userId, taskId);
        if (claim != null) {
            return claim;
        }
        LocalDateTime now = LocalDateTime.now();
        BountyClaim created = new BountyClaim();
        created.setBountyTaskId(taskId);
        created.setUserId(userId);
        created.setStatus(CLAIM_ACCEPTED);
        created.setSubmitCount(0);
        created.setAcceptedAt(now);
        bountyClaimMapper.insert(created);
        return bountyClaimMapper.findByUserIdAndTaskId(userId, taskId);
    }

    @Transactional
    public void markSubmitted(long taskId, long userId, long submissionId) {
        BountyClaim claim = ensureClaim(taskId, userId);
        LocalDateTime now = LocalDateTime.now();
        claim.setStatus(CLAIM_SUBMITTED);
        if (claim.getAcceptedAt() == null) {
            claim.setAcceptedAt(now);
        }
        claim.setSubmittedAt(now);
        claim.setLatestSubmissionId(submissionId);
        claim.setReviewedAt(null);
        int currentCount = claim.getSubmitCount() != null ? claim.getSubmitCount() : 0;
        claim.setSubmitCount(currentCount + 1);
        bountyClaimMapper.update(claim);
    }

    @Transactional
    public void markReviewed(long taskId, long userId, String status, Long submissionId) {
        if (taskId <= 0 || userId <= 0) {
            return;
        }
        BountyClaim claim = ensureClaim(taskId, userId);
        LocalDateTime now = LocalDateTime.now();
        if (CLAIM_APPROVED.equals(status)) {
            claim.setStatus(CLAIM_APPROVED);
        } else if (CLAIM_REJECTED.equals(status)) {
            claim.setStatus(CLAIM_REJECTED);
        } else {
            return;
        }
        claim.setReviewedAt(now);
        if (submissionId != null && submissionId > 0) {
            claim.setLatestSubmissionId(submissionId);
        }
        bountyClaimMapper.update(claim);
    }

    private List<BountyTaskResponse> toResponses(List<BountyTask> tasks, Long userId, boolean admin) {
        if (tasks == null || tasks.isEmpty()) {
            return new ArrayList<BountyTaskResponse>();
        }
        List<Long> taskIds = tasks.stream()
                .map(BountyTask::getId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
        Map<Long, BountyClaim> claimMap = buildClaimMap(userId, taskIds);
        Map<Long, BountyTaskStats> statsMap = admin ? buildStatsMap(taskIds) : Collections.emptyMap();
        return tasks.stream()
                .map(task -> toResponse(task, claimMap.get(task.getId()), statsMap.get(task.getId()), admin))
                .collect(Collectors.toList());
    }

    private Map<Long, BountyClaim> buildClaimMap(Long userId, List<Long> taskIds) {
        if (userId == null || userId <= 0 || taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return bountyClaimMapper.findByUserIdAndTaskIds(userId, taskIds).stream()
                .collect(Collectors.toMap(BountyClaim::getBountyTaskId, claim -> claim));
    }

    private Map<Long, BountyTaskStats> buildStatsMap(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, BountyTaskStats> result = new HashMap<Long, BountyTaskStats>();
        for (Long taskId : taskIds) {
            if (taskId != null && taskId > 0) {
                result.put(taskId, new BountyTaskStats());
            }
        }
        for (BountyClaimStat item : bountyClaimMapper.countByTaskIds(taskIds)) {
            if (item == null || item.getBountyTaskId() == null) {
                continue;
            }
            BountyTaskStats stats = result.get(item.getBountyTaskId());
            if (stats == null) {
                stats = new BountyTaskStats();
                result.put(item.getBountyTaskId(), stats);
            }
            int total = item.getTotal() != null ? item.getTotal() : 0;
            stats.totalClaims += total;
            if (CLAIM_SUBMITTED.equals(item.getStatus())) {
                stats.pendingReviewClaims += total;
            }
            if (CLAIM_APPROVED.equals(item.getStatus())) {
                stats.completedClaims += total;
            }
        }
        return result;
    }

    private void apply(BountyTask task, BountyTaskCreateRequest request) {
        validateCommissionRange(request.getCommissionMin(), request.getCommissionMax());
        task.setLocation(trimToNull(request.getLocation()));
        task.setCommissionMin(money(request.getCommissionMin()));
        task.setCommissionMax(money(request.getCommissionMax()));
        task.setCoverImageUrl(trimToNull(request.getCoverImageUrl()));
        task.setDetailImageUrl(trimToNull(request.getDetailImageUrl()));
        applySteps(task, normalizeSteps(
                request.getSteps(),
                request.getStep1Text(), request.getStep1ImageUrl(),
                request.getStep2Text(), request.getStep2ImageUrl(),
                request.getStep3Text(), request.getStep3ImageUrl()
        ));
        task.setStatus(normalizeStatus(request.getStatus()));
        task.setSort(request.getSort() != null ? request.getSort() : 0);
    }

    private void apply(BountyTask task, BountyTaskUpdateRequest request) {
        validateCommissionRange(request.getCommissionMin(), request.getCommissionMax());
        task.setLocation(trimToNull(request.getLocation()));
        task.setCommissionMin(money(request.getCommissionMin()));
        task.setCommissionMax(money(request.getCommissionMax()));
        task.setCoverImageUrl(trimToNull(request.getCoverImageUrl()));
        task.setDetailImageUrl(trimToNull(request.getDetailImageUrl()));
        applySteps(task, normalizeSteps(
                request.getSteps(),
                request.getStep1Text(), request.getStep1ImageUrl(),
                request.getStep2Text(), request.getStep2ImageUrl(),
                request.getStep3Text(), request.getStep3ImageUrl()
        ));
        task.setStatus(normalizeStatus(request.getStatus()));
        task.setSort(request.getSort() != null ? request.getSort() : 0);
    }

    private BountyTaskResponse toResponse(BountyTask task, BountyClaim claim, BountyTaskStats stats, boolean admin) {
        BountyTaskResponse response = new BountyTaskResponse();
        response.setId(task.getId());
        response.setLocation(task.getLocation());
        response.setCommissionMin(money(task.getCommissionMin()));
        response.setCommissionMax(money(task.getCommissionMax()));
        response.setCommissionRangeText(commissionRangeText(task.getCommissionMin(), task.getCommissionMax()));
        response.setCoverImageUrl(task.getCoverImageUrl());
        response.setDetailImageUrl(task.getDetailImageUrl());
        response.setSteps(parseSteps(task));
        response.setStep1Text(task.getStep1Text());
        response.setStep1ImageUrl(task.getStep1ImageUrl());
        response.setStep2Text(task.getStep2Text());
        response.setStep2ImageUrl(task.getStep2ImageUrl());
        response.setStep3Text(task.getStep3Text());
        response.setStep3ImageUrl(task.getStep3ImageUrl());
        response.setStatus(task.getStatus());
        response.setStatusText(statusText(task.getStatus()));
        response.setSort(task.getSort());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        if (claim != null) {
            response.setClaimStatus(claim.getStatus());
            response.setClaimStatusText(admin ? claimStatusText(claim.getStatus(), claim.getSubmitCount()) : "");
            response.setSubmitCount(claim.getSubmitCount());
            response.setActionText(admin ? claimActionText(claim.getStatus()) : "接取任务");
        } else {
            response.setActionText("接取任务");
        }
        if (admin) {
            response.setTotalClaims(stats != null ? stats.totalClaims : 0);
            response.setPendingReviewClaims(stats != null ? stats.pendingReviewClaims : 0);
            response.setCompletedClaims(stats != null ? stats.completedClaims : 0);
        }
        return response;
    }

    private void hydrateRecordState(BountyTaskResponse response, BountyClaim claim, WelfareSubmission submission) {
        if (claim == null) {
            if (submission != null) {
                response.setLatestSubmissionId(submission.getId());
                response.setSubmittedAt(submission.getCreatedAt());
                response.setReviewedAt(submission.getReviewedAt());
                response.setUpdatedAt(submission.getUpdatedAt() != null ? submission.getUpdatedAt() : response.getUpdatedAt());
                response.setLatestSubmissionStatus(submission.getStatus());
                response.setLatestSubmissionStatusText(welfareStatusText(submission.getStatus()));
                response.setLatestSubmissionRewardAmount(money(submission.getRewardAmount()));
                response.setLatestSubmissionReviewNote(trimToNull(submission.getReviewNote()));
                response.setLatestSubmissionScreenshotUrl(trimToNull(submission.getScreenshotUrl()));
                response.setLatestSubmissionCreatedAt(submission.getCreatedAt());
            }
            return;
        }
        response.setClaimStatusText(claimStatusText(claim.getStatus(), claim.getSubmitCount()));
        response.setSubmitCount(claim.getSubmitCount());
        response.setActionText(claimActionText(claim.getStatus()));
        response.setAcceptedAt(claim.getAcceptedAt());
        response.setSubmittedAt(claim.getSubmittedAt());
        response.setReviewedAt(claim.getReviewedAt());
        response.setUpdatedAt(claim.getUpdatedAt() != null ? claim.getUpdatedAt() : response.getUpdatedAt());
        if (submission == null) {
            response.setLatestSubmissionId(claim.getLatestSubmissionId());
            return;
        }
        response.setLatestSubmissionId(submission.getId());
        response.setSubmittedAt(submission.getCreatedAt() != null ? submission.getCreatedAt() : response.getSubmittedAt());
        response.setReviewedAt(submission.getReviewedAt() != null ? submission.getReviewedAt() : response.getReviewedAt());
        response.setUpdatedAt(submission.getUpdatedAt() != null ? submission.getUpdatedAt() : response.getUpdatedAt());
        response.setLatestSubmissionStatus(submission.getStatus());
        response.setLatestSubmissionStatusText(welfareStatusText(submission.getStatus()));
        response.setLatestSubmissionRewardAmount(money(submission.getRewardAmount()));
        response.setLatestSubmissionReviewNote(trimToNull(submission.getReviewNote()));
        response.setLatestSubmissionScreenshotUrl(trimToNull(submission.getScreenshotUrl()));
        response.setLatestSubmissionCreatedAt(submission.getCreatedAt());
    }

    private void applySubmissionRecordState(BountyTaskResponse response, WelfareSubmission submission) {
        if (response == null || submission == null) {
            return;
        }
        String claimStatus = submissionStatusToClaimStatus(submission.getStatus());
        response.setClaimStatus(claimStatus);
        response.setClaimStatusText(welfareStatusText(submission.getStatus()));
        response.setActionText(claimActionText(claimStatus));
    }

    private void applySteps(BountyTask task, List<BountyTaskStepItem> steps) {
        List<BountyTaskStepItem> normalized = steps != null ? steps : new ArrayList<BountyTaskStepItem>();
        task.setStepsJson(writeSteps(normalized));
        task.setStep1Text(stepText(normalized, 0));
        task.setStep1ImageUrl(stepImage(normalized, 0));
        task.setStep2Text(stepText(normalized, 1));
        task.setStep2ImageUrl(stepImage(normalized, 1));
        task.setStep3Text(stepText(normalized, 2));
        task.setStep3ImageUrl(stepImage(normalized, 2));
    }

    private List<BountyTaskStepItem> normalizeSteps(List<BountyTaskStepItem> steps,
                                                    String step1Text, String step1ImageUrl,
                                                    String step2Text, String step2ImageUrl,
                                                    String step3Text, String step3ImageUrl) {
        List<BountyTaskStepItem> result = new ArrayList<BountyTaskStepItem>();
        if (steps != null) {
            for (BountyTaskStepItem item : steps) {
                BountyTaskStepItem normalized = normalizeStep(item);
                if (normalized != null) {
                    result.add(normalized);
                }
            }
        }
        if (!result.isEmpty()) {
            return result;
        }
        addLegacyStep(result, step1Text, step1ImageUrl);
        addLegacyStep(result, step2Text, step2ImageUrl);
        addLegacyStep(result, step3Text, step3ImageUrl);
        return result;
    }

    private void addLegacyStep(List<BountyTaskStepItem> result, String text, String imageUrl) {
        String normalizedText = trimToNull(text);
        String normalizedImageUrl = trimToNull(imageUrl);
        if (normalizedText == null && normalizedImageUrl == null) {
            return;
        }
        BountyTaskStepItem item = new BountyTaskStepItem();
        item.setType(normalizedImageUrl != null && normalizedText == null ? "image" : "text");
        item.setText(normalizedText);
        item.setImageUrl(normalizedImageUrl);
        result.add(item);
    }

    private BountyTaskStepItem normalizeStep(BountyTaskStepItem item) {
        if (item == null) {
            return null;
        }
        String text = trimToNull(item.getText());
        String imageUrl = trimToNull(item.getImageUrl());
        if (text == null && imageUrl == null) {
            return null;
        }
        BountyTaskStepItem normalized = new BountyTaskStepItem();
        String type = trimToNull(item.getType());
        if ("image".equalsIgnoreCase(type) && imageUrl != null) {
            normalized.setType("image");
        } else if ("text".equalsIgnoreCase(type) && text != null) {
            normalized.setType("text");
        } else {
            normalized.setType(imageUrl != null && text == null ? "image" : "text");
        }
        normalized.setText(text);
        normalized.setImageUrl(imageUrl);
        return normalized;
    }

    private List<BountyTaskStepItem> parseSteps(BountyTask task) {
        if (task == null) {
            return new ArrayList<BountyTaskStepItem>();
        }
        String stepsJson = trimToNull(task.getStepsJson());
        if (stepsJson != null) {
            try {
                List<BountyTaskStepItem> items = objectMapper.readValue(stepsJson, new TypeReference<List<BountyTaskStepItem>>() {});
                List<BountyTaskStepItem> result = new ArrayList<BountyTaskStepItem>();
                if (items != null) {
                    for (BountyTaskStepItem item : items) {
                        BountyTaskStepItem normalized = normalizeStep(item);
                        if (normalized != null) {
                            result.add(normalized);
                        }
                    }
                }
                if (!result.isEmpty()) {
                    return result;
                }
            } catch (Exception ignored) {
                // Fallback to legacy fields.
            }
        }
        return normalizeSteps(null,
                task.getStep1Text(), task.getStep1ImageUrl(),
                task.getStep2Text(), task.getStep2ImageUrl(),
                task.getStep3Text(), task.getStep3ImageUrl());
    }

    private String writeSteps(List<BountyTaskStepItem> steps) {
        if (steps == null || steps.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (Exception ex) {
            throw new IllegalArgumentException("任务说明保存失败");
        }
    }

    private String stepText(List<BountyTaskStepItem> steps, int index) {
        if (steps == null || index < 0 || index >= steps.size()) {
            return null;
        }
        return trimToNull(steps.get(index).getText());
    }

    private String stepImage(List<BountyTaskStepItem> steps, int index) {
        if (steps == null || index < 0 || index >= steps.size()) {
            return null;
        }
        return trimToNull(steps.get(index).getImageUrl());
    }

    private void validateCommissionRange(BigDecimal min, BigDecimal max) {
        if (min == null || max == null) {
            return;
        }
        if (money(max).compareTo(money(min)) < 0) {
            throw new IllegalArgumentException("最高佣金不能小于最低佣金");
        }
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

    private String normalizeStatus(String status) {
        return STATUS_INACTIVE.equalsIgnoreCase(trimToNull(status)) ? STATUS_INACTIVE : STATUS_ACTIVE;
    }

    private String statusText(String status) {
        return STATUS_INACTIVE.equals(status) ? "已停用" : "启用中";
    }

    private String commissionRangeText(BigDecimal min, BigDecimal max) {
        return "预计佣金 " + money(min).toPlainString() + "-" + money(max).toPlainString() + " 元";
    }

    private String claimStatusText(String status, Integer submitCount) {
        int count = submitCount != null ? submitCount : 0;
        boolean isResubmit = count > 1;
        if (CLAIM_ACCEPTED.equals(status)) {
            return "已接取";
        }
        if (CLAIM_SUBMITTED.equals(status)) {
            return isResubmit ? "重新审核中" : "审核中";
        }
        if (CLAIM_APPROVED.equals(status)) {
            return "已完成";
        }
        if (CLAIM_REJECTED.equals(status)) {
            return "未通过";
        }
        return "";
    }

    private String claimStatusText(String status) {
        return claimStatusText(status, null);
    }

    private String claimActionText(String status) {
        if (CLAIM_ACCEPTED.equals(status)) {
            return "去提交";
        }
        if (CLAIM_SUBMITTED.equals(status)) {
            return "查看进度";
        }
        if (CLAIM_APPROVED.equals(status)) {
            return "查看结果";
        }
        if (CLAIM_REJECTED.equals(status)) {
            return "重新提交";
        }
        return "接取任务";
    }

    private String welfareStatusText(String status) {
        if (WelfareSubmissionService.STATUS_PENDING.equals(status)) {
            return "审核中";
        }
        if (WelfareSubmissionService.STATUS_APPROVED.equals(status)) {
            return "已完成";
        }
        if (WelfareSubmissionService.STATUS_REJECTED.equals(status)) {
            return "未通过";
        }
        return status;
    }

    private String submissionStatusToClaimStatus(String submissionStatus) {
        if (WelfareSubmissionService.STATUS_PENDING.equals(submissionStatus)) {
            return CLAIM_SUBMITTED;
        }
        if (WelfareSubmissionService.STATUS_APPROVED.equals(submissionStatus)) {
            return CLAIM_APPROVED;
        }
        if (WelfareSubmissionService.STATUS_REJECTED.equals(submissionStatus)) {
            return CLAIM_REJECTED;
        }
        return CLAIM_ACCEPTED;
    }

    private static class BountyTaskStats {
        private int totalClaims;
        private int pendingReviewClaims;
        private int completedClaims;
    }
}
