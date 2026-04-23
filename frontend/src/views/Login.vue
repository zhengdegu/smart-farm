<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header><h2>🌱 智慧农业平台</h2></template>
      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item><el-input v-model="form.username" placeholder="用户名" prefix-icon="User" /></el-form-item>
        <el-form-item><el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password /></el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../api'
import { ElMessage } from 'element-plus'
const router = useRouter()
const form = ref({ username: '', password: '' })
const loading = ref(false)
const handleLogin = async () => {
  loading.value = true
  try {
    const res = await authApi.login(form.value)
    localStorage.setItem('token', res.token)
    localStorage.setItem('username', form.value.username)
    router.push('/')
    ElMessage.success('登录成功')
  } catch (e) { /* handled by interceptor */ }
  finally { loading.value = false }
}
</script>
<style scoped>
.login-page { height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.login-card { width: 400px; }
.login-card h2 { text-align: center; margin: 0; }
</style>
