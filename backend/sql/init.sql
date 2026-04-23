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
CREATE TABLE gateway (
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
    sign VARCHAR(128),
    updated_at TIMESTAMPTZ DEFAULT NOW()
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

-- ============================================================
-- 12. 种植模板（6.5）
-- ============================================================
CREATE TABLE crop_template (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT REFERENCES tenant(id), -- NULL 表示系统预设
    crop_type VARCHAR(32) NOT NULL,          -- TOMATO/CUCUMBER/LEAFY/...
    name VARCHAR(100) NOT NULL,
    stages JSONB NOT NULL DEFAULT '[]',      -- 各生长阶段灌溉参数
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_crop_template_tenant ON crop_template(tenant_id);
CREATE INDEX idx_crop_template_type ON crop_template(crop_type);

-- ============================================================
-- 13. 大棚种植记录（6.5）
-- ============================================================
CREATE TABLE greenhouse_planting (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    greenhouse_no VARCHAR(20) NOT NULL,
    crop_template_id BIGINT NOT NULL REFERENCES crop_template(id),
    planting_date DATE NOT NULL,
    expected_harvest_date DATE,
    current_stage VARCHAR(32),               -- 当前所处阶段名称
    status VARCHAR(16) NOT NULL DEFAULT 'GROWING', -- GROWING/HARVESTED/ABANDONED
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_planting_tenant_greenhouse ON greenhouse_planting(tenant_id, greenhouse_no);

-- ============================================================
-- 14. 灌溉日统计（6.6）
-- ============================================================
CREATE TABLE irrigation_daily_summary (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    greenhouse_no VARCHAR(20) NOT NULL,
    summary_date DATE NOT NULL,
    irrigation_count INTEGER NOT NULL DEFAULT 0,
    total_duration_min INTEGER NOT NULL DEFAULT 0,
    estimated_water_liters NUMERIC(10,2) NOT NULL DEFAULT 0,
    avg_soil_moisture NUMERIC(5,2),
    auto_trigger_count INTEGER NOT NULL DEFAULT 0,
    manual_trigger_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (tenant_id, greenhouse_no, summary_date)
);

CREATE INDEX idx_daily_summary_tenant_date ON irrigation_daily_summary(tenant_id, summary_date DESC);

-- ============================================================
-- 15. 月度报告（6.6）
-- ============================================================
CREATE TABLE monthly_report (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    report_month DATE NOT NULL,              -- 存月份首日，如 2025-04-01
    water_saved_percent NUMERIC(5,2),        -- 节水率 %
    total_irrigation_count INTEGER NOT NULL DEFAULT 0,
    total_water_liters NUMERIC(12,2) NOT NULL DEFAULT 0,
    device_online_rate NUMERIC(5,2),         -- 设备在线率 %
    alert_count INTEGER NOT NULL DEFAULT 0,
    report_url VARCHAR(500),                 -- 生成的报告文件地址
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_monthly_report_tenant_month ON monthly_report(tenant_id, report_month DESC);

-- ============================================================
-- 系统预设种植模板
-- ============================================================
INSERT INTO crop_template (tenant_id, crop_type, name, is_system, stages) VALUES
(NULL, 'TOMATO', '番茄标准种植模板', TRUE, '[
  {
    "stage": "seedling",
    "name": "育苗期",
    "duration_days": 25,
    "irrigation": {
      "mode": "SCHEDULE",
      "times_per_day": 2,
      "duration_min": 10,
      "soil_moisture_low": 55,
      "soil_moisture_high": 75
    }
  },
  {
    "stage": "transplant",
    "name": "定植缓苗期",
    "duration_days": 10,
    "irrigation": {
      "mode": "THRESHOLD",
      "times_per_day": 3,
      "duration_min": 15,
      "soil_moisture_low": 60,
      "soil_moisture_high": 80
    }
  },
  {
    "stage": "vegetative",
    "name": "营养生长期",
    "duration_days": 30,
    "irrigation": {
      "mode": "THRESHOLD",
      "times_per_day": 2,
      "duration_min": 20,
      "soil_moisture_low": 60,
      "soil_moisture_high": 75
    }
  },
  {
    "stage": "flowering",
    "name": "开花坐果期",
    "duration_days": 20,
    "irrigation": {
      "mode": "THRESHOLD",
      "times_per_day": 2,
      "duration_min": 25,
      "soil_moisture_low": 65,
      "soil_moisture_high": 80
    }
  },
  {
    "stage": "fruiting",
    "name": "果实膨大期",
    "duration_days": 40,
    "irrigation": {
      "mode": "THRESHOLD",
      "times_per_day": 3,
      "duration_min": 30,
      "soil_moisture_low": 70,
      "soil_moisture_high": 85
    }
  },
  {
    "stage": "ripening",
    "name": "成熟采收期",
    "duration_days": 30,
    "irrigation": {
      "mode": "SCHEDULE",
      "times_per_day": 1,
      "duration_min": 15,
      "soil_moisture_low": 55,
      "soil_moisture_high": 70
    }
  }
]'),

(NULL, 'CUCUMBER', '黄瓜标准种植模板', TRUE, '[
  {
    "stage": "seedling",
    "name": "育苗期",
    "duration_days": 15,
    "irrigation": {
      "mode": "SCHEDULE",
      "times_per_day": 2,
      "duration_min": 8,
      "soil_moisture_low": 60,
      "soil_moisture_high": 80
    }
  },
  {
    "stage": "transplant",
    "name": "定植缓苗期",
    "duration_days": 7,
    "irrigation": {
      "mode": "THRESHOLD",
      "times_per_day": 3,
      "duration_min": 12,
      "soil_moisture_low": 65,
      "soil_moisture_high": 85
    }
  },
  {
    "stage": "vegetative",
    "name": "伸蔓期",
    "duration_days": 20,
    "irrigation": {
      "mode": "THRESHOLD",
      "times_per_day": 2,
      "duration_min": 20,
      "soil_moisture_low": 65,
      "soil_moisture_high": 80
    }
  },
  {
    "stage": "flowering",
    "name": "开花期",
    "duration_days": 15,
    "irrigation": {
      "mode": "THRESHOLD",
      "times_per_day": 3,
      "duration_min": 20,
      "soil_moisture_low": 70,
      "soil_moisture_high": 85
    }
  },
  {
    "stage": "fruiting",
    "name": "结瓜盛期",
    "duration_days": 50,
    "irrigation": {
      "mode": "THRESHOLD",
      "times_per_day": 3,
      "duration_min": 25,
      "soil_moisture_low": 70,
      "soil_moisture_high": 88
    }
  }
]'),

(NULL, 'LEAFY', '叶菜通用种植模板', TRUE, '[
  {
    "stage": "germination",
    "name": "发芽期",
    "duration_days": 5,
    "irrigation": {
      "mode": "SCHEDULE",
      "times_per_day": 3,
      "duration_min": 5,
      "soil_moisture_low": 70,
      "soil_moisture_high": 90
    }
  },
  {
    "stage": "seedling",
    "name": "幼苗期",
    "duration_days": 10,
    "irrigation": {
      "mode": "SCHEDULE",
      "times_per_day": 2,
      "duration_min": 8,
      "soil_moisture_low": 65,
      "soil_moisture_high": 85
    }
  },
  {
    "stage": "growth",
    "name": "旺盛生长期",
    "duration_days": 20,
    "irrigation": {
      "mode": "THRESHOLD",
      "times_per_day": 2,
      "duration_min": 12,
      "soil_moisture_low": 65,
      "soil_moisture_high": 85
    }
  },
  {
    "stage": "harvest",
    "name": "采收期",
    "duration_days": 10,
    "irrigation": {
      "mode": "SCHEDULE",
      "times_per_day": 1,
      "duration_min": 8,
      "soil_moisture_low": 60,
      "soil_moisture_high": 80
    }
  }
]');
