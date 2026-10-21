CREATE TABLE review_tasks (
    id UUID PRIMARY KEY,
    source_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    started_time TIMESTAMP WITHOUT TIME ZONE,
    completed_time TIMESTAMP WITHOUT TIME ZONE,
    failure_reason TEXT
);

CREATE TABLE review_files (
    id UUID PRIMARY KEY,
    review_task_id UUID NOT NULL REFERENCES review_tasks (id),
    file_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    language VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    created_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_review_files_task_path UNIQUE (review_task_id, file_path)
);

CREATE INDEX idx_review_files_task_id
    ON review_files (review_task_id);

CREATE TABLE review_task_submissions (
    id UUID PRIMARY KEY,
    review_task_id UUID NOT NULL UNIQUE REFERENCES review_tasks (id),
    status VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL,
    next_attempt_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    lease_until TIMESTAMP WITHOUT TIME ZONE,
    last_error TEXT,
    created_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    sent_time TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT ck_review_task_submissions_attempt_count
        CHECK (attempt_count >= 0)
);

CREATE INDEX idx_review_task_submissions_dispatch
    ON review_task_submissions (status, next_attempt_time, lease_until);
