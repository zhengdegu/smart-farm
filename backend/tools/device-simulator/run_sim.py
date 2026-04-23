import os
os.environ["MQTT_BROKER"] = "localhost"
os.environ["MQTT_PORT"] = "1883"
os.environ["TENANT_ID"] = "t001"
os.environ["SENSOR_COUNT"] = "5"
os.environ["REPORT_INTERVAL_SEC"] = "5"
os.environ["VALVE_COUNT"] = "2"

exec(open(r"D:\env\openclaw\workspace\smart-farm\backend\tools\device-simulator\simulator.py", encoding="utf-8").read())
