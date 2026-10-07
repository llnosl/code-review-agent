package com.codereview.server.domain.model;

/**
 * 审查任务的生命周期状态。
 */
public enum ReviewTaskStatus {
    /** 等待审查。 */
    PENDING,

    /** 正在审查。 */
    RUNNING,

    /** 审查完成。 */
    COMPLETED,

    /** 审查失败。 */
    FAILED;

    /**
     * 判断任务是否结束。
     */
    public boolean isFinished() {
        return this == COMPLETED || this == FAILED;
    }
}
