<template>
  <el-card header="用户管理">
    <el-table :data="users" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="role" label="角色" width="120">
        <template #default="{row}"><el-tag :type="row.role==='ADMIN'?'danger':row.role==='OPERATOR'?'primary':'info'" size="small">{{ row.role }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{row}"><el-tag :type="row.status==='ACTIVE'?'success':'info'" size="small">{{ row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
    </el-table>
  </el-card>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api'
const users = ref([])
onMounted(async () => { try { users.value = await api.get('/auth/users') } catch {} })
</script>
