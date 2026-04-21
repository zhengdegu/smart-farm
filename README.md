# 🌱 智慧农业灌溉平台 (Smart Farm)

> 面向设施温室蔬菜种植基地的智慧灌溉解决方案

## 快速开始

```bash
# 1. 克隆项目
git clone <repo-url> && cd smart-farm

# 2. 启动开发环境（PostgreSQL + EMQX + Redis + 设备模拟器）
docker compose -f docker-compose-dev.yml up -d

# 3. 启动后端
./gradlew bootRun

# 4. 访问
# - API文档: http://localhost:8080/swagger-ui.html
# - EMQX控制台: http://localhost:18083 (admin/public)
# - 数据库管理: http://localhost:8081 (dev/dev123)
```

## 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 17+ |
| Docker | 20+ |
| Docker Compose | 2.0+ |
| Node.js | 18+（小程序开发） |
| 微信开发者工具 | 最新版 |

## 项目结构

```
smart-farm/
├── src/main/java/com/smartfarm/
│   ├── device/          # 设备管理上下文
│   ├── telemetry/       # 遥测数据上下文
│   ├── irrigation/      # 灌溉控制上下文
│   ├── alert/           # 告警上下文
│   ├── user/            # 用户上下文
│   └── shared/          # 公共模块（MQTT、安全、配置）
├── sql/                 # 数据库DDL脚本
├── docs/                # API文档（OpenAPI）
├── tools/
│   └── device-simulator/  # 设备模拟器（Python）
├── docker-compose-dev.yml # 开发环境
└── docker-compose.yml     # 生产环境
```

## 开发环境服务

| 服务 | 端口 | 说明 |
|------|------|------|
| PostgreSQL + TimescaleDB | 5432 | 用户: dev, 密码: dev123, 库: smartfarm_dev |
| EMQX (MQTT Broker) | 1883 (MQTT), 18083 (Dashboard) | 开发环境允许匿名连接 |
| Redis | 6379 | 缓存、Token存储 |
| Adminer | 8081 | 数据库Web管理 |
| 设备模拟器 | — | 自动上报传感器数据 |

## 设备模拟器

开发环境自动启动，也可单独运行：

```bash
cd tools/device-simulator
pip install -r requirements.txt
python simulator.py
```

环境变量配置：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| MQTT_BROKER | localhost | EMQX地址 |
| SENSOR_COUNT | 5 | 模拟传感器数量 |
| REPORT_INTERVAL_SEC | 10 | 上报间隔(秒) |
| VALVE_COUNT | 2 | 模拟阀门数量 |
| ANOMALY_RATE | 0.05 | 异常数据概率 |
| CMD_FAIL_RATE | 0.1 | 指令失败概率 |

## MQTT Topic 规范

```
/{tenant_id}/{device_type}/{device_id}/{data_type}

示例：
/t001/sensor/sensor_001/telemetry      # 传感器上报
/t001/valve/valve_001/command           # 阀门控制指令
/t001/valve/valve_001/command_ack       # 阀门指令确认
/t001/gateway/gw_001/status             # 网关状态
```

## API 文档

OpenAPI 规范文件: `docs/openapi.yaml`

开发环境启动后访问 Swagger UI: http://localhost:8080/swagger-ui.html

## Git 分支策略

| 分支 | 用途 |
|------|------|
| main | 生产代码，保护分支 |
| develop | 开发主线 |
| feature/{模块}-{功能} | 功能开发 |
| hotfix/{描述} | 紧急修复 |

Commit 规范: `feat:` / `fix:` / `docs:` / `refactor:` / `test:` / `# 常见问题

**Q: EMQX 连接失败？**
确认 EMQX 容器已启动: `docker compose -f docker-compose-dev.yml ps`

**Q: 数据库连接失败？**
确认 PostgreSQL 容器健康: `docker compose -f docker-compose-dev.yml logs postgres`

**Q: 设备模拟器无数据？**
检查 EMQX Dashboard (http://localhost:18083) 中的客户端连接和消息统计

**Q: TimescaleDB 扩展未加载？**
init.sql 会自动创建扩展，如果手动建库需执行: `CREATE EXTENSION IF NOT EXISTS timescaledb;`
