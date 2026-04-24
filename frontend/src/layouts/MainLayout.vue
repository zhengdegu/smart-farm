<template>
  <div class="layout">
    <nav class="sidebar">
      <div class="sidebar-logo">🌱 智慧农业平台</div>
      <template v-for="group in navGroups" :key="group.label">
        <div class="nav-group-label">{{ group.label }}</div>
        <div
          v-for="item in group.items"
          :key="item.path"
          class="nav-item"
          :class="{ active: route.path === '/' + item.path }"
          @click="router.push('/' + item.path)"
        >
          <span class="nav-icon">{{ item.meta.icon }}</span>
          {{ item.meta.title }}
          <span v-if="item.path === 'alerts' && alertStore.pendingCount > 0" class="nav-badge">
            {{ alertStore.pendingCount }}
          </span>
          <span v-if="item.path === 'ai/patrol' && aiStore.patrolUnread > 0" class="nav-badge">
            {{ aiStore.patrolUnread }}
          </span>
        </div>
      </template>
    </nav>
    <div class="main">
      <div class="topbar">
        <div class="topbar-left">
          <span class="topbar-title">{{ route.meta.title }}</span>
          <span class="topbar-breadcrumb">{{ route.meta.breadcrumb }}</span>
        </div>
        <div class="topbar-right">
          <input class="topbar-search" placeholder="🔍 搜索设备、告警..." />
          <span class="topbar-bell">🔔 <span v-if="alertStore.pendingCount > 0" class="badge">{{ alertStore.pendingCount }}</span></span>
          <span>👤 {{ username }}</span>
        </div>
      </div>
      <div class="content">
        <router-view />
      </div>
      <FloatingButton />
      <ChatPanel />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAlertStore } from '../stores/alert'
import { useAiStore } from '../stores/ai'
import FloatingButton from '../components/FloatingButton.vue'
import ChatPanel from '../components/ChatPanel.vue'

const router = useRouter()
const route = useRoute()
const alertStore = useAlertStore()
const aiStore = useAiStore()
const username = ref(localStorage.getItem('username') || '管理员')

const childRoutes = computed(() => router.options.routes.find(r => r.path === '/')?.children || [])

const navGroups = computed(() => {
  const groups = []
  const groupMap = {}
  for (const item of childRoutes.value) {
    const groupLabel = item.meta?.group
    if (!groupLabel) continue
    if (!groupMap[groupLabel]) {
      groupMap[groupLabel] = { label: groupLabel, items: [] }
      groups.push(groupMap[groupLabel])
    }
    groupMap[groupLabel].items.push(item)
  }
  return groups
})

onMounted(() => {
  alertStore.fetchStats()
  aiStore.fetchPatrolUnread()
})
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* Sidebar */
.sidebar {
  width: var(--sf-sidebar-width, 220px);
  background: #1B2A1E;
  color: #fff;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow-y: auto;
}

.sidebar-logo {
  padding: 20px;
  font-size: 18px;
  font-weight: 700;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-group-label {
  padding: 16px 20px 6px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.nav-item {
  padding: 11px 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.15s;
  user-select: none;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.nav-item.active {
  background: #2E7D32;
  color: #fff;
  font-weight: 600;
}

.nav-icon {
  font-size: 16px;
  width: 20px;
  text-align: center;
}

.nav-badge {
  margin-left: auto;
  background: var(--sf-l1, #D32F2F);
  color: #fff;
  border-radius: 10px;
  padding: 1px 7px;
  font-size: 11px;
}

/* Main area */
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Topbar */
.topbar {
  height: var(--sf-topbar-height, 56px);
  background: #fff;
  border-bottom: 1px solid var(--sf-border, #E0E0E0);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.topbar-title {
  font-size: 16px;
  font-weight: 600;
}

.topbar-breadcrumb {
  font-size: 13px;
  color: var(--sf-text-secondary, #757575);
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 14px;
  color: var(--sf-text-secondary, #757575);
}

.topbar-search {
  border: 1px solid var(--sf-border, #E0E0E0);
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 13px;
  width: 200px;
  outline: none;
}

.topbar-search:focus {
  border-color: var(--sf-primary-light, #4CAF50);
}

.topbar-bell {
  position: relative;
}

.badge {
  background: var(--sf-l1, #D32F2F);
  color: #fff;
  border-radius: 10px;
  padding: 2px 8px;
  font-size: 11px;
}

/* Content */
.content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  background: var(--sf-bg, #F0F2F5);
}
</style>
