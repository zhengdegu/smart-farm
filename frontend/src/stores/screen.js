import { defineStore } from 'pinia'
import { screenApi } from '../api'

let pollTimer = null

export const useScreenStore = defineStore('screen', {
  state: () => ({
    screenData: null,
    loading: false,
  }),

  actions: {
    async fetchScreenData() {
      this.loading = true
      try {
        const res = await screenApi.getData()
        this.screenData = res.data || res
      } catch (error) {
        console.error('Failed to fetch screen data:', error)
      } finally {
        this.loading = false
      }
    },

    pollScreenData(interval = 5000) {
      this.stopPolling()
      this.fetchScreenData()
      pollTimer = setInterval(() => {
        this.fetchScreenData()
      }, interval)
    },

    stopPolling() {
      if (pollTimer) {
        clearInterval(pollTimer)
        pollTimer = null
      }
    },
  },
})
