const api = require('../../utils/api')

Page({
  data: {
    alerts: [],
    total: 0,
    page: 1,
    filter: 'all',
    loading: true,
    hasMore: true
  },

  onLoad() { this.loadAlerts() },
  onPullDownRefresh() {
    this.setData({ page: 1, alerts: [], hasMore: true })
    this.loadAlerts().then(() => wx.stopPullDownRefresh())
  },
  onReachBottom() {
    if (this.data.hasMore) this.loadAlerts()
  },

  async loadAlerts() {
    try {
      const params = { page: this.data.page, size: 20 }
      if (this.data.filter !== 'all') params.status = this.data.filter
      const res = await api.getAlerts(params)
      const items = res.items || []
      this.setData({
        alerts: this.data.page === 1 ? items : [...this.data.alerts, ...items],
        total: res.total || 0,
        page: this.data.page + 1,
        hasMore: items.length >= 20,
        loading: false
      })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  onFilterChange(e) {
    this.setData({ filter: e.currentTarget.dataset.status, page: 1, alerts: [], hasMore: true })
    this.loadAlerts()
  },

  async onAcknowledge(e) {
    const id = e.currentTarget.dataset.id
    try {
      await api.ackAlert(id)
      wx.showToast({ title: '已确认', icon: 'success' })
      this.setData({ page: 1, alerts: [], hasMore: true })
      this.loadAlerts()
    } catch (e) {}
  }
})
