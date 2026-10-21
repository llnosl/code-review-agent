package com.codereview.server.domain.repository;

import com.codereview.server.domain.model.ReviewFile;
import com.codereview.server.domain.model.ReviewTask;
import com.codereview.server.domain.model.ReviewTaskSubmission;
import java.util.List;

/**
 * 原子保存审查任务、文件和提交事件。
 */
public interface ReviewTaskCreationRepository {

    /**
     * 在同一事务中保存创建审查任务所需的全部数据。
     */
    void save(
            ReviewTask reviewTask,
            List<ReviewFile> reviewFiles,
            ReviewTaskSubmission submission);
}
