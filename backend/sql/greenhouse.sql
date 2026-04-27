CREATE TABLE IF NOT EXISTS greenhouse (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    name          VARCHAR(100) NOT NULL,
    greenhouse_no VARCHAR(20)  NOT NULL,
    area          DOUBLE PRECISION,
    location      VARCHAR(200),
    description   TEXT,
    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_greenhouse_tenant_no UNIQUE (tenant_id, greenhouse_no)
);
CREATE INDEX IF NOT EXISTS idx_greenhouse_tenant ON greenhouse(tenant_id);
CREATE INDEX IF NOT EXISTS idx_greenhouse_status ON greenhouse(tenant_id, status);
