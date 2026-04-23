import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layouts/MainLayout.vue'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/screen3d', name: 'Screen3D', component: () => import('../views/screen3d/Index.vue') },
  {
    path: '/', component: Layout, redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/dashboard/Index.vue'), meta: { title: '仪表盘', icon: 'Odometer' } },
      { path: 'devices', name: 'Devices', component: () => import('../views/device/Index.vue'), meta: { title: '设备管理', icon: 'Monitor' } },
      { path: 'irrigation', name: 'Irrigation', component: () => import('../views/irrigation/Index.vue'), meta: { title: '灌溉管理', icon: 'Ship' } },
      { path: 'alerts', name: 'Alerts', component: () => import('../views/alert/Index.vue'), meta: { title: '告警管理', icon: 'Bell' } },
      { path: 'reports', name: 'Reports', component: () => import('../views/report/Index.vue'), meta: { title: '数据报表', icon: 'DataAnalysis' } },
      { path: 'crop', name: 'Crop', component: () => import('../views/crop/Index.vue'), meta: { title: '种植管理', icon: 'Cherry' } },
      { path: 'users', name: 'Users', component: () => import('../views/user/Index.vue'), meta: { title: '用户管理', icon: 'User' } },
      { path: 'settings', name: 'Settings', component: () => import('../views/settings/Index.vue'), meta: { title: '系统设置', icon: 'Setting' } },
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
