import { defineStore } from 'pinia'
import { deviceApi } from '../api'

export const useDeviceStore = defineStore('device', {
  state: () => ({
    devices: [],
    loading: false,
    filters: {
      type: '',
      status: '',
    },
    pagination: {
      page: 1,
      pageSize: 10,
      total: 0,
    },
  }),

  getters: {
    filteredDevices(state) {
      return state.devices.filter((device) => {
        const matchType = !state.filters.type || device.type === state.filters.type
        const matchStatus = !state.filters.status || device.status === state.filters.status
        return matchType && matchStatus
      })
    },
    onlineCount(state) {
      return state.devices.filter((d) => d.status === 'online').length
    },
    offlineCount(state) {
      return state.devices.filter((d) => d.status === 'offline').length
    },
    faultCount(state) {
      return state.devices.filter((d) => d.status === 'fault').length
    },
  },

  actions: {
    async fetchDevices(params) {
      this.loading = true
      try {
        const res = await deviceApi.list(params)
        this.devices = res.data || res.content || res || []
        if (res.total !== undefined) {
          this.pagination.total = res.total
        }
        if (res.page !== undefined) {
          this.pagination.page = res.page
        }
      } catch (error) {
        console.error('Failed to fetch devices:', error)
      } finally {
        this.loading = false
      }
    },

    async getDevice(id) {
      try {
        const res = await deviceApi.get(id)
        return res.data || res
      } catch (error) {
        console.error('Failed to get device:', error)
        return null
      }
    },
  },
})
