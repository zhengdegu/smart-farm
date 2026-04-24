<script setup>
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  height: {
    type: Number,
    default: 220
  },
  option: {
    type: Object,
    default: null
  }
})

const chartRef = ref(null)
let chartInstance = null

const hasData = computed(() => {
  return props.option && Object.keys(props.option).length > 0
})

function initChart() {
  if (!chartRef.value || !hasData.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  chartInstance.setOption(props.option)
}

function handleResize() {
  chartInstance?.resize()
}

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})

watch(
  () => props.option,
  (newOption) => {
    if (newOption && Object.keys(newOption).length > 0) {
      if (!chartInstance && chartRef.value) {
        chartInstance = echarts.init(chartRef.value)
      }
      chartInstance?.setOption(newOption, true)
    } else {
      if (chartInstance) {
        chartInstance.dispose()
        chartInstance = null
      }
    }
  },
  { deep: true }
)
</script>

<template>
  <div class="chart-card">
    <div class="title">
      <span>{{ title }}</span>
      <span v-if="subtitle" class="sub">{{ subtitle }}</span>
    </div>
    <div v-if="hasData" ref="chartRef" class="chart-area" :style="{ height: height + 'px' }" />
    <div v-else class="chart-empty" :style="{ height: height + 'px' }">
      暂无数据
    </div>
  </div>
</template>

<style scoped>
.chart-card {
  background: var(--sf-card);
  border-radius: var(--sf-radius);
  padding: 20px;
  box-shadow: var(--sf-shadow);
}

.title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title .sub {
  font-size: 12px;
  color: var(--sf-text-secondary);
  font-weight: 400;
}

.chart-area {
  width: 100%;
}

.chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--sf-text-secondary);
  font-size: 14px;
}
</style>
