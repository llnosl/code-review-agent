package com.codereview.server.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 管理单次代码审查生命周期的聚合根。
 */
public final class ReviewTask {

    /** 任务标识。 */
    private final UUID id;

    /** 代码来源。 */
    private final ReviewSourceType sourceType;

    /** 任务状态。 */
    private ReviewTaskStatus status;

    /** 创建时间。 */
    private final LocalDateTime createdTime;

    /** 开始时间。 */
    private LocalDateTime startedTime;

    /** 完成时间。 */
    private LocalDateTime completedTime;

    /** 失败原因。 */
    private String failureReason;

    /**
     * 构建并校验审查任务。
     */
    private ReviewTask(
            UUID id,
            ReviewSourceType sourceType,
            ReviewTaskStatus status,
            LocalDateTime createdTime,
            LocalDateTime startedTime,
            LocalDateTime completedTime,
            String failureReason
    ) {
        this.id = Objects.requireNonNull(id, "任务标识不能为空");
        this.sourceType = Objects.requireNonNull(sourceType, "任务来源类型不能为空");
        this.status = Objects.requireNonNull(status, "任务状态不能为空");
        this.createdTime = Objects.requireNonNull(createdTime, "创建时间不能为空");
        this.startedTime = startedTime;
        this.completedTime = completedTime;
        this.failureReason = normalizeFailureReason(failureReason);
        validateState();
    }

    /**
     * 创建待处理的审查任务。
     */
    public static ReviewTask create(ReviewSourceType sourceType, LocalDateTime createdTime) {
        return new ReviewTask(
                UUID.randomUUID(),
                sourceType,
                ReviewTaskStatus.PENDING,
                createdTime,
                null,
                null,
                null
        );
    }

    /**
     * 恢复已持久化的审查任务。
     */
    public static ReviewTask restore(
            UUID id,
            ReviewSourceType sourceType,
            ReviewTaskStatus status,
            LocalDateTime createdTime,
            LocalDateTime startedTime,
            LocalDateTime completedTime,
            String failureReason
    ) {
        return new ReviewTask(
                id,
                sourceType,
                status,
                createdTime,
                startedTime,
                completedTime,
                failureReason
        );
    }

    /**
     * 启动待处理的审查任务。
     */
    public void start(LocalDateTime startTime) {
        requireStatus(ReviewTaskStatus.PENDING);
        LocalDateTime checkedStartTime = Objects.requireNonNull(startTime, "开始时间不能为空");
        requireNotBefore(checkedStartTime, createdTime, "开始时间不能早于创建时间");
        status = ReviewTaskStatus.RUNNING;
        startedTime = checkedStartTime;
    }

    /**
     * 完成执行中的审查任务。
     */
    public void complete(LocalDateTime completeTime) {
        requireStatus(ReviewTaskStatus.RUNNING);
        LocalDateTime checkedCompleteTime = Objects.requireNonNull(
                completeTime,
                "完成时间不能为空"
        );
        requireNotBefore(checkedCompleteTime, startedTime, "完成时间不能早于开始时间");
        status = ReviewTaskStatus.COMPLETED;
        completedTime = checkedCompleteTime;
        failureReason = null;
    }

    /**
     * 将未结束的审查任务标记为失败。
     */
    public void fail(String reason, LocalDateTime failTime) {
        if (status.isFinished()) {
            throw new IllegalStateException("已结束的审查任务不能再次标记为失败");
        }
        String checkedReason = normalizeFailureReason(reason);
        if (checkedReason == null) {
            throw new IllegalArgumentException("失败原因不能为空");
        }
        LocalDateTime checkedFailTime = Objects.requireNonNull(failTime, "失败时间不能为空");
        LocalDateTime earliestTime = startedTime == null ? createdTime : startedTime;
        requireNotBefore(checkedFailTime, earliestTime, "失败时间不能早于任务生命周期时间");
        status = ReviewTaskStatus.FAILED;
        completedTime = checkedFailTime;
        failureReason = checkedReason;
    }

    /**
     * 校验生命周期字段的一致性。
     */
    private void validateState() {
        if (startedTime != null) {
            requireNotBefore(startedTime, createdTime, "开始时间不能早于创建时间");
        }
        if (completedTime != null) {
            LocalDateTime earliestTime = startedTime == null ? createdTime : startedTime;
            requireNotBefore(completedTime, earliestTime, "完成时间不能早于任务生命周期时间");
        }
        switch (status) {
            case PENDING -> require(startedTime == null && completedTime == null && failureReason == null,
                    "待处理任务不能包含执行时间或失败原因");
            case RUNNING -> require(startedTime != null && completedTime == null && failureReason == null,
                    "执行中任务必须且只能包含开始时间");
            case COMPLETED -> require(startedTime != null && completedTime != null && failureReason == null,
                    "已完成任务的生命周期数据无效");
            case FAILED -> require(completedTime != null && failureReason != null,
                    "失败任务必须包含完成时间和失败原因");
        }
    }

    /**
     * 校验当前任务状态。
     */
    private void requireStatus(ReviewTaskStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new IllegalStateException("审查任务状态必须为：" + expectedStatus);
        }
    }

    /**
     * 校验时间不早于基准时间。
     */
    private static void requireNotBefore(LocalDateTime value, LocalDateTime reference, String message) {
        if (value.isBefore(reference)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 校验业务条件。
     */
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 将空失败原因转换为 null。
     */
    private static String normalizeFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return null;
        }
        return failureReason.trim();
    }

    /**
     * 获取任务标识。
     */
    public UUID getId() {
        return id;
    }

    /**
     * 获取任务来源类型。
     */
    public ReviewSourceType getSourceType() {
        return sourceType;
    }

    /**
     * 获取当前任务状态。
     */
    public ReviewTaskStatus getStatus() {
        return status;
    }

    /**
     * 获取创建时间。
     */
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    /**
     * 获取开始时间。
     */
    public LocalDateTime getStartedTime() {
        return startedTime;
    }

    /**
     * 获取完成时间。
     */
    public LocalDateTime getCompletedTime() {
        return completedTime;
    }

    /**
     * 获取失败原因。
     */
    public String getFailureReason() {
        return failureReason;
    }
}
