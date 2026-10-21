package com.codereview.server.infrastructure.scheduling;

import com.codereview.server.application.CreateReviewFileCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 审查提交事件定时派发器。
 */
@Component
public class ReviewSubmissionScheduler {

    /** 日志记录器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewSubmissionScheduler.class);

    /** 审查提交事件派发服务。 */
    private final CreateReviewFileCommand.DispatchReviewTaskSubmissionsService dispatchService;

    /**
     * 创建审查提交事件定时派发器。
     *
     * @param dispatchService 提交事件派发服务
     */
    public ReviewSubmissionScheduler(CreateReviewFileCommand.DispatchReviewTaskSubmissionsService dispatchService) {
        this.dispatchService = dispatchService;
    }

    /**
     * 周期性领取并派发待处理提交事件。
     */
    @Scheduled(fixedDelayString = "${review.submission-dispatch.interval-millis:1000}")
    public void dispatch() {
        try {
            dispatchService.dispatchBatch();
        } catch (RuntimeException exception) {
            LOGGER.error("审查提交事件定时派发失败", exception);
        }
    }
}
