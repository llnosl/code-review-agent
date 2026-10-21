package com.codereview.server.domain.repository;

import com.codereview.server.domain.model.ReviewTask;
import com.codereview.server.domain.model.ReviewTaskSubmission;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审查任务提交事件存储接口。
 */
public interface ReviewTaskSubmissionRepository {

    /**
     * 抢占一批到期的提交事件。
     */
    List<ReviewTaskSubmission> claimDue(
            LocalDateTime now,
            LocalDateTime leaseUntil,
            int batchSize);

    /**
     * 保存等待重试的提交事件。
     */
    void saveRetry(ReviewTaskSubmission submission);

    /**
     * 原子保存提交成功事件和已启动任务。
     */
    void saveSent(ReviewTaskSubmission submission, ReviewTask reviewTask);

    /**
     * 原子保存最终失败事件和失败任务。
     */
    void saveFailed(ReviewTaskSubmission submission, ReviewTask reviewTask);
}
