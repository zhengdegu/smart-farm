import { defineStore } from 'pinia'
import { telemetryApi } from '../api'

export const useTelemetryStore = defineStore('telemetry', {
  state: () => ({
    dashboard: null,
    trend: [],
    loading: false,
  }),

  actions: {
    async fetchDashboard() {
      this.loading = true
      try {
        const res = await telemetryApi.dashboard()
        this.dashboard = res.data || res
      } catch (error) {
        console.error('Failed to fetch dashboard data:', error)
      } finally {
        this.loading = false
      }
    },

    async fetchTrend(params) {
      this.loading = true
      try {
        const res = await telemetryApi.trend(params)
        this.trend = res.data || res.content || res || []
      } catch (error) {
        console.error('Failed to fetch trend data:', error)
      } finally {
        this.loading = false
      }
    },
  },
})
