package com.codereview.server.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 描述一次代码审查发现的问题。
 */
public final class ReviewIssue {

    /** 问题标识。 */
    private final UUID id;

    /** 所属任务标识。 */
    private final UUID reviewTaskId;

    /** 问题分类。 */
    private final String category;

    /** 严重程度。 */
    private final ReviewIssueSeverity severity;

    /** 问题标题。 */
    private final String title;

    /** 问题说明。 */
    private final String description;

    /** 创建时间。 */
    private final LocalDateTime createdTime;

    /**
     * 构建并校验审查问题。
     */
    private ReviewIssue(
            UUID id,
            UUID reviewTaskId,
            String category,
            ReviewIssueSeverity severity,
            String title,
            String description,
            LocalDateTime createdTime
    ) {
        this.id = Objects.requireNonNull(id, "问题标识不能为空");
        this.reviewTaskId = Objects.requireNonNull(reviewTaskId, "任务标识不能为空");
        this.category = requireText(category, "问题分类不能为空");
        this.severity = Objects.requireNonNull(severity, "严重程度不能为空");
        this.title = requireText(title, "问题标题不能为空");
        this.description = requireText(description, "问题说明不能为空");
        this.createdTime = Objects.requireNonNull(createdTime, "创建时间不能为空");
    }

    /**
     * 创建新的审查问题。
     */
    public static ReviewIssue create(
            UUID reviewTaskId,
            String category,
            ReviewIssueSeverity severity,
            String title,
            String description,
            LocalDateTime createdTime
    ) {
        return new ReviewIssue(
                UUID.randomUUID(),
                reviewTaskId,
                category,
                severity,
                title,
                description,
                createdTime
        );
    }

    /**
     * 恢复已持久化的审查问题。
     */
    public static ReviewIssue restore(
            UUID id,
            UUID reviewTaskId,
            String category,
            ReviewIssueSeverity severity,
            String title,
            String description,
            LocalDateTime createdTime
    ) {
        return new ReviewIssue(
                id,
                reviewTaskId,
                category,
                severity,
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
     * 获取问题标识。
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
     * 获取问题分类。
     */
    public String getCategory() {
        return category;
    }

    /**
     * 获取严重程度。
     */
    public ReviewIssueSeverity getSeverity() {
        return severity;
    }

    /**
     * 获取问题标题。
     */
    public String getTitle() {
        return title;
    }

    /**
     * 获取问题说明。
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
