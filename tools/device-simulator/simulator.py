"""
智慧农业平台 - 设备模拟器
模拟传感器数据上报和阀门指令响应
"""
import json
import time
import random
import os
import threading
from datetime import datetime, timezone

try:
    import paho.mqtt.client as mqtt
except ImportError:
    print("请安装依赖: pip install paho-mqtt")
    exit(1)

# 配置
MQTT_BROKER = os.getenv("MQTT_BROKER", "localhost")
MQTT_PORT = int(os.getenv("MQTT_PORT", "1883"))
TENANT_ID = os.getenv("TENANT_ID", "t001")
SENSOR_COUNT = int(os.getenv("SENSOR_COUNT", "5"))
REPORT_INTERVAL = int(os.getenv("REPORT_INTERVAL_SEC", "10"))
VALVE_COUNT = int(os.getenv("VALVE_COUNT", "2"))

# 模拟异常概率
ANOMALY_RATE = float(os.getenv("ANOMALY_RATE", "0.05"))  # 5%概率产生异常数据
CMD_FAIL_RATE = float(os.getenv("CMD_FAIL_RATE", "0.1"))  # 10%概率指令执行失败


class SensorSimulator:
    """传感器模拟器"""

    def __init__(self, device_id, sensor_type):
        self.device_id = device_id
        self.sensor_type = sensor_type
        self.base_value = self._init_base_value()

    def _init_base_value(self):
        defaults = {
            "soil_moisture": 55.0,
            "soil_temp": 22.0,
            "air_temp": 26.0,
            "air_humidity": 65.0,
        }
        return defaults.get(self.sensor_type, 50.0)

    def read(self):
        """生成模拟读数"""
        # 正常波动
        drift = random.gauss(0, 2)
        value = self.base_value + drift

        # 模拟异常
        if random.random() < ANOMALY_RATE:
            if self.sensor_type == "soil_moisture":
                value = random.choice([15.0, 95.0])  # 极干或极湿
            elif self.sensor_type == "air_temp":
                value = random.choice([5.0, 42.0])  # 极冷或极热

        # 缓慢漂移
        self.base_value += random.gauss(0, 0.1)
        self.base_value = max(0, min(100, self.base_value))

        return round(value, 1)


class ValveSimulator:
    """阀门模拟器"""

    def __init__(self, device_id):
        self.device_id = device_id
        self.state = "CLOSED"
        self.open_since = None

    def handle_command(self, cmd_payload):
        """处理控制指令"""
        cmd = json.loads(cmd_payload)
        cmd_id = cmd.get("cmd_id", "unknown")
        action = cmd.get("action", "")

        print(f"  [VALVE {self.device_id}] 收到指令: {action} (cmd_id={cmd_id})")

        # 模拟执行延迟
        time.sleep(random.uniform(0.5, 2.0))

        # 模拟失败
        if random.random() < CMD_FAIL_RATE:
            print(f"  [VALVE {self.device_id}] ❌ 执行失败!")
            return {
                "cmd_id": cmd_id,
                "device_id": self.device_id,
                "status": "FAILED",
                "timestamp": int(time.time()),
                "error": "VALVE_STUCK",
            }

        # 执行成功
        if action == "OPEN_VALVE":
            self.state = "OPEN"
            self.open_since = time.time()
        elif action == "CLOSE_VALVE":
            self.state = "CLOSED"
            self.open_since = None

        print(f"  [VALVE {self.device_id}] ✓ 执行成功, 状态={self.state}")
        return {
            "cmd_id": cmd_id,
            "device_id": self.device_id,
            "status": "EXECUTED",
            "timestamp": int(time.time()),
            "result": {"valve_state": self.state},
        }


def main():
    print("=" * 60)
    print("  智慧农业平台 - 设备模拟器")
    print(f"  MQTT: {MQTT_BROKER}:{MQTT_PORT}")
    print(f"  租户: {TENANT_ID}")
    print(f"  传感器: {SENSOR_COUNT}个, 上报间隔: {REPORT_INTERVAL}s")
    print(f"  阀门: {VALVE_COUNT}个")
    print("=" * 60)

    # 创建传感器
    sensors = []
    sensor_types = ["soil_moisture", "soil_temp", "air_temp", "air_humidity"]
    for i in range(SENSOR_COUNT):
        s_type = sensor_types[i % len(senstypes)]
        sensor = SensorSimulator(f"sensor_{i+1:03d}", s_type)
        sensors.append(sensor)

    # 创建阀门
    valves = {}
    for i in range(VALVE_COUNT):
        vid = f"valve_{i+1:03d}"
        valves[vid] = ValveSimulator(vid)

    # MQTT 回调
    def on_connect(client, userdata, flags, rc, properties=None):
        if rc == 0:
            print("[MQTT] ✓ 连接成功")
            # 订阅阀门控制指令
            for vid in valves:
                topic = f"/{TENANT_ID}/valve/{vid}/command"
                client.subscribe(topic, qos=1)
                print(f"[MQTT] 订阅: {topic}")
        else:
            print(f"[MQ接失败, rc={rc}")

    def on_message(client, userdata, msg):
        device_id = msg.topic.split("/")[3]
        if device_id in valves:
            response = valves[device_id].handle_command(msg.payload.decode())
            # 发送ACK
            ack_topic = f"/{TENANT_ID}/valve/{device_id}/command_ack"
            client.publish(ack_topic, json.dumps(response), qos=1)

    # 连接MQTT
    client = mqtt.Client(client_id=f"simulator_{TENANT_ID}", protocol=mqtt.MQTTv5)
    client.on_connect = on_connect
    client.on_message = on_message

    print(f"[MQTT] 连接 {MQTT_BROKER}:{MQTT_PORT}...")
    try:
        client.connect(MQTT_BROKER, MQTT_PORT, 60)
    except Exception as e:
        print(f"[MQTT] 连接失败: {e}")
        print("  请确认 EMQX 已启动: docker compose up -d emqx")
        return

    client.loop_start()
    time.sleep(2)  # 等待连接建立

    # 主循环：周期性上报传感器数据
    print(f"\n[SIM] 开始上报数据 (间隔 {REPORT_INTERVAL}s)...\n")
    try:
        while True:
            for sensor in sensors:
                value = sensor.read()
                payload = {
                    "device_id": sensor.device_id,
                    "data_type": sensor.sensor_type,
                    "value": value,
                    "unit": _get_unit(sensor.sensor_type),
                    "timestamp": datetime.now(timezone.utc).isoformat(),
                }
                topic = f"/{TENANT_ID}/sensor/{sensor.device_id}/telemetry"
                client.publish(topic, json.dumps(payload), qos=1)

                # 打印异常数据
                if _is_anomaly(sensor.sensor_type, value):
                    print(f"  ⚠️  [{sensor.device_id}] {sensor.sensor_type}={value} (异常!)")

            # 打印心跳
            ts = datetime.now().strftime("%H:%M:%S")
            print(f"  [{ts}] 已上报 {len(sensors)} 个传感器数据")

            time.sleep(REPORT_INTERVAL)

    except KeyboardInterrupt:
        print("\n[SIM] 停止模拟器...")
        client.loop_stop()
        client.disconnect()
        print("[SIM] 已断开连接")


def _get_unit(sensor_type):
    units = {
        "soil_moisture": "%",
        "soil_temp": "°C",
        "air_temp": "°C",
        "air_humidity": "%",
    }
    return units.get(sensor_type, "")


def _is_anomaly(sensor_type, value):
    thresholds = {
        "soil_moisture": (20, 90),
        "soil_temp": (5, 40),
        "air_temp": (5, 40),
        "aity": (20, 95),
    }
    low, high = thresholds.get(sensor_type, (0, 100))
    return value < low or value > high


if __name__ == "__main__":
    main()
