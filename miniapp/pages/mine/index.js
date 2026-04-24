const app = getApp()

Page({
  data: {
    profile: {
      avatar: '👨‍🌾',
      name: '张三',
      role: '管理员',
      base: '📍 示范基地'
    },
    menuGroups: [
      {
        items: [
          { icon: '🏠', label: '基地管理', key: 'base' },
          { icon: '🔔', label: '通知设置', key: 'notif' },
          { icon: '🌙', label: '免打扰时段', key: 'dnd', aux: '22:00-06:00' },
        ]
      },
      {
        items: [
          { icon: '👥', label: '操作员管理', key: 'operators', aux: '3人' },
          { icon: '📋', label: '操作日志', key: 'logs' },
        ]
      },
      {
        items: [
          { icon: '📖', label: '操作视频教程', key: 'tutorial' },
          { icon: '❓', label: '帮助与反馈', key: 'help' },
          { icon: 'ℹ️', label: '关于', key: 'about', aux: 'V1.0.0' },
        ]
      }
    ],
    isLoggedIn: false,
  },

  onShow() {
    const token = wx.getStorageSync('token')
    this.setData({ isLoggedIn: !!token })
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]))
        this.setData({
          'profile.name': payload.sub || '用户',
          'profile.role': payload.role || '操作员'
        })
      } catch (e) {}
    }
  },

  onMenuTap(e) {
    const key = e.currentTarget.dataset.key
    // Navigate to sub-pages or show detail based on key
    switch (key) {
      case 'base':
        wx.showToast({ title: '基地管理', icon: 'none' })
        break
      case 'notif':
        wx.showToast({ title: '通知设置', icon: 'none' })
        break
      case 'dnd':
        wx.showToast({ title: '免打扰时段', icon: 'none' })
        break
      case 'operators':
        wx.showToast({ title: '操作员管理', icon: 'none' })
        break
      case 'logs':
        wx.showToast({ title: '操作日志', icon: 'none' })
        break
      case 'tutorial':
        wx.showToast({ title: '操作视频教程', icon: 'none' })
        break
      case 'help':
        wx.showToast({ title: '帮助与反馈', icon: 'none' })
        break
      case 'about':
        wx.showToast({ title: '关于 V1.0.0', icon: 'none' })
        break
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
          this.setData({ isLoggedIn: false })
          wx.showToast({ title: '已退出', icon: 'success' })
        }
      }
    })
  }
})
