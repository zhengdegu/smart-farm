import { defineStore } from 'pinia'
import { alertApi } from '../api'

export const useAlertStore = defineStore('alert', {
  state: () => ({
    alerts: [],
    loading: false,
    stats: {
      total: 0,
      pending: 0,
      todayCount: 0,
    },
    filter: 'all',
    selectedIds: [],
    pagination: {
      page: 1,
      pageSize: 10,
      total: 0,
    },
  }),

  getters: {
    filteredAlerts(state) {
      if (state.filter === 'all') return state.alerts
      return state.alerts.filter((alert) => alert.level === state.filter)
    },
    pendingCount(state) {
      return state.alerts.filter((a) => a.status === 'pending').length
    },
  },

  actions: {
    async fetchAlerts(params) {
      this.loading = true
      try {
        const res = await alertApi.list(params)
        this.alerts = res.data || res.content || res || []
        if (res.total !== undefined) {
          this.pagination.total = res.total
        }
        if (res.page !== undefined) {
          this.pagination.page = res.page
        }
      } catch (error) {
        console.error('Failed to fetch alerts:', error)
      } finally {
        this.loading = false
      }
    },

    async acknowledgeAlert(id) {
      try {
        await alertApi.acknowledge(id)
        const alert = this.alerts.find((a) => a.id === id)
        if (alert) {
          alert.status = 'resolved'
        }
      } catch (error) {
        console.error('Failed to acknowledge alert:', error)
      }
    },

    async batchAcknowledge(ids) {
      try {
        await Promise.all(ids.map((id) => alertApi.acknowledge(id)))
        ids.forEach((id) => {
          const alert = this.alerts.find((a) => a.id === id)
          if (alert) {
            alert.status = 'resolved'
          }
        })
        this.selectedIds = []
      } catch (error) {
        console.error('Failed to batch acknowledge alerts:', error)
      }
    },

    async fetchStats() {
      try {
        const res = await alertApi.stats()
        const data = res.data || res
        this.stats.total = data.total ?? 0
        this.stats.pending = data.pending ?? 0
        this.stats.todayCount = data.todayCount ?? 0
      } catch (error) {
        console.error('Failed to fetch alert stats:', error)
      }
    },
  },
})
