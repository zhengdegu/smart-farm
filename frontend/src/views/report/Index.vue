<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card header="灌溉统计">
          <el-date-picker v-model="dateRange" type="daterange" start-placeholder="开始" end-placeholder="结束" style="margin-bottom:16px" @change="loadDaily" />
          <div ref="dailyChart" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="节水率对比">
          <div ref="waterChart" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <el-card header="告警统计">
          <div ref="alertChart" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="传感器趋势">
          <el-select v-model="trendType" style="margin-bottom:12px" @change="loadTrend">
            <el-option label="土壤湿度" value="soil_moisture" /><el-option label="土壤温度" value="soil_temp" />
            <el-option label="空气温度" value="air_temp" /><el-option label="空气湿度" value="air_humidity" />
          </el-select>
          <div ref="trendChart" style="height:260px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, GaugeChart, PieChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
echarts.use([BarChart, GaugeChart, PieChart, LineChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer])
import { reportApi, telemetryApi } from '../../api'

const dateRange = ref([new Date(Date.now()-7*86400000), new Date()])
const trendType = ref('soil_moisture')
const dailyChart = ref(null), waterChart = ref(null), alertChart = ref(null), trendChart = ref(null)
let charts = []

const initChart = (el, opt) => { const c = echarts.init(el); c.setOption(opt); charts.push(c); return c }

const loadDaily = async () => {
  try {
    const [start, end] = dateRange.value
    const data = await reportApi.dailyStats({ start: fmt(start), end: fmt(end) })
    initChart(dailyChart.value, {
      tooltip: { trigger: 'axis' }, xAxis: { type: 'category', data: data.map(d => d.summaryDate) },
      yAxis: { type: 'value' }, series: [{ name: '灌溉次数', type: 'bar', data: data.map(d => d.irrigationCount) }]
    })
  } catch {}
}

const fmt = (d) => d.toISOString().slice(0, 10)

onMounted(() => {
  loadDaily()
  initChart(waterChart.value, { tooltip:{}, series:[{ type:'gauge', data:[{value:32,name:'节水率%'}], detail:{formatter:'{value}%'} }] })
  initChart(alertChart.value, { tooltip:{trigger:'item'}, series:[{ type:'pie', radius:'60%', data:[{value:3,name:'L1紧急'},{value:8,name:'L2重要'},{value:15,name:'L3一般'}] }] })
  loadTrend()
})

const loadTrend = () => {
  initChart(trendChart.value, {
    tooltip:{trigger:'axis'}, xAxis:{type:'time'}, yAxis:{type:'value'},
    series:[{name:trendType.value, type:'line', smooth:true, data:[]}]
  })
}

onUnmounted(() => charts.forEach(c => c.dispose()))
</script>
