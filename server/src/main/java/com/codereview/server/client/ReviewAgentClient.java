package com.codereview.server.client;

import com.codereview.server.domain.model.ReviewFile;
import com.codereview.server.domain.model.ReviewTask;
import java.util.List;

/**
 * Agent-RAG 审查客户端。
 */
public interface ReviewAgentClient {

    /**
     * 提交已经持久化的审查任务。
     */
    void submitReview(ReviewTask reviewTask, List<ReviewFile> reviewFiles);
}
