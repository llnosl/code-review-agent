package com.codereview.server.infrastructure.client;

import com.codereview.server.domain.model.ReviewFile;
import com.codereview.server.domain.model.ReviewTask;
import com.codereview.server.client.ReviewAgentClient;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 基于 HTTP 的审查 Agent 客户端。
 */
@Component
public class HttpReviewAgentClient implements ReviewAgentClient {

    /** Agent 接收幂等键的请求头名称。 */
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    /** 用于调用 Agent 服务的 HTTP 客户端。 */
    private final RestClient restClient;

    /** Agent 接收审查任务的接口路径。 */
    private final String reviewPath;

    /**
     * 创建 Agent HTTP 客户端。
     *
     * @param builder HTTP 客户端构建器
     * @param baseUrl Agent 服务地址
     * @param reviewPath 审查提交路径
     */
    public HttpReviewAgentClient(
            RestClient.Builder builder,
            @Value("${agent.base-url}") String baseUrl,
            @Value("${agent.review-path:/api/reviews}") String reviewPath) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.reviewPath = reviewPath;
    }

    /**
     * 向 Agent 提交审查，并以任务编号作为幂等键。
     *
     * @param reviewTask 审查任务
     * @param reviewFiles 待审查文件
     */
    @Override
    public void submitReview(ReviewTask reviewTask, List<ReviewFile> reviewFiles) {
        AgentReviewRequest request = new AgentReviewRequest(
                reviewTask.getId(),
                reviewTask.getSourceType().name(),
                reviewFiles.stream().map(AgentReviewFile::from).toList());

        restClient.post()
                .uri(reviewPath)
                .header(IDEMPOTENCY_KEY_HEADER, reviewTask.getId().toString())
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Agent 审查请求。
     *
     * @param reviewTaskId 审查任务编号
     * @param sourceType 来源类型
     * @param files 待审查文件
     */
    private record AgentReviewRequest(
            UUID reviewTaskId,
            String sourceType,
            List<AgentReviewFile> files) {
    }

    /**
     * Agent 审查文件请求。
     *
     * @param reviewFileId 审查文件编号
     * @param fileName 文件名
     * @param filePath 文件路径
     * @param language 编程语言
     * @param content 文件内容
     */
    private record AgentReviewFile(
            UUID reviewFileId,
            String fileName,
            String filePath,
            String language,
            String content) {

        /**
         * 从领域对象构造 Agent 文件请求。
         *
         * @param reviewFile 审查文件
         * @return Agent 文件请求
         */
        private static AgentReviewFile from(ReviewFile reviewFile) {
            return new AgentReviewFile(
                    reviewFile.getId(),
                    reviewFile.getFileName(),
                    reviewFile.getFilePath(),
                    reviewFile.getLanguage(),
                    reviewFile.getContent());
        }
    }
}
