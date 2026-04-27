<template>
  <div class="greenhouse-detail">
    <!-- 基本信息 -->
    <div class="detail-card">
      <div class="card-title">基本信息</div>
      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">棚号</span>
          <span class="info-value">{{ greenhouse.greenhouseNo }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">名称</span>
          <span class="info-value">{{ greenhouse.name }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">面积</span>
          <span class="info-value">{{ greenhouse.area != null ? greenhouse.area + ' ㎡' : '—' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">位置</span>
          <span class="info-value">{{ greenhouse.location || '—' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">状态</span>
          <span :class="['gh-status-tag', greenhouse.status === 'ACTIVE' ? 'active' : 'idle']">
            {{ greenhouse.status === 'ACTIVE' ? '使用中' : '空闲' }}
          </span>
        </div>
        <div class="info-item full-width" v-if="greenhouse.description">
          <span class="info-label">描述</span>
          <span class="info-value">{{ greenhouse.description }}</span>
        </div>
      </div>
    </div>

    <!-- 关联设备 -->
    <div class="detail-card">
      <div class="card-title">关联设备</div>
      <el-table v-if="devices.length" :data="devices" size="small">
        <el-table-column prop="name" label="设备名称" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            {{ deviceTypeMap[row.deviceType] || row.deviceType }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span :class="['dev-status', row.status?.toLowerCase()]">
              {{ deviceStatusMap[row.status] || row.status }}
            </span>
          </template>
        </el-table-column>
      </el-table>
      <div v-else class="empty-state">暂无设备</div>
    </div>

    <!-- 种植记录 -->
    <div class="detail-card">
      <div class="card-title">种植记录</div>
      <div v-if="plantings.length" class="planting-list">
        <div v-for="p in plantings" :key="p.id" class="planting-item">
          <div class="planting-crop">{{ p.cropTemplate?.cropType || '—' }}</div>
          <div class="planting-meta">
            <span>种植日期: {{ p.plantingDate || '—' }}</span>
            <span>阶段: {{ p.currentStage || '—' }}</span>
            <span>状态: {{ p.status || '—' }}</span>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">暂无种植</div>
    </div>

    <!-- 环境数据 -->
    <div class="detail-card">
      <div class="card-title">环境数据</div>
      <div v-if="Object.keys(environment).length" class="env-grid">
        <div v-for="(val, key) in environment" :key="key" class="env-item">
          <div class="env-label">{{ envLabelMap[key] || key }}</div>
          <div class="env-value">{{ val.value }}<span class="env-unit">{{ val.unit }}</span></div>
          <div class="env-device">{{ val.deviceName }}</div>
        </div>
      </div>
      <div v-else class="empty-state">暂无数据</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { greenhouseApi, cropApi } from '../../api'

const props = defineProps({
  greenhouse: { type: Object, required: true },
})

const devices = ref([])
const plantings = ref([])
const environment = ref({})

const deviceTypeMap = { SENSOR: '传感器', VALVE: '阀门', GATEWAY: '网关' }
const deviceStatusMap = { ONLINE: '在线', OFFLINE: '离线', FAULT: '故障' }
const envLabelMap = {
  soilMoisture: '土壤湿度',
  soilTemperature: '土壤温度',
  airTemperature: '空气温度',
  airHumidity: '空气湿度',
}

async function loadData() {
  if (!props.greenhouse?.id) return

  try {
    const devRes = await greenhouseApi.listDevices(props.greenhouse.id)
    devices.value = devRes.data || devRes || []
  } catch { devices.value = [] }

  try {
    const plantRes = await cropApi.listPlantings({ greenhouseNo: props.greenhouse.greenhouseNo })
    const all = plantRes.data || plantRes || []
    plantings.value = all.filter(p => p.greenhouseNo === props.greenhouse.greenhouseNo)
  } catch { plantings.value = [] }

  try {
    const envRes = await greenhouseApi.getEnvironment(props.greenhouse.id)
    environment.value = envRes.data || envRes || {}
  } catch { environment.value = {} }
}

watch(() => props.greenhouse, () => loadData(), { immediate: false })
onMounted(() => loadData())
</script>

<style scoped>
.greenhouse-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-card {
  background: var(--sf-card);
  border-radius: var(--sf-radius);
  padding: 20px;
  box-shadow: var(--sf-shadow);
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--sf-border);
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item.full-width {
  grid-column: 1 / -1;
}

.info-label {
  font-size: 12px;
  color: var(--sf-text-secondary);
}

.info-value {
  font-size: 14px;
  color: var(--sf-text);
}

.gh-status-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  width: fit-content;
}

.gh-status-tag.active {
  background: #E8F5E9;
  color: #2E7D32;
}

.gh-status-tag.idle {
  background: #FFF3E0;
  color: #F57C00;
}

.dev-status {
  font-size: 12px;
  font-weight: 500;
}

.dev-status.online { color: #2E7D32; }
.dev-status.offline { color: #999; }
.dev-status.fault { color: #D32F2F; }

.planting-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.planting-item {
  padding: 12px;
  background: #f9f9f9;
  border-radius: var(--sf-radius-sm);
}

.planting-crop {
  font-weight: 600;
  margin-bottom: 4px;
}

.planting-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--sf-text-secondary);
}

.env-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.env-item {
  padding: 12px;
  background: #f9f9f9;
  border-radius: var(--sf-radius-sm);
  text-align: center;
}

.env-label {
  font-size: 12px;
  color: var(--sf-text-secondary);
  margin-bottom: 4px;
}

.env-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--sf-primary);
}

.env-unit {
  font-size: 14px;
  font-weight: 400;
  margin-left: 2px;
}

.env-device {
  font-size: 11px;
  color: var(--sf-text-secondary);
  margin-top: 4px;
}

.empty-state {
  text-align: center;
  padding: 24px;
  color: var(--sf-text-secondary);
  font-size: 14px;
}
</style>
