package com.codereview.server.domain.model;

/**
 * 代码问答的处理状态。
 */
public enum ReviewQaStatus {
    /** 等待回答。 */
    PENDING,

    /** 正在回答。 */
    ANSWERING,

    /** 回答完成。 */
    COMPLETED,

    /** 回答失败。 */
    FAILED;

    /**
     * 判断问答是否已结束。
     */
    public boolean isFinished() {
        return this == COMPLETED || this == FAILED;
    }
}
