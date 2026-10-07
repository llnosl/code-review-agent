package com.codereview.server.application;

import com.codereview.server.client.ReviewAgentClient;
import com.codereview.server.domain.model.ReviewFile;
import com.codereview.server.domain.model.ReviewTask;
import com.codereview.server.domain.repository.ReviewFileRepository;
import com.codereview.server.domain.repository.ReviewTaskRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 创建审查任务用例。
 */
public final class CreateReviewTaskService {

    /** 审查任务存储接口。 */
    private final ReviewTaskRepository reviewTaskRepository;

    /** 审查文件存储接口。 */
    private final ReviewFileRepository reviewFileRepository;

    /** Agent-RAG 审查客户端。 */
    private final ReviewAgentClient reviewAgentClient;

    /** 系统时钟。 */
    private final Clock clock;

    /**
     * 创建审查任务用例。
     */
    public CreateReviewTaskService(
            ReviewTaskRepository reviewTaskRepository,
            ReviewFileRepository reviewFileRepository,
            ReviewAgentClient reviewAgentClient,
            Clock clock) {
        this.reviewTaskRepository = Objects.requireNonNull(
                reviewTaskRepository, "审查任务存储接口不能为空");
        this.reviewFileRepository = Objects.requireNonNull(
                reviewFileRepository, "审查文件存储接口不能为空");
        this.reviewAgentClient = Objects.requireNonNull(
                reviewAgentClient, "Agent-RAG 审查客户端不能为空");
        this.clock = Objects.requireNonNull(clock, "系统时钟不能为空");
    }

    /**
     * 创建、持久化并提交审查任务。
     */
    public CreateReviewTaskResult create(CreateReviewTaskCommand command) {
        Objects.requireNonNull(command, "创建审查任务命令不能为空");
        LocalDateTime createdTime = LocalDateTime.now(clock);
        ReviewTask reviewTask = ReviewTask.create(command.sourceType(), createdTime);
        List<ReviewFile> reviewFiles = createFiles(reviewTask, command, createdTime);

        validateDistinctPaths(reviewFiles);
        reviewTaskRepository.save(reviewTask);
        reviewFileRepository.saveAll(reviewFiles);

        reviewTask.start(LocalDateTime.now(clock));
        reviewTaskRepository.save(reviewTask);
        submitToAgent(reviewTask, reviewFiles);

        return new CreateReviewTaskResult(
                reviewTask.getId(), reviewTask.getStatus(), reviewTask.getCreatedTime());
    }

    /**
     * 根据命令创建审查文件。
     */
    private List<ReviewFile> createFiles(
            ReviewTask reviewTask,
            CreateReviewTaskCommand command,
            LocalDateTime createdTime) {
        return command.files().stream()
                .map(file -> ReviewFile.create(
                        reviewTask.getId(),
                        file.fileName(),
                        file.filePath(),
                        file.language(),
                        file.content(),
                        createdTime))
                .toList();
    }

    /**
     * 校验任务内的文件路径不重复。
     */
    private void validateDistinctPaths(List<ReviewFile> reviewFiles) {
        Set<String> filePaths = new HashSet<>();
        boolean duplicated = reviewFiles.stream()
                .map(ReviewFile::getFilePath)
                .anyMatch(filePath -> !filePaths.add(filePath));
        if (duplicated) {
            throw new IllegalArgumentException("同一审查任务中不能包含重复的文件路径");
        }
    }

    /**
     * 提交任务，并在提交失败时保存失败状态。
     */
    private void submitToAgent(ReviewTask reviewTask, List<ReviewFile> reviewFiles) {
        try {
            reviewAgentClient.submitReview(reviewTask, List.copyOf(reviewFiles));
        } catch (RuntimeException exception) {
            reviewTask.fail("Agent-RAG 审查任务提交失败", LocalDateTime.now(clock));
            reviewTaskRepository.save(reviewTask);
            throw new IllegalStateException("Agent-RAG 审查任务提交失败", exception);
        }
    }
}
