package com.codereview.server.domain.model;

/**
 * 用户对修复方案的反馈类型。
 */
public enum ReviewFeedbackType {
    /** 认可并采用方案。 */
    LIKE,

    /** 不采用方案。 */
    NOT_ADOPTED,

    /** 认为方案存在问题。 */
    DISLIKE;

    /**
     * 判断反馈是否需要写入 Agent-RAG。
     */
    public boolean shouldWriteToAgentRag() {
        return this == LIKE || this == DISLIKE;
    }
}
