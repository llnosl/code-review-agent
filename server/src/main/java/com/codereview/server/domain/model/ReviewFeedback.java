package com.codereview.server.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 记录用户对修复方案的反馈。
 */
public final class ReviewFeedback {

    /** 反馈标识。 */
    private final UUID id;

    /** 关联修复方案标识。 */
    private final UUID reviewSuggestionId;

    /** 反馈类型。 */
    private final ReviewFeedbackType type;

    /** 创建时间。 */
    private final LocalDateTime createdTime;

    /**
     * 构建并校验方案反馈。
     */
    private ReviewFeedback(
            UUID id,
            UUID reviewSuggestionId,
            ReviewFeedbackType type,
            LocalDateTime createdTime
    ) {
        this.id = Objects.requireNonNull(id, "反馈标识不能为空");
        this.reviewSuggestionId = Objects.requireNonNull(reviewSuggestionId, "修复方案标识不能为空");
        this.type = Objects.requireNonNull(type, "反馈类型不能为空");
        this.createdTime = Objects.requireNonNull(createdTime, "创建时间不能为空");
    }

    /**
     * 创建新的方案反馈。
     */
    public static ReviewFeedback create(
            UUID reviewSuggestionId,
            ReviewFeedbackType type,
            LocalDateTime createdTime
    ) {
        return new ReviewFeedback(
                UUID.randomUUID(),
                reviewSuggestionId,
                type,
                createdTime
        );
    }

    /**
     * 恢复已持久化的方案反馈。
     */
    public static ReviewFeedback restore(
            UUID id,
            UUID reviewSuggestionId,
            ReviewFeedbackType type,
            LocalDateTime createdTime
    ) {
        return new ReviewFeedback(
                id,
                reviewSuggestionId,
                type,
                createdTime
        );
    }

    /**
     * 判断反馈是否需要写入 Agent-RAG。
     */
    public boolean shouldWriteToAgentRag() {
        return type.shouldWriteToAgentRag();
    }

    /**
     * 获取反馈标识。
     */
    public UUID getId() {
        return id;
    }

    /**
     * 获取关联修复方案标识。
     */
    public UUID getReviewSuggestionId() {
        return reviewSuggestionId;
    }

    /**
     * 获取反馈类型。
     */
    public ReviewFeedbackType getType() {
        return type;
    }

    /**
     * 获取创建时间。
     */
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
}
