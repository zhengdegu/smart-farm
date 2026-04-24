<template>
  <div class="monthly-page">
    <!-- Top bar with generate button -->
    <div class="filter-bar">
      <div style="flex: 1" />
      <button class="btn btn-primary" @click="generateReport" :disabled="generating">
        📄 生成本月报告
      </button>
    </div>

    <!-- Report List -->
    <div class="table-card">
      <div class="table-header">
        <span class="title">报告列表</span>
      </div>
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="reports.length === 0" class="empty-state">暂无报告</div>
      <template v-else>
        <div
          v-for="report in reports"
          :key="report.id"
          class="report-item"
        >
          <div class="report-icon">📄</div>
          <div class="report-info">
            <div class="report-name">{{ report.name }}</div>
            <div class="report-meta">{{ report.meta }}</div>
          </div>
          <div class="report-actions">
            <template v-if="report.status === 'generating'">
              <StatusTag status="pending" text="生成中" />
            </template>
            <template v-else>
              <button class="btn btn-outline btn-sm" @click="downloadReport(report)">📥 下载PDF</button>
              <button class="btn btn-outline btn-sm" @click="previewReport(report)">👁 预览</button>
            </template>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import { reportApi } from '../../api'

const reports = ref([])
const loading = ref(false)
const generating = ref(false)

// Load report list
async function loadReports() {
  loading.value = true
  try {
    const data = await reportApi.monthlyList()
    reports.value = Array.isArray(data) ? data : []
  } catch {
    // Fallback to demo data if API not available
    reports.value = [
      { id: 0, name: '2026年4月运营报告', meta: '生成中... · 预计 2026-04-30', status: 'generating' },
      { id: 1, name: '2026年3月运营报告', meta: '2026-03-31 生成 · 12页 · 2.4MB', status: 'completed' },
      { id: 2, name: '2026年2月运营报告', meta: '2026-02-28 生成 · 10页 · 2.1MB', status: 'completed' },
      { id: 3, name: '2026年1月运营报告', meta: '2026-01-31 生成 · 11页 · 2.3MB', status: 'completed' },
      { id: 4, name: '2025年12月运营报告', meta: '2025-12-31 生成 · 14页 · 2.8MB', status: 'completed' },
    ]
  } finally {
    loading.value = false
  }
}

// Generate monthly report
async function generateReport() {
  generating.value = true
  try {
    await reportApi.generateMonthly()
    ElMessage.success('报告生成任务已提交')
    await loadReports()
  } catch {
    ElMessage.error('报告生成失败')
  } finally {
    generating.value = false
  }
}

// Download report PDF
async function downloadReport(report) {
  try {
    const blob = await reportApi.downloadMonthly(report.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${report.name}.pdf`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载失败')
  }
}

// Preview report
function previewReport(report) {
  ElMessage.info(`预览: ${report.name}`)
}

// Load data on mount
onMounted(() => {
  loadReports()
})
</script>

<style scoped>
.monthly-page {
  padding: 0;
}

/* Filter Bar */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

/* Buttons */
.btn {
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  border: none;
  transition: opacity 0.2s;
  white-space: nowrap;
}

.btn:hover {
  opacity: 0.85;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-outline {
  background: var(--sf-card);
  border: 1px solid var(--sf-border);
  color: var(--sf-text);
}

.btn-primary {
  background: var(--sf-primary);
  color: #fff;
}

.btn-sm {
  padding: 4px 10px;
  font-size: 12px;
}

/* Table Card */
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

/* Report Item */
.report-item {
  display: flex;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
  gap: 12px;
}

.report-item:last-child {
  border-bottom: none;
}

.report-icon {
  font-size: 28px;
}

.report-info {
  flex: 1;
}

.report-name {
  font-size: 14px;
  font-weight: 500;
}

.report-meta {
  font-size: 12px;
  color: var(--sf-text-secondary);
  margin-top: 2px;
}

.report-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* States */
.loading-state,
.empty-state {
  text-align: center;
  padding: 40px 0;
  color: var(--sf-text-secondary);
  font-size: 14px;
}
</style>
