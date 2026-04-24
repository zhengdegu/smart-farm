<template>
  <div class="irrigation-page">
    <!-- Sub-tabs -->
    <div class="sub-tabs">
      <div
        class="sub-tab"
        :class="{ active: activeTab === 'rules' }"
        @click="activeTab = 'rules'"
      >
        灌溉规则
      </div>
      <div
        class="sub-tab"
        :class="{ active: activeTab === 'history' }"
        @click="activeTab = 'history'"
      >
        执行记录
      </div>
    </div>

    <!-- 灌溉规则 Tab -->
    <div v-if="activeTab === 'rules'">
      <div class="filter-bar">
        <div style="flex: 1"></div>
        <el-button type="primary" @click="showAdd = true">+ 新建规则</el-button>
      </div>

      <div class="table-card">
        <el-table
          :data="irrigationStore.rules"
          style="width: 100%"
          v-loading="irrigationStore.loading"
        >
          <el-table-column prop="name" label="规则名称" />
          <el-table-column label="触发条件" min-width="160">
            <template #default="{ row }">
              {{ formatTrigger(row) }}
            </template>
          </el-table-column>
          <el-table-column label="目标阀门" width="140">
            <template #default="{ row }">
              {{ row.targetValves || row.deviceId || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="灌溉时长" width="100">
            <template #default="{ row }">
              {{ row.duration || row.durationMin || 0 }} 分钟
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-switch
                :model-value="row.enabled"
                @change="irrigationStore.toggleRule(row.id)"
              />
            </template>
          </el-table-column>
          <el-table-column label="上次执行" width="140">
            <template #default="{ row }">
              {{ row.lastExecution || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="editRule(row)">编辑</el-button>
              <el-button size="small" @click="delRule(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 执行记录 Tab -->
    <div v-if="activeTab === 'history'">
      <div class="table-card">
        <div class="table-header">
          <span class="title">执行记录</span>
          <span class="table-count">近7天共 {{ irrigationStore.commandPagination.total }} 次灌溉</span>
        </div>
        <el-table
          :data="irrigationStore.commands"
          style="width: 100%"
          v-loading="irrigationStore.loading"
        >
          <el-table-column label="时间" width="140">
            <template #default="{ row }">
              {{ row.time || row.createdAt || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="触发规则">
            <template #default="{ row }">
              {{ row.ruleName || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="阀门" width="120">
            <template #default="{ row }">
              {{ row.valve || row.deviceId || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="时长" width="100">
            <template #default="{ row }">
              {{ row.duration || row.durationMin || 0 }} 分钟
            </template>
          </el-table-column>
          <el-table-column label="用水量" width="100">
            <template #default="{ row }">
              {{ row.waterUsage != null ? row.waterUsage + ' 吨' : '—' }}
            </template>
          </el-table-column>
          <el-table-column label="触发方式" width="100">
            <template #default="{ row }">
              {{ row.triggerType === 'manual' ? '手动' : '自动' }}
            </template>
          </el-table-column>
          <el-table-column label="结果" width="100">
            <template #default="{ row }">
              <StatusTag
                v-if="row.result === 'success' || row.status === 'EXECUTED'"
                status="online"
                text="成功"
              />
              <StatusTag
                v-else-if="row.result === 'failed' || row.status === 'FAILED'"
                status="fault"
                text="失败"
              />
              <StatusTag
                v-else
                status="pending"
                :text="row.result || row.status || '—'"
              />
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination
            small
            layout="total, prev, pager, next"
            :total="irrigationStore.commandPagination.total"
            :page-size="irrigationStore.commandPagination.pageSize"
            v-model:current-page="currentPage"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- Add/Edit Rule Dialog -->
    <el-dialog v-model="showAdd" :title="editingRule ? '编辑灌溉规则' : '新增灌溉规则'" width="500">
      <el-form :model="form" label-width="90px">
        <el-form-item label="规则名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="规则类型">
          <el-radio-group v-model="form.ruleType">
            <el-radio value="THRESHOLD">阈值</el-radio>
            <el-radio value="SCHEDULE">定时</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="阀门设备ID">
          <el-input v-model="form.deviceId" />
        </el-form-item>
        <el-form-item v-if="form.ruleType === 'THRESHOLD'" label="传感器ID">
          <el-input v-model="form.sensorDeviceId" />
        </el-form-item>
        <el-form-item v-if="form.ruleType === 'THRESHOLD'" label="湿度下限">
          <el-input-number v-model="form.thresholdLow" :min="0" :max="100" />
        </el-form-item>
        <el-form-item v-if="form.ruleType === 'THRESHOLD'" label="湿度上限">
          <el-input-number v-model="form.thresholdHigh" :min="0" :max="100" />
        </el-form-item>
        <el-form-item v-if="form.ruleType === 'SCHEDULE'" label="Cron表达式">
          <el-input v-model="form.cronExpression" placeholder="0 6,18 * * *" />
        </el-form-item>
        <el-form-item label="灌溉时长(分)">
          <el-input-number v-model="form.durationMin" :min="1" :max="120" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="submitRule">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { irrigationApi } from '../../api'
import { ElMessage } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import { useIrrigationStore } from '../../stores/irrigation'

const irrigationStore = useIrrigationStore()
const activeTab = ref('rules')
const currentPage = ref(1)
const showAdd = ref(false)
const editingRule = ref(null)

const form = ref({
  name: '',
  ruleType: 'THRESHOLD',
  deviceId: '',
  sensorDeviceId: '',
  thresholdLow: 40,
  thresholdHigh: 70,
  cronExpression: '',
  durationMin: 30,
})

function formatTrigger(row) {
  if (row.trigger) return row.trigger
  if (row.ruleType === 'THRESHOLD') {
    return `土壤湿度 ${row.thresholdLow}% ~ ${row.thresholdHigh}%`
  }
  if (row.ruleType === 'SCHEDULE' && row.cronExpression) {
    return `定时 ${row.cronExpression}`
  }
  return '—'
}

function editRule(row) {
  editingRule.value = row.id
  form.value = {
    name: row.name || '',
    ruleType: row.ruleType || 'THRESHOLD',
    deviceId: row.deviceId || '',
    sensorDeviceId: row.sensorDeviceId || '',
    thresholdLow: row.thresholdLow ?? 40,
    thresholdHigh: row.thresholdHigh ?? 70,
    cronExpression: row.cronExpression || '',
    durationMin: row.durationMin || row.duration || 30,
  }
  showAdd.value = true
}

async function submitRule() {
  try {
    if (editingRule.value) {
      await irrigationApi.updateRule(editingRule.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await irrigationApi.createRule(form.value)
      ElMessage.success('创建成功')
    }
    showAdd.value = false
    editingRule.value = null
    form.value = {
      name: '',
      ruleType: 'THRESHOLD',
      deviceId: '',
      sensorDeviceId: '',
      thresholdLow: 40,
      thresholdHigh: 70,
      cronExpression: '',
      durationMin: 30,
    }
    irrigationStore.fetchRules()
  } catch (error) {
    // Error handled by API interceptor
  }
}

async function delRule(id) {
  try {
    await irrigationApi.deleteRule(id)
    ElMessage.success('已删除')
    irrigationStore.fetchRules()
  } catch (error) {
    // Error handled by API interceptor
  }
}

function handlePageChange(page) {
  irrigationStore.commandPagination.page = page
  irrigationStore.fetchCommands({
    page,
    pageSize: irrigationStore.commandPagination.pageSize,
  })
}

onMounted(() => {
  irrigationStore.fetchRules()
  irrigationStore.fetchCommands()
})
</script>

<style scoped>
.irrigation-page {
  padding: 0;
}

.sub-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
  border-bottom: 2px solid var(--sf-border);
}

.sub-tab {
  padding: 10px 20px;
  font-size: 14px;
  cursor: pointer;
  color: var(--sf-text-secondary);
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: color 0.2s, border-color 0.2s;
}

.sub-tab.active {
  color: var(--sf-primary);
  font-weight: 600;
  border-bottom-color: var(--sf-primary);
}

.sub-tab:hover:not(.active) {
  color: var(--sf-text);
}

.filter-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
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
