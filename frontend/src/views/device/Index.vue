<template>
  <div class="device-page">
    <!-- Filter Bar -->
    <FilterBar
      :filters="filterConfig"
      :actions="actionConfig"
      @filter-change="onFilterChange"
    />

    <!-- Stats Row -->
    <div class="stats-row">
      <StatCard
        icon="✅"
        iconBg="green"
        label="在线设备"
        :value="deviceStore.onlineCount"
        valueColor="var(--sf-primary)"
      />
      <StatCard
        icon="⚡"
        iconBg="orange"
        label="离线设备"
        :value="deviceStore.offlineCount"
        valueColor="var(--sf-text-secondary)"
      />
      <StatCard
        icon="🔧"
        iconBg="red"
        label="故障设备"
        :value="deviceStore.faultCount"
        valueColor="var(--sf-l1)"
      />
    </div>

    <!-- Device Table -->
    <div class="table-card">
      <div class="table-header">
        <span class="title">设备列表</span>
        <span class="table-count">共 {{ deviceStore.pagination.total }} 台设备</span>
      </div>
      <el-table
        :data="deviceStore.filteredDevices"
        style="width: 100%"
        v-loading="deviceStore.loading"
      >
        <el-table-column prop="name" label="设备名称" />
        <el-table-column prop="deviceId" label="设备ID" width="120" />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="greenhouse" label="棚号" width="100" />
        <el-table-column prop="latestData" label="最新数据" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="lastOnline" label="最后在线" width="140" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'fault'">
              <el-button size="small" @click="handleAction(row, 'detail')">详情</el-button>
              <el-button size="small" type="warning" @click="handleAction(row, 'restart')">重启</el-button>
            </template>
            <template v-else-if="row.type === 'valve' || row.type === '阀门'">
              <el-button size="small" @click="handleAction(row, 'control')">控制</el-button>
              <el-button size="small" @click="editDevice(row)">编辑</el-button>
            </template>
            <template v-else>
              <el-button size="small" @click="handleAction(row, 'detail')">详情</el-button>
              <el-button size="small" @click="editDevice(row)">编辑</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          small
          layout="total, prev, pager, next"
          :total="deviceStore.pagination.total"
          :page-size="deviceStore.pagination.pageSize"
          v-model:current-page="currentPage"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- Add/Edit Device Dialog -->
    <el-dialog v-model="showDialog" :title="editingDevice ? '编辑设备' : '注册设备'" width="500">
      <el-form :model="form" label-width="80px">
        <el-form-item label="设备ID" v-if="!editingDevice">
          <el-input v-model="form.deviceId" />
        </el-form-item>
        <el-form-item label="类型" v-if="!editingDevice">
          <el-select v-model="form.deviceType">
            <el-option label="传感器" value="SENSOR" />
            <el-option label="阀门" value="VALVE" />
            <el-option label="网关" value="GATEWAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="大棚号">
          <el-input v-model="form.greenhouseNo" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="submitDevice">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { deviceApi } from '../../api'
import { ElMessage } from 'element-plus'
import StatCard from '../../components/StatCard.vue'
import FilterBar from '../../components/FilterBar.vue'
import StatusTag from '../../components/StatusTag.vue'
import { useDeviceStore } from '../../stores/device'

const deviceStore = useDeviceStore()
const currentPage = ref(1)

// Filter configuration
const filterConfig = [
  {
    key: 'type',
    label: '全部类型',
    options: [
      { value: '', label: '全部类型' },
      { value: 'sensor', label: '传感器' },
      { value: 'valve', label: '阀门' },
      { value: 'gateway', label: '网关' },
    ],
  },
  {
    key: 'status',
    label: '全部状态',
    options: [
      { value: '', label: '全部状态' },
      { value: 'online', label: '在线' },
      { value: 'offline', label: '离线' },
      { value: 'fault', label: '故障' },
    ],
  },
]

// Action buttons configuration
const showDialog = ref(false)
const editingDevice = ref(null)
const form = ref({
  deviceId: '',
  deviceType: 'SENSOR',
  name: '',
  location: '',
  greenhouseNo: '',
})

const actionConfig = [
  { label: '📥 导出', type: 'outline' },
  { label: '+ 注册设备', type: 'primary', onClick: () => { openAddDialog() } },
]

// Filter change handler
function onFilterChange(filters) {
  deviceStore.filters.type = filters.type || ''
  deviceStore.filters.status = filters.status || ''
  deviceStore.fetchDevices({
    page: 1,
    pageSize: deviceStore.pagination.pageSize,
    type: filters.type || undefined,
    status: filters.status || undefined,
  })
  currentPage.value = 1
}

// Pagination
function handlePageChange(page) {
  deviceStore.fetchDevices({
    page,
    pageSize: deviceStore.pagination.pageSize,
    type: deviceStore.filters.type || undefined,
    status: deviceStore.filters.status || undefined,
  })
}

// Action handlers
function handleAction(row, action) {
  if (action === 'restart') {
    ElMessage.info(`正在重启设备: ${row.name}`)
  } else if (action === 'control') {
    ElMessage.info(`控制设备: ${row.name}`)
  } else {
    ElMessage.info(`查看详情: ${row.name}`)
  }
}

// Dialog handlers
function openAddDialog() {
  editingDevice.value = null
  form.value = { deviceId: '', deviceType: 'SENSOR', name: '', location: '', greenhouseNo: '' }
  showDialog.value = true
}

function editDevice(row) {
  editingDevice.value = row.deviceId || row.id
  form.value = {
    name: row.name,
    location: row.location,
    greenhouseNo: row.greenhouseNo || row.greenhouse,
  }
  showDialog.value = true
}

async function submitDevice() {
  try {
    if (editingDevice.value) {
      await deviceApi.update(editingDevice.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await deviceApi.create(form.value)
      ElMessage.success('注册成功')
    }
    showDialog.value = false
    editingDevice.value = null
    form.value = { deviceId: '', deviceType: 'SENSOR', name: '', location: '', greenhouseNo: '' }
    deviceStore.fetchDevices({
      page: currentPage.value,
      pageSize: deviceStore.pagination.pageSize,
    })
  } catch (error) {
    // Error handled by API interceptor
  }
}

// Fetch data on mount
onMounted(() => {
  deviceStore.fetchDevices({ page: 1, pageSize: 10 })
})
</script>

<style scoped>
.device-page {
  padding: 0;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
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

.table-count {
  font-size: 13px;
  color: var(--sf-text-secondary);
}

.pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
}
</style>
