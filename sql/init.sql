-- ============================================================
-- 智慧农业平台 数据库初始化脚本
-- PostgreSQL + TimescaleDB
-- ============================================================

-- 启用扩展
CREATE EXTENSION IF NOT EXISTS timescaledb;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- 1. 租户/基地
-- ============================================================
CREATE TABLE tenant (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    contact_name VARCHAR(50),
    contact_phone VARCHAR(20),
    address TEXT,
    status VARCHAR(16) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- 2. 用户
-- ============================================================
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    username VARCHAR(64) UNIQUE NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    nickname VARCHAR(50),
    phone VARCHAR(20),
    role VARCHAR(16) NOT NULL DEFAULT 'OPERATOR', -- ADMIN/OPERATOR/VIEWER
    openid VARCHAR(64),  -- 微信openid
    status VARCHAR(16) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- 3. 设备
-- ============================================================
CREATE TABLE device (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    device_id VARCHAR(64) UNIQUE NOT NULL,
    device_secret_encrypted BYTEA NOT NULL,
    device_type VARCHAR(32) NOT NULL, -- SENSOR/VALVE/GATEWAY
    name VARCHAR(100),
    location VARCHAR(200),  -- 安装位置描述
    greenhouse_no VARCHAR(20), -- 棚号
    status VARCHAR(16) DEFAULT 'OFFLINE', -- ONLINE/OFFLINE/FAULT
    last_online_at TIMESTAMPTZ,
    firmware_version VARCHAR(32),
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_device_tenant ON device(tenant_id);
CREATE INDEX idx_device_type ON device(device_type);
CREATE INDEX idx_device_status ON device(status);

-- ============================================================
-- 4. 网关
-- ============================================================
CREgateway (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    gateway_id VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(100),
    location VARCHAR(200),
    status VARCHAR(16) DEFAULT 'OFFLINE',
    ip_address VARCHAR(45),
    firmware_version VARCHAR(32),
    last_heartbeat_at TIMESTAMPTZ,
    config JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- 5. 传感器数据（时序表）
-- ============================================================
CREATE TABLE sensor_data (
    time TIMESTAMPTZ NOT NULL,
    device_id VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL,
    data_type VARCHAR(32) NOT NULL, -- soil_moisture/soil_temp/air_temp/air_humidity
    value DOUBLE PRECISION NOT NULL,
    unit VARCHAR(16),
    quality INTEGER DEFAULT 100, -- 数据质量 0-100
    raw_payload JSONB
);

-- 转换为 TimescaleDB hypertable
SELECT create_hypertable('sensor_data', 'time', chunk_time_interval => INTERVAL '1 day');

CREATE INDEX idx_sensor_device_time ON sensor_data(device_id, time DESC);
CREATE INDEX idx_sensor_tenant_time ON sensor_data(tenant_id, time DESC);

-- 数据保留策略：原始数据保留90天
SELECT add_retention_policy('sensor_data', INTERVAL '90 days');

-- ============================================================
-- 6. 灌溉指令
-- ============================================================
CREATE TABLE command_log (
    id BIGSERIAL PRIMARY KEY,
    cmd_id VARCHAR(64) UNIQUE NOT NULL,
    tenant_id BIGINT NOT NULL,
    device_id VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL, -- OPEN_VALVE/CLOSE_VALVE/SET_CONFIG
    params JSONB,
    constraints JSONB,
    status VARCHAR(16) NOT NULL DEFAULT 'CREATED', -- CREATED/SENT/CONFIRMED/EXECUTED/FAILED
    priority INTEGER DEFAULT 0, -- 0=NORMAL, 1=HIGH, 9=EMERGENCY
    retry_count INTEGER DEFAULT 0,
    max_retry INTEGER DEFAULT 3,
    -- 时间追踪
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMPTZ,
    confirmed_at TIMESTAMPTZ,
    executed_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    -- 结果
    result JSONB,
    error_message TEXT,
    error_code VARCHAR(10),
    -- 触发来源
    triggered_by VARCHAR(64), -- AUTO:rule_id / USER:user_id
    sign VARCHAR(128)
);

CREATE INDEX idx_cmd_device_status ON command_log(device_id, status);
CREATE INDEX idx_cmd_tenant_time ON command_log(tenant_id, created_at DESC);
CREATE INDEX idx_cmd_status ON command_log(status) WHERE status IN ('CREATED', 'SENT', 'CONFIRMED');

-- ============================================================
-- 7. 灌溉规则
-- ============================================================
CREATE TABLE irrigation_rule (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(16) NOT NULL, -- THRESHOLD/SCHEDULE
    device_id VARCHAR(64) NOT NULL, -- 关联的阀门设备
    sensor_device_id VARCHAR(64), -- 关联的传感器
    -- 阈值规则
    threshold_low DOUBLE PRECISION, -- 低于此值开阀
    threshold_high DOUBLE PRECISION, -- 高于此值关阀
    -- 定时规则
    cron_expression VARCHAR(64),
    duration_min INTEGER DEFAULT 30,
    -- 状态
    enabled BOOLEAN DEFAULT TRUE,
    last_triggered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- 8. 告警记录
-- ============================================================
CREATE TABLE alert (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    device_id VARCHAR(64),
    level VARCHAR(4) NOT NULL, -- L1/L2/L3
    type VARCHAR(32) NOT NULL, -- DEVICE_OFFLINE/MOISTURE_LOW/CMD_FAILED/FLOW_ABNORMAL
    title VARCHAR(200) NOT NULL,
    content TEXT,
    status VARCHAR(16) DEFAULT 'PENDING', -- PENDING/ACKNOWLEDGED/RESOLVED
    notified BOOLEAN DEFAULT FALSE,
    acknowledged_by BIGINT,
    acknowledged_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_alert_tenant_status ON alert(tenant_id, status, created_at DESC);

-- ============================================================
-- 9. 操作审计日志
-- ============================================================
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(32), -- DEVICE/RULE/USER/COMMAND
    target_id VARCHAR(64),
    detail JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_audit_tenant_time ON audit_log(tenant_id, created_at DESC);

-- ============================================================
-- 10. 设备Token缓存（用于EMQX认证回调）
-- ============================================================
CREATE TABLE device_token (
    device_id VARCHAR(64) PRIMARY KEY,
    token VARCHAR(512) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- 11. 网关离线数据同步记录
-- ============================================================
CREATE TABLE gateway_sync_log (
    id BIGSERIAL PRIMARY KEY,
    gateway_id VARCHAR(64) NOT NULL,
    offline_start TIMESTAMPTZ,
    offline_end TIMESTAMPTZ,
    records_synced INTEGER DEFAULT 0,
    clock_drift_ms INTEGER DEFAULT 0,
    status VARCHAR(16) DEFAULT 'SYNCING', -- SYNCING/COMPLETED/FAILED
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- 初始数据
-- ============================================================
INSERT INTO tenant (name, contact_name, contact_phone) VALUES
('示范基地', '张三', '13800138000');

INSERT INTO sys_user (tenant_id, username, password_hash, nickname, role) VALUES
(1, 'admin', '$2a$10$placeholder_bcrypt_hash', '管理员', 'ADMIN');
