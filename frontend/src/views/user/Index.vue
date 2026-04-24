<template>
  <div class="user-page">
    <!-- Sub-tabs -->
    <div class="sub-tabs">
      <div
        class="sub-tab"
        :class="{ active: activeTab === 'userlist' }"
        @click="activeTab = 'userlist'"
      >
        用户列表
      </div>
      <div
        class="sub-tab"
        :class="{ active: activeTab === 'oplog' }"
        @click="activeTab = 'oplog'"
      >
        操作日志
      </div>
    </div>

    <!-- 用户列表 Tab -->
    <div v-if="activeTab === 'userlist'">
      <div class="filter-bar">
        <div style="flex: 1"></div>
        <el-button type="primary" @click="showAddUser = true">+ 添加用户</el-button>
      </div>

      <div class="table-card">
        <el-table
          :data="users"
          style="width: 100%"
          v-loading="loading"
        >
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="nickname" label="姓名" />
          <el-table-column label="角色" width="120">
            <template #default="{ row }">
              <span :class="['role-badge', getRoleClass(row.role)]">{{ getRoleLabel(row.role) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="手机号" width="140">
            <template #default="{ row }">
              {{ maskPhone(row.phone) }}
            </template>
          </el-table-column>
          <el-table-column prop="baseName" label="所属基地" />
          <el-table-column prop="lastLogin" label="最后登录" width="140" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <StatusTag
                :status="row.status === 'ACTIVE' ? 'online' : 'offline'"
                :text="row.status === 'ACTIVE' ? '正常' : '禁用'"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="editUser(row)">编辑</el-button>
              <el-button
                v-if="row.status === 'ACTIVE'"
                size="small"
                @click="resetPassword(row.id)"
              >
                重置密码
              </el-button>
              <el-button
                v-else
                size="small"
                @click="toggleUserStatus(row.id)"
              >
                启用
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 操作日志 Tab -->
    <div v-if="activeTab === 'oplog'">
      <div class="table-card">
        <div class="table-header">
          <span class="title">操作日志</span>
          <span class="table-count">近7天操作记录</span>
        </div>
        <el-table
          :data="logs"
          style="width: 100%"
          v-loading="logLoading"
        >
          <el-table-column prop="time" label="时间" width="140" />
          <el-table-column prop="user" label="用户" width="100" />
          <el-table-column prop="type" label="操作类型" width="120" />
          <el-table-column prop="content" label="操作内容" />
          <el-table-column prop="ip" label="IP地址" width="140" />
          <el-table-column label="结果" width="100">
            <template #default="{ row }">
              <StatusTag
                v-if="row.result === 'success'"
                status="online"
                text="成功"
              />
              <StatusTag
                v-else-if="row.result === 'failed'"
                status="fault"
                text="失败"
              />
              <StatusTag
                v-else
                status="pending"
                :text="row.result || '—'"
              />
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination
            small
            layout="total, prev, pager, next"
            :total="logPagination.total"
            :page-size="logPagination.pageSize"
            v-model:current-page="logPagination.page"
            @current-change="handleLogPageChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { userApi } from '../../api'
import StatusTag from '../../components/StatusTag.vue'

const activeTab = ref('userlist')

// User list state
const users = ref([])
const loading = ref(false)
const showAddUser = ref(false)

// Operation logs state
const logs = ref([])
const logLoading = ref(false)
const logPagination = ref({ page: 1, pageSize: 10, total: 0 })

// Role helpers
function getRoleClass(role) {
  const map = { ADMIN: 'admin', OPERATOR: 'operator', READONLY: 'readonly' }
  return map[role] || 'readonly'
}

function getRoleLabel(role) {
  const map = { ADMIN: '管理员', OPERATOR: '操作员', READONLY: '只读' }
  return map[role] || role
}

// Phone masking
function maskPhone(phone) {
  if (!phone) return '—'
  if (phone.length >= 7) {
    return phone.substring(0, 3) + '****' + phone.substring(phone.length - 4)
  }
  return phone
}

// User actions
function editUser(row) {
  // TODO: open edit dialog
}

async function resetPassword(id) {
  try {
    await userApi.resetPassword(id)
  } catch {
    // Error handled by API interceptor
  }
}

async function toggleUserStatus(id) {
  try {
    await userApi.toggleStatus(id)
    await fetchUsers()
  } catch {
    // Error handled by API interceptor
  }
}

// Data fetching
async function fetchUsers() {
  loading.value = true
  try {
    const res = await userApi.list()
    users.value = Array.isArray(res) ? res : (res?.content || res?.data || [])
  } catch {
    // Error handled by API interceptor
  } finally {
    loading.value = false
  }
}

async function fetchLogs(page = 1) {
  logLoading.value = true
  try {
    const res = await userApi.operationLogs({ page, pageSize: logPagination.value.pageSize })
    if (Array.isArray(res)) {
      logs.value = res
      logPagination.value.total = res.length
    } else {
      logs.value = res?.content || res?.data || []
      logPagination.value.total = res?.total || res?.totalElements || logs.value.length
    }
  } catch {
    // Error handled by API interceptor
  } finally {
    logLoading.value = false
  }
}

function handleLogPageChange(page) {
  logPagination.value.page = page
  fetchLogs(page)
}

onMounted(() => {
  fetchUsers()
  fetchLogs()
})
</script>

<style scoped>
.user-page {
  padding: 0;
}

.sub-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
  border-bottom: 2px solid var(--sf-border);
}

.sub-tab {
  padding: 10px 20px;
  font-size: 14px;
  cursor: pointer;
  color: var(--sf-text-secondary);
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: color 0.2s, border-color 0.2s;
}

.sub-tab.active {
  color: var(--sf-primary);
  font-weight: 600;
  border-bottom-color: var(--sf-primary);
}

.sub-tab:hover:not(.active) {
  color: var(--sf-text);
}

.filter-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

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

.table-count {
  font-size: 13px;
  color: var(--sf-text-secondary);
}

.pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
}

/* Role badges */
.role-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.role-badge.admin {
  background: rgba(25, 118, 210, 0.1);
  color: #1976D2;
}

.role-badge.operator {
  background: rgba(46, 125, 50, 0.1);
  color: #2E7D32;
}

.role-badge.readonly {
  background: #F5F5F5;
  color: #999;
}
</style>
