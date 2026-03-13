CREATE TABLE IF NOT EXISTS aero_rules (
    id VARCHAR(255) PRIMARY KEY,
    description TEXT,
    priority INT,
    condition TEXT NOT NULL,
    on_success_action VARCHAR(255),
    on_success_metadata JSON,
    on_failure_action VARCHAR(255),
    on_failure_metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
