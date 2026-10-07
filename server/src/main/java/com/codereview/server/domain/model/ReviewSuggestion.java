package com.codereview.server.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 表示审查问题的一套完整修复方案。
 */
public final class ReviewSuggestion {

    /** 修复方案标识。 */
    private final UUID id;

    /** 所属问题标识。 */
    private final UUID reviewIssueId;

    /** 修复方案标题。 */
    private final String title;

    /** 修复方案说明。 */
    private final String description;

    /** 创建时间。 */
    private final LocalDateTime createdTime;

    /**
     * 构建并校验修复方案。
     */
    private ReviewSuggestion(
            UUID id,
            UUID reviewIssueId,
            String title,
            String description,
            LocalDateTime createdTime
    ) {
        this.id = Objects.requireNonNull(id, "修复方案标识不能为空");
        this.reviewIssueId = Objects.requireNonNull(reviewIssueId, "问题标识不能为空");
        this.title = requireText(title, "修复方案标题不能为空");
        this.description = requireText(description, "修复方案说明不能为空");
        this.createdTime = Objects.requireNonNull(createdTime, "创建时间不能为空");
    }

    /**
     * 创建新的修复方案。
     */
    public static ReviewSuggestion create(
            UUID reviewIssueId,
            String title,
            String description,
            LocalDateTime createdTime
    ) {
        return new ReviewSuggestion(
                UUID.randomUUID(),
                reviewIssueId,
                title,
                description,
                createdTime
        );
    }

    /**
     * 恢复已持久化的修复方案。
     */
    public static ReviewSuggestion restore(
            UUID id,
            UUID reviewIssueId,
            String title,
            String description,
            LocalDateTime createdTime
    ) {
        return new ReviewSuggestion(
                id,
                reviewIssueId,
                title,
                description,
                createdTime
        );
    }

    /**
     * 校验文本不为空。
     */
    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 获取修复方案标识。
     */
    public UUID getId() {
        return id;
    }

    /**
     * 获取所属问题标识。
     */
    public UUID getReviewIssueId() {
        return reviewIssueId;
    }

    /**
     * 获取修复方案标题。
     */
    public String getTitle() {
        return title;
    }

    /**
     * 获取修复方案说明。
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取创建时间。
     */
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
}
