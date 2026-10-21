package com.codereview.server.infrastructure.persistence;

import com.codereview.server.domain.model.ReviewTask;
import com.codereview.server.domain.model.ReviewTaskSubmission;
import com.codereview.server.domain.model.ReviewTaskSubmissionStatus;
import com.codereview.server.domain.repository.ReviewTaskSubmissionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 基于 JDBC 的审查任务提交事件仓储。
 */
@Repository
public class JdbcReviewTaskSubmissionRepository implements ReviewTaskSubmissionRepository {

    /** 数据库访问模板。 */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建提交事件仓储。
     *
     * @param jdbcTemplate 数据库访问模板
     */
    public JdbcReviewTaskSubmissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 加锁领取到期事件，并立即写入租约。
     *
     * @param now 当前时间
     * @param leaseUntil 租约截止时间
     * @param batchSize 单次领取数量
     * @return 已领取事件
     */
    @Override
    @Transactional
    public List<ReviewTaskSubmission> claimDue(
            LocalDateTime now, LocalDateTime leaseUntil, int batchSize) {
        List<ReviewTaskSubmission> submissions = jdbcTemplate.query(
                """
                SELECT id, review_task_id, status, attempt_count, next_attempt_time,
                       lease_until, last_error, created_time, updated_time, sent_time
                FROM review_task_submissions
                WHERE (status = 'PENDING' AND next_attempt_time <= ?)
                   OR (status = 'PROCESSING' AND lease_until <= ?)
                ORDER BY next_attempt_time, created_time
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                """,
                this::mapSubmission,
                Timestamp.valueOf(now), Timestamp.valueOf(now), batchSize);

        for (ReviewTaskSubmission submission : submissions) {
            submission.claim(now, leaseUntil);
            jdbcTemplate.update(
                    """
                    UPDATE review_task_submissions
                    SET status = ?, attempt_count = ?, lease_until = ?, updated_time = ?
                    WHERE id = ?
                    """,
                    submission.getStatus().name(),
                    submission.getAttemptCount(),
                    Timestamp.valueOf(submission.getLeaseUntil()),
                    Timestamp.valueOf(submission.getUpdatedTime()),
                    submission.getId());
        }
        return submissions;
    }

    /**
     * 保存事件的下次重试计划。
     *
     * @param submission 提交事件
     */
    @Override
    @Transactional
    public void saveRetry(ReviewTaskSubmission submission) {
        int changed = jdbcTemplate.update(
                """
                UPDATE review_task_submissions
                SET status = ?, next_attempt_time = ?, lease_until = NULL,
                    last_error = ?, updated_time = ?
                WHERE id = ? AND status = 'PROCESSING' AND attempt_count = ?
                """,
                submission.getStatus().name(),
                Timestamp.valueOf(submission.getNextAttemptTime()),
                submission.getLastError(),
                Timestamp.valueOf(submission.getUpdatedTime()),
                submission.getId(),
                submission.getAttemptCount());
        ensureSingleUpdate(changed, submission);
    }

    /**
     * 原子保存发送成功事件和已启动任务。
     *
     * @param submission 提交事件
     * @param task 审查任务
     */
    @Override
    @Transactional
    public void saveSent(ReviewTaskSubmission submission, ReviewTask task) {
        int eventChanged = jdbcTemplate.update(
                """
                UPDATE review_task_submissions
                SET status = ?, lease_until = NULL, last_error = NULL,
                    updated_time = ?, sent_time = ?
                WHERE id = ? AND status = 'PROCESSING' AND attempt_count = ?
                """,
                submission.getStatus().name(),
                Timestamp.valueOf(submission.getUpdatedTime()),
                Timestamp.valueOf(submission.getSentTime()),
                submission.getId(),
                submission.getAttemptCount());
        ensureSingleUpdate(eventChanged, submission);

        int taskChanged = jdbcTemplate.update(
                """
                UPDATE review_tasks
                SET status = ?, started_time = ?, failure_reason = NULL
                WHERE id = ? AND status = 'PENDING'
                """,
                task.getStatus().name(), Timestamp.valueOf(task.getStartedTime()), task.getId());
        ensureTaskUpdated(taskChanged, task);
    }

    /**
     * 原子保存终止事件和失败任务。
     *
     * @param submission 提交事件
     * @param task 审查任务
     */
    @Override
    @Transactional
    public void saveFailed(ReviewTaskSubmission submission, ReviewTask task) {
        int eventChanged = jdbcTemplate.update(
                """
                UPDATE review_task_submissions
                SET status = ?, lease_until = NULL, last_error = ?, updated_time = ?
                WHERE id = ? AND status = 'PROCESSING' AND attempt_count = ?
                """,
                submission.getStatus().name(),
                submission.getLastError(),
                Timestamp.valueOf(submission.getUpdatedTime()),
                submission.getId(),
                submission.getAttemptCount());
        ensureSingleUpdate(eventChanged, submission);

        int taskChanged = jdbcTemplate.update(
                """
                UPDATE review_tasks
                SET status = ?, completed_time = ?, failure_reason = ?
                WHERE id = ? AND status = 'PENDING'
                """,
                task.getStatus().name(),
                Timestamp.valueOf(task.getCompletedTime()),
                task.getFailureReason(),
                task.getId());
        ensureTaskUpdated(taskChanged, task);
    }

    /**
     * 将查询结果转换为提交事件。
     *
     * @param resultSet 查询结果
     * @param rowNumber 行号
     * @return 提交事件
     * @throws SQLException 读取结果失败
     */
    private ReviewTaskSubmission mapSubmission(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return ReviewTaskSubmission.restore(
                resultSet.getObject("id", java.util.UUID.class),
                resultSet.getObject("review_task_id", java.util.UUID.class),
                ReviewTaskSubmissionStatus.valueOf(resultSet.getString("status")),
                resultSet.getInt("attempt_count"),
                resultSet.getObject("next_attempt_time", LocalDateTime.class),
                nullableTime(resultSet, "lease_until"),
                resultSet.getString("last_error"),
                resultSet.getObject("created_time", LocalDateTime.class),
                resultSet.getObject("updated_time", LocalDateTime.class),
                nullableTime(resultSet, "sent_time"));
    }

    /**
     * 读取可为空的时间字段。
     *
     * @param resultSet 查询结果
     * @param columnName 字段名
     * @return 时间或空值
     * @throws SQLException 读取结果失败
     */
    private LocalDateTime nullableTime(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * 校验事件仍由当前派发尝试持有。
     *
     * @param changed 更新行数
     * @param submission 提交事件
     */
    private void ensureSingleUpdate(int changed, ReviewTaskSubmission submission) {
        if (changed != 1) {
            throw new IllegalStateException(
                    "提交事件已被其他派发器处理，事件编号：" + submission.getId());
        }
    }

    /**
     * 校验任务状态更新成功。
     *
     * @param changed 更新行数
     * @param task 审查任务
     */
    private void ensureTaskUpdated(int changed, ReviewTask task) {
        if (changed != 1) {
            throw new IllegalStateException("审查任务状态已变化，任务编号：" + task.getId());
        }
    }
}
