<template>
  <div class="alert-page">
    <!-- Filter Bar with Chips + Actions -->
    <FilterBar
      :chips="chipConfig"
      :actions="actionConfig"
      @chip-change="onChipChange"
    />

    <!-- Alert Table -->
    <div class="table-card">
      <div class="table-header">
        <span class="title">告警列表</span>
        <span class="table-stats">
          待处理 <strong class="pending-count">{{ alertStore.stats.pending }}</strong> 条 · 今日共 {{ alertStore.stats.todayCount }} 条
        </span>
      </div>
      <el-table
        :data="alertStore.filteredAlerts"
        style="width: 100%"
        v-loading="alertStore.loading"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column label="级别" width="80">
          <template #default="{ row }">
            <LevelBadge :level="row.level" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="告警内容" />
        <el-table-column prop="device" label="设备" width="140" />
        <el-table-column prop="area" label="区域" width="120" />
        <el-table-column prop="time" label="时间" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'pending'"
              size="small"
              type="primary"
              @click="handleAlert(row)"
            >
              处理
            </el-button>
            <el-button
              v-else
              size="small"
              plain
              @click="viewDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          small
          layout="total, prev, pager, next"
          :total="alertStore.pagination.total"
          :page-size="alertStore.pagination.pageSize"
          v-model:current-page="currentPage"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import FilterBar from '../../components/FilterBar.vue'
import LevelBadge from '../../components/LevelBadge.vue'
import StatusTag from '../../components/StatusTag.vue'
import { useAlertStore } from '../../stores/alert'

const alertStore = useAlertStore()
const currentPage = ref(1)
const selectedRows = ref([])

// Chip filter configuration
const chipConfig = [
  { value: 'all', label: '全部' },
  { value: 'L1', label: 'L1 紧急', color: '#D32F2F' },
  { value: 'L2', label: 'L2 重要', color: '#F57C00' },
  { value: 'L3', label: 'L3 一般' },
]

// Batch confirm handler
function batchConfirm() {
  const pendingIds = selectedRows.value
    .filter((row) => row.status === 'pending')
    .map((row) => row.id)
  if (pendingIds.length === 0) {
    ElMessage.warning('请选择待处理的告警')
    return
  }
  alertStore.batchAcknowledge(pendingIds).then(() => {
    ElMessage.success(`已批量确认 ${pendingIds.length} 条告警`)
    alertStore.fetchAlerts({ page: currentPage.value, pageSize: alertStore.pagination.pageSize })
    alertStore.fetchStats()
  })
}

// Action buttons configuration
const actionConfig = [
  { label: '✓ 批量确认', type: 'primary', onClick: batchConfirm },
  { label: '📥 导出', type: 'outline' },
]

// Chip change handler - update store filter
function onChipChange(value) {
  alertStore.filter = value
}

// Selection change handler
function onSelectionChange(rows) {
  selectedRows.value = rows
}

// Handle single alert
function handleAlert(row) {
  alertStore.acknowledgeAlert(row.id).then(() => {
    ElMessage.success('告警已处理')
    alertStore.fetchStats()
  })
}

// View alert detail
function viewDetail(row) {
  ElMessage.info(`查看告警详情: ${row.title}`)
}

// Pagination
function handlePageChange(page) {
  alertStore.fetchAlerts({ page, pageSize: alertStore.pagination.pageSize })
}

// Fetch data on mount
onMounted(() => {
  alertStore.fetchAlerts({ page: 1, pageSize: 10 })
  alertStore.fetchStats()
})
</script>

<style scoped>
.alert-page {
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

.table-stats {
  font-size: 13px;
  color: var(--sf-text-secondary);
}

.table-stats .pending-count {
  color: var(--sf-l1);
  font-weight: 700;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
}
</style>
