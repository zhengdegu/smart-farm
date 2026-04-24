<template>
  <div class="suggestions-page">
    <FilterBar
      :chips="chipConfig"
      @chip-change="onChipChange"
    />

    <div class="card-list">
      <div v-if="filteredSuggestions.length === 0 && !loading" class="empty-state">
        暂无优化建议
      </div>
      <div
        v-for="item in filteredSuggestions"
        :key="item.id"
        class="suggestion-card"
      >
        <div class="card-header">
          <span class="card-title">🏠 {{ item.greenhouseNo }} · {{ mapSuggestionType(item.suggestionType) }}</span>
          <el-tag
            :color="mapSuggestionStatus(item.status).color"
            effect="dark"
            size="small"
            style="border: none;"
          >
            {{ mapSuggestionStatus(item.status).label }}
          </el-tag>
        </div>
        <div class="card-body">
          <div class="card-row">
            <span class="label">当前值:</span>
            <span>{{ item.currentValue }}</span>
          </div>
          <div class="card-row">
            <span class="label">建议值:</span>
            <span>{{ item.suggestedValue }}</span>
          </div>
          <div class="card-row confidence-row">
            <span class="label">置信度:</span>
            <el-progress
              :percentage="Math.round((item.confidence || 0) * 100)"
              :stroke-width="14"
              :color="'var(--sf-primary)'"
              style="flex: 1; margin-left: 8px;"
            />
          </div>
          <div class="card-row reasoning">
            <span class="label">AI推理说明:</span>
            <span>{{ item.reasoning }}</span>
          </div>
        </div>
        <div v-if="item.status === 'PENDING'" class="card-actions">
          <el-button plain @click="handleReject(item)">拒绝</el-button>
          <el-button type="primary" @click="handleAccept(item)">接受</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import FilterBar from '../../components/FilterBar.vue'
import { aiApi } from '../../api'
import { mapSuggestionType, mapSuggestionStatus, filterByField } from '../../utils/ai-helpers'

const suggestions = ref([])
const loading = ref(false)
const activeStatus = ref('')

const filteredSuggestions = computed(() => {
  return filterByField(suggestions.value, 'status', activeStatus.value)
})

const chipConfig = [
  { value: '', label: '全部' },
  { value: 'PENDING', label: '待审批' },
  { value: 'ACCEPTED', label: '已接受' },
  { value: 'REJECTED', label: '已拒绝' },
]

function onChipChange(value) {
  activeStatus.value = value
}

async function fetchSuggestions() {
  loading.value = true
  try {
    const data = await aiApi.suggestions()
    suggestions.value = Array.isArray(data) ? data : []
  } catch {
    ElMessage.error('加载优化建议失败')
  } finally {
    loading.value = false
  }
}

async function handleAccept(item) {
  try {
    await ElMessageBox.confirm(
      `确认接受该优化建议？\n棚号: ${item.greenhouseNo}\n建议: ${item.suggestedValue}`,
      '确认接受',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'info' }
    )
    await aiApi.acceptSuggestion(item.id)
    ElMessage.success('建议已接受')
    await fetchSuggestions()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

async function handleReject(item) {
  try {
    await aiApi.rejectSuggestion(item.id)
    ElMessage.success('建议已拒绝')
    await fetchSuggestions()
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  fetchSuggestions()
})
</script>

<style scoped>
.suggestions-page {
  padding: 0;
}

.card-list {
  max-width: 720px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: var(--sf-text-secondary);
  font-size: 14px;
}

.suggestion-card {
  background: var(--sf-card);
  border-radius: var(--sf-radius);
  box-shadow: var(--sf-shadow);
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--sf-border);
}

.card-title {
  font-size: 15px;
  font-weight: 600;
}

.card-body {
  padding: 16px 20px;
}

.card-row {
  margin-bottom: 10px;
  font-size: 14px;
  line-height: 1.6;
}

.card-row .label {
  color: var(--sf-text-secondary);
  margin-right: 8px;
}

.confidence-row {
  display: flex;
  align-items: center;
}

.reasoning {
  color: var(--sf-text-secondary);
  font-size: 13px;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 20px;
  border-top: 1px solid var(--sf-border);
}
</style>
