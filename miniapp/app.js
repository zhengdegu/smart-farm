App({
  globalData: {
    baseUrl: 'https://your-domain.com/api/v1',
    token: ''
  },
  onLaunch() {
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
    }
  }
})
