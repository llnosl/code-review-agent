package com.codereview.server.application;

import com.codereview.server.client.ReviewAgentClient;
import com.codereview.server.domain.model.ReviewFile;
import com.codereview.server.domain.model.ReviewTask;
import com.codereview.server.domain.model.ReviewTaskSubmission;
import com.codereview.server.domain.repository.ReviewFileRepository;
import com.codereview.server.domain.repository.ReviewTaskRepository;
import com.codereview.server.domain.repository.ReviewTaskSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 创建审查任务时提交的文件信息。
 *
 * @param fileName 文件名称
 * @param filePath 文件相对路径
 * @param language 编程语言
 * @param content 原始代码内容
 */
public record CreateReviewFileCommand(
        String fileName,
        String filePath,
        String language,
        String content) {

    /**
     * 校验文件输入。
     */
    public CreateReviewFileCommand {
        fileName = requireText(fileName, "文件名称不能为空");
        filePath = requireText(filePath, "文件路径不能为空");
        language = requireText(language, "编程语言不能为空");
        content = requireText(content, "文件内容不能为空");
    }

    /**
     * 校验文本字段并保留原值。
     */
    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * 审查任务提交事件派发服务。
     */
    @Service
    public static class DispatchReviewTaskSubmissionsService {

        private static final Logger LOGGER =
                LoggerFactory.getLogger(DispatchReviewTaskSubmissionsService.class);

        /** 提交事件仓储。 */
        private final ReviewTaskSubmissionRepository submissionRepository;

        /** 审查任务仓储。 */
        private final ReviewTaskRepository reviewTaskRepository;

        /** 审查文件仓储。 */
        private final ReviewFileRepository reviewFileRepository;

        /** Agent 客户端。 */
        private final ReviewAgentClient reviewAgentClient;

        /** 系统时钟。 */
        private final Clock clock;

        /** 单次领取数量。 */
        private final int batchSize;

        /** 领取租约时长。 */
        private final Duration leaseDuration;

        /** 最大尝试次数。 */
        private final int maxAttempts;

        /** 初始重试间隔。 */
        private final Duration initialRetryDelay;

        /** 最大重试间隔。 */
        private final Duration maxRetryDelay;

        /**
         * 创建提交事件派发服务。
         */
        public DispatchReviewTaskSubmissionsService(
                ReviewTaskSubmissionRepository submissionRepository,
                ReviewTaskRepository reviewTaskRepository,
                ReviewFileRepository reviewFileRepository,
                ReviewAgentClient reviewAgentClient,
                Clock clock,
                @Value("${review.submission-dispatch.batch-size:20}") int batchSize,
                @Value("${review.submission-dispatch.lease-duration:PT1M}") Duration leaseDuration,
                @Value("${review.submission-dispatch.max-attempts:8}") int maxAttempts,
                @Value("${review.submission-dispatch.initial-retry-delay:PT5S}")
                        Duration initialRetryDelay,
                @Value("${review.submission-dispatch.max-retry-delay:PT10M}")
                        Duration maxRetryDelay) {
            if (batchSize <= 0 || maxAttempts <= 0) {
                throw new IllegalArgumentException("派发批次大小和最大尝试次数必须大于零");
            }
            if (leaseDuration.isNegative() || leaseDuration.isZero()
                    || initialRetryDelay.isNegative() || initialRetryDelay.isZero()
                    || maxRetryDelay.compareTo(initialRetryDelay) < 0) {
                throw new IllegalArgumentException("派发时间配置不合法");
            }
            this.submissionRepository = submissionRepository;
            this.reviewTaskRepository = reviewTaskRepository;
            this.reviewFileRepository = reviewFileRepository;
            this.reviewAgentClient = reviewAgentClient;
            this.clock = clock;
            this.batchSize = batchSize;
            this.leaseDuration = leaseDuration;
            this.maxAttempts = maxAttempts;
            this.initialRetryDelay = initialRetryDelay;
            this.maxRetryDelay = maxRetryDelay;
        }

        /**
         * 领取并派发一批到期的提交事件。
         *
         * @return 本次领取的事件数量
         */
        public int dispatchBatch() {
            LocalDateTime now = LocalDateTime.now(clock);
            List<ReviewTaskSubmission> submissions =
                    submissionRepository.claimDue(now, now.plus(leaseDuration), batchSize);
            for (ReviewTaskSubmission submission : submissions) {
                try {
                    dispatchOne(submission);
                } catch (RuntimeException exception) {
                    LOGGER.error(
                            "审查任务提交事件处理异常，将在租约到期后重新领取，提交事件ID：{}",
                            submission.getId(),
                            exception);
                }
            }
            return submissions.size();
        }

        /**
         * 派发单个提交事件。
         */
        private void dispatchOne(ReviewTaskSubmission submission) {
            UUID reviewTaskId = submission.getReviewTaskId();
            ReviewTask reviewTask = reviewTaskRepository.findById(reviewTaskId)
                    .orElseThrow(() -> new IllegalStateException(
                            "提交事件对应的审查任务不存在，任务ID：" + reviewTaskId));
            List<ReviewFile> reviewFiles =
                    reviewFileRepository.findAllByReviewTaskId(reviewTaskId);

            try {
                if (reviewFiles.isEmpty()) {
                    throw new IllegalStateException("审查任务没有可提交的文件");
                }
                reviewAgentClient.submitReview(reviewTask, reviewFiles);
            } catch (RuntimeException exception) {
                handleDispatchFailure(submission, reviewTask, exception);
                return;
            }

            LocalDateTime sentTime = LocalDateTime.now(clock);
            reviewTask.start(sentTime);
            submission.markSent(sentTime);
            submissionRepository.saveSent(submission, reviewTask);
        }

        /**
         * 保存派发失败结果或安排下一次重试。
         */
        private void handleDispatchFailure(
                ReviewTaskSubmission submission,
                ReviewTask reviewTask,
                RuntimeException exception) {
            LocalDateTime now = LocalDateTime.now(clock);
            String error = normalizeError(exception);
            if (submission.hasReachedMaxAttempts(maxAttempts)) {
                reviewTask.fail(error, now);
                submission.markFailed(error, now);
                submissionRepository.saveFailed(submission, reviewTask);
                return;
            }
            submission.scheduleRetry(now.plus(calculateRetryDelay(submission.getAttemptCount())),
                    error, now);
            submissionRepository.saveRetry(submission);
        }

        /**
         * 按尝试次数计算有上限的指数退避时间。
         */
        private Duration calculateRetryDelay(int attemptCount) {
            Duration delay = initialRetryDelay;
            for (int attempt = 1;
                    attempt < attemptCount && delay.compareTo(maxRetryDelay) < 0;
                    attempt++) {
                if (delay.compareTo(maxRetryDelay.dividedBy(2)) > 0) {
                    return maxRetryDelay;
                }
                delay = delay.multipliedBy(2);
            }
            return delay.compareTo(maxRetryDelay) > 0 ? maxRetryDelay : delay;
        }

        /**
         * 生成可持久化的简洁错误信息。
         */
        private String normalizeError(RuntimeException exception) {
            String message = exception.getMessage();
            String error = message == null || message.isBlank()
                    ? exception.getClass().getSimpleName()
                    : message;
            return error.length() <= 2000 ? error : error.substring(0, 2000);
        }
    }
}
