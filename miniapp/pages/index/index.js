const api = require('../../utils/api')

Page({
  data: {
    baseName: '示范基地',
    weather: { temp: '28°C', humidity: '65%', wind: '东南风 2级' },
    alertCount: 3,
    cards: [
      { icon: '💧', value: '32.5', unit: '%', label: '土壤湿度', status: 'low', statusText: '⚠ 偏低', type: 'warn' },
      { icon: '🌡', value: '24.1', unit: '°C', label: '土壤温度', status: 'ok', statusText: '✓ 正常', type: 'normal' },
      { icon: '🌤', value: '28.6', unit: '°C', label: '空气温度', status: 'ok', statusText: '✓ 正常', type: 'normal' },
      { icon: '💨', value: '68.3', unit: '%', label: '空气湿度', status: 'ok', statusText: '✓ 正常', type: 'normal' },
    ],
    advice: { stage: '番茄 · 开花坐果期 第58天', text: '土壤湿度偏低（32.5%），建议灌溉 25分钟，目标湿度 65%' },
    summary: { irrigationCount: 5, totalDuration: 120, waterUsage: 2.4 },
    showIrrigationModal: false,
    showEmergencyModal: false,
    showChat: false,
    irrigationForm: { valve: '阀门 A1 (1号棚A区)', duration: 25, targetHumidity: 65 },
    valveOptions: ['阀门 A1 (1号棚A区)', '阀门 A2 (1号棚B区)', '阀门 B1 (2号棚A区)', '全部阀门'],
  },

  onLoad() { this.loadData() },
  onPullDownRefresh() { this.loadData().then(() => wx.stopPullDownRefresh()) },

  async loadData() {
    try {
      const dashboard = await api.getDashboard()
      // Update data from API response if available
      if (dashboard) {
        // Map API response to page data as needed
      }
    } catch (e) {
      console.error('Failed to load dashboard:', e)
    }
  },

  onStartIrrigation() {
    this.setData({ showIrrigationModal: true })
  },

  onOpenChat() {
    this.setData({ showChat: true })
  },

  onCloseChat() {
    this.setData({ showChat: false })
  },

  onCloseIrrigationModal() {
    this.setData({ showIrrigationModal: false })
  },

  onConfirmIrrigation() {
    const { irrigationForm } = this.data
    api.sendCommand({
      valve: irrigationForm.valve,
      duration: irrigationForm.duration,
      targetHumidity: irrigationForm.targetHumidity
    })
    wx.showToast({ title: '灌溉指令已发送' })
    this.setData({ showIrrigationModal: false })
  },

  onEmergencyStop() {
    this.setData({ showEmergencyModal: true })
  },

  onCloseEmergencyModal() {
    this.setData({ showEmergencyModal: false })
  },

  onConfirmEmergency() {
    api.emergencyStop()
    wx.showToast({ title: '已紧急停止所有灌溉' })
    this.setData({ showEmergencyModal: false })
  },

  onValveChange(e) {
    this.setData({ 'irrigationForm.valve': this.data.valveOptions[e.detail.value] })
  },

  onDurationInput(e) {
    this.setData({ 'irrigationForm.duration': e.detail.value })
  },

  onHumidityInput(e) {
    this.setData({ 'irrigationForm.targetHumidity': e.detail.value })
  },
})
