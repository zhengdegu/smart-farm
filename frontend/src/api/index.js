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
  listRules: () => api.get('/irrigation/rules'),
  createRule: (data) => api.post('/irrigation/rules', data),
  updateRule: (id, data) => api.put(`/irrigation/rules/${id}`, data),
  deleteRule: (id) => api.delete(`/irrigation/rules/${id}`),
  toggleRule: (id) => api.patch(`/irrigation/rules/${id}/toggle`),
  sendCommand: (data) => api.post('/irrigation/commands', data),
  listCommands: () => api.get('/irrigation/commands'),
  emergencyStop: () => api.post('/irrigation/commands/emergency-stop'),
}

// Alert
export const alertApi = {
  list: (params) => api.get('/alerts', { params }),
  acknowledge: (id) => api.patch(`/alerts/${id}/acknowledge`),
  stats: () => api.get('/alerts/stats'),
}

// Report
export const reportApi = {
  dailyStats: (params) => api.get('/reports/irrigation/daily', { params }),
  monthlyStats: (params) => api.get('/reports/irrigation/monthly', { params }),
  waterSaving: (params) => api.get('/reports/water-saving', { params }),
  alertStats: (params) => api.get('/reports/alerts/stats', { params }),
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
  me: () => api.get('/auth/me'),
}
