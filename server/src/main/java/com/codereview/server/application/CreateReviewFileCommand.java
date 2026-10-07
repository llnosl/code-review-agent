package com.codereview.server.application;

/**
 * 创建审查任务时提交的文件信息。
 *
 * @param fileName 文件名称
 * @param filePath 文件相对路径
 * @param language 编程语言
 * @param content 原始代码内容
 */
public record CreateReviewFileCommand(
        String fileName,
        String filePath,
        String language,
        String content) {

    /**
     * 校验文件输入。
     */
    public CreateReviewFileCommand {
        fileName = requireText(fileName, "文件名称不能为空");
        filePath = requireText(filePath, "文件路径不能为空");
        language = requireText(language, "编程语言不能为空");
        content = requireText(content, "文件内容不能为空");
    }

    /**
     * 校验文本字段并保留原值。
     */
    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
