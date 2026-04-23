const api = require('../../utils/api')

Page({
  data: {
    rules: [],
    commands: [],
    tab: 'rules',
    loading: true
  },

  onLoad() { this.loadData() },
  onPullDownRefresh() { this.loadData().then(() => wx.stopPullDownRefresh()) },

  async loadData() {
    try {
      const [rules, commands] = await Promise.all([api.getRules(), api.getCommands()])
      this.setData({
        rules: rules || [],
        commands: (commands || []).slice(0, 20),
        loading: false
      })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  onTabChange(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
  },

  async onToggleRule(e) {
    const id = e.currentTarget.dataset.id
    try {
      await api.toggleRule(id)
      wx.showToast({ title: '已切换', icon: 'success' })
      this.loadData()
    } catch (e) {
      wx.showToast({ title: '操作失败', icon: 'none' })
    }
  },

  onManualIrrigate() {
    wx.showActionSheet({
      itemList: ['valve_001 - 1号棚', 'valve_002 - 2号棚'],
      success: (res) => {
        const deviceId = res.tapIndex === 0 ? 'valve_001' : 'valve_002'
        wx.showModal({
          title: '手动灌溉',
          content: `确认打开 ${deviceId}？`,
          success: async (r) => {
            if (r.confirm) {
              try {
                await api.sendCommand({ deviceId, action: 'OPEN_VALVE', params: { duration_min: 30 } })
                wx.showToast({ title: '指令已发送', icon: 'success' })
                setTimeout(() => this.loadData(), 2000)
              } catch (e) {}
            }
          }
        })
      }
    })
  },

  async onEmergencyStop() {
    wx.showModal({
      title: '紧急停止',
      content: '确认关闭所有阀门？',
      confirmColor: '#fa5151',
      success: async (res) => {
        if (res.confirm) {
          try {
            const result = await api.emergencyStop()
            wx.showToast({ title: `已关闭 ${result.stoppedCount || 0} 个阀门`, icon: 'success' })
            this.loadData()
          } catch (e) {}
        }
      }
    })
  }
})
