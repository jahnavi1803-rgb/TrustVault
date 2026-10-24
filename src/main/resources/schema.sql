-- TrustVault PostgreSQL Reproducible Schema
-- Security: Strict foreign keys, unique constraints, and indexes

CREATE TABLE IF NOT EXISTS app_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL UNIQUE,
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    owner_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS document_permissions (
    id BIGSERIAL PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    permission_type VARCHAR(10) NOT NULL,
    granted_by_id BIGINT NOT NULL REFERENCES app_users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_doc_user UNIQUE (document_id, user_id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    sequence_number BIGINT NOT NULL UNIQUE,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_id BIGINT,
    actor_username VARCHAR(100) NOT NULL,
    actor_role VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100),
    resource_type VARCHAR(50) NOT NULL,
    ip_address VARCHAR(50),
    user_agent VARCHAR(255),
    outcome VARCHAR(20) NOT NULL,
    details TEXT,
    previous_hash VARCHAR(64) NOT NULL,
    current_hash VARCHAR(64) NOT NULL
);

-- Performance and Access-Control Indexes
CREATE INDEX IF NOT EXISTS idx_doc_owner ON documents(owner_id);
CREATE INDEX IF NOT EXISTS idx_doc_category ON documents(category_id);
CREATE INDEX IF NOT EXISTS idx_doc_created ON documents(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_perm_user ON document_permissions(user_id);
CREATE INDEX IF NOT EXISTS idx_perm_doc ON document_permissions(document_id);
CREATE INDEX IF NOT EXISTS idx_audit_seq ON audit_logs(sequence_number ASC);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_logs(actor_username);
