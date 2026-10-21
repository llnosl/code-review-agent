package com.codereview.server.application;

import com.codereview.server.domain.model.ReviewFile;
import com.codereview.server.domain.model.ReviewTask;
import com.codereview.server.domain.model.ReviewTaskSubmission;
import com.codereview.server.domain.repository.ReviewTaskCreationRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 创建审查任务用例。
 */
@Service
public final class CreateReviewTaskService {

    /** 审查任务原子创建存储接口。 */
    private final ReviewTaskCreationRepository creationRepository;

    /** 系统时钟。 */
    private final Clock clock;

    /**
     * 创建审查任务用例。
     */
    public CreateReviewTaskService(
            ReviewTaskCreationRepository creationRepository,
            Clock clock) {
        this.creationRepository = Objects.requireNonNull(
                creationRepository, "审查任务原子创建存储接口不能为空");
        this.clock = Objects.requireNonNull(clock, "系统时钟不能为空");
    }

    /**
     * 创建并原子持久化审查任务及其提交事件。
     */
    public CreateReviewTaskResult create(CreateReviewTaskCommand command) {
        Objects.requireNonNull(command, "创建审查任务命令不能为空");
        LocalDateTime createdTime = LocalDateTime.now(clock);
        ReviewTask reviewTask = ReviewTask.create(command.sourceType(), createdTime);
        List<ReviewFile> reviewFiles = createFiles(reviewTask, command, createdTime);
        ReviewTaskSubmission submission = ReviewTaskSubmission.create(
                reviewTask.getId(), createdTime);

        validateDistinctPaths(reviewFiles);
        creationRepository.save(reviewTask, reviewFiles, submission);

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

}
