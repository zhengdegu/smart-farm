<template>
  <div class="patrol-page">
    <FilterBar
      :filters="filterConfig"
      :actions="actionConfig"
      @filter-change="onFilterChange"
    />

    <div class="table-card">
      <div class="table-header">
        <span class="title">巡检日志</span>
      </div>
      <el-table :data="filteredLogs" style="width: 100%" v-loading="loading">
        <el-table-column label="巡检时间" width="180">
          <template #default="{ row }">
            {{ row.createdAt ? new Date(row.createdAt).toLocaleString('zh-CN') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="巡检类型" width="120">
          <template #default="{ row }">
            {{ mapPatrolType(row.patrolType) }}
          </template>
        </el-table-column>
        <el-table-column label="严重程度" width="100">
          <template #default="{ row }">
            <el-tag
              :color="mapSeverity(row.severity).color"
              effect="dark"
              size="small"
              style="border: none;"
            >
              {{ mapSeverity(row.severity).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="finding" label="发现内容" />
        <el-table-column label="采取行动" width="120">
          <template #default="{ row }">
            {{ mapActionTaken(row.actionTaken) }}
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Manual patrol dialog -->
    <el-dialog v-model="patrolDialogVisible" title="手动巡检" width="400px">
      <el-select v-model="selectedPatrolType" placeholder="选择巡检类型" style="width: 100%">
        <el-option label="趋势检查" value="TREND_CHECK" />
        <el-option label="设备健康" value="DEVICE_HEALTH" />
        <el-option label="每日摘要" value="DAILY_SUMMARY" />
        <el-option label="灌溉评估" value="IRRIGATION_EVAL" />
      </el-select>
      <template #footer>
        <el-button @click="patrolDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="triggerLoading" @click="doTriggerPatrol">开始巡检</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import FilterBar from '../../components/FilterBar.vue'
import { aiApi } from '../../api'
import { mapPatrolType, mapSeverity, mapActionTaken, filterByField } from '../../utils/ai-helpers'

const logs = ref([])
const loading = ref(false)
const patrolDialogVisible = ref(false)
const selectedPatrolType = ref('')
const triggerLoading = ref(false)

// Filter state
const currentFilters = ref({ patrolType: '', severity: '' })

const filteredLogs = computed(() => {
  let result = logs.value
  result = filterByField(result, 'patrolType', currentFilters.value.patrolType)
  result = filterByField(result, 'severity', currentFilters.value.severity)
  return result
})

// Filter config
const filterConfig = [
  {
    key: 'patrolType',
    label: '巡检类型',
    options: [
      { label: '全部', value: '' },
      { label: '趋势检查', value: 'TREND_CHECK' },
      { label: '设备健康', value: 'DEVICE_HEALTH' },
      { label: '每日摘要', value: 'DAILY_SUMMARY' },
      { label: '灌溉评估', value: 'IRRIGATION_EVAL' },
    ],
  },
  {
    key: 'severity',
    label: '严重程度',
    options: [
      { label: '全部', value: '' },
      { label: 'INFO', value: 'INFO' },
      { label: 'WARNING', value: 'WARNING' },
      { label: 'CRITICAL', value: 'CRITICAL' },
    ],
  },
]

const actionConfig = [
  { label: '🔍 手动巡检', type: 'primary', onClick: () => { patrolDialogVisible.value = true } },
]

function onFilterChange(values) {
  currentFilters.value = values
}

async function fetchLogs() {
  loading.value = true
  try {
    const data = await aiApi.patrolLogs()
    logs.value = Array.isArray(data) ? data : []
  } catch {
    ElMessage.error('加载巡检日志失败')
  } finally {
    loading.value = false
  }
}

async function doTriggerPatrol() {
  if (!selectedPatrolType.value) {
    ElMessage.warning('请选择巡检类型')
    return
  }
  triggerLoading.value = true
  try {
    await aiApi.triggerPatrol(selectedPatrolType.value)
    ElMessage.success('巡检已触发')
    patrolDialogVisible.value = false
    selectedPatrolType.value = ''
    await fetchLogs()
  } catch {
    ElMessage.error('巡检触发失败')
  } finally {
    triggerLoading.value = false
  }
}

onMounted(() => {
  fetchLogs()
})
</script>

<style scoped>
.patrol-page {
  padding: 0;
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
</style>
