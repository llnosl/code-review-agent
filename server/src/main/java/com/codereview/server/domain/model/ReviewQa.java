package com.codereview.server.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 记录审查任务中的一次代码问答。
 */
public final class ReviewQa {

    /** 问答标识。 */
    private final UUID id;

    /** 所属任务标识。 */
    private final UUID reviewTaskId;

    /** 当前关注的文件标识。 */
    private final UUID reviewFileId;

    /** 用户问题。 */
    private final String question;

    /** Agent 回答。 */
    private String answer;

    /** 问答状态。 */
    private ReviewQaStatus status;

    /** 失败原因。 */
    private String failureReason;

    /** 创建时间。 */
    private final LocalDateTime createdTime;

    /** 回答完成时间。 */
    private LocalDateTime answeredTime;

    /**
     * 构建并校验代码问答。
     */
    private ReviewQa(
            UUID id,
            UUID reviewTaskId,
            UUID reviewFileId,
            String question,
            String answer,
            ReviewQaStatus status,
            String failureReason,
            LocalDateTime createdTime,
            LocalDateTime answeredTime
    ) {
        this.id = Objects.requireNonNull(id, "问答标识不能为空");
        this.reviewTaskId = Objects.requireNonNull(reviewTaskId, "任务标识不能为空");
        this.reviewFileId = reviewFileId;
        this.question = requireText(question, "用户问题不能为空");
        this.answer = normalizeText(answer);
        this.status = Objects.requireNonNull(status, "问答状态不能为空");
        this.failureReason = normalizeText(failureReason);
        this.createdTime = Objects.requireNonNull(createdTime, "创建时间不能为空");
        this.answeredTime = answeredTime;
        validateState();
    }

    /**
     * 创建等待回答的代码问答。
     */
    public static ReviewQa create(
            UUID reviewTaskId,
            UUID reviewFileId,
            String question,
            LocalDateTime createdTime
    ) {
        return new ReviewQa(
                UUID.randomUUID(),
                reviewTaskId,
                reviewFileId,
                question,
                null,
                ReviewQaStatus.PENDING,
                null,
                createdTime,
                null
        );
    }

    /**
     * 恢复已持久化的代码问答。
     */
    public static ReviewQa restore(
            UUID id,
            UUID reviewTaskId,
            UUID reviewFileId,
            String question,
            String answer,
            ReviewQaStatus status,
            String failureReason,
            LocalDateTime createdTime,
            LocalDateTime answeredTime
    ) {
        return new ReviewQa(
                id,
                reviewTaskId,
                reviewFileId,
                question,
                answer,
                status,
                failureReason,
                createdTime,
                answeredTime
        );
    }

    /**
     * 标记 Agent 开始回答。
     */
    public void startAnswering() {
        requireStatus(ReviewQaStatus.PENDING);
        status = ReviewQaStatus.ANSWERING;
    }

    /**
     * 保存 Agent 回答并完成问答。
     */
    public void complete(String answer, LocalDateTime answeredTime) {
        requireStatus(ReviewQaStatus.ANSWERING);
        String checkedAnswer = requireText(answer, "Agent 回答不能为空");
        LocalDateTime checkedAnsweredTime = Objects.requireNonNull(answeredTime, "回答完成时间不能为空");
        if (checkedAnsweredTime.isBefore(createdTime)) {
            throw new IllegalArgumentException("回答完成时间不能早于创建时间");
        }
        this.answer = checkedAnswer;
        this.status = ReviewQaStatus.COMPLETED;
        this.failureReason = null;
        this.answeredTime = checkedAnsweredTime;
    }

    /**
     * 将未结束的问答标记为失败。
     */
    public void fail(String failureReason) {
        if (status.isFinished()) {
            throw new IllegalStateException("已结束的问答不能再次标记为失败");
        }
        this.status = ReviewQaStatus.FAILED;
        this.answer = null;
        this.failureReason = requireText(failureReason, "失败原因不能为空");
        this.answeredTime = null;
    }

    /**
     * 校验问答状态与数据一致。
     */
    private void validateState() {
        switch (status) {
            case PENDING, ANSWERING -> require(answer == null && failureReason == null && answeredTime == null,
                    "未结束的问答不能包含回答结果");
            case COMPLETED -> require(answer != null && failureReason == null && answeredTime != null,
                    "已完成的问答必须包含回答和完成时间");
            case FAILED -> require(answer == null && failureReason != null && answeredTime == null,
                    "失败的问答必须且只能包含失败原因");
        }
        if (answeredTime != null && answeredTime.isBefore(createdTime)) {
            throw new IllegalArgumentException("回答完成时间不能早于创建时间");
        }
    }

    /**
     * 校验当前问答状态。
     */
    private void requireStatus(ReviewQaStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new IllegalStateException("问答状态必须为：" + expectedStatus);
        }
    }

    /**
     * 校验文本不为空。
     */
    private static String requireText(String value, String message) {
        String normalizedValue = normalizeText(value);
        if (normalizedValue == null) {
            throw new IllegalArgumentException(message);
        }
        return normalizedValue;
    }

    /**
     * 将空文本转换为 null。
     */
    private static String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
     * 获取问答标识。
     */
    public UUID getId() {
        return id;
    }

    /**
     * 获取所属任务标识。
     */
    public UUID getReviewTaskId() {
        return reviewTaskId;
    }

    /**
     * 获取当前关注的文件标识。
     */
    public UUID getReviewFileId() {
        return reviewFileId;
    }

    /**
     * 获取用户问题。
     */
    public String getQuestion() {
        return question;
    }

    /**
     * 获取 Agent 回答。
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * 获取问答状态。
     */
    public ReviewQaStatus getStatus() {
        return status;
    }

    /**
     * 获取失败原因。
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * 获取创建时间。
     */
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    /**
     * 获取回答完成时间。
     */
    public LocalDateTime getAnsweredTime() {
        return answeredTime;
    }
}
