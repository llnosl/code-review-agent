package com.codereview.server.domain.repository;

import com.codereview.server.domain.model.ReviewTask;
import java.util.Optional;
import java.util.UUID;

/**
 * 审查任务存储接口。
 */
public interface ReviewTaskRepository {

    /**
     * 按标识查询审查任务。
     */
    Optional<ReviewTask> findById(UUID reviewTaskId);
}
