package com.codereview.server.domain.model;

/**
 * 审查问题的严重程度。
 */
public enum ReviewIssueSeverity {
    /** 提示信息。 */
    INFO,

    /** 低风险问题。 */
    LOW,

    /** 中风险问题。 */
    MEDIUM,

    /** 高风险问题。 */
    HIGH,

    /** 严重风险问题。 */
    CRITICAL
}
