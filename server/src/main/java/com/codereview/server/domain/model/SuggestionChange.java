package com.codereview.server.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 记录修复方案在单个文件中的一处代码修改。
 */
public final class SuggestionChange {

    /** 修改标识。 */
    private final UUID id;

    /** 所属修复方案标识。 */
    private final UUID reviewSuggestionId;

    /** 关联文件标识。 */
    private final UUID reviewFileId;

    /** 原代码起始行号。 */
    private final int startLine;

    /** 原代码结束行号。 */
    private final int endLine;

    /** 修改前的连续代码片段。 */
    private final String originalCode;

    /** 修改后的连续代码片段。 */
    private final String replacementCode;

    /** 供 Agent-RAG 写入和审计使用的 Unified Diff。 */
    private final String diffText;

    /** 同一方案内的修改应用顺序。 */
    private final int sequence;

    /** 创建时间。 */
    private final LocalDateTime createdTime;

    /**
     * 构建并校验代码修改。
     */
    private SuggestionChange(
            UUID id,
            UUID reviewSuggestionId,
            UUID reviewFileId,
            int startLine,
            int endLine,
            String originalCode,
            String replacementCode,
            String diffText,
            int sequence,
            LocalDateTime createdTime
    ) {
        this.id = Objects.requireNonNull(id, "修改标识不能为空");
        this.reviewSuggestionId = Objects.requireNonNull(reviewSuggestionId, "修复方案标识不能为空");
        this.reviewFileId = Objects.requireNonNull(reviewFileId, "文件标识不能为空");
        validateLineRange(startLine, endLine);
        this.startLine = startLine;
        this.endLine = endLine;
        this.originalCode = requireText(originalCode, "原代码不能为空");
        this.replacementCode = Objects.requireNonNull(replacementCode, "替换代码不能为 null");
        this.diffText = requireText(diffText, "Unified Diff 不能为空");
        if (sequence < 0) {
            throw new IllegalArgumentException("修改应用顺序不能小于零");
        }
        this.sequence = sequence;
        this.createdTime = Objects.requireNonNull(createdTime, "创建时间不能为空");
    }

    /**
     * 创建新的代码修改。
     */
    public static SuggestionChange create(
            UUID reviewSuggestionId,
            UUID reviewFileId,
            int startLine,
            int endLine,
            String originalCode,
            String replacementCode,
            String diffText,
            int sequence,
            LocalDateTime createdTime
    ) {
        return new SuggestionChange(
                UUID.randomUUID(),
                reviewSuggestionId,
                reviewFileId,
                startLine,
                endLine,
                originalCode,
                replacementCode,
                diffText,
                sequence,
                createdTime
        );
    }

    /**
     * 恢复已持久化的代码修改。
     */
    public static SuggestionChange restore(
            UUID id,
            UUID reviewSuggestionId,
            UUID reviewFileId,
            int startLine,
            int endLine,
            String originalCode,
            String replacementCode,
            String diffText,
            int sequence,
            LocalDateTime createdTime
    ) {
        return new SuggestionChange(
                id,
                reviewSuggestionId,
                reviewFileId,
                startLine,
                endLine,
                originalCode,
                replacementCode,
                diffText,
                sequence,
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
     * 校验文本不为空。
     */
    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * 获取修改标识。
     */
    public UUID getId() {
        return id;
    }

    /**
     * 获取所属修复方案标识。
     */
    public UUID getReviewSuggestionId() {
        return reviewSuggestionId;
    }

    /**
     * 获取关联文件标识。
     */
    public UUID getReviewFileId() {
        return reviewFileId;
    }

    /**
     * 获取原代码起始行号。
     */
    public int getStartLine() {
        return startLine;
    }

    /**
     * 获取原代码结束行号。
     */
    public int getEndLine() {
        return endLine;
    }

    /**
     * 获取修改前代码。
     */
    public String getOriginalCode() {
        return originalCode;
    }

    /**
     * 获取修改后代码。
     */
    public String getReplacementCode() {
        return replacementCode;
    }

    /**
     * 获取 Unified Diff。
     */
    public String getDiffText() {
        return diffText;
    }

    /**
     * 获取修改应用顺序。
     */
    public int getSequence() {
        return sequence;
    }

    /**
     * 获取创建时间。
     */
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
}
