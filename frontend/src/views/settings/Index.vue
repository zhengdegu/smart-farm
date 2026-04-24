<template>
  <div class="settings-page">
    <!-- 基地信息 -->
    <div class="form-section">
      <div class="form-section-title">基地信息</div>
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">基地名称</label>
          <input class="form-input" v-model="baseInfo.name" />
        </div>
        <div class="form-group">
          <label class="form-label">基地编号</label>
          <input class="form-input" v-model="baseInfo.code" disabled style="background: #f5f5f5" />
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">联系人</label>
          <input class="form-input" v-model="baseInfo.contact" />
        </div>
        <div class="form-group">
          <label class="form-label">联系电话</label>
          <input class="form-input" v-model="baseInfo.phone" />
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">基地地址</label>
        <input class="form-input" v-model="baseInfo.address" />
      </div>
      <button class="btn btn-primary" @click="saveBaseInfo">💾 保存修改</button>
    </div>

    <!-- 通知配置 -->
    <div class="form-section">
      <div class="form-section-title">通知配置</div>
      <div
        v-for="item in notifications"
        :key="item.key"
        class="notif-row"
      >
        <div class="notif-info">
          <div class="notif-name">{{ item.name }}</div>
          <div class="notif-desc">{{ item.desc }}</div>
        </div>
        <el-switch
          v-model="item.enabled"
          @change="handleNotifChange"
        />
      </div>
    </div>

    <!-- MQTT 连接状态 -->
    <div class="form-section">
      <div class="form-section-title">MQTT 连接状态</div>
      <div class="mqtt-status-row">
        <div class="mqtt-indicator connected">
          <span class="mqtt-dot"></span>
          <span>已连接</span>
        </div>
        <span class="mqtt-broker-text">Broker: {{ mqttStatus.broker }}:{{ mqttStatus.port }}</span>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">Broker 地址</label>
          <input class="form-input" v-model="mqttStatus.broker" />
        </div>
        <div class="form-group">
          <label class="form-label">端口</label>
          <input class="form-input" v-model="mqttStatus.port" />
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">客户端ID</label>
          <input class="form-input" v-model="mqttStatus.clientId" disabled style="background: #f5f5f5" />
        </div>
        <div class="form-group">
          <label class="form-label">上次重连</label>
          <input class="form-input" v-model="mqttStatus.lastReconnect" disabled style="background: #f5f5f5" />
        </div>
      </div>
      <button class="btn btn-outline" @click="reconnectMqtt">🔄 重新连接</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { settingsApi } from '../../api'
import { ElMessage } from 'element-plus'

// Base info
const baseInfo = reactive({
  name: '',
  code: '',
  contact: '',
  phone: '',
  address: '',
})

// Notifications
const notifications = ref([
  { key: 'l1Alert', name: 'L1 紧急告警通知', desc: '设备故障、灌溉失败等紧急告警，通过短信+微信推送', enabled: true },
  { key: 'l2Alert', name: 'L2 重要告警通知', desc: '传感器异常、阈值告警等，通过微信推送', enabled: true },
  { key: 'l3Alert', name: 'L3 一般通知', desc: '任务完成、报告生成等，仅站内消息', enabled: true },
  { key: 'quietHours', name: '免打扰时段', desc: '22:00 - 06:00 期间不发送非紧急通知', enabled: true },
  { key: 'dailySummary', name: '每日摘要', desc: '每日 08:00 发送前一天运营摘要', enabled: false },
])

// MQTT status
const mqttStatus = reactive({
  broker: 'mqtt.smartfarm.local',
  port: '1883',
  clientId: 'smartfarm-server-01',
  lastReconnect: '',
  connected: true,
})

// Actions
async function saveBaseInfo() {
  try {
    await settingsApi.updateBaseInfo({
      name: baseInfo.name,
      code: baseInfo.code,
      contact: baseInfo.contact,
      phone: baseInfo.phone,
      address: baseInfo.address,
    })
    ElMessage.success('保存成功')
  } catch {
    // Error handled by API interceptor
  }
}

async function handleNotifChange() {
  try {
    const data = {}
    notifications.value.forEach(item => {
      data[item.key] = item.enabled
    })
    await settingsApi.updateNotifications(data)
  } catch {
    // Error handled by API interceptor
  }
}

async function reconnectMqtt() {
  try {
    await settingsApi.reconnectMqtt()
    ElMessage.success('重连请求已发送')
    await fetchMqttStatus()
  } catch {
    // Error handled by API interceptor
  }
}

// Data fetching
async function fetchBaseInfo() {
  try {
    const res = await settingsApi.getBaseInfo()
    if (res) {
      baseInfo.name = res.name || ''
      baseInfo.code = res.code || ''
      baseInfo.contact = res.contact || ''
      baseInfo.phone = res.phone || ''
      baseInfo.address = res.address || ''
    }
  } catch {
    // Error handled by API interceptor
  }
}

async function fetchNotifications() {
  try {
    const res = await settingsApi.getNotifications()
    if (res) {
      notifications.value.forEach(item => {
        if (res[item.key] !== undefined) {
          item.enabled = res[item.key]
        }
      })
    }
  } catch {
    // Error handled by API interceptor
  }
}

async function fetchMqttStatus() {
  try {
    const res = await settingsApi.getMqttStatus()
    if (res) {
      mqttStatus.broker = res.broker || mqttStatus.broker
      mqttStatus.port = res.port || mqttStatus.port
      mqttStatus.clientId = res.clientId || mqttStatus.clientId
      mqttStatus.lastReconnect = res.lastReconnect || mqttStatus.lastReconnect
      mqttStatus.connected = res.connected !== undefined ? res.connected : true
    }
  } catch {
    // Error handled by API interceptor
  }
}

onMounted(() => {
  fetchBaseInfo()
  fetchNotifications()
  fetchMqttStatus()
})
</script>

<style scoped>
.settings-page {
  padding: 0;
}

/* Form sections */
.form-section {
  background: #FFF;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  margin-bottom: 20px;
}

.form-section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid #E0E0E0;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-group {
  margin-bottom: 16px;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: #757575;
  margin-bottom: 6px;
  display: block;
}

.form-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #E0E0E0;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: var(--sf-primary);
}

.form-input:disabled {
  cursor: not-allowed;
  color: #999;
}

/* Buttons */
.btn {
  padding: 8px 20px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  border: none;
  transition: opacity 0.2s;
}

.btn:hover {
  opacity: 0.85;
}

.btn-primary {
  background: var(--sf-primary);
  color: #FFF;
}

.btn-outline {
  background: transparent;
  color: var(--sf-text);
  border: 1px solid #E0E0E0;
}

.btn-outline:hover {
  border-color: var(--sf-primary);
  color: var(--sf-primary);
}

/* Notification rows */
.notif-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.notif-row:last-child {
  border-bottom: none;
}

.notif-info {
  flex: 1;
}

.notif-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--sf-text);
}

.notif-desc {
  font-size: 12px;
  color: #757575;
  margin-top: 2px;
}

/* MQTT status */
.mqtt-status-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.mqtt-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.mqtt-indicator.connected {
  background: #E8F5E9;
  color: #2E7D32;
}

.mqtt-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #2E7D32;
  box-shadow: 0 0 6px rgba(46, 125, 50, 0.5);
  animation: mqttPulse 2s infinite;
}

.mqtt-broker-text {
  font-size: 13px;
  color: var(--sf-text-secondary);
}

@keyframes mqttPulse {
  0%, 100% {
    box-shadow: 0 0 6px rgba(46, 125, 50, 0.5);
  }
  50% {
    box-shadow: 0 0 12px rgba(46, 125, 50, 0.8);
  }
}
</style>
