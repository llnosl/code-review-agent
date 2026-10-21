package com.codereview.server.domain.model;

/** 审查任务提交事件状态。 */
public enum ReviewTaskSubmissionStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED
}
