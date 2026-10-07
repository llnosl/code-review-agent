package com.codereview.server.domain.repository;

import com.codereview.server.domain.model.ReviewTask;

/**
 * 审查任务存储接口。
 */
public interface ReviewTaskRepository {

    /**
     * 保存审查任务。
     */
    void save(ReviewTask reviewTask);
}
