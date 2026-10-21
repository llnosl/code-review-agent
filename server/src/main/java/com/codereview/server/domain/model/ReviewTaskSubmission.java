package com.codereview.server.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** 审查任务提交事件，负责约束 Outbox 事件的状态流转。 */
public final class ReviewTaskSubmission {

    /** 提交事件标识。 */
    private final UUID id;
    /** 审查任务标识。 */
    private final UUID reviewTaskId;
    /** 当前提交状态。 */
    private ReviewTaskSubmissionStatus status;
    /** 已派发次数。 */
    private int attemptCount;
    /** 下次允许派发时间。 */
    private LocalDateTime nextAttemptTime;
    /** 当前派发租约截止时间。 */
    private LocalDateTime leaseUntil;
    /** 最近一次错误。 */
    private String lastError;
    /** 创建时间。 */
    private final LocalDateTime createdTime;
    /** 更新时间。 */
    private LocalDateTime updatedTime;
    /** 成功提交时间。 */
    private LocalDateTime sentTime;

    /**
     * 从持久化数据恢复提交事件。
     *
     * @param id 提交事件 ID
     * @param reviewTaskId 审查任务 ID
     * @param status 提交状态
     * @param attemptCount 已尝试次数
     * @param nextAttemptTime 下次可尝试时间
     * @param leaseUntil 当前处理租约截止时间
     * @param lastError 最近一次错误
     * @param createdTime 创建时间
     * @param updatedTime 更新时间
     * @param sentTime 成功提交时间
     */
    private ReviewTaskSubmission(UUID id, UUID reviewTaskId,
            ReviewTaskSubmissionStatus status, int attemptCount,
            LocalDateTime nextAttemptTime, LocalDateTime leaseUntil,
            String lastError, LocalDateTime createdTime,
            LocalDateTime updatedTime, LocalDateTime sentTime) {
        this.id = Objects.requireNonNull(id, "提交事件标识不能为空");
        this.reviewTaskId = Objects.requireNonNull(reviewTaskId, "审查任务标识不能为空");
        this.status = Objects.requireNonNull(status, "提交状态不能为空");
        this.attemptCount = attemptCount;
        this.nextAttemptTime = Objects.requireNonNull(nextAttemptTime, "下次派发时间不能为空");
        this.leaseUntil = leaseUntil;
        this.lastError = normalizeError(lastError);
        this.createdTime = Objects.requireNonNull(createdTime, "创建时间不能为空");
        this.updatedTime = Objects.requireNonNull(updatedTime, "更新时间不能为空");
        this.sentTime = sentTime;
        validateState();
    }

    /** 创建待派发的提交事件。 */
    public static ReviewTaskSubmission create(UUID reviewTaskId, LocalDateTime now) {
        return new ReviewTaskSubmission(UUID.randomUUID(), reviewTaskId,
                ReviewTaskSubmissionStatus.PENDING, 0, now, null,
                null, now, now, null);
    }

    /** 从持久化数据恢复提交事件。 */
    public static ReviewTaskSubmission restore(UUID id, UUID reviewTaskId,
            ReviewTaskSubmissionStatus status, int attemptCount,
            LocalDateTime nextAttemptTime, LocalDateTime leaseUntil,
            String lastError, LocalDateTime createdTime,
            LocalDateTime updatedTime, LocalDateTime sentTime) {
        return new ReviewTaskSubmission(id, reviewTaskId, status, attemptCount,
                nextAttemptTime, leaseUntil, lastError, createdTime,
                updatedTime, sentTime);
    }

    /** 获取事件派发租约。 */
    public void claim(LocalDateTime now, LocalDateTime newLeaseUntil) {
        Objects.requireNonNull(now, "派发时间不能为空");
        Objects.requireNonNull(newLeaseUntil, "租约截止时间不能为空");
        if (!newLeaseUntil.isAfter(now)) {
            throw new IllegalArgumentException("租约截止时间必须晚于派发时间");
        }
        boolean pendingDue = status == ReviewTaskSubmissionStatus.PENDING
                && !nextAttemptTime.isAfter(now);
        boolean leaseExpired = status == ReviewTaskSubmissionStatus.PROCESSING
                && leaseUntil != null && !leaseUntil.isAfter(now);
        if (!pendingDue && !leaseExpired) {
            throw new IllegalStateException("当前提交事件不可获取派发租约");
        }
        status = ReviewTaskSubmissionStatus.PROCESSING;
        attemptCount++;
        leaseUntil = newLeaseUntil;
        updatedTime = now;
    }

    /** 标记提交成功。 */
    public void markSent(LocalDateTime now) {
        requireProcessing();
        status = ReviewTaskSubmissionStatus.SENT;
        leaseUntil = null;
        lastError = null;
        sentTime = Objects.requireNonNull(now, "提交成功时间不能为空");
        updatedTime = now;
    }

    /** 安排下一次重试。 */
    public void scheduleRetry(LocalDateTime nextTime, String error, LocalDateTime now) {
        requireProcessing();
        Objects.requireNonNull(now, "更新时间不能为空");
        if (Objects.requireNonNull(nextTime, "下次重试时间不能为空").isBefore(now)) {
            throw new IllegalArgumentException("下次重试时间不能早于当前时间");
        }
        status = ReviewTaskSubmissionStatus.PENDING;
        nextAttemptTime = nextTime;
        leaseUntil = null;
        lastError = requireError(error);
        updatedTime = now;
    }

    /** 标记提交永久失败。 */
    public void markFailed(String error, LocalDateTime now) {
        requireProcessing();
        status = ReviewTaskSubmissionStatus.FAILED;
        leaseUntil = null;
        lastError = requireError(error);
        updatedTime = Objects.requireNonNull(now, "失败时间不能为空");
    }

    /** 判断是否达到最大派发次数。 */
    public boolean hasReachedMaxAttempts(int maxAttempts) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("最大派发次数必须大于零");
        }
        return attemptCount >= maxAttempts;
    }

    /** 校验恢复后的领域状态。 */
    private void validateState() {
        if (attemptCount < 0 || updatedTime.isBefore(createdTime)) {
            throw new IllegalArgumentException("提交事件持久化数据不合法");
        }
        if (status == ReviewTaskSubmissionStatus.PROCESSING
                && (attemptCount == 0 || leaseUntil == null)) {
            throw new IllegalArgumentException("处理中事件必须持有有效租约");
        }
        if (status != ReviewTaskSubmissionStatus.PROCESSING && leaseUntil != null) {
            throw new IllegalArgumentException("非处理中事件不能持有租约");
        }
        if (status == ReviewTaskSubmissionStatus.SENT && sentTime == null) {
            throw new IllegalArgumentException("已提交事件必须记录提交时间");
        }
        if (status != ReviewTaskSubmissionStatus.SENT && sentTime != null) {
            throw new IllegalArgumentException("未提交事件不能记录提交时间");
        }
    }

    /** 校验事件正在派发。 */
    private void requireProcessing() {
        if (status != ReviewTaskSubmissionStatus.PROCESSING) {
            throw new IllegalStateException("只有处理中的提交事件才能更新派发结果");
        }
    }

    /** 校验错误信息。 */
    private static String requireError(String error) {
        String normalized = normalizeError(error);
        if (normalized == null) {
            throw new IllegalArgumentException("错误信息不能为空");
        }
        return normalized;
    }

    /** 规范化错误信息。 */
    private static String normalizeError(String error) {
        return error == null || error.isBlank() ? null : error.trim();
    }

    /** 获取提交事件标识。 */
    public UUID getId() { return id; }
    /** 获取审查任务标识。 */
    public UUID getReviewTaskId() { return reviewTaskId; }
    /** 获取提交状态。 */
    public ReviewTaskSubmissionStatus getStatus() { return status; }
    /** 获取已派发次数。 */
    public int getAttemptCount() { return attemptCount; }
    /** 获取下次允许派发时间。 */
    public LocalDateTime getNextAttemptTime() { return nextAttemptTime; }
    /** 获取租约截止时间。 */
    public LocalDateTime getLeaseUntil() { return leaseUntil; }
    /** 获取最近一次错误。 */
    public String getLastError() { return lastError; }
    /** 获取创建时间。 */
    public LocalDateTime getCreatedTime() { return createdTime; }
    /** 获取更新时间。 */
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    /** 获取成功提交时间。 */
    public LocalDateTime getSentTime() { return sentTime; }
}
