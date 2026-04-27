import { defineStore } from 'pinia'
import { greenhouseApi } from '../api'

export const useGreenhouseStore = defineStore('greenhouse', {
  state: () => ({
    greenhouses: [],
    loading: false,
    stats: { total: 0, active: 0, idle: 0 },
    filters: {
      status: '',
    },
  }),

  getters: {
    filteredGreenhouses(state) {
      return state.greenhouses.filter((g) => {
        return !state.filters.status || g.status === state.filters.status
      })
    },
  },

  actions: {
    async fetchGreenhouses(params) {
      this.loading = true
      try {
        const res = await greenhouseApi.list(params)
        this.greenhouses = res.data || res.content || res || []
      } catch (error) {
        console.error('Failed to fetch greenhouses:', error)
      } finally {
        this.loading = false
      }
    },

    async fetchStats() {
      try {
        const res = await greenhouseApi.stats()
        const data = res.data || res
        this.stats = {
          total: data.total || 0,
          active: data.active || 0,
          idle: data.idle || 0,
        }
      } catch (error) {
        console.error('Failed to fetch greenhouse stats:', error)
      }
    },
  },
})
