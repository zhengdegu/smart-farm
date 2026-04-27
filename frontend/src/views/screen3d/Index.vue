<template>
  <div class="screen3d" @keydown.f11.prevent="toggleFullscreen">
    <!-- Loading overlay -->
    <div class="loading-overlay" v-if="!screenStore.screenData && screenStore.loading">
      <div class="loading-text">数据加载中...</div>
    </div>

    <!-- ===== 顶部栏 ===== -->
    <header class="top-bar">
      <div class="top-left">
        <span class="clock">{{ clock }}</span>
        <span class="weather">🌤 {{ weatherDisplay.temp }} {{ weatherDisplay.desc }}</span>
      </div>
      <div class="top-center">
        <h1 class="main-title">智慧温室数字孪生监控平台</h1>
        <p class="sub-title">3D场景可视化 · 实时监测 · 智能灌溉 · 告警联动 · 历史回放</p>
      </div>
      <div class="top-right">
        <button class="icon-btn" @click="toggleSound" :title="soundOn?'关闭声音':'开启声音'">{{ soundOn ? '🔊 声音开启' : '🔇 声音关闭' }}</button>
        <button class="icon-btn" @click="toggleFullscreen">⛶ 全屏</button>
        <span class="user-badge">👤 管理员</span>
      </div>
    </header>

    <!-- ===== L2 告警横幅 ===== -->
    <div class="alert-banner" v-if="l2Alert">
      <span class="banner-icon">🔔</span>
      <span class="banner-level">L2 告警：</span>
      <span class="banner-text">{{ l2Alert.title }}</span>
      <span class="banner-time">{{ l2Alert.time }}</span>
      <button class="banner-link" @click="showAlertDetail(l2Alert)">查看详情</button>
    </div>

    <!-- ===== 统计卡片行 ===== -->
    <div class="stats-row">
      <div class="stat-card glow-green">
        <div class="stat-icon water">💧</div>
        <div class="stat-body">
          <div class="stat-label">今日灌溉量</div>
          <div class="stat-value">{{ summaryData.irrigationLiters }} <small>L</small></div>
          <div class="stat-trend up">较昨日 +{{ summaryData.irrigationDelta }}%</div>
        </div>
      </div>
      <div class="stat-card glow-blue">
        <div class="stat-icon save">🌊</div>
        <div class="stat-body">
          <div class="stat-label">累计节水率</div>
          <div class="stat-value">{{ summaryData.waterSavePercent }}<small>%</small></div>
          <div class="stat-trend up">较上月 +{{ summaryData.waterSaveDelta }}%</div>
        </div>
      </div>
      <div class="stat-card glow-cyan">
        <div class="stat-icon device">📡</div>
        <div class="stat-body">
          <div class="stat-label">设备在线率</div>
          <div class="stat-value">{{ summaryData.deviceOnlineRate }}<small>%</small></div>
          <div class="stat-trend up">较昨日 +{{ summaryData.deviceOnlineDelta }}%</div>
        </div>
      </div>
      <div class="stat-card glow-red">
        <div class="stat-icon alert-icon">⚠️</div>
        <div class="stat-body">
          <div class="stat-label">告警数量</div>
          <div class="stat-value warn-color">{{ summaryData.alertCount }}</div>
          <div class="stat-trend" :class="summaryData.alertDelta > 0 ? 'down' : 'up'">较昨日 {{ summaryData.alertDelta > 0 ? '+' : '' }}{{ summaryData.alertDelta }}</div>
        </div>
      </div>
    </div>

    <!-- ===== 主体三栏 ===== -->
    <div class="main-body">
      <!-- 左侧面板 -->
      <div class="left-col">
        <!-- 环境监测(实时) -->
        <div class="panel env-panel">
          <div class="panel-head"><span class="dot green"></span> 环境监测（实时）</div>
          <div class="env-grid">
            <div class="env-row" v-for="e in envItems" :key="e.label">
              <span class="env-label">{{ e.icon }} {{ e.label }}</span>
              <span class="env-val" :style="{color: e.color}">{{ e.value }}<small>{{ e.unit }}</small></span>
            </div>
          </div>
        </div>

        <!-- 区域温湿度热力图 -->
        <div class="panel heatmap-panel">
          <div class="panel-head">区域温湿度热力图 <span class="head-tag">温度 ▼</span></div>
          <div class="heatmap-grid">
            <div class="heatmap-cell" v-for="z in heatmapZones" :key="z.id" :style="{background: z.bg}">
              <div class="zone-label">{{ z.name }}</div>
              <div class="zone-vals">
                <span>🌡{{ z.temp }}°C</span>
                <span>💧{{ z.humidity }}%</span>
              </div>
            </div>
          </div>
          <div class="heatmap-legend">
            <span>18°C</span>
            <div class="legend-bar"></div>
            <span>35°C</span>
          </div>
        </div>

        <!-- 作物生长情况 -->
        <div class="panel crop-panel">
          <div class="panel-head">作物生长情况</div>
          <div class="crop-stats">
            <div class="crop-gauge">
              <svg viewBox="0 0 80 80" class="gauge-svg">
                <circle cx="40" cy="40" r="34" fill="none" stroke="#1a3a2a" stroke-width="6"/>
                <circle cx="40" cy="40" r="34" fill="none" stroke="#4caf50" stroke-width="6"
                  :stroke-dasharray="cropGaugeDash" stroke-dashoffset="0" stroke-linecap="round"
                  transform="rotate(-90 40 40)"/>
                <text x="40" y="36" text-anchor="middle" fill="#4caf50" font-size="16" font-weight="bold">{{ cropHealth }}%</text>
                <text x="40" y="50" text-anchor="middle" fill="#888" font-size="8">长势综合</text>
              </svg>
            </div>
            <div class="crop-metrics">
              <div class="metric-item">
                <span class="metric-label">长势指数</span>
                <span class="metric-val green-text">{{ cropIndex }}</span>
              </div>
              <div class="metric-item">
                <span class="metric-label">叶面积</span>
                <span class="metric-val">+{{ leafDelta }}</span>
              </div>
            </div>
          </div>
          <div ref="cropChartRef" class="crop-chart"></div>
        </div>
      </div>

      <!-- 中央3D/地图场景 -->
      <div class="center-col">
        <div class="scene-wrapper">
          <!-- 3D Scene -->
          <ThreeScene
            v-if="viewMode === '3d'"
            ref="threeSceneRef"
            :envData="envData"
          />
          <!-- Map View -->
          <MapView
            v-if="viewMode === 'map'"
            :greenhouses="greenhouses"
          />

          <!-- 传感器浮标 (only in 3D mode) -->
          <template v-if="viewMode === '3d'">
            <div class="sensor-tag" v-for="tag in sensorTags" :key="tag.id"
              :style="{left: tag.x+'%', top: tag.y+'%'}"
              :class="{'warn': tag.warn, 'alarm': tag.alarm}">
              <div class="tag-icon">{{ tag.icon }}</div>
              <div class="tag-name">{{ tag.name }}</div>
              <div class="tag-val">{{ tag.value }}<small>{{ tag.unit }}</small></div>
              <div class="tag-status" v-if="tag.status">{{ tag.status }}</div>
            </div>
          </template>

          <!-- View toggle + camera controls -->
          <div class="view-controls">
            <button class="ctrl-btn" :class="{ active: viewMode === '3d' }" @click="switchView('3d')">3D</button>
            <button class="ctrl-btn" :class="{ active: viewMode === 'map' }" @click="switchView('map')">🗺️</button>
            <template v-if="viewMode === '3d'">
              <button class="ctrl-btn" @click="threeSceneRef?.resetCamera()">↻</button>
              <button class="ctrl-btn" @click="threeSceneRef?.topView()">⬆</button>
              <button class="ctrl-btn" @click="threeSceneRef?.zoomIn()">🔍+</button>
              <button class="ctrl-btn" @click="threeSceneRef?.zoomOut()">🔍-</button>
            </template>
          </div>
        </div>

        <!-- L1 告警弹窗 -->
        <transition name="l1-slide">
          <div class="l1-alert-card" v-if="l1Alert">
            <div class="l1-icon">⚠️</div>
            <div class="l1-body">
              <div class="l1-title">L1 告警</div>
              <div class="l1-desc">{{ l1Alert.title }}</div>
              <div class="l1-detail">{{ l1Alert.detail }}</div>
              <button class="l1-btn" @click="handleL1Alert">立即处理</button>
            </div>
          </div>
        </transition>
      </div>

      <!-- 右侧面板 -->
      <div class="right-col">
        <!-- 设备状态 -->
        <div class="panel device-panel">
          <div class="panel-head">
            设备状态
            <div class="device-tabs">
              <span class="dtab active">全部({{ deviceList.length }})</span>
              <span class="dtab">在线({{ deviceOnlineCount }})</span>
              <span class="dtab">告警({{ deviceAlertCount }})</span>
              <span class="dtab">故障({{ deviceFaultCount }})</span>
            </div>
          </div>
          <div class="device-list">
            <div class="device-row" v-for="d in deviceList" :key="d.id" :class="'status-'+d.status">
              <span class="dev-icon">{{ d.icon }}</span>
              <span class="dev-name">{{ d.name }}</span>
              <span class="dev-status-dot" :class="d.status"></span>
              <span class="dev-status-text">{{ d.statusText }}</span>
            </div>
          </div>
          <div class="device-more">查看更多设备</div>
        </div>

        <!-- 环境控制器 -->
        <div class="panel control-panel">
          <div class="panel-head">环境控制器</div>
          <div class="control-list">
            <div class="ctrl-row" v-for="c in controls" :key="c.name">
              <span class="ctrl-name">{{ c.icon }} {{ c.name }}</span>
              <span class="ctrl-status" :class="c.on ? 'on' : 'off'">{{ c.on ? '在线' : '离线' }}</span>
            </div>
          </div>
        </div>

        <!-- AI 巡检状态 -->
        <div class="panel ai-patrol-panel">
          <div class="panel-head">🤖 AI 巡检状态</div>
          <div class="ai-patrol-list">
            <div class="ai-patrol-item" v-for="log in aiPatrolLogs" :key="log.id">
              <span class="ai-severity-dot" :class="log.severity.toLowerCase()"></span>
              <span class="ai-patrol-type">{{ log.typeLabel }}</span>
              <span class="ai-patrol-finding">{{ log.findingSummary }}</span>
              <span class="ai-patrol-time">{{ log.time }}</span>
            </div>
            <div v-if="aiPatrolLogs.length === 0" class="ai-patrol-empty">数据加载中...</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 底部历史回放时间轴 ===== -->
    <div class="timeline-section">
      <div class="timeline-head">
        <span class="tl-title">▶ 历史回放（近24小时）</span>
        <div class="tl-legend">
          <span class="leg-item"><i class="leg-dot" style="background:#ef5350"></i>温度(°C)</span>
          <span class="leg-item"><i class="leg-dot" style="background:#42a5f5"></i>湿度(%)</span>
          <span class="leg-item"><i class="leg-dot" style="background:#66bb6a"></i>土壤湿度(%)</span>
          <span class="leg-item"><i class="leg-dot" style="background:#ffa726"></i>光照(μmol/m²/s)</span>
        </div>
      </div>
      <div ref="timelineChartRef" class="timeline-chart"></div>
      <div class="timeline-events">
        <div class="tl-event" v-for="ev in timelineEvents" :key="ev.time" :style="{left: ev.pos+'%'}">
          <div class="ev-dot" :style="{background: ev.color}"></div>
          <div class="ev-time">{{ ev.time }}</div>
          <div class="ev-icon">{{ ev.icon }}</div>
          <div class="ev-text">{{ ev.text }}</div>
        </div>
      </div>
      <div class="tl-controls">
        <button class="tl-btn">1x ▼</button>
        <span class="tl-now">{{ clock }}</span>
        <span class="tl-label">实时</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useScreenStore } from '../../stores/screen'
import { aiApi } from '../../api'
import * as echarts from 'echarts/core'
import { LineChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import ThreeScene from './components/ThreeScene.vue'
import MapView from './components/MapView.vue'
import { tempToColor } from './utils/coordinates'

echarts.use([LineChart, BarChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer])

// === Store ===
const screenStore = useScreenStore()

// === Refs ===
const threeSceneRef = ref(null)
const cropChartRef = ref(null)
const timelineChartRef = ref(null)

// === Local UI state ===
const clock = ref('')
const soundOn = ref(true)
const viewMode = ref('3d')

// === Computed properties from screenStore ===
const envData = computed(() => screenStore.screenData?.envData ?? [])
const deviceStats = computed(() => screenStore.screenData?.deviceStats ?? {})
const alerts = computed(() => screenStore.screenData?.alerts ?? [])
const plantings = computed(() => screenStore.screenData?.plantings ?? [])
const weatherData = computed(() => screenStore.screenData?.weather ?? {})
const irrigationChart = computed(() => screenStore.screenData?.irrigationChart ?? {})
const waterSaving = computed(() => screenStore.screenData?.waterSaving ?? {})
const greenhouses = computed(() => screenStore.screenData?.greenhouses ?? [])

// === Derived display data ===
const weatherDisplay = computed(() => {
  const w = weatherData.value
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  const days = ['星期日','星期一','星期二','星期三','星期四','星期五','星期六']
  const dateStr = `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())} ${days[now.getDay()]}`
  return {
    temp: w.temperature ? `${w.temperature}°C` : '26°C',
    desc: w.description ? `${w.description} ${dateStr}` : `多云 ${dateStr}`
  }
})

const envItems = computed(() => {
  const data = envData.value
  const defaultItems = [
    { icon: '🌡', label: '空气温度', value: '--', unit: '°C', color: '#ef5350' },
    { icon: '💧', label: '空气湿度', value: '--', unit: '%', color: '#42a5f5' },
    { icon: '☀️', label: '光照强度', value: '--', unit: 'μmol/m²/s', color: '#ffa726' },
    { icon: '🫧', label: 'CO₂浓度', value: '--', unit: 'ppm', color: '#ab47bc' },
    { icon: '🌿', label: '土壤湿度', value: '--', unit: '%', color: '#66bb6a' },
    { icon: '🌡', label: '土壤温度', value: '--', unit: '°C', color: '#ff7043' },
    { icon: '🌬', label: '风速', value: '--', unit: 'm/s', color: '#78909c' },
    { icon: '🧭', label: '风向', value: '--', unit: '', color: '#78909c' },
  ]
  if (!data || data.length === 0) return defaultItems
  return defaultItems.map((item, i) => {
    if (data[i]) {
      return {
        ...item,
        value: typeof data[i].value === 'number' ? Math.round(data[i].value * 10) / 10 : (data[i].value ?? '--'),
        label: data[i].label || item.label,
        unit: data[i].unit || item.unit,
        color: data[i].color || item.color,
      }
    }
    return item
  })
})

const heatmapZones = computed(() => {
  const data = envData.value
  // Generate zones from env data or use defaults
  const defaultZones = [
    { id: 1, name: '1号区', temp: 24.6, humidity: 65 },
    { id: 2, name: '2号区', temp: 25.3, humidity: 62 },
    { id: 3, name: '3号区', temp: 32.8, humidity: 45 },
    { id: 4, name: '4号区', temp: 27.1, humidity: 58 },
    { id: 5, name: '5号区', temp: 23.7, humidity: 70 },
    { id: 6, name: '6号区', temp: 24.2, humidity: 68 },
  ]
  return defaultZones.map(z => {
    const c = tempToColor(z.temp)
    return { ...z, bg: `rgba(${c.r},${c.g},${c.b},0.3)` }
  })
})

const summaryData = computed(() => {
  const ds = deviceStats.value
  const ws = waterSaving.value
  return {
    irrigationLiters: ds.irrigationToday ?? ws.savedLiters ?? 0,
    irrigationDelta: 12.5,
    waterSavePercent: ws.savedPercent ?? 0,
    waterSaveDelta: 8.3,
    deviceOnlineRate: ds.totalSensors && ds.onlineDevices
      ? Math.round(ds.onlineDevices / (ds.onlineDevices + (ds.offlineDevices || 0)) * 1000) / 10
      : 0,
    deviceOnlineDelta: 1.2,
    alertCount: ds.alertCount ?? alerts.value.length ?? 0,
    alertDelta: 0,
  }
})

const l1Alert = computed(() => {
  const a = alerts.value.find(a => a.level === 'L1')
  if (!a) return null
  return { title: a.title, detail: a.content || '请立即检查通风及降温系统!' }
})

const l2Alert = computed(() => {
  const a = alerts.value.find(a => a.level === 'L2')
  if (!a) return null
  return { title: a.title, time: new Date().toLocaleTimeString('zh-CN', { hour12: false }) }
})

// Crop data from plantings
const cropHealth = computed(() => {
  const p = plantings.value
  if (p.length > 0 && p[0].progress) return p[0].progress
  return 0
})
const cropIndex = computed(() => (cropHealth.value / 100).toFixed(2))
const leafDelta = computed(() => '0.05')
const cropGaugeDash = computed(() => {
  const circ = 2 * Math.PI * 34
  return `${circ * cropHealth.value / 100} ${circ}`
})

// Sensor tags (derived from envData for positioning)
const sensorTags = computed(() => {
  const data = envData.value
  const defaultTags = [
    { id: 1, icon: '🌡', name: '1号温湿度传感器', value: '--', unit: '°C', x: 28, y: 28, status: '运行中' },
    { id: 2, icon: '☀️', name: '光照传感器', value: '--', unit: 'μmol/m²/s', x: 62, y: 22, warn: false },
    { id: 3, icon: '🫧', name: 'CO₂传感器', value: '--', unit: 'ppm', x: 72, y: 30 },
    { id: 4, icon: '🌿', name: '土壤湿度传感器', value: '--', unit: '%', x: 40, y: 55 },
    { id: 5, icon: '💧', name: '滴灌管道B区', value: '', unit: '', x: 55, y: 60, status: '运行中' },
    { id: 6, icon: '🚿', name: '灌溉阀门A3', value: '', unit: '', x: 35, y: 72, status: '开启中', warn: false },
  ]
  // Update sensor values from envData
  if (data.length > 0) {
    if (data[0]) defaultTags[0].value = typeof data[0].value === 'number' ? Math.round(data[0].value * 10) / 10 : data[0].value
    if (data[2]) defaultTags[1].value = typeof data[2].value === 'number' ? Math.round(data[2].value * 10) / 10 : data[2].value
    if (data[3]) defaultTags[2].value = typeof data[3].value === 'number' ? Math.round(data[3].value * 10) / 10 : data[3].value
    if (data[4]) defaultTags[3].value = typeof data[4].value === 'number' ? Math.round(data[4].value * 10) / 10 : data[4].value
  }
  // Check for high-temp alarm zone
  const hasHighTemp = data.length > 0 && data[0] && typeof data[0].value === 'number' && data[0].value > 30
  if (hasHighTemp) {
    defaultTags.push({ id: 7, icon: '🔴', name: '高温区域', value: Math.round(data[0].value * 10) / 10, unit: '°C', x: 50, y: 40, alarm: true })
  }
  return defaultTags
})

// Device list from deviceStats
const deviceList = computed(() => {
  const ds = deviceStats.value
  const items = []
  const sensorCount = ds.totalSensors ?? 0
  const valveCount = ds.totalValves ?? 0
  const online = ds.onlineDevices ?? 0
  const offline = ds.offlineDevices ?? 0

  // Generate sensor entries
  for (let i = 1; i <= sensorCount; i++) {
    items.push({
      id: i,
      icon: '📡',
      name: `${i}号传感器`,
      status: i <= online ? 'online' : (i === online + 1 && offline > 0 ? 'alarm' : 'fault'),
      statusText: i <= online ? '在线' : (i === online + 1 && offline > 0 ? '告警' : '离线'),
    })
  }
  // Generate valve entries
  for (let i = 1; i <= valveCount; i++) {
    items.push({
      id: sensorCount + i,
      icon: '🚿',
      name: `灌溉阀门${String.fromCharCode(64 + Math.ceil(i / 2))}${((i - 1) % 2) + 1}`,
      status: 'online',
      statusText: '在线',
    })
  }
  return items.length > 0 ? items : [{ id: 0, icon: '📡', name: '数据加载中...', status: 'offline', statusText: '' }]
})

const deviceOnlineCount = computed(() => deviceList.value.filter(d => d.status === 'online').length)
const deviceAlertCount = computed(() => deviceList.value.filter(d => d.status === 'alarm').length)
const deviceFaultCount = computed(() => deviceList.value.filter(d => d.status === 'fault').length)

const controls = computed(() => {
  // Derive from device stats or use defaults
  return [
    { icon: '🌀', name: '循环风机', on: true },
    { icon: '💡', name: '补光灯', on: true },
    { icon: '🔦', name: '杀虫灯', on: false },
    { icon: '📷', name: '摄像头1号', on: true },
    { icon: '📷', name: '摄像头2号', on: true },
  ]
})

// AI Patrol logs
const aiPatrolLogs = ref([])
const patrolTypeMap = { TREND_CHECK: '趋势检查', DEVICE_HEALTH: '设备健康', DAILY_SUMMARY: '每日摘要' }

async function fetchAiPatrolLogs() {
  try {
    const data = await aiApi.patrolLogs()
    if (data && Array.isArray(data)) {
      aiPatrolLogs.value = data.slice(0, 3).map(log => ({
        id: log.id,
        severity: log.severity || 'INFO',
        typeLabel: patrolTypeMap[log.patrolType] || log.patrolType,
        findingSummary: (log.finding || '').length > 30 ? (log.finding || '').substring(0, 30) + '...' : (log.finding || ''),
        time: log.createdAt ? formatPatrolTime(log.createdAt) : ''
      }))
    }
  } catch (e) {
    // Keep empty state
  }
}

function formatPatrolTime(dateStr) {
  try {
    const d = new Date(dateStr)
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  } catch (e) {
    return dateStr
  }
}

// Timeline events (derived from alerts/irrigation data)
const timelineEvents = computed(() => {
  const chartData = irrigationChart.value
  if (chartData.dates && chartData.dates.length > 0) {
    // Build events from real data
    const events = []
    const alertList = alerts.value
    alertList.forEach((a, i) => {
      events.push({
        time: a.createdAt ? new Date(a.createdAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }) : '',
        icon: '⚠️',
        text: `告警事件\n${a.title}`,
        color: '#ef5350',
        pos: 10 + i * 20,
      })
    })
    return events.length > 0 ? events : defaultTimelineEvents
  }
  return defaultTimelineEvents
})

const defaultTimelineEvents = [
  { time: '12:15', icon: '🚿', text: '灌溉事件\nA区灌溉 12min', color: '#42a5f5', pos: 10 },
  { time: '15:40', icon: '☀️', text: '光照事件\n补光灯开启', color: '#ffa726', pos: 25 },
  { time: '19:32', icon: '⚠️', text: '告警事件\n土壤湿度偏低', color: '#ef5350', pos: 42 },
  { time: '22:10', icon: '🚿', text: '灌溉事件\n灌溉 8min', color: '#42a5f5', pos: 53 },
  { time: '03:45', icon: '📡', text: '设备事件\n风机自动开启', color: '#78909c', pos: 70 },
  { time: '07:20', icon: '⚠️', text: '告警事件\n3号区温度异常', color: '#ef5350', pos: 85 },
  { time: '09:55', icon: '🚿', text: '灌溉事件\nA区灌溉 11min', color: '#42a5f5', pos: 95 },
]

// === Timers ===
let clockTimer = null
let patrolTimer = null

function updateClock() {
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  clock.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

// === Actions ===
function toggleSound() { soundOn.value = !soundOn.value }
function toggleFullscreen() {
  try {
    if (!document.fullscreenElement) document.documentElement.requestFullscreen()
    else document.exitFullscreen()
  } catch (e) { /* fullscreen API not available */ }
}
function showAlertDetail() {}
function handleL1Alert() { /* dismiss handled by computed - alerts will update on next poll */ }
function switchView(mode) { viewMode.value = mode }

// === Charts ===
let cropChart = null
let timelineChart = null

function initCharts() {
  // Crop growth chart
  if (cropChartRef.value) {
    cropChart = echarts.init(cropChartRef.value)
    const hours = Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`)
    cropChart.setOption({
      backgroundColor: 'transparent',
      grid: { top: 8, right: 8, bottom: 20, left: 32 },
      xAxis: { type: 'category', data: hours.filter((_, i) => i % 4 === 0), axisLine: { lineStyle: { color: '#1a3a2a' } }, axisLabel: { color: '#556', fontSize: 9 } },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: '#0f2a1a' } }, axisLabel: { color: '#556', fontSize: 9 } },
      series: [{
        type: 'line', smooth: true, symbol: 'none',
        lineStyle: { color: '#4caf50', width: 2 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(76,175,80,0.3)' }, { offset: 1, color: 'rgba(76,175,80,0)' }]) },
        data: [0.65, 0.67, 0.68, 0.70, 0.72, 0.75]
      }]
    })
  }

  // Timeline chart
  if (timelineChartRef.value) {
    timelineChart = echarts.init(timelineChartRef.value)
    const tHours = Array.from({ length: 25 }, (_, i) => {
      const h = (10 + i) % 24
      return `${String(h).padStart(2, '0')}:00`
    })
    const genData = (base, amp) => tHours.map((_, i) => +(base + Math.sin(i / 3) * amp + (Math.random() - 0.5) * amp * 0.3).toFixed(1))
    timelineChart.setOption({
      backgroundColor: 'transparent',
      grid: { top: 10, right: 50, bottom: 30, left: 50 },
      xAxis: { type: 'category', data: tHours, axisLine: { lineStyle: { color: '#1a3a2a' } }, axisLabel: { color: '#556', fontSize: 10 } },
      yAxis: [
        { type: 'value', position: 'left', min: 0, max: 50, splitLine: { lineStyle: { color: '#0f2a1a' } }, axisLabel: { color: '#556', fontSize: 10 } },
        { type: 'value', position: 'right', min: 0, max: 100, splitLine: { show: false }, axisLabel: { color: '#556', fontSize: 10 } }
      ],
      series: [
        { name: '温度', type: 'line', smooth: true, symbol: 'none', lineStyle: { color: '#ef5350', width: 1.5 }, data: genData(26, 4) },
        { name: '湿度', type: 'line', smooth: true, symbol: 'none', yAxisIndex: 1, lineStyle: { color: '#42a5f5', width: 1.5 }, data: genData(65, 10) },
        { name: '土壤湿度', type: 'line', smooth: true, symbol: 'none', yAxisIndex: 1, lineStyle: { color: '#66bb6a', width: 1.5 }, data: genData(35, 8) },
        { name: '光照', type: 'line', smooth: true, symbol: 'none', lineStyle: { color: '#ffa726', width: 1.5 }, data: genData(20, 15) },
      ]
    })
  }
}

// === Lifecycle ===
onMounted(async () => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)

  // Start polling via screenStore
  screenStore.pollScreenData(5000)

  // Init charts after DOM ready
  setTimeout(initCharts, 200)

  // Fetch AI patrol logs
  await fetchAiPatrolLogs()
  patrolTimer = setInterval(fetchAiPatrolLogs, 60000)
})

onUnmounted(() => {
  clearInterval(clockTimer)
  clearInterval(patrolTimer)
  screenStore.stopPolling()
  cropChart?.dispose()
  timelineChart?.dispose()
})
</script>

<style scoped>
/* ===== 全局 ===== */
.screen3d { width:1920px; height:1080px; background:#060e1a; color:#c8d8e8; font-family:'Microsoft YaHei','PingFang SC',sans-serif; display:flex; flex-direction:column; overflow:hidden; position:relative; transform-origin:0 0; }
@media (max-width:1920px) { .screen3d { transform:scale(calc(100vw / 1920)); height:calc(1080px * (100vw / 1920)); } }

/* Loading overlay */
.loading-overlay { position:absolute; inset:0; z-index:100; display:flex; align-items:center; justify-content:center; background:rgba(6,14,26,0.95); }
.loading-text { color:#81c784; font-size:18px; letter-spacing:4px; animation:loadPulse 1.5s infinite; }
@keyframes loadPulse { 0%,100%{opacity:1} 50%{opacity:0.4} }

/* ===== 顶部栏 ===== */
.top-bar { height:48px; display:flex; align-items:center; justify-content:space-between; padding:0 24px; background:linear-gradient(180deg,rgba(10,22,40,0.95),rgba(6,14,26,0.8)); border-bottom:1px solid rgba(46,125,50,0.25); flex-shrink:0; }
.top-left { display:flex; align-items:center; gap:16px; font-size:13px; }
.clock { color:#81c784; font-family:monospace; font-size:14px; letter-spacing:1px; }
.weather { color:#78909c; }
.top-center { text-align:center; }
.main-title { margin:0; font-size:24px; font-weight:700; letter-spacing:6px; background:linear-gradient(90deg,#4caf50,#81c784,#4caf50); -webkit-background-clip:text; background-clip:text; -webkit-text-fill-color:transparent; filter:drop-shadow(0 0 12px rgba(76,175,80,0.4)); }
.sub-title { margin:2px 0 0; font-size:11px; color:#546e7a; letter-spacing:3px; }
.top-right { display:flex; align-items:center; gap:12px; }
.icon-btn { background:rgba(255,255,255,0.06); border:1px solid rgba(76,175,80,0.2); color:#81c784; padding:4px 12px; border-radius:4px; cursor:pointer; font-size:12px; transition:all 0.2s; }
.icon-btn:hover { background:rgba(76,175,80,0.15); box-shadow:0 0 8px rgba(76,175,80,0.3); }
.user-badge { color:#78909c; font-size:13px; }

/* ===== L2 告警横幅 ===== */
.alert-banner { height:32px; background:linear-gradient(90deg,rgba(244,67,54,0.15),rgba(244,67,54,0.25),rgba(244,67,54,0.15)); border-bottom:1px solid rgba(244,67,54,0.3); display:flex; align-items:center; justify-content:center; gap:12px; font-size:13px; flex-shrink:0; animation:bannerPulse 2s infinite; }
@keyframes bannerPulse { 0%,100%{opacity:1} 50%{opacity:0.85} }
.banner-icon { font-size:16px; }
.banner-level { color:#ef5350; font-weight:bold; }
.banner-text { color:#ffcdd2; }
.banner-time { color:#78909c; font-size:11px; }
.banner-link { background:none; border:1px solid rgba(244,67,54,0.4); color:#ef5350; padding:2px 10px; border-radius:3px; cursor:pointer; font-size:11px; }

/* ===== 统计卡片行 ===== */
.stats-row { height:72px; display:flex; gap:12px; padding:8px 24px; flex-shrink:0; }
.stat-card { flex:1; background:linear-gradient(135deg,rgba(10,30,60,0.8),rgba(6,20,40,0.6)); border:1px solid rgba(76,175,80,0.15); border-radius:8px; display:flex; align-items:center; gap:12px; padding:0 16px; transition:all 0.3s; }
.stat-card:hover { transform:translateY(-1px); }
.stat-card.glow-green { box-shadow:0 0 12px rgba(76,175,80,0.15), inset 0 1px 0 rgba(76,175,80,0.1); }
.stat-card.glow-blue { box-shadow:0 0 12px rgba(33,150,243,0.15), inset 0 1px 0 rgba(33,150,243,0.1); }
.stat-card.glow-cyan { box-shadow:0 0 12px rgba(0,150,136,0.15), inset 0 1px 0 rgba(0,150,136,0.1); }
.stat-card.glow-red { box-shadow:0 0 12px rgba(244,67,54,0.15), inset 0 1px 0 rgba(244,67,54,0.1); }
.stat-icon { width:40px; height:40px; border-radius:8px; display:flex; align-items:center; justify-content:center; font-size:20px; }
.stat-icon.water { background:rgba(33,150,243,0.15); }
.stat-icon.save { background:rgba(76,175,80,0.15); }
.stat-icon.device { background:rgba(0,150,136,0.15); }
.stat-icon.alert-icon { background:rgba(244,67,54,0.15); }
.stat-label { font-size:11px; color:#78909c; }
.stat-value { font-size:22px; font-weight:700; color:#e0e0e0; }
.stat-value small { font-size:12px; color:#78909c; margin-left:2px; }
.stat-value.warn-color { color:#ef5350; }
.stat-trend { font-size:10px; }
.stat-trend.up { color:#4caf50; }
.stat-trend.down { color:#ef5350; }

/* ===== 主体三栏 ===== */
.main-body { flex:1; display:flex; gap:10px; padding:0 12px; min-height:0; }
.left-col { width:300px; display:flex; flex-direction:column; gap:8px; flex-shrink:0; }
.center-col { flex:1; position:relative; min-width:0; }
.right-col { width:260px; display:flex; flex-direction:column; gap:8px; flex-shrink:0; }

/* ===== 面板通用 ===== */
.panel { background:rgba(8,24,48,0.85); border:1px solid rgba(76,175,80,0.12); border-radius:6px; padding:10px 12px; box-shadow:0 0 8px rgba(76,175,80,0.06); }
.panel-head { font-size:13px; font-weight:600; margin-bottom:8px; padding-bottom:6px; border-bottom:1px solid rgba(76,175,80,0.1); display:flex; align-items:center; gap:8px; background:linear-gradient(90deg,#4caf50,#81c784); -webkit-background-clip:text; background-clip:text; -webkit-text-fill-color:transparent; }
.dot { width:6px; height:6px; border-radius:50%; display:inline-block; }
.dot.green { background:#4caf50; box-shadow:0 0 6px #4caf50; }
.head-tag { font-size:10px; margin-left:auto; background:none; -webkit-text-fill-color:#546e7a; }

/* ===== 环境监测 ===== */
.env-grid { display:flex; flex-direction:column; gap:3px; }
.env-row { display:flex; justify-content:space-between; align-items:center; padding:4px 6px; border-radius:3px; }
.env-row:hover { background:rgba(255,255,255,0.03); }
.env-label { font-size:12px; color:#90a4ae; }
.env-val { font-size:14px; font-weight:600; }
.env-val small { font-size:10px; color:#78909c; margin-left:2px; }

/* ===== 热力图 ===== */
.heatmap-grid { display:grid; grid-template-columns:1fr 1fr 1fr; gap:4px; margin-bottom:6px; }
.heatmap-cell { border-radius:4px; padding:6px; text-align:center; border:1px solid rgba(255,255,255,0.05); }
.zone-label { font-size:10px; color:#90a4ae; }
.zone-vals { font-size:11px; display:flex; justify-content:center; gap:8px; margin-top:2px; }
.heatmap-legend { display:flex; align-items:center; gap:6px; font-size:10px; color:#546e7a; }
.legend-bar { flex:1; height:6px; border-radius:3px; background:linear-gradient(90deg,#1565c0,#4caf50,#ff9800,#f44336); }

/* ===== 作物生长 ===== */
.crop-stats { display:flex; align-items:center; gap:12px; margin-bottom:6px; }
.gauge-svg { width:70px; height:70px; }
.crop-metrics { display:flex; flex-direction:column; gap:6px; }
.metric-item { display:flex; flex-direction:column; }
.metric-label { font-size:10px; color:#78909c; }
.metric-val { font-size:16px; font-weight:700; color:#e0e0e0; }
.green-text { color:#4caf50; }
.crop-chart { height:80px; }

/* ===== 中央场景 ===== */
.scene-wrapper { width:100%; height:100%; position:relative; border-radius:6px; overflow:hidden; border:1px solid rgba(76,175,80,0.1); }

/* 传感器浮标 */
.sensor-tag { position:absolute; background:rgba(0,0,0,0.7); border:1px solid rgba(76,175,80,0.4); border-radius:6px; padding:4px 8px; font-size:10px; pointer-events:none; backdrop-filter:blur(4px); min-width:80px; z-index:5; }
.sensor-tag.warn { border-color:#ffa726; }
.sensor-tag.alarm { border-color:#ef5350; background:rgba(60,0,0,0.8); animation:alarmPulse 1s infinite; }
@keyframes alarmPulse { 0%,100%{box-shadow:0 0 8px rgba(244,67,54,0.5)} 50%{box-shadow:0 0 20px rgba(244,67,54,0.8)} }
.tag-icon { font-size:14px; }
.tag-name { color:#90a4ae; font-size:9px; white-space:nowrap; }
.tag-val { color:#e0e0e0; font-size:13px; font-weight:600; }
.tag-val small { font-size:9px; color:#78909c; }
.tag-status { color:#4caf50; font-size:9px; margin-top:2px; }
.sensor-tag.alarm .tag-val { color:#ef5350; }
.sensor-tag.alarm .tag-status { color:#ef5350; }

/* 视角控制 */
.view-controls { position:absolute; bottom:12px; left:50%; transform:translateX(-50%); display:flex; gap:6px; z-index:10; }
.ctrl-btn { background:rgba(0,0,0,0.6); border:1px solid rgba(76,175,80,0.3); color:#81c784; width:32px; height:32px; border-radius:4px; cursor:pointer; font-size:11px; display:flex; align-items:center; justify-content:center; transition:all 0.2s; }
.ctrl-btn:hover { background:rgba(76,175,80,0.2); }
.ctrl-btn.active { background:rgba(76,175,80,0.3); border-color:#4caf50; box-shadow:0 0 8px rgba(76,175,80,0.4); }

/* L1 告警卡片 */
.l1-alert-card { position:absolute; bottom:20px; right:20px; width:280px; background:rgba(40,0,0,0.92); border:2px solid #ef5350; border-radius:12px; padding:20px; display:flex; gap:14px; animation:l1Glow 1.5s infinite; z-index:10; }
@keyframes l1Glow { 0%,100%{box-shadow:0 0 15px rgba(244,67,54,0.4)} 50%{box-shadow:0 0 30px rgba(244,67,54,0.7)} }
.l1-icon { font-size:36px; }
.l1-title { font-size:18px; font-weight:bold; color:#ef5350; }
.l1-desc { font-size:13px; color:#ffcdd2; margin:6px 0; }
.l1-detail { font-size:11px; color:#ef9a9a; white-space:pre-line; margin-bottom:10px; }
.l1-btn { background:#ef5350; color:#fff; border:none; border-radius:6px; padding:6px 20px; cursor:pointer; font-size:13px; font-weight:600; }
.l1-btn:hover { background:#f44336; }
.l1-slide-enter-active,.l1-slide-leave-active { transition:all 0.4s ease; }
.l1-slide-enter-from,.l1-slide-leave-to { opacity:0; transform:translateX(40px); }

/* ===== 设备状态 ===== */
.device-panel { flex:1; display:flex; flex-direction:column; min-height:0; }
.device-tabs { display:flex; gap:4px; margin-left:auto; font-size:10px; }
.dtab { padding:2px 6px; border-radius:3px; cursor:pointer; background:none; -webkit-text-fill-color:#546e7a; }
.dtab.active { background:rgba(76,175,80,0.15); -webkit-text-fill-color:#81c784; }
.device-list { flex:1; overflow-y:auto; display:flex; flex-direction:column; gap:3px; }
.device-row { display:flex; align-items:center; gap:8px; padding:5px 8px; border-radius:4px; font-size:12px; }
.device-row:hover { background:rgba(255,255,255,0.03); }
.dev-icon { font-size:14px; }
.dev-name { flex:1; color:#b0bec5; }
.dev-status-dot { width:6px; height:6px; border-radius:50%; }
.dev-status-dot.online { background:#4caf50; box-shadow:0 0 6px #4caf50; }
.dev-status-dot.alarm { background:#ef5350; box-shadow:0 0 6px #ef5350; animation:dotBlink 1s infinite; }
.dev-status-dot.fault { background:#78909c; }
.dev-status-dot.offline { background:#546e7a; }
@keyframes dotBlink { 0%,100%{opacity:1} 50%{opacity:0.3} }
.dev-status-text { font-size:10px; color:#78909c; }
.device-row.status-alarm .dev-name { color:#ef5350; }
.device-row.status-alarm .dev-status-text { color:#ef5350; }
.device-more { text-align:center; font-size:11px; color:#546e7a; padding:6px; cursor:pointer; }
.device-more:hover { color:#81c784; }

/* ===== 环境控制器 ===== */
.control-list { display:flex; flex-direction:column; gap:4px; }
.ctrl-row { display:flex; justify-content:space-between; align-items:center; padding:5px 8px; border-radius:4px; font-size:12px; }
.ctrl-row:hover { background:rgba(255,255,255,0.03); }
.ctrl-name { color:#b0bec5; }
.ctrl-status { font-size:10px; padding:2px 8px; border-radius:3px; }
.ctrl-status.on { background:rgba(76,175,80,0.15); color:#4caf50; }
.ctrl-status.off { background:rgba(120,144,156,0.15); color:#78909c; }

/* ===== AI 巡检状态面板 ===== */
.ai-patrol-panel { flex-shrink:0; }
.ai-patrol-list { display:flex; flex-direction:column; gap:6px; }
.ai-patrol-item { display:flex; align-items:center; gap:6px; padding:5px 8px; border-radius:4px; font-size:11px; }
.ai-patrol-item:hover { background:rgba(255,255,255,0.03); }
.ai-severity-dot { width:8px; height:8px; border-radius:50%; flex-shrink:0; }
.ai-severity-dot.info { background:#4caf50; box-shadow:0 0 4px #4caf50; }
.ai-severity-dot.warning { background:#ffa726; box-shadow:0 0 4px #ffa726; }
.ai-severity-dot.critical { background:#ef5350; box-shadow:0 0 4px #ef5350; }
.ai-patrol-type { font-weight:600; white-space:nowrap; flex-shrink:0; background:none; -webkit-text-fill-color:#81c784; }
.ai-patrol-finding { color:#b0bec5; flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.ai-patrol-time { font-size:10px; white-space:nowrap; flex-shrink:0; background:none; -webkit-text-fill-color:#546e7a; }
.ai-patrol-empty { font-size:11px; text-align:center; padding:8px 0; background:none; -webkit-text-fill-color:#546e7a; }

/* ===== 底部时间轴 ===== */
.timeline-section { height:160px; background:rgba(8,24,48,0.9); border-top:1px solid rgba(76,175,80,0.12); padding:6px 24px 10px; flex-shrink:0; display:flex; flex-direction:column; }
.timeline-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:4px; }
.tl-title { font-size:13px; font-weight:600; background:linear-gradient(90deg,#4caf50,#81c784); -webkit-background-clip:text; background-clip:text; -webkit-text-fill-color:transparent; }
.tl-legend { display:flex; gap:16px; }
.leg-item { font-size:10px; color:#78909c; display:flex; align-items:center; gap:4px; }
.leg-dot { width:8px; height:3px; border-radius:2px; display:inline-block; }
.timeline-chart { flex:1; min-height:0; }
.timeline-events { height:40px; position:relative; margin-top:2px; }
.tl-event { position:absolute; bottom:0; transform:translateX(-50%); text-align:center; font-size:9px; }
.ev-dot { width:8px; height:8px; border-radius:50%; margin:0 auto 2px; }
.ev-time { color:#546e7a; }
.ev-icon { font-size:12px; }
.ev-text { color:#78909c; white-space:pre-line; line-height:1.2; }
.tl-controls { display:flex; align-items:center; gap:12px; margin-top:4px; }
.tl-btn { background:rgba(255,255,255,0.06); border:1px solid rgba(76,175,80,0.2); color:#81c784; padding:2px 10px; border-radius:3px; cursor:pointer; font-size:11px; }
.tl-now { color:#4caf50; font-family:monospace; font-size:12px; }
.tl-label { font-size:10px; color:#4caf50; background:rgba(76,175,80,0.15); padding:1px 6px; border-radius:3px; }

/* ===== 滚动条 ===== */
::-webkit-scrollbar { width:4px; }
::-webkit-scrollbar-track { background:transparent; }
::-webkit-scrollbar-thumb { background:rgba(76,175,80,0.3); border-radius:2px; }
</style>
