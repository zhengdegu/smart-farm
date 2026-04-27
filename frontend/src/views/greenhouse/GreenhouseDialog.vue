<template>
  <el-dialog
    :model-value="visible"
    :title="greenhouse ? '编辑大棚' : '新增大棚'"
    width="500"
    @close="$emit('close')"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="棚号" prop="greenhouseNo">
        <el-input v-model="form.greenhouseNo" :disabled="!!greenhouse" placeholder="请输入棚号" />
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入名称" />
      </el-form-item>
      <el-form-item label="面积(㎡)">
        <el-input-number v-model="form.area" :min="0" :precision="1" controls-position="right" style="width: 100%" />
      </el-form-item>
      <el-form-item label="位置">
        <el-input v-model="form.location" placeholder="请输入位置" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
      </el-form-item>
      <el-form-item v-if="greenhouse" label="状态">
        <el-select v-model="form.status" style="width: 100%">
          <el-option label="使用中" value="ACTIVE" />
          <el-option label="空闲" value="IDLE" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('close')">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { greenhouseApi } from '../../api'

const props = defineProps({
  visible: { type: Boolean, default: false },
  greenhouse: { type: Object, default: null },
})

const emit = defineEmits(['close', 'saved'])

const formRef = ref(null)
const submitting = ref(false)
const form = ref({
  greenhouseNo: '',
  name: '',
  area: null,
  location: '',
  description: '',
  status: 'ACTIVE',
})

const rules = {
  greenhouseNo: [{ required: true, message: '请输入棚号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
}

watch(() => props.visible, (val) => {
  if (val) {
    if (props.greenhouse) {
      form.value = {
        greenhouseNo: props.greenhouse.greenhouseNo,
        name: props.greenhouse.name,
        area: props.greenhouse.area,
        location: props.greenhouse.location || '',
        description: props.greenhouse.description || '',
        status: props.greenhouse.status || 'ACTIVE',
      }
    } else {
      form.value = { greenhouseNo: '', name: '', area: null, location: '', description: '', status: 'ACTIVE' }
    }
    formRef.value?.clearValidate()
  }
})

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    if (props.greenhouse) {
      await greenhouseApi.update(props.greenhouse.id, {
        name: form.value.name,
        area: form.value.area,
        location: form.value.location,
        description: form.value.description,
        status: form.value.status,
      })
      ElMessage.success('更新成功')
    } else {
      await greenhouseApi.create(form.value)
      ElMessage.success('创建成功')
    }
    emit('saved')
  } catch (e) {
    // Error handled by API interceptor
  } finally {
    submitting.value = false
  }
}
</script>
