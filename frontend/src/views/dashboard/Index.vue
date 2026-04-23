<template>
  <div>
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="card in statCards" :key="card.title">
        <el-card shadow="hover">
          <el-statistic :title="card.title" :value="card.value">
            <template #prefix><el-icon :color="card.color"><component :is="card.icon" /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="16">
        <el-card header="传感器数据趋势（24h）">
          <div ref="chartRef" style="height:350px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card header="最近告警">
          <el-timeline>
            <el-timeline-item v-for="a in recentAlerts" :key="a.id" :type="alertColor(a.level)" :timestamp="a.createdAt">
              {{ a.title }}
            </el-timeline-item>
          </el-timeline>
          <el-empty v-if="!recentAlerts.length" description="暂无告警" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { telemetryApi, alertApi } from '../../api'

const statCards = ref([
  { title: '设备在线', value: 0, icon: 'Monitor', color: '#67c23a' },
  { title: '今日灌溉', value: 0, icon: 'Ship', color: '#409eff' },
  { title: '待处理告警', value: 0, icon: 'Bell', color: '#e6a23c' },
  { title: '节水率', value: 0, icon: 'DataAnalysis', color: '#909399' },
])
const recentAlerts = ref([])
const chartRef = ref(null)
let chart = null

const alertColor = (level) => level === 'L1' ? 'danger' : level === 'L2' ? 'warning' : 'info'

onMounted(async () => {
  try {
    const stats = await alertApi.stats()
    statCards.value[2].value = (stats.pending_l1 || 0) + (stats.pending_l2 || 0) + (stats.pending_l3 || 0)
  } catch {}

  try {
    const dashboard = await telemetryApi.dashboard()
    statCards.value[0].value = dashboard.sensors?.length || 0
  } catch {}

  chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['土壤湿度', '空气温度'] },
    xAxis: { type: 'time' },
    yAxis: [{ name: '湿度(%)', type: 'value' }, { name: '温度(°C)', type: 'value' }],
    series: [
      { name: '土壤湿度', type: 'line', smooth: true, data: [], yAxisIndex: 0 },
      { name: '空气温度', type: 'line', smooth: true, data: [], yAxisIndex: 1 },
    ]
  })
})

onUnmounted(() => chart?.dispose())
</script>
<style scoped>
.stat-row .el-card { text-align: center; }
</style>
