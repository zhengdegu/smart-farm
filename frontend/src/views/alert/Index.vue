<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>告警管理</span>
        <div style="display:flex;gap:8px">
          <el-select v-model="filter.level" placeholder="级别" clearable style="width:100px" @change="load">
            <el-option label="L1紧急" value="L1" /><el-option label="L2重要" value="L2" /><el-option label="L3一般" value="L3" />
          </el-select>
          <el-select v-model="filter.status" placeholder="状态" clearable style="width:100px" @change="load">
            <el-option label="待处理" value="PENDING" /><el-option label="已确认" value="ACKNOWLEDGED" /><el-option label="已解决" value="RESOLVED" />
          </el-select>
        </div>
      </div>
    </template>
    <el-table :data="alerts.content" stripe>
      <el-table-column prop="level" label="级别" width="80">
        <template #default="{row}"><el-tag :type="row.level==='L1'?'danger':row.level==='L2'?'warning':'info'" size="small" effect="dark">{{ row.level }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="160" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="deviceId" label="设备" width="140" />
      <el-table-column prop="status" label="状态" width="90"><template #default="{row}"><el-tag :type="row.status==='PENDING'?'warning':'success'" size="small">{{ row.status }}</el-tag></template></el-table-column>
      <el-table-column prop="createdAt" label="时间" width="170" />
      <el-table-column label="操作" width="100">
        <template #default="{row}"><el-button v-if="row.status==='PENDING'" size="small" type="primary" @click="ack(row.id)">确认</el-button></template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:16px;justify-content:end" :total="alerts.totalElements||0" :page-size="20" v-model:current-page="filter.page" @current-change="load" />
  </el-card>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { alertApi } from '../../api'
import { ElMessage } from 'element-plus'
const alerts = ref({ content: [] })
const filter = ref({ level: '', status: '', page: 1 })
const load = async () => { alerts.value = await alertApi.list(filter.value) }
onMounted(load)
const ack = async (id) => { await alertApi.acknowledge(id); ElMessage.success('已确认'); load() }
</script>
