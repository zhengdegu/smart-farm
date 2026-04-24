const api = require('../../utils/api')

Page({
  data: {
    suggestions: [],
    loading: true,
  },

  onLoad() {
    this.loadSuggestions()
  },

  onPullDownRefresh() {
    this.loadSuggestions().then(() => wx.stopPullDownRefresh())
  },

  async loadSuggestions() {
    this.setData({ loading: true })
    try {
      const res = await api.aiSuggestions()
      if (res && Array.isArray(res)) {
        const typeMap = { THRESHOLD: '阈值优化', DURATION: '时长优化', SCHEDULE: '计划调整' }
        const statusMap = { PENDING: '待审批', ACCEPTED: '已接受', REJECTED: '已拒绝' }
        const statusClassMap = { PENDING: 'pending', ACCEPTED: 'accepted', REJECTED: 'rejected' }
        const suggestions = res.map(s => ({
          id: s.id,
          greenhouseNo: s.greenhouseNo || '—',
          typeLabel: typeMap[s.suggestionType] || s.suggestionType,
          reasoningSummary: (s.reasoning || '').length > 50 ? (s.reasoning || '').substring(0, 50) + '...' : (s.reasoning || ''),
          statusLabel: statusMap[s.status] || s.status,
          statusClass: statusClassMap[s.status] || 'pending',
        }))
        this.setData({ suggestions })
      }
    } catch (e) {
      console.error('Failed to load suggestions:', e)
    } finally {
      this.setData({ loading: false })
    }
  },
})
