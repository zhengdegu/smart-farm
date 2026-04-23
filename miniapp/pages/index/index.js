const api = require('../../utils/api')

Page({
  data: {
    stats: { onlineDevices: 0, offlineDevices: 0, totalSensors: 0, totalValves: 0 },
    envData: [],
    alerts: [],
    plantings: [],
    loading: true
  },

  onLoad() { this.loadData() },
  onPullDownRefresh() { this.loadData().then(() => wx.stopPullDownRefresh()) },

  async loadData() {
    try {
      const data = await api.getScreenData()
      this.setData({
        stats: data.deviceStats || this.data.stats,
        envData: (data.envData || []).map(e => ({
          ...e,
          value: typeof e.value === 'number' ? Math.round(e.value * 10) / 10 : e.value
        })),
        alerts: (data.alerts || []).slice(0, 5),
        plantings: data.plantings || [],
        loading: false
      })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  goDevice() { wx.switchTab({ url: '/pages/device/index' }) },
  goIrrigation() { wx.switchTab({ url: '/pages/irrigation/index' }) },
  goAlert() { wx.switchTab({ url: '/pages/alert/index' }) },

  onEmergencyStop() {
    wx.showModal({
      title: '紧急停止',
      content: '确认停止所有灌溉？此操作将关闭所有阀门。',
      confirmColor: '#fa5151',
      success: async (res) => {
        if (res.confirm) {
          try {
            const result = await api.emergencyStop()
            wx.showToast({ title: `已关闭 ${result.stoppedCount || 0} 个阀门`, icon: 'success' })
            this.loadData()
          } catch (e) {
            wx.showToast({ title: '操作失败', icon: 'none' })
          }
        }
      }
    })
  }
})
