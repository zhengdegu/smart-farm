const app = getApp()

const request = (url, method = 'GET', data = {}) => {
  return new Promise((resolve, reject) => {
    wx.request({
      url: app.globalData.baseUrl + url,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        'Authorization': app.globalData.token ? `Bearer ${app.globalData.token}` : ''
      },
      success(res) {
        if (res.statusCode === 401) {
          wx.removeStorageSync('token')
          app.globalData.token = ''
          wx.redirectTo({ url: '/pages/login/index' })
          reject(new Error('未登录'))
          return
        }
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data)
        } else {
          const msg = res.data?.message || '请求失败'
          wx.showToast({ title: msg, icon: 'none' })
          reject(new Error(msg))
        }
      },
      fail(err) {
        wx.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = {
  // Auth
  login: (data) => request('/auth/login', 'POST', data),

  // Device
  getDevices: (params) => request('/devices?' + qs(params)),
  getDevice: (id) => request(`/devices/${id}`),

  // Telemetry
  getLatest: (deviceId) => request(`/telemetry/${deviceId}/latest`),
  getDashboard: () => request('/telemetry/dashboard'),
  getTrend: (params) => request('/telemetry/trend?' + qs(params)),

  // Irrigation
  getRules: () => request('/rules'),
  toggleRule: (id) => request(`/rules/${id}/toggle`, 'POST'),
  sendCommand: (data) => request('/commands', 'POST', data),
  getCommands: () => request('/commands/history'),
  emergencyStop: () => request('/commands/emergency-stop', 'POST'),

  // Alert
  getAlerts: (params) => request('/alerts?' + qs(params)),
  ackAlert: (id) => request(`/alerts/${id}/acknowledge`, 'POST'),
  getAlertStats: () => request('/alerts/stats'),

  // Report
  getDailyStats: (params) => request('/reports/irrigation/daily?' + qs(params)),
  getWaterSaving: (params) => request('/reports/water-saving?' + qs(params)),

  // Crop
  getPlantings: () => request('/greenhouse-plantings'),
  getStageInfo: (id) => request(`/greenhouse-plantings/${id}/stage`),

  // Screen (public)
  getScreenData: () => request('/screen/data'),
}

function qs(obj) {
  if (!obj) return ''
  return Object.keys(obj).filter(k => obj[k] != null).map(k => `${k}=${encodeURIComponent(obj[k])}`).join('&')
}
