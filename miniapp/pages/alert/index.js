const api = require('../../utils/api')

Page({
  data: {
    stats: { today: 5, pending: 3 },
    activeFilter: 'all',
    filters: [
      { key: 'all', label: '全部' },
      { key: 'l1', label: 'L1 紧急' },
      { key: 'l2', label: 'L2 重要' },
      { key: 'l3', label: 'L3 一般' },
      { key: 'pending', label: '未处理' },
      { key: 'resolved', label: '已处理' },
    ],
    allAlerts: [
      {
        id: 1, level: 'L1', levelClass: 'l1', title: '灌溉指令执行失败',
        device: '阀门 A2 · 1号棚B区', time: '10:15 · 15分钟前', status: 'pending',
        content: '阀门A2执行灌溉指令时通信超时，请检查设备连接状态和网关信号。',
        currentValue: '超时', threshold: '30秒'
      },
      {
        id: 2, level: 'L2', levelClass: 'l2', title: '土壤湿度低于阈值',
        device: '土壤湿度传感器 · 1号棚 · 当前 32.5%', time: '10:10 · 20分钟前', status: 'pending',
        content: '当前土壤湿度32.5%，低于设定阈值35%。建议立即灌溉25分钟。',
        currentValue: '32.5%', threshold: '35%'
      },
      {
        id: 3, level: 'L2', levelClass: 'l2', title: '3号温湿度传感器离线',
        device: '2号棚 · 已离线 2小时', time: '08:24 · 2小时前', status: 'pending',
        content: '传感器已离线超过2小时，可能原因：电池耗尽、信号干扰、设备损坏。',
        currentValue: '离线', threshold: '在线'
      },
      {
        id: 4, level: 'L3', levelClass: 'l3', title: '定时灌溉任务完成',
        device: '阀门 A1 · 灌溉 20分钟', time: '06:20 · 今早', status: 'resolved',
        content: '番茄定时灌溉规则执行完成，阀门A1灌溉20分钟，用水0.6吨。',
        currentValue: '完成', threshold: '—'
      },
      {
        id: 5, level: 'L3', levelClass: 'l3', title: '月度报告已生成',
        device: '2026年3月运营报告', time: '昨日 20:00', status: 'resolved',
        content: '2026年3月运营报告已自动生成，包含灌溉统计、节水分析、设备运行报告。',
        currentValue: '已生成', threshold: '—'
      },
    ],
    filteredAlerts: [],
    showDetail: false,
    detailAlert: null,
  },

  onLoad() {
    this.applyFilter()
    this.loadAlerts()
  },

  onPullDownRefresh() {
    this.loadAlerts().then(() => wx.stopPullDownRefresh())
  },

  async loadAlerts() {
    try {
      const [alertsRes, statsRes] = await Promise.all([
        api.getAlerts({ page: 1, size: 50 }),
        api.getAlertStats()
      ])
      if (alertsRes && alertsRes.items) {
        const allAlerts = alertsRes.items.map(a => ({
          id: a.id,
          level: a.level,
          levelClass: (a.level || '').toLowerCase(),
          title: a.title,
          device: a.deviceId || '',
          time: a.createdAt || '',
          status: (a.status || '').toLowerCase() === 'acknowledged' ? 'resolved' : 'pending',
          content: a.content || '',
          currentValue: a.currentValue || '—',
          threshold: a.threshold || '—'
        }))
        this.setData({ allAlerts })
        this.applyFilter()
      }
      if (statsRes) {
        this.setData({
          stats: {
            today: statsRes.todayCount || this.data.stats.today,
            pending: statsRes.pendingCount || this.data.stats.pending
          }
        })
      }
    } catch (e) {
      console.error('Failed to load alerts:', e)
    }
  },

  onFilterChange(e) {
    const key = e.currentTarget.dataset.key
    this.setData({ activeFilter: key })
    this.applyFilter()
  },

  applyFilter() {
    const { allAlerts, activeFilter } = this.data
    let filtered = allAlerts

    if (activeFilter === 'l1') {
      filtered = allAlerts.filter(a => a.level === 'L1')
    } else if (activeFilter === 'l2') {
      filtered = allAlerts.filter(a => a.level === 'L2')
    } else if (activeFilter === 'l3') {
      filtered = allAlerts.filter(a => a.level === 'L3')
    } else if (activeFilter === 'pending') {
      filtered = allAlerts.filter(a => a.status === 'pending')
    } else if (activeFilter === 'resolved') {
      filtered = allAlerts.filter(a => a.status === 'resolved')
    }

    this.setData({ filteredAlerts: filtered })
  },

  onAlertTap(e) {
    const alert = e.currentTarget.dataset.alert
    this.setData({ showDetail: true, detailAlert: alert })
  },

  onCloseDetail() {
    this.setData({ showDetail: false, detailAlert: null })
  },

  async onAcknowledge() {
    const { detailAlert } = this.data
    if (!detailAlert) return
    try {
      await api.ackAlert(detailAlert.id)
      wx.showToast({ title: '已确认处理', icon: 'success' })
      // Update local state
      const allAlerts = this.data.allAlerts.map(a =>
        a.id === detailAlert.id ? { ...a, status: 'resolved' } : a
      )
      this.setData({
        allAlerts,
        showDetail: false,
        detailAlert: null,
        'stats.pending': Math.max(0, this.data.stats.pending - 1)
      })
      this.applyFilter()
    } catch (e) {
      wx.showToast({ title: '操作失败', icon: 'none' })
    }
  },
})
