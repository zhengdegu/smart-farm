<template>
  <div class="screen3d" @keydown.f11.prevent="toggleFullscreen">
    <!-- ===== 顶部栏 ===== -->
    <header class="top-bar">
      <div class="top-left">
        <span class="clock">{{ clock }}</span>
        <span class="weather">🌤 {{ weather.temp }} {{ weather.desc }}</span>
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
      <div class="stat-card">
        <div class="stat-icon water">💧</div>
        <div class="stat-body">
          <div class="stat-label">今日灌溉量</div>
          <div class="stat-value">{{ summary.irrigationLiters }} <small>L</small></div>
          <div class="stat-trend up">较昨日 +{{ summary.irrigationDelta }}%</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon save">🌊</div>
        <div class="stat-body">
          <div class="stat-label">累计节水率</div>
          <div class="stat-value">{{ summary.waterSavePercent }}<small>%</small></div>
          <div class="stat-trend up">较上月 +{{ summary.waterSaveDelta }}%</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon device">📡</div>
        <div class="stat-body">
          <div class="stat-label">设备在线率</div>
          <div class="stat-value">{{ summary.deviceOnlineRate }}<small>%</small></div>
          <div class="stat-trend up">较昨日 +{{ summary.deviceOnlineDelta }}%</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon alert-icon">⚠️</div>
        <div class="stat-body">
          <div class="stat-label">告警数量</div>
          <div class="stat-value warn-color">{{ summary.alertCount }}</div>
          <div class="stat-trend" :class="summary.alertDelta > 0 ? 'down' : 'up'">较昨日 {{ summary.alertDelta > 0 ? '+' : '' }}{{ summary.alertDelta }}</div>
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

      <!-- 中央3D场景 -->
      <div class="center-col">
        <div class="scene-wrapper" ref="sceneWrapper">
          <canvas ref="canvasRef"></canvas>
          <!-- 传感器浮标 -->
          <div class="sensor-tag" v-for="tag in sensorTags" :key="tag.id"
            :style="{left: tag.x+'%', top: tag.y+'%'}"
            :class="{'warn': tag.warn, 'alarm': tag.alarm}">
            <div class="tag-icon">{{ tag.icon }}</div>
            <div class="tag-name">{{ tag.name }}</div>
            <div class="tag-val">{{ tag.value }}<small>{{ tag.unit }}</small></div>
            <div class="tag-status" v-if="tag.status">{{ tag.status }}</div>
          </div>
          <!-- 3D/视角控制 -->
          <div class="view-controls">
            <button class="ctrl-btn" @click="resetCamera">3D</button>
            <button class="ctrl-btn" @click="topView">3D</button>
            <button class="ctrl-btn" @click="zoomIn">🔍+</button>
            <button class="ctrl-btn" @click="zoomOut">🔍-</button>
          </div>
        </div>

        <!-- L1 告警弹窗（右下角浮动） -->
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
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts/core'
import { LineChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
echarts.use([LineChart, BarChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer])

const screenApi = axios.create({ baseURL: '/api/v1/screen', timeout: 10000 })

// === Refs ===
const canvasRef = ref(null)
const sceneWrapper = ref(null)
const cropChartRef = ref(null)
const timelineChartRef = ref(null)

// === State ===
const clock = ref('')
const weather = reactive({ temp: '26°C', desc: '多云 18~28°C' })
const soundOn = ref(true)
const l2Alert = ref(null)
const l1Alert = ref(null)

const summary = reactive({
  irrigationLiters: 1280, irrigationDelta: 12.5,
  waterSavePercent: 32, waterSaveDelta: 8.3,
  deviceOnlineRate: 96.8, deviceOnlineDelta: 1.2,
  alertCount: 3, alertDelta: 2
})

const envItems = ref([
  { icon: '🌡', label: '空气温度', value: '28.6', unit: '°C', color: '#ef5350' },
  { icon: '💧', label: '空气湿度', value: '68.3', unit: '%', color: '#42a5f5' },
  { icon: '☀️', label: '光照强度', value: '452', unit: 'μmol/m²/s', color: '#ffa726' },
  { icon: '🫧', label: 'CO₂浓度', value: '620', unit: 'ppm', color: '#ab47bc' },
  { icon: '🌿', label: '土壤湿度', value: '32.5', unit: '%', color: '#66bb6a' },
  { icon: '🌡', label: '土壤温度', value: '24.1', unit: '°C', color: '#ff7043' },
  { icon: '🌬', label: '风速', value: '1.2', unit: 'm/s', color: '#78909c' },
  { icon: '🧭', label: '风向', value: '东南风', unit: '', color: '#78909c' },
])

const heatmapZones = ref([
  { id: 1, name: '1号区', temp: 24.6, humidity: 65, bg: 'rgba(33,150,243,0.3)' },
  { id: 2, name: '2号区', temp: 25.3, humidity: 62, bg: 'rgba(33,150,243,0.4)' },
  { id: 3, name: '3号区', temp: 32.8, humidity: 45, bg: 'rgba(244,67,54,0.5)' },
  { id: 4, name: '4号区', temp: 27.1, humidity: 58, bg: 'rgba(76,175,80,0.3)' },
  { id: 5, name: '5号区', temp: 23.7, humidity: 70, bg: 'rgba(33,150,243,0.25)' },
  { id: 6, name: '6号区', temp: 24.2, humidity: 68, bg: 'rgba(33,150,243,0.3)' },
])

const cropHealth = ref(78)
const cropIndex = ref(0.78)
const leafDelta = ref(0.05)
const cropGaugeDash = computed(() => {
  const circ = 2 * Math.PI * 34
  return `${circ * cropHealth.value / 100} ${circ}`
})

const sensorTags = ref([
  { id: 1, icon: '🌡', name: '1号温湿度传感器', value: '28.5', unit: '°C', x: 28, y: 28, status: '运行中' },
  { id: 2, icon: '☀️', name: '光照传感器', value: '452', unit: 'μmol/m²/s', x: 62, y: 22, warn: false },
  { id: 3, icon: '🫧', name: 'CO₂传感器', value: '620', unit: 'ppm', x: 72, y: 30 },
  { id: 4, icon: '🌿', name: '土壤湿度传感器', value: '32.5', unit: '%', x: 40, y: 55 },
  { id: 5, icon: '💧', name: '滴灌管道B区', value: '', unit: '', x: 55, y: 60, status: '运行中' },
  { id: 6, icon: '🚿', name: '灌溉阀门A3', value: '', unit: '', x: 35, y: 72, status: '开启中', warn: false },
  { id: 7, icon: '🔴', name: '3号区域', value: '32.8', unit: '°C', x: 50, y: 40, alarm: true },
])

const deviceList = ref([
  { id: 1, icon: '📡', name: '1号温湿度传感器', status: 'online', statusText: '在线' },
  { id: 2, icon: '📡', name: '2号温湿度传感器', status: 'online', statusText: '在线' },
  { id: 3, icon: '📡', name: '3号温湿度传感器', status: 'alarm', statusText: '告警' },
  { id: 4, icon: '📡', name: '4号温湿度传感器', status: 'online', statusText: '在线' },
  { id: 5, icon: '📡', name: '5号温湿度传感器', status: 'online', statusText: '在线' },
  { id: 6, icon: '🫧', name: 'CO₂传感器', status: 'online', statusText: '在线' },
  { id: 7, icon: '☀️', name: '光照传感器', status: 'online', statusText: '在线' },
  { id: 8, icon: '🚿', name: '灌溉阀门A1', status: 'online', statusText: '在线' },
  { id: 9, icon: '🚿', name: '灌溉阀门A2', status: 'online', statusText: '在线' },
  { id: 10, icon: '🚿', name: '灌溉阀门A3', status: 'online', statusText: '在线' },
  { id: 11, icon: '🚿', name: '灌溉阀门B1', status: 'online', statusText: '在线' },
])
const deviceOnlineCount = computed(() => deviceList.value.filter(d => d.status === 'online').length)
const deviceAlertCount = computed(() => deviceList.value.filter(d => d.status === 'alarm').length)
const deviceFaultCount = computed(() => deviceList.value.filter(d => d.status === 'fault').length)

const controls = ref([
  { icon: '🌀', name: '循环风机', on: true },
  { icon: '💡', name: '补光灯', on: true },
  { icon: '🔦', name: '杀虫灯', on: false },
  { icon: '📷', name: '摄像头1号', on: true },
  { icon: '📷', name: '摄像头2号', on: true },
])

const timelineEvents = ref([
  { time: '12:15', icon: '🚿', text: '灌溉事件\nA区灌溉 12min', color: '#42a5f5', pos: 10 },
  { time: '15:40', icon: '☀️', text: '光照事件\n补光灯开启', color: '#ffa726', pos: 25 },
  { time: '19:32', icon: '⚠️', text: '告警事件\n土壤湿度偏低', color: '#ef5350', pos: 42 },
  { time: '22:10', icon: '🚿', text: '灌溉事件\n灌溉 8min', color: '#42a5f5', pos: 53 },
  { time: '03:45', icon: '📡', text: '设备事件\n风机自动开启', color: '#78909c', pos: 70 },
  { time: '07:20', icon: '⚠️', text: '告警事件\n3号区温度异常', color: '#ef5350', pos: 85 },
  { time: '09:55', icon: '🚿', text: '灌溉事件\nA区灌溉 11min', color: '#42a5f5', pos: 95 },
])

// === Timers ===
let clockTimer = null
let refreshTimer = null
let animId = null

function updateClock() {
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  const days = ['星期日','星期一','星期二','星期三','星期四','星期五','星期六']
  clock.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
  weather.desc = `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())} ${days[now.getDay()]}`
}

function toggleSound() { soundOn.value = !soundOn.value }
function toggleFullscreen() {
  if (!document.fullscreenElement) document.documentElement.requestFullscreen()
  else document.exitFullscreen()
}
function showAlertDetail() {}
function handleL1Alert() { l1Alert.value = null }
function resetCamera() {}
function topView() {}
function zoomIn() {}
function zoomOut() {}

// === Data fetch ===
async function fetchData() {
  try {
    const { data } = await screenApi.get('/data')
    if (data.deviceStats) {
      summary.irrigationLiters = data.deviceStats.irrigationToday || 1280
      summary.alertCount = data.deviceStats.alertCount || 3
    }
    if (data.envData) {
      data.envData.forEach((e, i) => {
        if (envItems.value[i]) {
          envItems.value[i].value = typeof e.value === 'number' ? Math.round(e.value * 10) / 10 : e.value
        }
      })
    }
    if (data.alerts) {
      const l1 = data.alerts.find(a => a.level === 'L1')
      const l2 = data.alerts.find(a => a.level === 'L2')
      if (l1) l1Alert.value = { title: l1.title, detail: l1.content || '请立即检查通风及降温系统!' }
      if (l2) l2Alert.value = { title: l2.title, time: new Date().toLocaleTimeString('zh-CN', {hour12:false}) }
    }
  } catch (e) {
    // 使用默认模拟数据
    l2Alert.value = { title: '土壤湿度偏低，请及时处理!', time: clock.value }
    l1Alert.value = { title: '3号区域温度异常升高', detail: '当前温度: 32.8°C 阈值: 30.0°C\n请立即检查通风及降温系统!' }
  }
}

// === 3D Scene ===
async function init3D() {
  const THREE = await import('three')
  const canvas = canvasRef.value
  const wrapper = sceneWrapper.value
  const w = wrapper.clientWidth
  const h = wrapper.clientHeight
  canvas.width = w; canvas.height = h

  const scene = new THREE.Scene()
  scene.background = new THREE.Color(0x0a1628)
  scene.fog = new THREE.FogExp2(0x0a1628, 0.012)

  const camera = new THREE.PerspectiveCamera(40, w / h, 0.1, 300)
  camera.position.set(35, 28, 45)
  camera.lookAt(0, 0, 0)

  const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true

  // Lights
  const ambient = new THREE.AmbientLight(0x304060, 3)
  scene.add(ambient)
  const dir = new THREE.DirectionalLight(0xffffff, 2)
  dir.position.set(30, 40, 20)
  dir.castShadow = true
  scene.add(dir)
  const pointGreen = new THREE.PointLight(0x4caf50, 2, 60)
  pointGreen.position.set(0, 15, 0)
  scene.add(pointGreen)

  // Ground with grid
  const ground = new THREE.Mesh(
    new THREE.PlaneGeometry(100, 80),
    new THREE.MeshStandardMaterial({ color: 0x0d1f0d, roughness: 0.95 })
  )
  ground.rotation.x = -Math.PI / 2
  ground.receiveShadow = true
  scene.add(ground)

  // Grid helper
  const grid = new THREE.GridHelper(100, 40, 0x1a3a2a, 0x0f2a1a)
  grid.position.y = 0.01
  scene.add(grid)

  // === Greenhouse structure (glass + steel frame) ===
  const ghGroup = new THREE.Group()

  // Steel frame
  const frameMat = new THREE.MeshStandardMaterial({ color: 0x607d8b, metalness: 0.8, roughness: 0.3 })
  // Vertical pillars
  const pillarGeo = new THREE.CylinderGeometry(0.12, 0.12, 8, 8)
  const pillarPositions = [
    [-15,-12],[-15,12],[-8,-12],[-8,12],[0,-12],[0,12],[8,-12],[8,12],[15,-12],[15,12]
  ]
  pillarPositions.forEach(([x,z]) => {
    const p = new THREE.Mesh(pillarGeo, frameMat)
    p.position.set(x, 4, z)
    p.castShadow = true
    ghGroup.add(p)
  })

  // Roof beams (arched)
  const roofCurve = new THREE.CatmullRomCurve3([
    new THREE.Vector3(-12, 8, 0), new THREE.Vector3(-6, 10.5, 0),
    new THREE.Vector3(0, 11, 0), new THREE.Vector3(6, 10.5, 0), new THREE.Vector3(12, 8, 0)
  ])
  const roofTube = new THREE.TubeGeometry(roofCurve, 20, 0.1, 6, false)
  for (let z = -12; z <= 12; z += 4) {
    const beam = new THREE.Mesh(roofTube, frameMat)
    beam.position.z = z
    ghGroup.add(beam)
  }

  // Glass panels
  const glassMat = new THREE.MeshPhysicalMaterial({
    color: 0x88ccff, transparent: true, opacity: 0.15,
    roughness: 0.1, metalness: 0, transmission: 0.6,
    side: THREE.DoubleSide
  })
  // Side walls
  const wallGeo = new THREE.PlaneGeometry(30, 8)
  const wallFront = new THREE.Mesh(wallGeo, glassMat)
  wallFront.position.set(0, 4, 12)
  ghGroup.add(wallFront)
  const wallBack = new THREE.Mesh(wallGeo, glassMat)
  wallBack.position.set(0, 4, -12)
  ghGroup.add(wallBack)
  const sideGeo = new THREE.PlaneGeometry(24, 8)
  const wallLeft = new THREE.Mesh(sideGeo, glassMat)
  wallLeft.rotation.y = Math.PI / 2
  wallLeft.position.set(-15, 4, 0)
  ghGroup.add(wallLeft)
  const wallRight = new THREE.Mesh(sideGeo, glassMat)
  wallRight.rotation.y = Math.PI / 2
  wallRight.position.set(15, 4, 0)
  ghGroup.add(wallRight)

  // Roof glass
  const roofShape = []
  for (let i = 0; i <= 20; i++) {
    const t = i / 20
    const pt = roofCurve.getPoint(t)
    roofShape.push(new THREE.Vector2(pt.x, pt.y))
  }
  const roofGlassGeo = new THREE.PlaneGeometry(30, 24)
  const roofGlass = new THREE.Mesh(roofGlassGeo, glassMat)
  roofGlass.rotation.x = -Math.PI / 2.2
  roofGlass.position.set(0, 10, 0)
  ghGroup.add(roofGlass)

  scene.add(ghGroup)

  // === Plants (rows of green boxes) ===
  const plantMat = new THREE.MeshStandardMaterial({ color: 0x2e7d32 })
  for (let row = -10; row <= 10; row += 3) {
    for (let col = -10; col <= 10; col += 2.5) {
      const h = 1.5 + Math.random() * 2
      const plant = new THREE.Mesh(
        new THREE.BoxGeometry(1.2, h, 1.2),
        new THREE.MeshStandardMaterial({ color: new THREE.Color().setHSL(0.3 + Math.random()*0.1, 0.7, 0.25 + Math.random()*0.15) })
      )
      plant.position.set(col, h/2, row)
      plant.castShadow = true
      scene.add(plant)
    }
  }

  // === Irrigation pipes ===
  const pipeMat = new THREE.MeshStandardMaterial({ color: 0x1565c0, metalness: 0.5, roughness: 0.4 })
  for (let z = -10; z <= 10; z += 5) {
    const pipe = new THREE.Mesh(new THREE.CylinderGeometry(0.08, 0.08, 28, 6), pipeMat)
    pipe.rotation.z = Math.PI / 2
    pipe.position.set(0, 0.3, z)
    scene.add(pipe)
  }

  // === Heatmap overlay (red zone for area 3) ===
  const heatGeo = new THREE.PlaneGeometry(10, 10)
  const heatMat = new THREE.MeshBasicMaterial({
    color: 0xff3333, transparent: true, opacity: 0.2, side: THREE.DoubleSide
  })
  const heatPlane = new THREE.Mesh(heatGeo, heatMat)
  heatPlane.rotation.x = -Math.PI / 2
  heatPlane.position.set(5, 0.1, -3)
  scene.add(heatPlane)

  // Blue heatmap zones
  const blueHeat = new THREE.Mesh(
    new THREE.PlaneGeometry(12, 12),
    new THREE.MeshBasicMaterial({ color: 0x2196f3, transparent: true, opacity: 0.12, side: THREE.DoubleSide })
  )
  blueHeat.rotation.x = -Math.PI / 2
  blueHeat.position.set(-5, 0.1, 3)
  scene.add(blueHeat)

  // === Water particles ===
  const particles = []
  const pGeo = new THREE.SphereGeometry(0.1, 4, 4)
  const pMat = new THREE.MeshBasicMaterial({ color: 0x4fc3f7, transparent: true, opacity: 0.6 })
  for (let i = 0; i < 60; i++) {
    const p = new THREE.Mesh(pGeo, pMat)
    p.userData = { speed: 0.02 + Math.random() * 0.05, t: Math.random(), pipe: Math.floor(Math.random() * 5) }
    scene.add(p)
    particles.push(p)
  }

  // Animation
  let angle = 0.3
  function animate() {
    animId = requestAnimationFrame(animate)
    angle += 0.001
    camera.position.x = 42 * Math.cos(angle)
    camera.position.z = 42 * Math.sin(angle)
    camera.position.y = 25 + Math.sin(angle * 2) * 3
    camera.lookAt(0, 3, 0)

    // Water flow
    particles.forEach(p => {
      p.userData.t += p.userData.speed
      if (p.userData.t > 1) p.userData.t = 0
      const z = -10 + p.userData.pipe * 5
      p.position.set(-14 + p.userData.t * 28, 0.5, z)
      p.material.opacity = 0.3 + Math.sin(p.userData.t * Math.PI) * 0.4
    })

    // Pulse heatmap
    heatMat.opacity = 0.15 + Math.sin(Date.now() * 0.003) * 0.08

    renderer.render(scene, camera)
  }
  animate()
}

// === Charts ===
let cropChart = null
let timelineChart = null

function initCharts() {
  // Crop growth chart
  cropChart = echarts.init(cropChartRef.value)
  const hours = Array.from({length:24}, (_,i) => `${String(i).padStart(2,'0')}:00`)
  cropChart.setOption({
    backgroundColor: 'transparent',
    grid: { top: 8, right: 8, bottom: 20, left: 32 },
    xAxis: { type: 'category', data: hours.filter((_,i)=>i%4===0), axisLine:{lineStyle:{color:'#1a3a2a'}}, axisLabel:{color:'#556',fontSize:9} },
    yAxis: { type: 'value', splitLine:{lineStyle:{color:'#0f2a1a'}}, axisLabel:{color:'#556',fontSize:9} },
    series: [{
      type: 'line', smooth: true, symbol: 'none',
      lineStyle: { color: '#4caf50', width: 2 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(76,175,80,0.3)'},{offset:1,color:'rgba(76,175,80,0)'}]) },
      data: [0.65,0.67,0.68,0.70,0.72,0.75]
    }]
  })

  // Timeline chart
  timelineChart = echarts.init(timelineChartRef.value)
  const tHours = Array.from({length:25}, (_,i) => {
    const h = (10 + i) % 24
    return `${String(h).padStart(2,'0')}:00`
  })
  const genData = (base, amp) => tHours.map((_,i) => +(base + Math.sin(i/3)*amp + (Math.random()-0.5)*amp*0.3).toFixed(1))
  timelineChart.setOption({
    backgroundColor: 'transparent',
    grid: { top: 10, right: 50, bottom: 30, left: 50 },
    xAxis: { type: 'category', data: tHours, axisLine:{lineStyle:{color:'#1a3a2a'}}, axisLabel:{color:'#556',fontSize:10} },
    yAxis: [
      { type:'value', position:'left', min:0, max:50, splitLine:{lineStyle:{color:'#0f2a1a'}}, axisLabel:{color:'#556',fontSize:10} },
      { type:'value', position:'right', min:0, max:100, splitLine:{show:false}, axisLabel:{color:'#556',fontSize:10} }
    ],
    series: [
      { name:'温度', type:'line', smooth:true, symbol:'none', lineStyle:{color:'#ef5350',width:1.5}, data: genData(26,4) },
      { name:'湿度', type:'line', smooth:true, symbol:'none', yAxisIndex:1, lineStyle:{color:'#42a5f5',width:1.5}, data: genData(65,10) },
      { name:'土壤湿度', type:'line', smooth:true, symbol:'none', yAxisIndex:1, lineStyle:{color:'#66bb6a',width:1.5}, data: genData(35,8) },
      { name:'光照', type:'line', smooth:true, symbol:'none', lineStyle:{color:'#ffa726',width:1.5}, data: genData(20,15) },
    ]
  })
}

// === Lifecycle ===
onMounted(async () => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  await init3D()
  initCharts()
  await fetchData()
  refreshTimer = setInterval(fetchData, 15000)
})

onUnmounted(() => {
  clearInterval(clockTimer)
  clearInterval(refreshTimer)
  cancelAnimationFrame(animId)
  cropChart?.dispose()
  timelineChart?.dispose()
})
</script>

<style scoped>
/* ===== 全局 ===== */
.screen3d { width:1920px; height:1080px; background:#060e1a; color:#c8d8e8; font-family:'Microsoft YaHei','PingFang SC',sans-serif; display:flex; flex-direction:column; overflow:hidden; position:relative; transform-origin:0 0; }
@media (max-width:1920px) { .screen3d { transform:scale(calc(100vw / 1920)); height:calc(1080px * (100vw / 1920)); } }

/* ===== 顶部栏 ===== */
.top-bar { height:48px; display:flex; align-items:center; justify-content:space-between; padding:0 24px; background:linear-gradient(180deg,rgba(10,22,40,0.95),rgba(6,14,26,0.8)); border-bottom:1px solid rgba(46,125,50,0.25); flex-shrink:0; }
.top-left { display:flex; align-items:center; gap:16px; font-size:13px; }
.clock { color:#81c784; font-family:monospace; font-size:14px; letter-spacing:1px; }
.weather { color:#78909c; }
.top-center { text-align:center; }
.main-title { margin:0; font-size:24px; font-weight:700; letter-spacing:6px; background:linear-gradient(90deg,#4caf50,#81c784,#4caf50); -webkit-background-clip:text; -webkit-text-fill-color:transparent; text-shadow:0 0 30px rgba(76,175,80,0.3); }
.sub-title { margin:2px 0 0; font-size:11px; color:#546e7a; letter-spacing:3px; }
.top-right { display:flex; align-items:center; gap:12px; }
.icon-btn { background:rgba(255,255,255,0.06); border:1px solid rgba(76,175,80,0.2); color:#81c784; padding:4px 12px; border-radius:4px; cursor:pointer; font-size:12px; }
.icon-btn:hover { background:rgba(76,175,80,0.15); }
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
.stat-card { flex:1; background:rgba(10,30,50,0.7); border:1px solid rgba(76,175,80,0.15); border-radius:8px; display:flex; align-items:center; gap:12px; padding:0 16px; }
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
.panel { background:rgba(8,24,48,0.85); border:1px solid rgba(76,175,80,0.12); border-radius:6px; padding:10px 12px; }
.panel-head { font-size:13px; font-weight:600; color:#81c784; margin-bottom:8px; padding-bottom:6px; border-bottom:1px solid rgba(76,175,80,0.1); display:flex; align-items:center; gap:8px; }
.dot { width:6px; height:6px; border-radius:50%; display:inline-block; }
.dot.green { background:#4caf50; box-shadow:0 0 6px #4caf50; }
.head-tag { font-size:10px; color:#546e7a; margin-left:auto; }

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

/* ===== 中央3D场景 ===== */
.scene-wrapper { width:100%; height:100%; position:relative; border-radius:6px; overflow:hidden; border:1px solid rgba(76,175,80,0.1); }
.scene-wrapper canvas { width:100%!important; height:100%!important; display:block; }

/* 传感器浮标 */
.sensor-tag { position:absolute; background:rgba(0,0,0,0.7); border:1px solid rgba(76,175,80,0.4); border-radius:6px; padding:4px 8px; font-size:10px; pointer-events:none; backdrop-filter:blur(4px); min-width:80px; }
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
.view-controls { position:absolute; bottom:12px; left:50%; transform:translateX(-50%); display:flex; gap:6px; }
.ctrl-btn { background:rgba(0,0,0,0.6); border:1px solid rgba(76,175,80,0.3); color:#81c784; width:32px; height:32px; border-radius:4px; cursor:pointer; font-size:11px; display:flex; align-items:center; justify-content:center; }
.ctrl-btn:hover { background:rgba(76,175,80,0.2); }

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
.dtab { padding:2px 6px; border-radius:3px; color:#546e7a; cursor:pointer; }
.dtab.active { background:rgba(76,175,80,0.15); color:#81c784; }
.device-list { flex:1; overflow-y:auto; display:flex; flex-direction:column; gap:3px; }
.device-row { display:flex; align-items:center; gap:8px; padding:5px 8px; border-radius:4px; font-size:12px; }
.device-row:hover { background:rgba(255,255,255,0.03); }
.dev-icon { font-size:14px; }
.dev-name { flex:1; color:#b0bec5; }
.dev-status-dot { width:6px; height:6px; border-radius:50%; }
.dev-status-dot.online { background:#4caf50; box-shadow:0 0 4px #4caf50; }
.dev-status-dot.alarm { background:#ef5350; box-shadow:0 0 4px #ef5350; animation:dotBlink 1s infinite; }
.dev-status-dot.fault { background:#78909c; }
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

/* ===== 底部时间轴 ===== */
.timeline-section { height:160px; background:rgba(8,24,48,0.9); border-top:1px solid rgba(76,175,80,0.12); padding:6px 24px 10px; flex-shrink:0; display:flex; flex-direction:column; }
.timeline-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:4px; }
.tl-title { font-size:13px; color:#81c784; font-weight:600; }
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
