<template>
  <el-container class="layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">
        <el-icon size="24"><Cherry /></el-icon>
        <span v-show="!isCollapse">智慧农业平台</span>
      </div>
      <el-menu :default-active="$route.path" router :collapse="isCollapse" background-color="#001529" text-color="#ffffffa6" active-text-color="#fff">
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="'/' + item.path">
          <el-icon><component :is="item.meta.icon" /></el-icon>
          <template #title>{{ item.meta.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <el-icon class="collapse-btn" @click="isCollapse = !isCollapse" size="20">
          <Fold v-if="!isCollapse" /><Expand v-else />
        </el-icon>
        <div class="header-right">
          <el-button type="danger" size="small" @click="emergencyStop" :icon="Warning">紧急停止</el-button>
          <el-dropdown @command="handleCommand">
            <span class="user-info">{{ username }} <el-icon><ArrowDown /></el-icon></span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Warning } from '@element-plus/icons-vue'
import { irrigationApi } from '../api'
import { ElMessageBox, ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const isCollapse = ref(false)
const username = ref(localStorage.getItem('username') || '管理员')

const menuItems = computed(() => router.options.routes[1]?.children || [])

const handleCommand = (cmd) => {
  if (cmd === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    router.push('/login')
  }
}

const emergencyStop = async () => {
  await ElMessageBox.confirm('确定要紧急停止所有灌溉吗？', '紧急停止', { type: 'warning', confirmButtonText: '确定停止', cancelButtonText: '取消' })
  const count = await irrigationApi.emergencyStop()
  ElMessage.success(`已关闭 ${count} 个阀门`)
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #001529; transition: width 0.3s; overflow: hidden; }
.logo { height: 60px; display: flex; align-items: center; justify-content: center; gap: 8px; color: #fff; font-size: 16px; font-weight: 600; white-space: nowrap; }
.header { display: flex; align-items: center; justify-content: space-between; background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
.collapse-btn { cursor: pointer; }
.header-right { display: flex; align-items: center; gap: 16px; }
.user-info { cursor: pointer; display: flex; align-items: center; gap: 4px; }
</style>
