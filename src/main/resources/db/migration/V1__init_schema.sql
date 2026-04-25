-- ============================================================
-- V1: Initial schema — all tables, indexes, and constraints
-- ============================================================

-- ------------------------------------------------------------
-- users
-- Self-referential: CUSTOMER rows point to their AGENT.
-- CHECK constraints enforce the relationship invariant at DB level.
-- ------------------------------------------------------------
CREATE TABLE users (
    id            BINARY(16)                        NOT NULL,
    username      VARCHAR(100)                      NOT NULL,
    password_hash VARCHAR(255)                      NOT NULL,
    full_name     VARCHAR(120)                      NOT NULL,
    role          ENUM('ADMIN','AGENT','CUSTOMER')  NOT NULL,
    agent_id      BINARY(16)                        NULL,
    active        BOOLEAN                           NOT NULL DEFAULT TRUE,
    version       BIGINT                            NOT NULL DEFAULT 0,
    created_at    TIMESTAMP(3)                      NOT NULL,
    updated_at    TIMESTAMP(3)                      NOT NULL,
    created_by    BINARY(16)                        NULL,
    updated_by    BINARY(16)                        NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username),
    INDEX        idx_users_agent_role (agent_id, role),

    CONSTRAINT fk_users_agent
        FOREIGN KEY (agent_id) REFERENCES users (id),

    -- Non-CUSTOMER roles must NOT have an agent_id
    CONSTRAINT chk_non_customer_no_agent
        CHECK (role = 'CUSTOMER' OR agent_id IS NULL),

    -- CUSTOMER role MUST have an agent_id
    CONSTRAINT chk_customer_has_agent
        CHECK (role <> 'CUSTOMER' OR agent_id IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- tickets
-- ------------------------------------------------------------
CREATE TABLE tickets (
    id          BINARY(16)                                      NOT NULL,
    title       VARCHAR(200)                                    NOT NULL,
    description TEXT                                            NOT NULL,
    status      ENUM('OPEN','IN_PROGRESS','RESOLVED','CLOSED')  NOT NULL DEFAULT 'OPEN',
    priority    ENUM('LOW','MEDIUM','HIGH','URGENT')             NOT NULL DEFAULT 'MEDIUM',
    customer_id BINARY(16)                                      NOT NULL,
    version     BIGINT                                          NOT NULL DEFAULT 0,
    created_at  TIMESTAMP(3)                                    NOT NULL,
    updated_at  TIMESTAMP(3)                                    NOT NULL,
    created_by  BINARY(16)                                      NULL,
    updated_by  BINARY(16)                                      NULL,

    PRIMARY KEY (id),
    INDEX idx_tickets_customer_created (customer_id, created_at DESC),

    CONSTRAINT fk_tickets_customer
        FOREIGN KEY (customer_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- refresh_tokens
-- Opaque token values exist ONLY in transit.
-- Only the SHA-256 hex digest (64 chars) is stored here.
-- family_id groups tokens issued from the same login;
-- revoking a family invalidates all tokens from that session.
-- ------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id             BINARY(16)   NOT NULL,
    user_id        BINARY(16)   NOT NULL,
    token_hash     CHAR(64)     NOT NULL,
    issued_at      TIMESTAMP(3) NOT NULL,
    expires_at     TIMESTAMP(3) NOT NULL,
    revoked_at     TIMESTAMP(3) NULL,
    user_agent     VARCHAR(500) NULL,
    ip_address     VARCHAR(45)  NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_rt_token_hash (token_hash),
    INDEX        idx_rt_user    (user_id),
    INDEX        idx_rt_expires_at  (expires_at),

    CONSTRAINT fk_rt_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

