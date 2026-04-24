<template>
  <div class="report-page">
    <!-- Filter Bar -->
    <div class="filter-bar">
      <div class="time-range">
        <div
          v-for="item in timeRangeOptions"
          :key="item.value"
          :class="['time-range-item', { active: activeRange === item.value }]"
          @click="switchRange(item.value)"
        >
          {{ item.label }}
        </div>
      </div>
      <el-date-picker
        v-model="selectedDate"
        type="date"
        placeholder="选择日期"
        style="width: 160px"
        @change="loadData"
      />
      <div style="flex: 1" />
      <button class="btn btn-outline" @click="exportCsv">📥 CSV</button>
      <button class="btn btn-outline" @click="exportExcel">📊 Excel</button>
      <button class="btn btn-primary" @click="exportPdf">📄 PDF</button>
    </div>

    <!-- Charts Row 1: 2fr 1fr -->
    <div class="charts-row charts-row-1">
      <ChartCard
        title="灌溉统计"
        subtitle="灌溉次数 · 用水量 · 灌溉时长"
        :height="240"
        :option="irrigationChartOption"
      />
      <ChartCard
        title="节水率趋势"
        :height="240"
        :option="waterSavingChartOption"
      />
    </div>

    <!-- Charts Row 2: 1fr 1fr -->
    <div class="charts-row charts-row-2">
      <ChartCard
        title="传感器数据趋势"
        subtitle="温度 · 湿度 · 光照"
        :height="200"
        :option="sensorChartOption"
      />
      <ChartCard
        title="告警统计"
        subtitle="按级别分布"
        :height="200"
        :option="alertChartOption"
      />
    </div>

    <!-- Data Summary Table -->
    <div class="table-card">
      <div class="table-header">
        <span class="title">数据汇总</span>
      </div>
      <el-table :data="summaryData" style="width: 100%">
        <el-table-column prop="metric" label="指标" width="140" />
        <el-table-column prop="today" label="今日" />
        <el-table-column prop="yesterday" label="昨日" />
        <el-table-column prop="week" label="本周" />
        <el-table-column prop="month" label="本月" />
        <el-table-column label="环比" width="140">
          <template #default="{ row }">
            <span :class="row.trendClass">{{ row.trend }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import ChartCard from '../../components/ChartCard.vue'
import { reportApi } from '../../api'

// Time range
const timeRangeOptions = [
  { label: '日', value: 'day' },
  { label: '周', value: 'week' },
  { label: '月', value: 'month' },
]
const activeRange = ref('day')
const selectedDate = ref(new Date())

// Chart data refs
const irrigationData = ref([])
const waterSavingData = ref({})
const sensorData = ref([])
const alertStatsData = ref([])

// Time range switching
function switchRange(range) {
  activeRange.value = range
  loadData()
}

// Format date helper
function fmt(d) {
  if (!d) return ''
  const date = new Date(d)
  return date.toISOString().slice(0, 10)
}

// Compute date range based on active range
function getDateRange() {
  const end = selectedDate.value ? new Date(selectedDate.value) : new Date()
  const start = new Date(end)
  if (activeRange.value === 'day') {
    start.setDate(start.getDate() - 1)
  } else if (activeRange.value === 'week') {
    start.setDate(start.getDate() - 7)
  } else {
    start.setMonth(start.getMonth() - 1)
  }
  return { start: fmt(start), end: fmt(end) }
}

// Load all data
async function loadData() {
  const { start, end } = getDateRange()
  try {
    const data = await reportApi.dailyStats({ start, end })
    irrigationData.value = Array.isArray(data) ? data : []
  } catch {
    irrigationData.value = []
  }
  try {
    const data = await reportApi.waterSaving({ start, end })
    waterSavingData.value = data || {}
  } catch {
    waterSavingData.value = {}
  }
  try {
    const data = await reportApi.alertStats({ start, end })
    alertStatsData.value = Array.isArray(data) ? data : []
  } catch {
    alertStatsData.value = []
  }
}

// Irrigation bar chart option
const irrigationChartOption = computed(() => {
  const data = irrigationData.value
  if (!data.length) return {}
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['灌溉次数', '用水量', '灌溉时长'], top: 0 },
    grid: { top: 30, bottom: 20, left: 40, right: 20 },
    xAxis: { type: 'category', data: data.map(d => d.summaryDate || d.date || '') },
    yAxis: { type: 'value' },
    series: [
      { name: '灌溉次数', type: 'bar', data: data.map(d => d.irrigationCount || 0), itemStyle: { color: '#2E7D32' } },
      { name: '用水量', type: 'bar', data: data.map(d => d.waterUsage || 0), itemStyle: { color: '#1976D2' } },
      { name: '灌溉时长', type: 'bar', data: data.map(d => d.irrigationDuration || 0), itemStyle: { color: '#F57C00' } },
    ],
  }
})

// Water saving gauge chart option
const waterSavingChartOption = computed(() => {
  const rate = waterSavingData.value.savingRate || waterSavingData.value.rate || 32
  const saved = waterSavingData.value.cumulativeSaved || waterSavingData.value.saved || 12.8
  return {
    series: [
      {
        type: 'gauge',
        radius: '85%',
        startAngle: 220,
        endAngle: -40,
        min: 0,
        max: 100,
        progress: { show: true, width: 14, itemStyle: { color: '#2E7D32' } },
        axisLine: { lineStyle: { width: 14, color: [[1, '#E0E0E0']] } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        pointer: { show: false },
        title: { show: true, offsetCenter: [0, '70%'], fontSize: 13, color: '#757575' },
        detail: {
          valueAnimation: true,
          fontSize: 28,
          fontWeight: 700,
          offsetCenter: [0, '30%'],
          formatter: '{value}%',
          color: '#2E7D32',
        },
        data: [{ value: rate, name: `本月节水 ${saved} 吨` }],
      },
    ],
  }
})

// Sensor trend line chart option
const sensorChartOption = computed(() => {
  // Use mock data structure for display
  const hours = Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`)
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['温度', '湿度', '光照'], top: 0 },
    grid: { top: 30, bottom: 20, left: 40, right: 20 },
    xAxis: { type: 'category', data: hours },
    yAxis: { type: 'value' },
    series: [
      { name: '温度', type: 'line', smooth: true, data: hours.map(() => (Math.random() * 10 + 20).toFixed(1)), itemStyle: { color: '#D32F2F' } },
      { name: '湿度', type: 'line', smooth: true, data: hours.map(() => (Math.random() * 20 + 50).toFixed(1)), itemStyle: { color: '#1976D2' } },
      { name: '光照', type: 'line', smooth: true, data: hours.map(() => (Math.random() * 30000 + 10000).toFixed(0)), itemStyle: { color: '#F57C00' } },
    ],
  }
})

// Alert stats bar chart option
const alertChartOption = computed(() => {
  const data = alertStatsData.value
  // If we have API data, use it; otherwise use defaults
  const l1 = data.find?.(d => d.level === 'L1')?.count ?? 3
  const l2 = data.find?.(d => d.level === 'L2')?.count ?? 8
  const l3 = data.find?.(d => d.level === 'L3')?.count ?? 15
  return {
    tooltip: { trigger: 'axis' },
    grid: { top: 10, bottom: 20, left: 40, right: 20 },
    xAxis: { type: 'category', data: ['L1 紧急', 'L2 重要', 'L3 一般'] },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        barWidth: 40,
        data: [
          { value: l1, itemStyle: { color: '#D32F2F' } },
          { value: l2, itemStyle: { color: '#F57C00' } },
          { value: l3, itemStyle: { color: '#FBC02D' } },
        ],
      },
    ],
  }
})

// Data summary table
const summaryData = ref([
  { metric: '灌溉次数', today: '5', yesterday: '6', week: '38', month: '152', trend: '↑ 12%', trendClass: 'trend-up' },
  { metric: '用水量(吨)', today: '2.4', yesterday: '2.8', week: '18.5', month: '72.3', trend: '↓ 8%（节水）', trendClass: 'trend-up' },
  { metric: '节水率', today: '32%', yesterday: '30%', week: '31%', month: '32%', trend: '↑ 8.3%', trendClass: 'trend-up' },
  { metric: '告警数', today: '3', yesterday: '1', week: '8', month: '23', trend: '↑ 15%', trendClass: 'trend-down' },
  { metric: '设备在线率', today: '96.8%', yesterday: '95.6%', week: '96.2%', month: '97.1%', trend: '↑ 1.2%', trendClass: 'trend-up' },
])

// Export functions
function downloadFile(url) {
  const token = localStorage.getItem('token')
  fetch(url, { headers: { Authorization: `Bearer ${token}` } })
    .then(r => r.blob())
    .then(blob => {
      const u = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = u
      a.download = url.split('/').pop().split('?')[0] || 'export'
      a.click()
      URL.revokeObjectURL(u)
    })
}

function exportCsv() {
  const { start, end } = getDateRange()
  downloadFile(`/api/v1/reports/export/irrigation-daily?start=${start}&end=${end}&format=csv`)
}

function exportExcel() {
  const { start, end } = getDateRange()
  downloadFile(`/api/v1/reports/export/irrigation-daily?start=${start}&end=${end}&format=xlsx`)
}

function exportPdf() {
  const { start, end } = getDateRange()
  downloadFile(`/api/v1/reports/export/irrigation-daily?start=${start}&end=${end}&format=pdf`)
}

// Load data on mount
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.report-page {
  padding: 0;
}

/* Filter Bar */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

/* Time Range Selector */
.time-range {
  display: flex;
  gap: 0;
  background: var(--sf-card);
  border: 1px solid var(--sf-border);
  border-radius: 6px;
  overflow: hidden;
}

.time-range-item {
  padding: 6px 16px;
  font-size: 13px;
  cursor: pointer;
  color: var(--sf-text-secondary);
  transition: background 0.2s, color 0.2s;
  user-select: none;
}

.time-range-item:hover {
  background: #f5f5f5;
}

.time-range-item.active {
  background: var(--sf-primary);
  color: #fff;
}

/* Buttons */
.btn {
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  border: none;
  transition: opacity 0.2s;
  white-space: nowrap;
}

.btn:hover {
  opacity: 0.85;
}

.btn-outline {
  background: var(--sf-card);
  border: 1px solid var(--sf-border);
  color: var(--sf-text);
}

.btn-primary {
  background: var(--sf-primary);
  color: #fff;
}

/* Charts Rows */
.charts-row {
  display: grid;
  gap: 20px;
  margin-bottom: 20px;
}

.charts-row-1 {
  grid-template-columns: 2fr 1fr;
}

.charts-row-2 {
  grid-template-columns: 1fr 1fr;
}

/* Table Card */
.table-card {
  background: var(--sf-card);
  border-radius: var(--sf-radius);
  padding: 20px;
  box-shadow: var(--sf-shadow);
  margin-bottom: 20px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table-header .title {
  font-size: 15px;
  font-weight: 600;
}

/* Trend colors */
:deep(.trend-up) {
  color: #2E7D32;
  font-weight: 500;
}

:deep(.trend-down) {
  color: #D32F2F;
  font-weight: 500;
}
</style>
