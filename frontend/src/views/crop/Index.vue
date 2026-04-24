<template>
  <div class="crop-page">
    <!-- Section Title -->
    <h3 class="section-title">作物模板</h3>

    <!-- Crop Template Cards (3-column grid) -->
    <div class="crop-cards">
      <div
        v-for="tpl in cropTemplates"
        :key="tpl.name"
        class="crop-tpl-card"
      >
        <div class="crop-tpl-icon">{{ tpl.icon }}</div>
        <div class="crop-tpl-name">{{ tpl.name }}</div>
        <div class="crop-tpl-desc">{{ tpl.desc }}</div>
        <div class="crop-tpl-params">
          <div class="p-item">
            <div class="p-val">{{ tpl.temp }}</div>
            <div>最佳温度</div>
          </div>
          <div class="p-item">
            <div class="p-val">{{ tpl.humidity }}</div>
            <div>湿度范围</div>
          </div>
          <div class="p-item">
            <div class="p-val">{{ tpl.frequency }}</div>
            <div>灌溉频次</div>
          </div>
          <div class="p-item">
            <div class="p-val">{{ tpl.duration }}</div>
            <div>单次时长</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Planting Records Table -->
    <div class="table-card">
      <div class="table-header">
        <span class="title">种植记录</span>
        <div class="table-actions">
          <el-button type="primary" @click="showPlanting = true">+ 新建种植</el-button>
        </div>
      </div>
      <el-table :data="plantings" style="width: 100%" v-loading="loading">
        <el-table-column label="作物" width="140">
          <template #default="{ row }">
            <span>{{ row.icon }} {{ row.cropName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="greenhouseNo" label="棚号" width="100" />
        <el-table-column prop="plantingDate" label="种植日期" width="120" />
        <el-table-column label="当前阶段" width="120">
          <template #default="{ row }">
            <StatusTag
              :status="row.stageStatus"
              :text="row.currentStage"
            />
          </template>
        </el-table-column>
        <el-table-column label="生长进度" width="180">
          <template #default="{ row }">
            <el-progress
              :percentage="row.progress"
              :stroke-width="8"
              :show-text="false"
              :color="'var(--sf-primary)'"
            />
          </template>
        </el-table-column>
        <el-table-column label="天数" width="140">
          <template #default="{ row }">
            第{{ row.currentDay }}天 / {{ row.totalDays }}天
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default>
            <el-button size="small" plain>详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Add Planting Dialog (kept from existing) -->
    <el-dialog v-model="showPlanting" title="新增种植记录" width="400">
      <el-form :model="plantForm" label-width="80px">
        <el-form-item label="大棚号">
          <el-input v-model="plantForm.greenhouseNo" />
        </el-form-item>
        <el-form-item label="种植模板">
          <el-select v-model="plantForm.templateId">
            <el-option
              v-for="t in templates"
              :key="t.id"
              :label="t.name"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="种植日期">
          <el-date-picker
            v-model="plantForm.plantingDate"
            type="date"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPlanting = false">取消</el-button>
        <el-button type="primary" @click="addPlanting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { cropApi } from '../../api'
import { ElMessage } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'

// Crop template card data (hardcoded with API fallback)
const defaultCropTemplates = [
  {
    icon: '🍅',
    name: '番茄',
    desc: '适合温室种植，生长周期约120天',
    temp: '20-28°C',
    humidity: '50-75%',
    frequency: '2次/天',
    duration: '25min',
    totalDays: 120,
  },
  {
    icon: '🥒',
    name: '黄瓜',
    desc: '喜温喜湿，生长周期约90天',
    temp: '22-30°C',
    humidity: '60-80%',
    frequency: '3次/天',
    duration: '15min',
    totalDays: 90,
  },
  {
    icon: '🥬',
    name: '叶菜',
    desc: '生长快速，周期约45天',
    temp: '15-25°C',
    humidity: '55-70%',
    frequency: '2次/天',
    duration: '10min',
    totalDays: 45,
  },
]

const cropTemplates = ref(defaultCropTemplates)
const templates = ref([])
const rawPlantings = ref([])
const loading = ref(false)
const showPlanting = ref(false)
const plantForm = ref({ greenhouseNo: '', templateId: null, plantingDate: '' })

// Icon mapping for crops
const cropIconMap = {
  '番茄': '🍅',
  '黄瓜': '🥒',
  '叶菜': '🥬',
}

// Total days mapping for crops
const cropDaysMap = {
  '番茄': 120,
  '黄瓜': 90,
  '叶菜': 45,
}

// Early stages use "pending" status, active stages use "online"
const earlyStages = ['苗期', '播种期', '发芽期']

// Compute planting records with display fields
const plantings = computed(() => {
  return rawPlantings.value.map((p) => {
    const cropName = p.cropTemplate?.cropType || p.cropTemplate?.name || '未知'
    const icon = cropIconMap[cropName] || '🌱'
    const totalDays = cropDaysMap[cropName] || 120
    const currentDay = calculateDaysSincePlanting(p.plantingDate)
    const progress = Math.min(Math.round((currentDay / totalDays) * 100), 100)
    const currentStage = p.currentStage || '生长期'
    const stageStatus = earlyStages.includes(currentStage) ? 'pending' : 'online'

    return {
      ...p,
      cropName,
      icon,
      totalDays,
      currentDay,
      progress,
      currentStage,
      stageStatus,
    }
  })
})

function calculateDaysSincePlanting(dateStr) {
  if (!dateStr) return 0
  const plantDate = new Date(dateStr)
  const now = new Date()
  const diffMs = now - plantDate
  const days = Math.floor(diffMs / (1000 * 60 * 60 * 24))
  return Math.max(days, 0)
}

const load = async () => {
  loading.value = true
  try {
    const [tplData, plantData] = await Promise.all([
      cropApi.listTemplates().catch(() => []),
      cropApi.listPlantings().catch(() => []),
    ])
    templates.value = tplData || []
    rawPlantings.value = plantData || []
  } finally {
    loading.value = false
  }
}

const addPlanting = async () => {
  await cropApi.createPlanting(plantForm.value)
  showPlanting.value = false
  plantForm.value = { greenhouseNo: '', templateId: null, plantingDate: '' }
  ElMessage.success('创建成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.crop-page {
  padding: 0;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
}

/* Crop Template Cards Grid */
.crop-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.crop-tpl-card {
  background: var(--sf-card);
  border-radius: var(--sf-radius);
  padding: 20px;
  box-shadow: var(--sf-shadow);
  transition: all 0.2s;
}

.crop-tpl-card:hover {
  box-shadow: var(--sf-shadow-hover);
  transform: translateY(-2px);
}

.crop-tpl-icon {
  font-size: 36px;
  margin-bottom: 8px;
}

.crop-tpl-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.crop-tpl-desc {
  font-size: 13px;
  color: var(--sf-text-secondary);
  line-height: 1.5;
}

.crop-tpl-params {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 12px;
  font-size: 12px;
}

.crop-tpl-params .p-item {
  background: var(--sf-bg);
  border-radius: 6px;
  padding: 6px 8px;
  text-align: center;
}

.crop-tpl-params .p-val {
  font-weight: 600;
  color: var(--sf-primary);
}

/* Planting Records Table Card */
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
