package com.codereview.server.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 记录审查问题涉及的文件和代码行范围。
 */
public final class ReviewIssueLocation {

    /** 位置标识。 */
    private final UUID id;

    /** 所属问题标识。 */
    private final UUID reviewIssueId;

    /** 关联文件标识。 */
    private final UUID reviewFileId;

    /** 起始行号。 */
    private final int startLine;

    /** 结束行号。 */
    private final int endLine;

    /** 位置作用类型。 */
    private final ReviewIssueLocationRole role;

    /** 创建时间。 */
    private final LocalDateTime createdTime;

    /**
     * 构建并校验问题位置。
     */
    private ReviewIssueLocation(
            UUID id,
            UUID reviewIssueId,
            UUID reviewFileId,
            int startLine,
            int endLine,
            ReviewIssueLocationRole role,
            LocalDateTime createdTime
    ) {
        this.id = Objects.requireNonNull(id, "位置标识不能为空");
        this.reviewIssueId = Objects.requireNonNull(reviewIssueId, "问题标识不能为空");
        this.reviewFileId = Objects.requireNonNull(reviewFileId, "文件标识不能为空");
        validateLineRange(startLine, endLine);
        this.startLine = startLine;
        this.endLine = endLine;
        this.role = Objects.requireNonNull(role, "位置作用类型不能为空");
        this.createdTime = Objects.requireNonNull(createdTime, "创建时间不能为空");
    }

    /**
     * 创建新的问题位置。
     */
    public static ReviewIssueLocation create(
            UUID reviewIssueId,
            UUID reviewFileId,
            int startLine,
            int endLine,
            ReviewIssueLocationRole role,
            LocalDateTime createdTime
    ) {
        return new ReviewIssueLocation(
                UUID.randomUUID(),
                reviewIssueId,
                reviewFileId,
                startLine,
                endLine,
                role,
                createdTime
        );
    }

    /**
     * 恢复已持久化的问题位置。
     */
    public static ReviewIssueLocation restore(
            UUID id,
            UUID reviewIssueId,
            UUID reviewFileId,
            int startLine,
            int endLine,
            ReviewIssueLocationRole role,
            LocalDateTime createdTime
    ) {
        return new ReviewIssueLocation(
                id,
                reviewIssueId,
                reviewFileId,
                startLine,
                endLine,
                role,
                createdTime
        );
    }

    /**
     * 校验代码行范围。
     */
    private static void validateLineRange(int startLine, int endLine) {
        if (startLine < 1) {
            throw new IllegalArgumentException("起始行号必须大于零");
        }
        if (endLine < startLine) {
            throw new IllegalArgumentException("结束行号不能小于起始行号");
        }
    }

    /**
     * 获取位置标识。
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
     * 获取关联文件标识。
     */
    public UUID getReviewFileId() {
        return reviewFileId;
    }

    /**
     * 获取起始行号。
     */
    public int getStartLine() {
        return startLine;
    }

    /**
     * 获取结束行号。
     */
    public int getEndLine() {
        return endLine;
    }

    /**
     * 获取位置作用类型。
     */
    public ReviewIssueLocationRole getRole() {
        return role;
    }

    /**
     * 获取创建时间。
     */
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
}
