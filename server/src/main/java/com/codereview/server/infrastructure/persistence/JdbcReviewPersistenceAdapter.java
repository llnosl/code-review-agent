package com.codereview.server.infrastructure.persistence;

import com.codereview.server.domain.model.ReviewFile;
import com.codereview.server.domain.model.ReviewSourceType;
import com.codereview.server.domain.model.ReviewTask;
import com.codereview.server.domain.model.ReviewTaskStatus;
import com.codereview.server.domain.model.ReviewTaskSubmission;
import com.codereview.server.domain.repository.ReviewFileRepository;
import com.codereview.server.domain.repository.ReviewTaskCreationRepository;
import com.codereview.server.domain.repository.ReviewTaskRepository;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 使用 JDBC 持久化审查任务及其文件。
 */
@Repository
public class JdbcReviewPersistenceAdapter implements
        ReviewTaskCreationRepository, ReviewTaskRepository, ReviewFileRepository {

    /** 数据库访问组件。 */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建审查任务持久化适配器。
     */
    public JdbcReviewPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 在同一数据库事务中保存任务、文件和提交事件。
     */
    @Override
    @Transactional
    public void save(
            ReviewTask reviewTask,
            List<ReviewFile> reviewFiles,
            ReviewTaskSubmission submission
    ) {
        insertTask(reviewTask);
        insertFiles(reviewFiles);
        insertSubmission(submission);
    }

    /**
     * 按标识查询审查任务。
     */
    @Override
    public Optional<ReviewTask> findById(UUID reviewTaskId) {
        List<ReviewTask> tasks = jdbcTemplate.query("""
                SELECT id, source_type, status, created_time, started_time,
                       completed_time, failure_reason
                FROM review_tasks
                WHERE id = ?
                """, (resultSet, rowNumber) -> ReviewTask.restore(
                resultSet.getObject("id", UUID.class),
                ReviewSourceType.valueOf(resultSet.getString("source_type")),
                ReviewTaskStatus.valueOf(resultSet.getString("status")),
                resultSet.getObject("created_time", java.time.LocalDateTime.class),
                resultSet.getObject("started_time", java.time.LocalDateTime.class),
                resultSet.getObject("completed_time", java.time.LocalDateTime.class),
                resultSet.getString("failure_reason")
        ), reviewTaskId);
        return tasks.stream().findFirst();
    }

    /**
     * 查询指定任务的全部文件。
     */
    @Override
    public List<ReviewFile> findAllByReviewTaskId(UUID reviewTaskId) {
        return jdbcTemplate.query("""
                SELECT id, review_task_id, file_name, file_path, language,
                       content, created_time
                FROM review_files
                WHERE review_task_id = ?
                ORDER BY created_time, id
                """, (resultSet, rowNumber) -> ReviewFile.restore(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("review_task_id", UUID.class),
                resultSet.getString("file_name"),
                resultSet.getString("file_path"),
                resultSet.getString("language"),
                resultSet.getString("content"),
                resultSet.getObject("created_time", java.time.LocalDateTime.class)
        ), reviewTaskId);
    }

    /**
     * 插入审查任务。
     */
    private void insertTask(ReviewTask task) {
        jdbcTemplate.update("""
                INSERT INTO review_tasks (
                    id, source_type, status, created_time, started_time,
                    completed_time, failure_reason
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                task.getId(), task.getSourceType().name(), task.getStatus().name(),
                task.getCreatedTime(), task.getStartedTime(), task.getCompletedTime(),
                task.getFailureReason());
    }

    /**
     * 批量插入审查文件。
     */
    private void insertFiles(List<ReviewFile> reviewFiles) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO review_files (
                    id, review_task_id, file_name, file_path, language,
                    content, created_time
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, reviewFiles, reviewFiles.size(), this::setFileParameters);
    }

    /**
     * 设置审查文件插入参数。
     */
    private void setFileParameters(PreparedStatement statement, ReviewFile file)
            throws java.sql.SQLException {
        statement.setObject(1, file.getId());
        statement.setObject(2, file.getReviewTaskId());
        statement.setString(3, file.getFileName());
        statement.setString(4, file.getFilePath());
        statement.setString(5, file.getLanguage());
        statement.setString(6, file.getContent());
        statement.setTimestamp(7, Timestamp.valueOf(file.getCreatedTime()));
    }

    /**
     * 插入待派发的提交事件。
     */
    private void insertSubmission(ReviewTaskSubmission submission) {
        jdbcTemplate.update("""
                INSERT INTO review_task_submissions (
                    id, review_task_id, status, attempt_count, next_attempt_time,
                    lease_until, last_error, created_time, updated_time, sent_time
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                submission.getId(), submission.getReviewTaskId(),
                submission.getStatus().name(), submission.getAttemptCount(),
                submission.getNextAttemptTime(), submission.getLeaseUntil(),
                submission.getLastError(), submission.getCreatedTime(),
                submission.getUpdatedTime(), submission.getSentTime());
    }
}
