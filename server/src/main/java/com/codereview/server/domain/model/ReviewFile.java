package com.codereview.server.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 保存审查任务中的原始代码文件。
 */
public final class ReviewFile {

    /** 文件标识。 */
    private final UUID id;

    /** 所属任务标识。 */
    private final UUID reviewTaskId;

    /** 文件名称。 */
    private final String fileName;

    /** 文件相对路径。 */
    private final String filePath;

    /** 编程语言。 */
    private final String language;

    /** 原始代码内容。 */
    private final String content;

    /** 创建时间。 */
    private final LocalDateTime createdTime;

    /**
     * 构建并校验审查文件。
     */
    private ReviewFile(
            UUID id,
            UUID reviewTaskId,
            String fileName,
            String filePath,
            String language,
            String content,
            LocalDateTime createdTime
    ) {
        this.id = Objects.requireNonNull(id, "文件标识不能为空");
        this.reviewTaskId = Objects.requireNonNull(reviewTaskId, "任务标识不能为空");
        this.fileName = requireText(fileName, "文件名称不能为空");
        this.filePath = normalizePath(filePath);
        this.language = requireText(language, "编程语言不能为空");
        this.content = requireText(content, "代码内容不能为空");
        this.createdTime = Objects.requireNonNull(createdTime, "创建时间不能为空");
    }

    /**
     * 创建新的审查文件。
     */
    public static ReviewFile create(
            UUID reviewTaskId,
            String fileName,
            String filePath,
            String language,
            String content,
            LocalDateTime createdTime
    ) {
        return new ReviewFile(
                UUID.randomUUID(),
                reviewTaskId,
                fileName,
                filePath,
                language,
                content,
                createdTime
        );
    }

    /**
     * 恢复已持久化的审查文件。
     */
    public static ReviewFile restore(
            UUID id,
            UUID reviewTaskId,
            String fileName,
            String filePath,
            String language,
            String content,
            LocalDateTime createdTime
    ) {
        return new ReviewFile(
                id,
                reviewTaskId,
                fileName,
                filePath,
                language,
                content,
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
        return value;
    }

    /**
     * 统一文件路径分隔符。
     */
    private static String normalizePath(String filePath) {
        return requireText(filePath, "文件路径不能为空").replace('\\', '/');
    }

    /**
     * 获取文件标识。
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
     * 获取文件名称。
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 获取文件相对路径。
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * 获取编程语言。
     */
    public String getLanguage() {
        return language;
    }

    /**
     * 获取原始代码内容。
     */
    public String getContent() {
        return content;
    }

    /**
     * 获取创建时间。
     */
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
}
