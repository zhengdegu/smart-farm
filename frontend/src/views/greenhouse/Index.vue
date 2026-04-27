<template>
  <div class="greenhouse-page">
    <!-- Stats Row -->
    <div class="stats-row">
      <StatCard
        icon="🏠"
        iconBg="green"
        label="大棚总数"
        :value="greenhouseStore.stats.total"
        valueColor="var(--sf-primary)"
      />
      <StatCard
        icon="✅"
        iconBg="blue"
        label="使用中"
        :value="greenhouseStore.stats.active"
        valueColor="var(--sf-blue)"
      />
      <StatCard
        icon="💤"
        iconBg="orange"
        label="空闲"
        :value="greenhouseStore.stats.idle"
        valueColor="var(--sf-text-secondary)"
      />
    </div>

    <!-- Filter Bar -->
    <FilterBar
      :chips="chipConfig"
      :actions="actionConfig"
      @chip-change="onChipChange"
    />

    <!-- Greenhouse Table -->
    <div class="table-card">
      <div class="table-header">
        <span class="title">大棚列表</span>
        <span class="table-count">共 {{ greenhouseStore.filteredGreenhouses.length }} 个大棚</span>
      </div>
      <el-table
        :data="greenhouseStore.filteredGreenhouses"
        style="width: 100%"
        v-loading="greenhouseStore.loading"
      >
        <el-table-column prop="greenhouseNo" label="棚号" width="100" />
        <el-table-column prop="name" label="名称" />
        <el-table-column label="面积(㎡)" width="100">
          <template #default="{ row }">
            {{ row.area != null ? row.area : '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="location" label="位置" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span :class="['gh-status-tag', row.status === 'ACTIVE' ? 'active' : 'idle']">
              {{ row.status === 'ACTIVE' ? '使用中' : '空闲' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row)">详情</el-button>
            <el-button size="small" @click="editGreenhouse(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteGreenhouse(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Detail Drawer -->
    <el-drawer
      v-model="detailVisible"
      :title="detailGreenhouse ? detailGreenhouse.name : ''"
      size="60%"
      direction="rtl"
    >
      <GreenhouseDetail v-if="detailGreenhouse" :greenhouse="detailGreenhouse" />
    </el-drawer>

    <!-- Add/Edit Dialog -->
    <GreenhouseDialog
      :visible="dialogVisible"
      :greenhouse="editingGreenhouse"
      @close="dialogVisible = false"
      @saved="onSaved"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { greenhouseApi } from '../../api'
import StatCard from '../../components/StatCard.vue'
import FilterBar from '../../components/FilterBar.vue'
import GreenhouseDialog from './GreenhouseDialog.vue'
import GreenhouseDetail from './GreenhouseDetail.vue'
import { useGreenhouseStore } from '../../stores/greenhouse'

const greenhouseStore = useGreenhouseStore()

const chipConfig = [
  { label: '全部', value: '' },
  { label: '使用中', value: 'ACTIVE' },
  { label: '空闲', value: 'IDLE' },
]

const dialogVisible = ref(false)
const editingGreenhouse = ref(null)
const detailVisible = ref(false)
const detailGreenhouse = ref(null)

const actionConfig = [
  { label: '+ 新增大棚', type: 'primary', onClick: () => openAddDialog() },
]

function onChipChange(value) {
  greenhouseStore.filters.status = value
}

function openAddDialog() {
  editingGreenhouse.value = null
  dialogVisible.value = true
}

function editGreenhouse(row) {
  editingGreenhouse.value = row
  dialogVisible.value = true
}

function showDetail(row) {
  detailGreenhouse.value = row
  detailVisible.value = true
}

async function deleteGreenhouse(row) {
  try {
    await ElMessageBox.confirm(`确定删除大棚「${row.name}」吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await greenhouseApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      // Error handled by API interceptor
    }
  }
}

function onSaved() {
  dialogVisible.value = false
  loadData()
}

function loadData() {
  greenhouseStore.fetchGreenhouses()
  greenhouseStore.fetchStats()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.greenhouse-page {
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

.gh-status-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.gh-status-tag.active {
  background: #E8F5E9;
  color: #2E7D32;
}

.gh-status-tag.idle {
  background: #FFF3E0;
  color: #F57C00;
}
</style>
