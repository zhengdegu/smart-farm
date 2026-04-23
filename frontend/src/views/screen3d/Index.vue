<template>
  <div class="screen3d">
    <div class="header-bar">
      <h1>🌱 智慧农业数据大屏</h1>
      <div class="time">{{ currentTime }}</div>
    </div>
    <div class="main-content">
      <div class="left-panel">
        <div class="panel-card">
          <h3>设备概览</h3>
          <div class="stat-grid">
            <div class="stat-item"><span class="num online">{{ stats.onlineDevices }}</span><span class="label">在线设备</span></div>
            <div class="stat-item"><span class="num offline">{{ stats.offlineDevices }}</span><span class="label">离线设备</span></div>
            <div class="stat-item"><span class="num">{{ stats.totalSensors }}</span><span class="label">传感器</span></div>
            <div class="stat-item"><span class="num">{{ stats.totalValves }}</span><span class="label">阀门</span></div>
          </div>
        </div>
        <div class="panel-card">
          <h3>实时环境</h3>
          <div class="env-list">
            <div class="env-item" v-for="e in envData" :key="e.label">
              <span class="env-label">{{ e.label }}</span>
              <span class="env-value" :style="{color: e.color}">{{ e.value }}{{ e.unit }}</span>
            </div>
          </div>
        </div>
        <div class="panel-card">
          <h3>告警信息</h3>
          <div class="alert-list">
            <div v-for="a in alerts" :key="a.id" class="alert-item" :class="'level-'+a.level">
              <span class="alert-level">{{ a.level }}</span>
              <span class="alert-title">{{ a.title }}</span>
            </div>
            <div v-if="!alerts.length" style="color:#666;text-align:center">暂无告警</div>
          </div>
        </div>
      </div>
      <div class="center-3d">
        <canvas ref="canvasRef"></canvas>
        <div class="greenhouse-labels">
          <div v-for="gh in greenhouses" :key="gh.no" class="gh-label" :style="gh.style">
            <span class="gh-no">{{ gh.no }}号棚</span>
            <span class="gh-moisture" :class="{warn: gh.moisture < 40}">💧{{ gh.moisture }}%</span>
          </div>
        </div>
      </div>
      <div class="right-panel">
        <div class="panel-card">
          <h3>灌溉统计</h3>
          <div ref="irrigChart" style="height:200px"></div>
        </div>
        <div class="panel-card">
          <h3>节水效果</h3>
          <div ref="waterChart" style="height:180px"></div>
        </div>
        <div class="panel-card">
          <h3>种植进度</h3>
          <div class="plant-list">
            <div v-for="p in plantings" :key="p.no" class="plant-item">
              <span>{{ p.no }}号棚 · {{ p.crop }}</span>
              <el-progress :percentage="p.progress" :stroke-width="10" :color="p.progress > 80 ? '#67c23a' : '#409eff'" />
              <span class="stage">{{ p.stage }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as THREE from 'three'
import * as echarts from 'echarts'

const canvasRef = ref(null)
const irrigChart = ref(null)
const waterChart = ref(null)
const currentTime = ref('')
let timer = null, animId = null

// 模拟数据
const stats = ref({ onlineDevices: 12, offlineDevices: 2, totalSensors: 10, totalValves: 4 })
const envData = ref([
  { label: '土壤湿度', value: 62, unit: '%', color: '#409eff' },
  { label: '土壤温度', value: 24.5, unit: '°C', color: '#e6a23c' },
  { label: '空气温度', value: 28.3, unit: '°C', color: '#f56c6c' },
  { label: '空气湿度', value: 71, unit: '%', color: '#67c23a' },
])
const alerts = ref([
  { id: 1, level: 'L2', title: '3号棚土壤湿度偏低' },
  { id: 2, level: 'L3', title: '传感器 sensor_003 数据延迟' },
])
const greenhouses = ref([
  { no: '1', moisture: 65, style: { left: '20%', top: '30%' } },
  { no: '2', moisture: 58, style: { left: '50%', top: '25%' } },
  { no: '3', moisture: 35, style: { left: '75%', top: '35%' } },
  { no: '4', moisture: 72, style: { left: '35%', top: '60%' } },
])
const plantings = ref([
  { no: '1', crop: '番茄', stage: '开花结果期', progress: 65 },
  { no: '2', crop: '黄瓜', stage: '营养生长期', progress: 40 },
  { no: '3', crop: '叶菜', stage: '生长期', progress: 55 },
  { no: '4', crop: '番茄', stage: '果实膨大期', progress: 80 },
])

const updateTime = () => {
  currentTime.value = new Date().toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  init3D()
  initCharts()
})

onUnmounted(() => {
  clearInterval(timer)
  cancelAnimationFrame(animId)
})

function init3D() {
  const canvas = canvasRef.value
  const w = canvas.parentElement.clientWidth
  const h = canvas.parentElement.clientHeight - 40
  canvas.width = w
  canvas.height = h

  const scene = new THREE.Scene()
  scene.background = new THREE.Color(0x0a1628)
  scene.fog = new THREE.Fog(0x0a1628, 50, 120)

  const camera = new THREE.PerspectiveCamera(45, w / h, 0.1, 200)
  camera.position.set(30, 25, 40)
  camera.lookAt(0, 0, 0)

  const renderer = new THREE.WebGLRenderer({ canvas, antialias: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))

  // 灯光
  scene.add(new THREE.AmbientLight(0x404060, 2))
  const dirLight = new THREE.DirectionalLight(0xffffff, 3)
  dirLight.position.set(20, 30, 20)
  scene.add(dirLight)

  // 地面
  const ground = new THREE.Mesh(
    new THREE.PlaneGeometry(80, 60),
    new THREE.MeshStandardMaterial({ color: 0x1a3a1a, roughness: 0.9 })
  )
  ground.rotation.x = -Math.PI / 2
  scene.add(ground)

  // 温室大棚（4个）
  const ghPositions = [[-12, 0, -8], [8, 0, -8], [-12, 0, 8], [8, 0, 8]]
  const ghColors = [0x4fc3f7, 0x81c784, 0xffb74d, 0x4fc3f7]
  ghPositions.forEach((pos, i) => {
    // 棚体
    const shape = new THREE.Shape()
    shape.moveTo(-6, 0)
    shape.lineTo(-6, 3)
    shape.quadraticCurveTo(-6, 6, 0, 7)
    shape.quadraticCurveTo(6, 6, 6, 3)
    shape.lineTo(6, 0)
    shape.lineTo(-6, 0)

    const extrudeSettings = { depth: 10, bevelEnabled: false }
    const ghGeo = new THREE.ExtrudeGeometry(shape, extrudeSettings)
    const ghMat = new THREE.MeshStandardMaterial({
      color: ghColors[i], transparent: true, opacity: 0.3, side: THREE.DoubleSide
    })
    const ghMesh = new THREE.Mesh(ghGeo, ghMat)
    ghMesh.position.set(pos[0], pos[1], pos[2] - 5)
    scene.add(ghMesh)

    // 骨架线
    const edges = new THREE.EdgesGeometry(ghGeo)
    const line = new THREE.LineSegments(edges, new THREE.LineBasicMaterial({ color: ghColors[i], opacity: 0.6, transparent: true }))
    line.position.copy(ghMesh.position)
    scene.add(line)
  })

  // 水管网络
  const pipeMat = new THREE.MeshStandardMaterial({ color: 0x2196f3, metalness: 0.6, roughness: 0.3 })
  const pipeGeo = new THREE.CylinderGeometry(0.15, 0.15, 30, 8)
  const pipe1 = new THREE.Mesh(pipeGeo, pipeMat)
  pipe1.rotation.z = Math.PI / 2
  pipe1.position.set(0, 0.2, 0)
  scene.add(pipe1)

  const pipe2 = new THREE.Mesh(new THREE.CylinderGeometry(0.15, 0.15, 20, 8), pipeMat)
  pipe2.position.set(-12, 0.2, 0)
  scene.add(pipe2)

  const pipe3 = new THREE.Mesh(new THREE.CylinderGeometry(0.15, 0.15, 20, 8), pipeMat)
  pipe3.position.set(8, 0.2, 0)
  scene.add(pipe3)

  // 动画
  let angle = 0
  function animate() {
    animId = requestAnimationFrame(animate)
    angle += 0.002
    camera.position.x = 40 * Math.cos(angle)
    camera.position.z = 40 * Math.sin(angle)
    camera.lookAt(0, 2, 0)
    renderer.render(scene, camera)
  }
  animate()
}

function initCharts() {
  // 灌溉统计
  const ic = echarts.init(irrigChart.value)
  ic.setOption({
    backgroundColor: 'transparent',
    textStyle: { color: '#aaa' },
    tooltip: { trigger: 'axis' },
    grid: { top: 10, right: 10, bottom: 20, left: 40 },
    xAxis: { type: 'category', data: ['周一','周二','周三','周四','周五','周六','周日'], axisLine: { lineStyle: { color: '#333' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#1a2a3a' } }, axisLine: { lineStyle: { color: '#333' } } },
    series: [
      { type: 'bar', data: [5,3,6,4,7,2,5], itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#409eff'},{offset:1,color:'#0d47a1'}]) } }
    ]
  })

  // 节水效果
  const wc = echarts.init(waterChart.value)
  wc.setOption({
    backgroundColor: 'transparent',
    series: [{
      type: 'gauge', radius: '90%', startAngle: 200, endAngle: -20,
      min: 0, max: 100,
      axisLine: { lineStyle: { width: 20, color: [[0.3,'#f56c6c'],[0.7,'#e6a23c'],[1,'#67c23a']] } },
      pointer: { length: '60%', width: 6 },
      axisTick: { show: false }, splitLine: { show: false },
      axisLabel: { color: '#aaa', fontSize: 10 },
      detail: { formatter: '{value}%', fontSize: 20, color: '#67c23a', offsetCenter: [0, '70%'] },
      data: [{ value: 32, name: '节水率' }]
    }]
  })
}
</script>

<style scoped>
.screen3d { width: 100vw; height: 100vh; background: #0a1628; color: #e0e0e0; overflow: hidden; display: flex; flex-direction: column; font-family: 'Microsoft YaHei', sans-serif; }
.header-bar { height: 60px; display: flex; align-items: center; justify-content: space-between; padding: 0 30px; background: linear-gradient(90deg, rgba(10,22,40,0.9), rgba(20,60,120,0.6), rgba(10,22,40,0.9)); border-bottom: 1px solid rgba(64,156,255,0.3); }
.header-bar h1 { font-size: 22px; background: linear-gradient(90deg, #4fc3f7, #81c784); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin: 0; }
.time { font-size: 16px; color: #4fc3f7; font-family: monospace; }
.main-content { flex: 1; display: flex; gap: 12px; padding: 12px; }
.left-panel, .right-panel { width: 300px; display: flex; flex-direction: column; gap: 12px; }
.center-3d { flex: 1; position: relative; }
.center-3d canvas { width: 100%; height: 100%; }
.panel-card { background: rgba(10,30,60,0.8); border: 1px solid rgba(64,156,255,0.2); border-radius: 8px; padding: 12px; }
.panel-card h3 { margin: 0 0 10px; font-size: 14px; color: #4fc3f7; border-left: 3px solid #409eff; padding-left: 8px; }
.stat-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.stat-item { text-align: center; padding: 8px; background: rgba(0,0,0,0.3); border-radius: 6px; }
.stat-item .num { display: block; font-size: 24px; font-weight: bold; color: #4fc3f7; }
.stat-item .num.online { color: #67c23a; }
.stat-item .num.offline { color: #f56c6c; }
.stat-item .label { font-size: 12px; color: #888; }
.env-list { display: flex; flex-direction: column; gap: 6px; }
.env-item { display: flex; justify-content: space-between; padding: 6px 8px; background: rgba(0,0,0,0.2); border-radius: 4px; }
.env-label { color: #aaa; }
.env-value { font-weight: bold; font-size: 16px; }
.alert-list { display: flex; flex-direction: column; gap: 4px; max-height: 150px; overflow-y: auto; }
.alert-item { display: flex; gap: 8px; padding: 4px 8px; border-radius: 4px; font-size: 13px; }
.alert-item.level-L1 { background: rgba(245,108,108,0.15); border-left: 3px solid #f56c6c; }
.alert-item.level-L2 { background: rgba(230,162,60,0.15); border-left: 3px solid #e6a23c; }
.alert-item.level-L3 { background: rgba(64,156,255,0.1); border-left: 3px solid #409eff; }
.alert-level { font-weight: bold; }
.greenhouse-labels { position: absolute; top: 40px; left: 0; right: 0; bottom: 0; pointer-events: none; }
.gh-label { position: absolute; background: rgba(0,0,0,0.6); border: 1px solid rgba(64,156,255,0.4); border-radius: 6px; padding: 4px 10px; display: flex; flex-direction: column; align-items: center; font-size: 12px; }
.gh-no { color: #4fc3f7; }
.gh-moisture { color: #67c23a; }
.gh-moisture.warn { color: #f56c6c; }
.plant-list { display: flex; flex-direction: column; gap: 10px; }
.plant-item { font-size: 13px; }
.plant-item .stage { font-size: 11px; color: #888; }
</style>
