const api = require('../../utils/api')
const app = getApp()

Page({
  data: {
    username: '',
    password: '',
    loading: false
  },

  onUsernameInput(e) { this.setData({ username: e.detail.value }) },
  onPasswordInput(e) { this.setData({ password: e.detail.value }) },

  async onLogin() {
    const { username, password } = this.data
    if (!username || !password) {
      wx.showToast({ title: '请输入账号密码', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    try {
      const res = await api.login({ username, password })
      const token = res.token
      if (token) {
        app.globalData.token = token
        wx.setStorageSync('token', token)
        wx.switchTab({ url: '/pages/index/index' })
      } else {
        wx.showToast({ title: '登录失败', icon: 'none' })
      }
    } catch (e) {
      wx.showToast({ title: '登录失败', icon: 'none' })
    }
    this.setData({ loading: false })
  }
})
