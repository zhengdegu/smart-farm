<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="14">
        <el-card>
          <template #header><div style="display:flex;justify-content:space-between;align-items:center"><span>种植模板</span><el-button type="primary" size="small" @click="showAdd=true">自定义模板</el-button></div></template>
          <el-table :data="templates" stripe>
            <el-table-column prop="name" label="模板名称" />
            <el-table-column prop="cropType" label="作物" width="100"><template #default="{row}"><el-tag size="small">{{ row.cropType }}</el-tag></template></el-table-column>
            <el-table-column label="阶段数" width="80"><template #default="{row}">{{ row.stages?.length || 0 }}</template></el-table-column>
            <el-table-column prop="isSystem" label="类型" width="80"><template #default="{row}"><el-tag :type="row.isSystem?'':'success'" size="small">{{ row.isSystem?'系统':'自定义' }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="100"><template #default="{row}"><el-button v-if="!row.isSystem" size="small" type="danger" @click="delTemplate(row.id)">删除</el-button></template></el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header><div style="display:flex;justify-content:space-between;align-items:center"><span>大棚种植记录</span><el-button type="primary" size="small" @click="showPlanting=true">新增</el-button></div></template>
          <el-table :data="plantings" stripe size="small">
            <el-table-column prop="greenhouseNo" label="大棚" width="70" />
            <el-table-column label="作物"><template #default="{row}">{{ row.cropTemplate?.name }}</template></el-table-column>
            <el-table-column prop="currentStage" label="当前阶段" width="90" />
            <el-table-column prop="plantingDate" label="种植日期" width="110" />
            <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag :type="row.status==='ACTIVE'?'success':'info'" size="small">{{ row.status }}</el-tag></template></el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
    <el-dialog v-model="showPlanting" title="新增种植记录" width="400">
      <el-form :model="plantForm" label-width="80px">
        <el-form-item label="大棚号"><el-input v-model="plantForm.greenhouseNo" /></el-form-item>
        <el-form-item label="种植模板"><el-select v-model="plantForm.templateId"><el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" /></el-select></el-form-item>
        <el-form-item label="种植日期"><el-date-picker v-model="plantForm.plantingDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showPlanting=false">取消</el-button><el-button type="primary" @click="addPlanting">确定</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { cropApi } from '../../api'
import { ElMessage } from 'element-plus'
const templates = ref([]), plantings = ref([]), showAdd = ref(false), showPlanting = ref(false)
const plantForm = ref({ greenhouseNo: '', templateId: null, plantingDate: '' })
const load = async () => { templates.value = await cropApi.listTemplates(); plantings.value = await cropApi.listPlantings() }
onMounted(load)
const delTemplate = async (id) => { await cropApi.deleteTemplate(id); ElMessage.success('已删除'); load() }
const addPlanting = async () => { await cropApi.createPlanting(plantForm.value); showPlanting.value = false; ElMessage.success('创建成功'); load() }
</script>
