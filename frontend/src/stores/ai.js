import { defineStore } from 'pinia'
import { aiApi } from '../api'
import { checkRateLimit } from '../utils/ai-helpers'

function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

export const useAiStore = defineStore('ai', {
  state: () => ({
    chatOpen: false,
    messages: [],        // [{id, role, content, time, loading?, error?}]
    sessionId: null,
    loading: false,
    patrolUnread: 0,
    sendTimestamps: [],
    rateLimited: false,
  }),

  getters: {
    canSend(state) {
      return !state.loading && !state.rateLimited && state.chatOpen
    },
  },

  actions: {
    toggleChat() {
      this.chatOpen = !this.chatOpen
      if (this.chatOpen && !this.sessionId) {
        this.sessionId = generateUUID()
        // Add welcome message
        this.messages.push({
          id: generateUUID(),
          role: 'assistant',
          content: '你好！我是小农助手 🌱，有什么可以帮你的？你可以问我温室数据、控制灌溉、查看告警等。',
          time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
        })
      }
    },

    async sendMessage(text) {
      if (!text || !text.trim()) return
      const trimmed = text.trim()

      // Rate limit check
      const now = Date.now()
      this.sendTimestamps = this.sendTimestamps.filter(t => now - t < 60000)
      if (checkRateLimit(this.sendTimestamps, now)) {
        this.rateLimited = true
        setTimeout(() => { this.rateLimited = false }, 30000)
        return
      }
      this.sendTimestamps.push(now)

      // Add user message
      const userMsg = {
        id: generateUUID(),
        role: 'user',
        content: trimmed,
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
      }
      this.messages.push(userMsg)
      this.loading = true

      try {
        const res = await aiApi.chat({ message: trimmed, sessionId: this.sessionId })
        const reply = res.reply || res.content || res.data?.reply || '抱歉，我没有理解你的问题。'
        this.messages.push({
          id: generateUUID(),
          role: 'assistant',
          content: reply,
          time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
        })
      } catch (error) {
        this.messages.push({
          id: generateUUID(),
          role: 'error',
          content: '网络异常，请稍后重试',
          time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
        })
      } finally {
        this.loading = false
      }
    },

    async fetchPatrolUnread() {
      try {
        const logs = await aiApi.patrolLogs()
        const list = Array.isArray(logs) ? logs : (logs?.data || [])
        this.patrolUnread = list.filter(l => l.severity === 'WARNING' || l.severity === 'CRITICAL').length
      } catch {
        this.patrolUnread = 0
      }
    },

    clearSession() {
      this.messages = []
      this.sessionId = null
      this.loading = false
    },
  },
})
