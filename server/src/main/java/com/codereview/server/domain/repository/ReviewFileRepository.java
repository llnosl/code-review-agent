package com.codereview.server.domain.repository;

import com.codereview.server.domain.model.ReviewFile;
import java.util.List;
import java.util.UUID;

/**
 * 审查文件存储接口。
 */
public interface ReviewFileRepository {

    /**
     * 查询指定审查任务的全部文件。
     */
    List<ReviewFile> findAllByReviewTaskId(UUID reviewTaskId);
}
