import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const api = axios.create({ baseURL: '/api/v1', timeout: 15000 })

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  res => res.data,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }
    ElMessage.error(err.response?.data?.message || '请求失败')
    return Promise.reject(err)
  }
)

export default api

// Device
export const deviceApi = {
  list: (params) => api.get('/devices', { params }),
  get: (id) => api.get(`/devices/${id}`),
  create: (data) => api.post('/devices', data),
  update: (id, data) => api.put(`/devices/${id}`, data),
  delete: (id) => api.delete(`/devices/${id}`),
}

// Telemetry
export const telemetryApi = {
  latest: (deviceId) => api.get(`/telemetry/${deviceId}/latest`),
  history: (deviceId, params) => api.get(`/telemetry/${deviceId}/history`, { params }),
  aggregate: (deviceId, params) => api.get(`/telemetry/${deviceId}/aggregate`, { params }),
  trend: (params) => api.get('/telemetry/trend', { params }),
  dashboard: () => api.get('/telemetry/dashboard'),
}

// Irrigation
export const irrigationApi = {
  listRules: () => api.get('/rules'),
  createRule: (data) => api.post('/rules', data),
  updateRule: (id, data) => api.put(`/rules/${id}`, data),
  deleteRule: (id) => api.delete(`/rules/${id}`),
  toggleRule: (id) => api.post(`/rules/${id}/toggle`),
  sendCommand: (data) => api.post('/commands', data),
  listCommands: () => api.get('/commands/history'),
  emergencyStop: () => api.post('/commands/emergency-stop'),
}

// Alert
export const alertApi = {
  list: (params) => api.get('/alerts', { params }),
  acknowledge: (id) => api.post(`/alerts/${id}/acknowledge`),
  stats: () => api.get('/alerts/stats'),
}

// Report
export const reportApi = {
  dailyStats: (params) => api.get('/reports/irrigation/daily', { params }),
  monthlyStats: (params) => api.get('/reports/irrigation/monthly', { params }),
  waterSaving: (params) => api.get('/reports/water-saving', { params }),
  alertStats: (params) => api.get('/reports/alerts/stats', { params }),
  monthlyList: () => api.get('/reports/monthly'),
  generateMonthly: () => api.post('/reports/monthly/generate'),
  downloadMonthly: (id) => api.get(`/reports/monthly/${id}/download`, { responseType: 'blob' }),
}

// Crop
export const cropApi = {
  listTemplates: (params) => api.get('/crop-templates', { params }),
  createTemplate: (data) => api.post('/crop-templates', data),
  updateTemplate: (id, data) => api.put(`/crop-templates/${id}`, data),
  deleteTemplate: (id) => api.delete(`/crop-templates/${id}`),
  listPlantings: (params) => api.get('/greenhouse-plantings', { params }),
  createPlanting: (data) => api.post('/greenhouse-plantings', data),
  getStage: (id) => api.get(`/greenhouse-plantings/${id}/stage`),
}

// Auth
export const authApi = {
  login: (data) => api.post('/auth/login', data),
}

// Screen
export const screenApi = {
  getData: () => api.get('/screen/data'),
}

// User
export const userApi = {
  list: (params) => api.get('/users', { params }),
  create: (data) => api.post('/users', data),
  update: (id, data) => api.put(`/users/${id}`, data),
  resetPassword: (id) => api.post(`/users/${id}/reset-password`),
  toggleStatus: (id) => api.post(`/users/${id}/toggle-status`),
  operationLogs: (params) => api.get('/operation-logs', { params }),
}

// Settings
export const settingsApi = {
  getBaseInfo: () => api.get('/settings/base'),
  updateBaseInfo: (data) => api.put('/settings/base', data),
  getNotifications: () => api.get('/settings/notifications'),
  updateNotifications: (data) => api.put('/settings/notifications', data),
  getMqttStatus: () => api.get('/settings/mqtt-status'),
  reconnectMqtt: () => api.post('/settings/mqtt-reconnect'),
}

// Greenhouse
export const greenhouseApi = {
  list: (params) => api.get('/greenhouses', { params }),
  get: (id) => api.get(`/greenhouses/${id}`),
  create: (data) => api.post('/greenhouses', data),
  update: (id, data) => api.put(`/greenhouses/${id}`, data),
  delete: (id) => api.delete(`/greenhouses/${id}`),
  listDevices: (id) => api.get(`/greenhouses/${id}/devices`),
  getEnvironment: (id) => api.get(`/greenhouses/${id}/environment`),
  stats: () => api.get('/greenhouses/stats'),
}

// AI
export const aiApi = {
  chat: (data) => api.post('/ai/chat', data),
  chatHistory: (sessionId) => api.get('/ai/chat/history', { params: { sessionId } }),
  patrolLogs: () => api.get('/ai/patrol/logs'),
  triggerPatrol: (type) => api.post('/ai/patrol/trigger', null, { params: { type } }),
  suggestions: (params) => api.get('/ai/suggestions', { params }),
  acceptSuggestion: (id) => api.post(`/ai/suggestions/${id}/accept`),
  rejectSuggestion: (id) => api.post(`/ai/suggestions/${id}/reject`),
}
