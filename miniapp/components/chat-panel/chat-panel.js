const api = require('../../utils/api')

Component({
  properties: {
    show: { type: Boolean, value: false }
  },
  data: {
    messages: [],
    inputText: '',
    loading: false,
    sessionId: '',
    scrollToMsg: '',
  },
  lifetimes: {
    attached() {
      this.setData({ sessionId: 'sess_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9) })
      this.setData({
        messages: [{
          id: 'welcome',
          role: 'assistant',
          content: '你好！我是小农助手 🌱，有什么可以帮你的？',
          time: this._formatTime()
        }]
      })
    }
  },
  methods: {
    onClose() { this.triggerEvent('close') },
    onInput(e) { this.setData({ inputText: e.detail.value }) },
    async onSend() {
      const text = this.data.inputText.trim()
      if (!text || this.data.loading) return
      const userMsg = { id: 'msg_' + Date.now(), role: 'user', content: text, time: this._formatTime() }
      const msgs = [...this.data.messages, userMsg]
      this.setData({ messages: msgs, inputText: '', loading: true, scrollToMsg: 'msg-' + userMsg.id })
      try {
        const res = await api.aiChat({ message: text, sessionId: this.data.sessionId })
        const reply = res.reply || res.content || '抱歉，我没有理解你的问题。'
        const aiMsg = { id: 'msg_' + Date.now(), role: 'assistant', content: reply, time: this._formatTime() }
        this.setData({ messages: [...this.data.messages, aiMsg], scrollToMsg: 'msg-' + aiMsg.id })
      } catch (e) {
        wx.showToast({ title: '网络异常', icon: 'none' })
        const errMsg = { id: 'msg_' + Date.now(), role: 'error', content: '网络异常，请稍后重试', time: this._formatTime() }
        this.setData({ messages: [...this.data.messages, errMsg], scrollToMsg: 'msg-' + errMsg.id })
      } finally {
        this.setData({ loading: false })
      }
    },
    _formatTime() {
      const d = new Date()
      return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
    }
  }
})
