<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>设备管理</span>
          <div style="display:flex;gap:8px">
            <el-select v-model="filter.type" placeholder="设备类型" clearable style="width:120px" @change="loadDevices">
              <el-option label="传感器" value="SENSOR" /><el-option label="阀门" value="VALVE" /><el-option label="网关" value="GATEWAY" />
            </el-select>
            <el-select v-model="filter.status" placeholder="状态" clearable style="width:100px" @change="loadDevices">
              <el-option label="在线" value="ONLINE" /><el-option label="离线" value="OFFLINE" /><el-option label="故障" value="FAULT" />
            </el-select>
            <el-button type="primary" @click="showAdd = true" :icon="Plus">注册设备</el-button>
          </div>
        </div>
      </template>
      <el-table :data="devices" stripe>
        <el-table-column prop="deviceId" label="设备ID" width="160" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="deviceType" label="类型" width="100">
          <template #default="{row}">
            <el-tag :type="row.deviceType==='SENSOR'?'success':row.deviceType==='VALVE'?'primary':'warning'" size="small">{{ row.deviceType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{row}">
            <el-tag :type="row.status==='ONLINE'?'success':row.status==='FAULT'?'danger':'info'" size="small" effect="dark">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="greenhouseNo" label="大棚" width="80" />
        <el-table-column prop="location" label="位置" />
        <el-table-column prop="lastOnlineAt" label="最后在线" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{row}">
            <el-button size="small" @click="editDevice(row)">编辑</el-button>
            <el-popconfirm title="确定删除?" @confirm="delDevice(row.deviceId)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="showAdd" :title="editingDevice ? '编辑设备' : '注册设备'" width="500">
      <el-form :model="form" label-width="80px">
        <el-form-item label="设备ID" v-if="!editingDevice"><el-input v-model="form.deviceId" /></el-form-item>
        <el-form-item label="类型" v-if="!editingDevice">
          <el-select v-model="form.deviceType"><el-option label="传感器" value="SENSOR" /><el-option label="阀门" value="VALVE" /><el-option label="网关" value="GATEWAY" /></el-select>
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="位置"><el-input v-model="form.location" /></el-form-item>
        <el-form-item label="大棚号"><el-input v-model="form.greenhouseNo" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd=false">取消</el-button>
        <el-button type="primary" @click="submitDevice">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { deviceApi } from '../../api'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const devices = ref([])
const filter = ref({ type: '', status: '' })
const showAdd = ref(false)
const editingDevice = ref(null)
const form = ref({ deviceId: '', deviceType: 'SENSOR', name: '', location: '', greenhouseNo: '' })

const loadDevices = async () => { devices.value = await deviceApi.list(filter.value) }
onMounted(loadDevices)

const editDevice = (row) => {
  editingDevice.value = row.deviceId
  form.value = { name: row.name, location: row.location, greenhouseNo: row.greenhouseNo }
  showAdd.value = true
}

const submitDevice = async () => {
  if (editingDevice.value) {
    await deviceApi.update(editingDevice.value, form.value)
    ElMessage.success('更新成功')
  } else {
    await deviceApi.create(form.value)
    ElMessage.success('注册成功')
  }
  showAdd.value = false
  editingDevice.value = null
  form.value = { deviceId: '', deviceType: 'SENSOR', name: '', location: '', greenhouseNo: '' }
  loadDevices()
}

const delDevice = async (id) => { await deviceApi.delete(id); ElMessage.success('已删除'); loadDevices() }
</script>
