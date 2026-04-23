<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="14">
        <el-card header="灌溉规则">
          <template #header><div style="display:flex;justify-content:space-between;align-items:center"><span>灌溉规则</span><el-button type="primary" size="small" @click="showAdd=true">新增规则</el-button></div></template>
          <el-table :data="rules" stripe>
            <el-table-column prop="name" label="规则名称" />
            <el-table-column prop="ruleType" label="类型" width="100"><template #default="{row}"><el-tag size="small">{{ row.ruleType }}</el-tag></template></el-table-column>
            <el-table-column prop="deviceId" label="阀门" width="140" />
            <el-table-column label="条件" width="200"><template #default="{row}">{{ row.ruleType==='THRESHOLD' ? `${row.thresholdLow}% ~ ${row.thresholdHigh}%` : row.cronExpression }}</template></el-table-column>
            <el-table-column prop="enabled" label="状态" width="80"><template #default="{row}"><el-switch :model-value="row.enabled" @change="toggleRule(row.id)" /></template></el-table-column>
            <el-table-column label="操作" width="100"><template #default="{row}"><el-button size="small" type="danger" @click="delRule(row.id)">删除</el-button></template></el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card header="指令记录">
          <el-table :data="commands" stripe size="small" max-height="400">
            <el-table-column prop="cmdId" label="指令ID" width="140" />
            <el-table-column prop="action" label="动作" width="100" />
            <el-table-column prop="status" label="状态" width="100"><template #default="{row}"><el-tag :type="cmdColor(row.status)" size="small">{{ row.status }}</el-tag></template></el-table-column>
            <el-table-column prop="createdAt" label="时间" width="160" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
    <el-dialog v-model="showAdd" title="新增灌溉规则" width="500">
      <el-form :model="form" label-width="90px">
        <el-form-item label="规则名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="规则类型"><el-radio-group v-model="form.ruleType"><el-radio value="THRESHOLD">阈值</el-radio><el-radio value="SCHEDULE">定时</el-radio></el-radio-group></el-form-item>
        <el-form-item label="阀门设备ID"><el-input v-model="form.deviceId" /></el-form-item>
        <el-form-item label="传感器ID" v-if="form.ruleType==='THRESHOLD'"><el-input v-model="form.sensorDeviceId" /></el-form-item>
        <el-form-item label="湿度下限" v-if="form.ruleType==='THRESHOLD'"><el-input-number v-model="form.thresholdLow" :min="0" :max="100" /></el-form-item>
        <el-form-item label="湿度上限" v-if="form.ruleType==='THRESHOLD'"><el-input-number v-model="form.thresholdHigh" :min="0" :max="100" /></el-form-item>
        <el-form-item label="Cron表达式" v-if="form.ruleType==='SCHEDULE'"><el-input v-model="form.cronExpression" placeholder="0 6,18 * * *" /></el-form-item>
        <el-form-item label="灌溉时长(分)"><el-input-number v-model="form.durationMin" :min="1" :max="120" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showAdd=false">取消</el-button><el-button type="primary" @click="addRule">确定</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { irrigationApi } from '../../api'
import { ElMessage } from 'element-plus'
const rules = ref([]), commands = ref([]), showAdd = ref(false)
const form = ref({ name:'', ruleType:'THRESHOLD', deviceId:'', sensorDeviceId:'', thresholdLow:40, thresholdHigh:70, cronExpression:'', durationMin:30 })
const cmdColor = (s) => ({ EXECUTED:'success', FAILED:'danger', SENT:'warning', CONFIRMED:'primary' }[s] || 'info')
const load = async () => { rules.value = await irrigationApi.listRules(); commands.value = await irrigationApi.listCommands() }
onMounted(load)
const addRule = async () => { await irrigationApi.createRule(form.value); showAdd.value=false; ElMessage.success('创建成功'); load() }
const toggleRule = async (id) => { await irrigationApi.toggleRule(id); load() }
const delRule = async (id) => { await irrigationApi.deleteRule(id); ElMessage.success('已删除'); load() }
</script>
