const api = require('../../utils/api')

Page({
  data: {
    devices: [],
    filter: 'all', // all/SENSOR/VALVE
    loading: true
  },

  onLoad() { this.loadDevices() },
  onPullDownRefresh() { this.loadDevices().then(() => wx.stopPullDownRefresh()) },

  async loadDevices() {
    try {
      const type = this.data.filter === 'all' ? null : this.data.filter
      const devices = await api.getDevices({ type })
      this.setData({ devices, loading: false })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  onFilterChange(e) {
    this.setData({ filter: e.currentTarget.dataset.type })
    this.loadDevices()
  },

  onDeviceTap(e) {
    const device = e.currentTarget.dataset.device
    wx.navigateTo({ url: `/pages/device/detail?deviceId=${device.deviceId}` })
  },

  async onRefreshDevice(e) {
    const deviceId = e.currentTarget.dataset.id
    try {
      const data = await api.getLatest(deviceId)
      wx.showToast({ title: `最新值: ${data.value}${data.unit || ''}`, icon: 'none' })
    } catch (err) {
      wx.showToast({ title: '暂无数据', icon: 'none' })
    }
  }
})
