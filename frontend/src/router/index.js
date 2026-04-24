import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layouts/MainLayout.vue'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/screen3d', name: 'Screen3D', component: () => import('../views/screen3d/Index.vue') },
  {
    path: '/', component: Layout, redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/dashboard/Index.vue'), meta: { title: '总览仪表盘', icon: '📊', group: '概览', breadcrumb: '首页 / 总览仪表盘' } },
      { path: 'devices', name: 'Devices', component: () => import('../views/device/Index.vue'), meta: { title: '设备管理', icon: '📡', group: '业务管理', breadcrumb: '业务管理 / 设备管理' } },
      { path: 'irrigation', name: 'Irrigation', component: () => import('../views/irrigation/Index.vue'), meta: { title: '灌溉管理', icon: '💧', group: '业务管理', breadcrumb: '业务管理 / 灌溉管理' } },
      { path: 'alerts', name: 'Alerts', component: () => import('../views/alert/Index.vue'), meta: { title: '告警中心', icon: '🔔', group: '业务管理', breadcrumb: '业务管理 / 告警中心' } },
      { path: 'crop', name: 'Crop', component: () => import('../views/crop/Index.vue'), meta: { title: '种植管理', icon: '🌿', group: '业务管理', breadcrumb: '业务管理 / 种植管理' } },
      { path: 'reports', name: 'Reports', component: () => import('../views/report/Index.vue'), meta: { title: '数据报表', icon: '📈', group: '数据分析', breadcrumb: '数据分析 / 数据报表' } },
      { path: 'monthly', name: 'Monthly', component: () => import('../views/report/Monthly.vue'), meta: { title: '月度报告', icon: '📄', group: '数据分析', breadcrumb: '数据分析 / 月度报告' } },
      { path: 'users', name: 'Users', component: () => import('../views/user/Index.vue'), meta: { title: '用户管理', icon: '👥', group: '系统', breadcrumb: '系统 / 用户管理' } },
      { path: 'settings', name: 'Settings', component: () => import('../views/settings/Index.vue'), meta: { title: '系统设置', icon: '⚙️', group: '系统', breadcrumb: '系统 / 系统设置' } },
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) next('/login')
  else next()
})

export default router
