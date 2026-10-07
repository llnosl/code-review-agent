package com.codereview.server.application;

import com.codereview.server.domain.model.ReviewTaskStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 创建审查任务结果。
 *
 * @param reviewTaskId 审查任务标识
 * @param status 审查任务状态
 * @param createdTime 创建时间
 */
public record CreateReviewTaskResult(
        UUID reviewTaskId,
        ReviewTaskStatus status,
        LocalDateTime createdTime) {
}
