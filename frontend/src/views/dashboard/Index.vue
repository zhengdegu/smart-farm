<template>
  <div class="dashboard-page">
    <!-- Stats Row -->
    <div class="stats-row">
      <StatCard
        icon="📡"
        iconBg="green"
        label="设备在线率"
        :value="deviceOnlineRate"
        trend="↑ 1.2% 较昨日"
        trendType="up"
      />
      <StatCard
        icon="💧"
        iconBg="blue"
        label="今日灌溉"
        :value="todayIrrigation"
        trend="总时长 120 分钟"
        trendType="up"
      />
      <StatCard
        icon="⚠️"
        iconBg="orange"
        label="待处理告警"
        :value="String(alertStore.stats.pending)"
        valueColor="var(--sf-l2)"
        trend="↑ 2 较昨日"
        trendType="down"
      />
      <StatCard
        icon="🌊"
        iconBg="green"
        label="本月节水率"
        :value="monthlyWaterSaving"
        valueColor="var(--sf-primary)"
        trend="↑ 8.3% 较上月"
        trendType="up"
      />
    </div>

    <!-- Charts Row -->
    <div class="charts-row">
      <ChartCard
        title="传感器数据趋势（24h）"
        subtitle="土壤湿度 · 土壤温度 · 空气温度"
        :height="280"
        :option="trendChartOption"
      />
      <ChartCard
        title="节水率"
        :height="280"
        :option="gaugeChartOption"
      />
    </div>

    <!-- Device List Table -->
    <div class="table-card">
      <div class="table-header">
        <span class="title">设备列表</span>
        <div class="table-actions">
          <el-button size="small">📥 导出</el-button>
          <el-button type="primary" size="small">+ 注册设备</el-button>
        </div>
      </div>
      <el-table :data="deviceStore.devices" style="width: 100%" v-loading="deviceStore.loading">
        <el-table-column prop="name" label="设备名称" />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="greenhouse" label="棚号" width="120" />
        <el-table-column prop="latestData" label="最新数据" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="lastOnline" label="最后在线" width="140" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              v-if="row.type === '阀门' || row.type === 'valve'"
              size="small"
              @click="handleDeviceAction(row, 'control')"
            >控制</el-button>
            <el-button
              v-else
              size="small"
              @click="handleDeviceAction(row, 'detail')"
            >详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          small
          layout="total, prev, pager, next"
          :total="deviceStore.pagination.total"
          :page-size="deviceStore.pagination.pageSize"
          v-model:current-page="devicePage"
          @current-change="handleDevicePageChange"
        />
      </div>
    </div>

    <!-- Recent Alerts Table -->
    <div class="table-card">
      <div class="table-header">
        <span class="title">最近告警</span>
        <div class="table-actions">
          <el-button size="small" @click="router.push('/alerts')">全部告警 →</el-button>
        </div>
      </div>
      <el-table :data="alertStore.alerts" style="width: 100%" v-loading="alertStore.loading">
        <el-table-column label="级别" width="80">
          <template #default="{ row }">
            <LevelBadge :level="row.level" />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="告警内容" />
        <el-table-column prop="device" label="设备" width="160" />
        <el-table-column prop="time" label="时间" width="120">
          <template #default="{ row }">
            {{ formatAlertTime(row.time || row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'pending'"
              type="primary"
              size="small"
              @click="handleAlertAction(row, 'handle')"
            >处理</el-button>
            <el-button
              v-else
              size="small"
              @click="handleAlertAction(row, 'detail')"
            >详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import StatCard from '../../components/StatCard.vue'
import ChartCard from '../../components/ChartCard.vue'
import LevelBadge from '../../components/LevelBadge.vue'
import StatusTag from '../../components/StatusTag.vue'
import { useTelemetryStore } from '../../stores/telemetry'
import { useDeviceStore } from '../../stores/device'
import { useAlertStore } from '../../stores/alert'

const router = useRouter()
const telemetryStore = useTelemetryStore()
const deviceStore = useDeviceStore()
const alertStore = useAlertStore()

const devicePage = ref(1)

// Computed values from telemetry store
const deviceOnlineRate = computed(() => {
  if (telemetryStore.dashboard?.deviceOnlineRate != null) {
    return telemetryStore.dashboard.deviceOnlineRate + '%'
  }
  return '96.8%'
})

const todayIrrigation = computed(() => {
  if (telemetryStore.dashboard?.todayIrrigationCount != null) {
    return telemetryStore.dashboard.todayIrrigationCount + ' 次'
  }
  return '5 次'
})

const monthlyWaterSaving = computed(() => {
  if (telemetryStore.dashboard?.monthlyWaterSaving != null) {
    return telemetryStore.dashboard.monthlyWaterSaving + '%'
  }
  return '32%'
})

const waterSavingPercent = computed(() => {
  if (telemetryStore.dashboard?.monthlyWaterSaving != null) {
    return telemetryStore.dashboard.monthlyWaterSaving
  }
  return 32
})

const cumulativeWaterSaved = computed(() => {
  if (telemetryStore.dashboard?.cumulativeWaterSaved != null) {
    return telemetryStore.dashboard.cumulativeWaterSaved
  }
  return 12.8
})

// Trend chart option (line chart with 3 series)
const trendChartOption = computed(() => {
  const trendData = telemetryStore.trend
  if (trendData && trendData.length > 0) {
    const times = trendData.map(d => d.time || d.timestamp)
    return {
      tooltip: { trigger: 'axis' },
      legend: { data: ['土壤湿度', '土壤温度', '空气温度'], bottom: 0 },
      grid: { top: 10, right: 20, bottom: 40, left: 50 },
      xAxis: { type: 'category', data: times, boundaryGap: false },
      yAxis: { type: 'value' },
      series: [
        {
          name: '土壤湿度',
          type: 'line',
          smooth: true,
          data: trendData.map(d => d.soilMoisture ?? d.soil_moisture),
          itemStyle: { color: '#4CAF50' },
        },
        {
          name: '土壤温度',
          type: 'line',
          smooth: true,
          data: trendData.map(d => d.soilTemperature ?? d.soil_temperature),
          itemStyle: { color: '#FF9800' },
        },
        {
          name: '空气温度',
          type: 'line',
          smooth: true,
          data: trendData.map(d => d.airTemperature ?? d.air_temperature),
          itemStyle: { color: '#2196F3' },
        },
      ],
    }
  }
  // Fallback with sample data for display
  const hours = Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`)
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['土壤湿度', '土壤温度', '空气温度'], bottom: 0 },
    grid: { top: 10, right: 20, bottom: 40, left: 50 },
    xAxis: { type: 'category', data: hours, boundaryGap: false },
    yAxis: { type: 'value' },
    series: [
      {
        name: '土壤湿度',
        type: 'line',
        smooth: true,
        data: [45, 44, 43, 42, 41, 40, 42, 44, 46, 48, 47, 45, 43, 42, 41, 40, 39, 41, 43, 45, 46, 47, 46, 45],
        itemStyle: { color: '#4CAF50' },
      },
      {
        name: '土壤温度',
        type: 'line',
        smooth: true,
        data: [18, 17, 17, 16, 16, 16, 17, 19, 21, 23, 25, 26, 27, 28, 28, 27, 26, 24, 22, 21, 20, 19, 18, 18],
        itemStyle: { color: '#FF9800' },
      },
      {
        name: '空气温度',
        type: 'line',
        smooth: true,
        data: [20, 19, 18, 18, 17, 17, 18, 20, 23, 25, 27, 28, 29, 30, 29, 28, 27, 25, 23, 22, 21, 20, 20, 20],
        itemStyle: { color: '#2196F3' },
      },
    ],
  }
})

// Gauge chart option (ring/pie chart for water saving)
const gaugeChartOption = computed(() => {
  return {
    tooltip: { show: false },
    series: [
      {
        type: 'pie',
        radius: ['60%', '80%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        label: {
          show: true,
          position: 'center',
          formatter: () => `${waterSavingPercent.value}%`,
          fontSize: 32,
          fontWeight: 'bold',
          color: '#2E7D32',
        },
        emphasis: { scale: false },
        labelLine: { show: false },
        data: [
          {
            value: waterSavingPercent.value,
            name: '节水率',
            itemStyle: { color: '#4CAF50' },
          },
          {
            value: 100 - waterSavingPercent.value,
            name: '',
            itemStyle: { color: '#E8F5E9' },
          },
        ],
      },
    ],
    graphic: [
      {
        type: 'text',
        left: 'center',
        bottom: 20,
        style: {
          text: `累计节水 ${cumulativeWaterSaved.value} 吨`,
          fontSize: 13,
          fill: '#757575',
        },
      },
    ],
  }
})

// Format alert time
function formatAlertTime(time) {
  if (!time) return ''
  if (time.length <= 5) return time
  try {
    const date = new Date(time)
    if (isNaN(date.getTime())) return time
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  } catch {
    return time
  }
}

// Device actions
function handleDeviceAction(row, action) {
  if (action === 'control') {
    router.push(`/irrigation`)
  } else {
    router.push(`/devices`)
  }
}

// Alert actions
function handleAlertAction(row, action) {
  if (action === 'handle') {
    alertStore.acknowledgeAlert(row.id)
  } else {
    router.push('/alerts')
  }
}

// Pagination
function handleDevicePageChange(page) {
  deviceStore.fetchDevices({ page, pageSize: deviceStore.pagination.pageSize })
}

// Fetch data on mount
onMounted(async () => {
  telemetryStore.fetchDashboard()
  telemetryStore.fetchTrend({ period: '24h' })
  deviceStore.fetchDevices({ page: 1, pageSize: 10 })
  alertStore.fetchAlerts({ pageSize: 10 })
  alertStore.fetchStats()
})
</script>

<style scoped>
.dashboard-page {
  padding: 0;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

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

.table-actions {
  display: flex;
  gap: 8px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
}
</style>
