const app = getApp()

Page({
  data: {
    userInfo: null,
    isLoggedIn: false
  },

  onShow() {
    const token = wx.getStorageSync('token')
    this.setData({ isLoggedIn: !!token })
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]))
        this.setData({ userInfo: { username: payload.sub, role: payload.role } })
      } catch (e) {}
    }
  },

  onLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  },

  onLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确认退出？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token')
          app.globalData.token = ''
          this.setData({ isLoggedIn: false, userInfo: null })
          wx.showToast({ title: '已退出', icon: 'success' })
        }
      }
    })
  }
})
