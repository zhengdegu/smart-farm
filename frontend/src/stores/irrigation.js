import { defineStore } from 'pinia'
import { irrigationApi } from '../api'

export const useIrrigationStore = defineStore('irrigation', {
  state: () => ({
    rules: [],
    commands: [],
    loading: false,
    commandPagination: {
      page: 1,
      pageSize: 10,
      total: 0,
    },
  }),

  actions: {
    async fetchRules() {
      this.loading = true
      try {
        const res = await irrigationApi.listRules()
        this.rules = res.data || res.content || res || []
      } catch (error) {
        console.error('Failed to fetch irrigation rules:', error)
      } finally {
        this.loading = false
      }
    },

    async toggleRule(id) {
      try {
        await irrigationApi.toggleRule(id)
        const rule = this.rules.find((r) => r.id === id)
        if (rule) {
          rule.enabled = !rule.enabled
        }
      } catch (error) {
        console.error('Failed to toggle rule:', error)
      }
    },

    async fetchCommands(params) {
      this.loading = true
      try {
        const res = await irrigationApi.listCommands(params)
        this.commands = res.data || res.content || res || []
        if (res.total !== undefined) {
          this.commandPagination.total = res.total
        }
        if (res.page !== undefined) {
          this.commandPagination.page = res.page
        }
      } catch (error) {
        console.error('Failed to fetch commands:', error)
      } finally {
        this.loading = false
      }
    },

    async sendCommand(data) {
      try {
        await irrigationApi.sendCommand(data)
      } catch (error) {
        console.error('Failed to send command:', error)
      }
    },

    async emergencyStop() {
      try {
        const res = await irrigationApi.emergencyStop()
        return res
      } catch (error) {
        console.error('Failed to execute emergency stop:', error)
        throw error
      }
    },
  },
})
