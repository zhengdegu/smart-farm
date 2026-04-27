-- ============================================================
-- 测试数据 — 覆盖所有业务模块
-- ============================================================

-- 额外用户
INSERT INTO sys_user (tenant_id, username, password_hash, nickname, phone, role) VALUES
(1, 'operator1', '$2b$10$1DDF4hXdn1iXWyYcdv9A1uHW4j6BmJDB8p.wL/B9Q6PnzXnVC9TW.', '李四', '13900139001', 'OPERATOR'),
(1, 'operator2', '$2b$10$1DDF4hXdn1iXWyYcdv9A1uHW4j6BmJDB8p.wL/B9Q6PnzXnVC9TW.', '王五', '13700137001', 'OPERATOR'),
(1, 'viewer1', '$2b$10$1DDF4hXdn1iXWyYcdv9A1uHW4j6BmJDB8p.wL/B9Q6PnzXnVC9TW.', '赵六', '13600136001', 'VIEWER')
ON CONFLICT (username) DO NOTHING;

-- 大棚
INSERT INTO greenhouse (tenant_id, name, greenhouse_no, area, location, description, status) VALUES
(1, '1号温室大棚', 'GH01', 2000, '园区东侧A区', '番茄种植专用棚，配备自动灌溉系统', 'ACTIVE'),
(1, '2号温室大棚', 'GH02', 1500, '园区东侧B区', '黄瓜种植棚', 'ACTIVE'),
(1, '3号温室大棚', 'GH03', 1800, '园区西侧', '叶菜轮作棚', 'ACTIVE'),
(1, '4号备用大棚', 'GH04', 1200, '园区北侧', '备用大棚，待整修', 'IDLE')
ON CONFLICT ON CONSTRAINT uk_greenhouse_tenant_no DO NOTHING;

-- 设备：传感器
INSERT INTO device (tenant_id, device_id, device_secret_encrypted, device_type, name, location, greenhouse_no, status, last_online_at) VALUES
(1, 'TH-001', '\x00', 'SENSOR', '1号温湿度传感器', '1号棚A区中部', 'GH01', 'ONLINE', NOW()),
(1, 'TH-002', '\x00', 'SENSOR', '2号温湿度传感器', '1号棚B区中部', 'GH01', 'ONLINE', NOW() - INTERVAL '1 minute'),
(1, 'TH-003', '\x00', 'SENSOR', '3号温湿度传感器', '2号棚中部', 'GH02', 'FAULT', NOW() - INTERVAL '2 hours'),
(1, 'SM-001', '\x00', 'SENSOR', '土壤湿度传感器', '1号棚A区地面', 'GH01', 'ONLINE', NOW()),
(1, 'LX-001', '\x00', 'SENSOR', '光照传感器', '1号棚顶部', 'GH01', 'ONLINE', NOW()),
(1, 'CO2-001', '\x00', 'SENSOR', 'CO₂传感器', '1号棚中部', 'GH01', 'ONLINE', NOW()),
(1, 'TH-004', '\x00', 'SENSOR', '4号温湿度传感器', '3号棚中部', 'GH03', 'ONLINE', NOW())
ON CONFLICT (device_id) DO NOTHING;

-- 设备：阀门
INSERT INTO device (tenant_id, device_id, device_secret_encrypted, device_type, name, location, greenhouse_no, status, last_online_at) VALUES
(1, 'VLV-A1', '\x00', 'VALVE', '灌溉阀门 A1', '1号棚A区', 'GH01', 'ONLINE', NOW()),
(1, 'VLV-A2', '\x00', 'VALVE', '灌溉阀门 A2', '1号棚B区', 'GH01', 'ONLINE', NOW()),
(1, 'VLV-B1', '\x00', 'VALVE', '灌溉阀门 B1', '2号棚A区', 'GH02', 'ONLINE', NOW()),
(1, 'VLV-C1', '\x00', 'VALVE', '灌溉阀门 C1', '3号棚', 'GH03', 'ONLINE', NOW())
ON CONFLICT (device_id) DO NOTHING;

-- 设备：网关
INSERT INTO device (tenant_id, device_id, device_secret_encrypted, device_type, name, location, status, last_online_at) VALUES
(1, 'GW-001', '\x00', 'GATEWAY', '主网关 GW-01', '园区机房', 'ONLINE', NOW())
ON CONFLICT (device_id) DO NOTHING;

-- 传感器数据（最近24小时，每小时一条）
INSERT INTO sensor_data (time, device_id, tenant_id, data_type, value, unit)
SELECT
  NOW() - (n || ' hours')::INTERVAL,
  'SM-001', 1, 'soil_moisture',
  35 + 15 * sin(n * 0.3) + random() * 5,
  '%'
FROM generate_series(0, 23) AS n
ON CONFLICT DO NOTHING;

INSERT INTO sensor_data (time, device_id, tenant_id, data_type, value, unit)
SELECT
  NOW() - (n || ' hours')::INTERVAL,
  'TH-001', 1, 'air_temperature',
  22 + 8 * sin((n - 6) * 0.26) + random() * 2,
  '°C'
FROM generate_series(0, 23) AS n
ON CONFLICT DO NOTHING;

INSERT INTO sensor_data (time, device_id, tenant_id, data_type, value, unit)
SELECT
  NOW() - (n || ' hours')::INTERVAL,
  'TH-001', 1, 'air_humidity',
  55 + 15 * sin(n * 0.25) + random() * 5,
  '%'
FROM generate_series(0, 23) AS n
ON CONFLICT DO NOTHING;

INSERT INTO sensor_data (time, device_id, tenant_id, data_type, value, unit)
SELECT
  NOW() - (n || ' hours')::INTERVAL,
  'TH-001', 1, 'soil_temperature',
  18 + 6 * sin((n - 8) * 0.26) + random() * 1.5,
  '°C'
FROM generate_series(0, 23) AS n
ON CONFLICT DO NOTHING;

INSERT INTO sensor_data (time, device_id, tenant_id, data_type, value, unit)
SELECT
  NOW() - (n || ' hours')::INTERVAL,
  'LX-001', 1, 'light',
  CASE WHEN n BETWEEN 6 AND 18 THEN 200 + 300 * sin((n - 6) * 0.26) + random() * 50 ELSE random() * 10 END,
  'μmol/m²/s'
FROM generate_series(0, 23) AS n
ON CONFLICT DO NOTHING;

INSERT INTO sensor_data (time, device_id, tenant_id, data_type, value, unit)
SELECT
  NOW() - (n || ' hours')::INTERVAL,
  'CO2-001', 1, 'co2',
  400 + 200 * sin(n * 0.2) + random() * 50,
  'ppm'
FROM generate_series(0, 23) AS n
ON CONFLICT DO NOTHING;

-- 灌溉规则
INSERT INTO irrigation_rule (tenant_id, name, rule_type, device_id, sensor_device_id, threshold_low, threshold_high, duration_min, enabled) VALUES
(1, '番茄定时灌溉', 'SCHEDULE', 'VLV-A1', NULL, NULL, NULL, 20, TRUE),
(1, '湿度联动灌溉', 'THRESHOLD', 'VLV-A1', 'SM-001', 35, 70, 25, TRUE),
(1, '高温应急灌溉', 'THRESHOLD', 'VLV-A2', 'TH-001', NULL, NULL, 15, TRUE),
(1, '黄瓜夜间灌溉', 'SCHEDULE', 'VLV-B1', NULL, NULL, NULL, 15, FALSE)
ON CONFLICT DO NOTHING;

-- 灌溉指令记录
INSERT INTO command_log (cmd_id, tenant_id, device_id, action, status, triggered_by, created_at, executed_at, params) VALUES
('cmd-001', 1, 'VLV-A1', 'OPEN_VALVE', 'EXECUTED', 'AUTO:rule_1', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '5 hours 40 minutes', '{"duration_min":20}'),
('cmd-002', 1, 'VLV-A2', 'OPEN_VALVE', 'EXECUTED', 'AUTO:rule_1', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '5 hours 40 minutes', '{"duration_min":20}'),
('cmd-003', 1, 'VLV-A1', 'OPEN_VALVE', 'EXECUTED', 'AUTO:rule_2', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour 35 minutes', '{"duration_min":25}'),
('cmd-004', 1, 'VLV-A2', 'OPEN_VALVE', 'FAILED', 'USER:1', NOW() - INTERVAL '30 minutes', NULL, '{"duration_min":25}'),
('cmd-005', 1, 'VLV-B1', 'OPEN_VALVE', 'EXECUTED', 'USER:2', NOW() - INTERVAL '4 hours', NOW() - INTERVAL '3 hours 50 minutes', '{"duration_min":10}')
ON CONFLICT (cmd_id) DO NOTHING;

-- 告警
INSERT INTO alert (tenant_id, device_id, level, type, title, content, status, created_at) VALUES
(1, 'VLV-A2', 'L1', 'CMD_FAILED', '灌溉指令执行失败', '阀门A2执行灌溉指令时通信超时，请检查设备连接状态和网关信号。', 'PENDING', NOW() - INTERVAL '30 minutes'),
(1, 'SM-001', 'L2', 'MOISTURE_LOW', '土壤湿度低于阈值 (32.5%)', '当前土壤湿度32.5%，低于设定阈值35%。建议立即灌溉25分钟。', 'PENDING', NOW() - INTERVAL '35 minutes'),
(1, 'TH-003', 'L2', 'DEVICE_OFFLINE', '传感器离线超过30分钟', '3号温湿度传感器已离线超过2小时，可能原因：电池耗尽、信号干扰、设备损坏。', 'PENDING', NOW() - INTERVAL '2 hours'),
(1, 'VLV-A1', 'L3', 'CMD_FAILED', '定时灌溉任务完成', '番茄定时灌溉规则执行完成，阀门A1灌溉20分钟，用水0.6吨。', 'ACKNOWLEDGED', NOW() - INTERVAL '6 hours'),
(1, NULL, 'L3', 'CMD_FAILED', '月度报告已生成', '2026年3月运营报告已自动生成，包含灌溉统计、节水分析、设备运行报告。', 'ACKNOWLEDGED', NOW() - INTERVAL '1 day')
ON CONFLICT DO NOTHING;

-- 种植记录
INSERT INTO greenhouse_planting (tenant_id, greenhouse_no, crop_template_id, planting_date, expected_harvest_date, current_stage, status) VALUES
(1, 'GH01', (SELECT id FROM crop_template WHERE crop_type='TOMATO' AND is_system=TRUE LIMIT 1), '2026-03-01', '2026-07-30', '开花坐果期', 'GROWING'),
(1, 'GH02', (SELECT id FROM crop_template WHERE crop_type='CUCUMBER' AND is_system=TRUE LIMIT 1), '2026-03-15', '2026-07-01', '伸蔓期', 'GROWING'),
(1, 'GH03', (SELECT id FROM crop_template WHERE crop_type='LEAFY' AND is_system=TRUE LIMIT 1), '2026-04-10', '2026-05-25', '幼苗期', 'GROWING')
ON CONFLICT DO NOTHING;

-- 灌溉日统计（最近7天）
INSERT INTO irrigation_daily_summary (tenant_id, greenhouse_no, summary_date, irrigation_count, total_duration_min, estimated_water_liters, avg_soil_moisture, auto_trigger_count, manual_trigger_count)
SELECT
  1, 'GH01', CURRENT_DATE - n,
  3 + floor(random() * 4)::int,
  60 + floor(random() * 60)::int,
  800 + floor(random() * 600)::numeric,
  40 + random() * 20,
  2 + floor(random() * 3)::int,
  floor(random() * 2)::int
FROM generate_series(0, 6) AS n
ON CONFLICT (tenant_id, greenhouse_no, summary_date) DO NOTHING;

INSERT INTO irrigation_daily_summary (tenant_id, greenhouse_no, summary_date, irrigation_count, total_duration_min, estimated_water_liters, avg_soil_moisture, auto_trigger_count, manual_trigger_count)
SELECT
  1, 'GH02', CURRENT_DATE - n,
  2 + floor(random() * 3)::int,
  40 + floor(random() * 40)::int,
  500 + floor(random() * 400)::numeric,
  45 + random() * 15,
  1 + floor(random() * 2)::int,
  floor(random() * 2)::int
FROM generate_series(0, 6) AS n
ON CONFLICT (tenant_id, greenhouse_no, summary_date) DO NOTHING;

-- 月度报告
INSERT INTO monthly_report (tenant_id, report_month, water_saved_percent, total_irrigation_count, total_water_liters, device_online_rate, alert_count, report_url) VALUES
(1, '2026-03-01', 30.5, 152, 72300, 97.1, 23, '/reports/2026-03.pdf'),
(1, '2026-02-01', 28.2, 138, 68500, 96.5, 18, '/reports/2026-02.pdf'),
(1, '2026-01-01', 25.8, 145, 71200, 95.8, 21, '/reports/2026-01.pdf')
ON CONFLICT DO NOTHING;

-- 温室坐标数据（基于山东寿光蔬菜产业园）
UPDATE greenhouse SET latitude = 36.8847, longitude = 118.7372,
  boundary_geojson = '{"type":"Polygon","coordinates":[[[118.7365,36.8842],[118.7379,36.8842],[118.7379,36.8852],[118.7365,36.8852],[118.7365,36.8842]]]}'
WHERE greenhouse_no = 'GH01' AND tenant_id = 1;

UPDATE greenhouse SET latitude = 36.8853, longitude = 118.7380,
  boundary_geojson = '{"type":"Polygon","coordinates":[[[118.7374,36.8849],[118.7386,36.8849],[118.7386,36.8857],[118.7374,36.8857],[118.7374,36.8849]]]}'
WHERE greenhouse_no = 'GH02' AND tenant_id = 1;

UPDATE greenhouse SET latitude = 36.8840, longitude = 118.7358,
  boundary_geojson = '{"type":"Polygon","coordinates":[[[118.7350,36.8835],[118.7366,36.8835],[118.7366,36.8845],[118.7350,36.8845],[118.7350,36.8835]]]}'
WHERE greenhouse_no = 'GH03' AND tenant_id = 1;

UPDATE greenhouse SET latitude = 36.8860, longitude = 118.7365,
  boundary_geojson = '{"type":"Polygon","coordinates":[[[118.7358,36.8856],[118.7372,36.8856],[118.7372,36.8864],[118.7358,36.8864],[118.7358,36.8856]]]}'
WHERE greenhouse_no = 'GH04' AND tenant_id = 1;
