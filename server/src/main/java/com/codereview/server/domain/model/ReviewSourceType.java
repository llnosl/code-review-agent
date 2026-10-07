package com.codereview.server.domain.model;

/**
 * 审查任务的代码来源。
 */
public enum ReviewSourceType {
    /** 复制输入的代码。 */
    CODE_TEXT,

    /** 上传的代码文件。 */
    FILE_UPLOAD
}
