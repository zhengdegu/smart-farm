const api = require('../../utils/api')

Page({
  data: {
    summary: { online: 12, offline: 1, fault: 1 },
    groups: [
      {
        title: '传感器',
        count: 8,
        devices: [
          { id: 'S001', name: '1号温湿度传感器', icon: '🌡', meta: '1号棚 · 28.5°C / 65%', status: 'online', iconBg: 'on' },
          { id: 'S002', name: '2号温湿度传感器', icon: '🌡', meta: '1号棚 · 27.8°C / 62%', status: 'online', iconBg: 'on' },
          { id: 'S003', name: '3号温湿度传感器', icon: '🌡', meta: '2号棚 · 离线 2小时', status: 'fault', iconBg: 'off' },
          { id: 'S004', name: '土壤湿度传感器', icon: '💧', meta: '1号棚 · 32.5%', status: 'online', iconBg: 'on' },
          { id: 'S005', name: '光照传感器', icon: '☀️', meta: '1号棚 · 452 μmol/m²/s', status: 'online', iconBg: 'on' },
        ]
      },
      {
        title: '阀门',
        count: 4,
        devices: [
          { id: 'V001', name: '灌溉阀门 A1', icon: '🚿', meta: '1号棚A区 · 关闭', status: 'online', iconBg: 'on' },
          { id: 'V002', name: '灌溉阀门 A2', icon: '🚿', meta: '1号棚B区 · 开启中 (剩余12min)', status: 'online', iconBg: 'on' },
        ]
      },
      {
        title: '网关',
        count: 1,
        devices: [
          { id: 'G001', name: '主网关 GW-01', icon: '📡', meta: '管理 13 台设备 · 在线', status: 'online', iconBg: 'on' },
        ]
      }
    ],
    showDetail: false,
    detailDevice: null,
    detailInfo: {
      currentValue: '—',
      status: '在线',
      onlineRate: '98.5%',
      lastReport: '刚刚'
    },
    loading: true
  },

  onLoad() { this.loadDevices() },
  onPullDownRefresh() { this.loadDevices().then(() => wx.stopPullDownRefresh()) },

  async loadDevices() {
    try {
      const devices = await api.getDevices()
      if (devices && devices.length) {
        // Group devices by type
        const sensorDevices = []
        const valveDevices = []
        const gatewayDevices = []
        let online = 0, offline = 0, fault = 0

        devices.forEach(d => {
          const item = {
            id: d.deviceId,
            name: d.name || d.deviceId,
            icon: d.deviceType === 'SENSOR' ? '🌡' : d.deviceType === 'VALVE' ? '🚿' : '📡',
            meta: (d.greenhouseNo ? d.greenhouseNo + '号棚' : '') + (d.location ? ' · ' + d.location : ''),
            status: d.status === 'ONLINE' ? 'online' : d.status === 'FAULT' ? 'fault' : 'offline',
            iconBg: d.status === 'ONLINE' ? 'on' : 'off'
          }
          if (d.deviceType === 'SENSOR') sensorDevices.push(item)
          else if (d.deviceType === 'VALVE') valveDevices.push(item)
          else gatewayDevices.push(item)

          if (d.status === 'ONLINE') online++
          else if (d.status === 'FAULT') fault++
          else offline++
        })

        const groups = []
        if (sensorDevices.length) groups.push({ title: '传感器', count: sensorDevices.length, devices: sensorDevices })
        if (valveDevices.length) groups.push({ title: '阀门', count: valveDevices.length, devices: valveDevices })
        if (gatewayDevices.length) groups.push({ title: '网关', count: gatewayDevices.length, devices: gatewayDevices })

        this.setData({ groups, summary: { online, offline, fault }, loading: false })
      } else {
        this.setData({ loading: false })
      }
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  onDeviceTap(e) {
    const device = e.currentTarget.dataset.device
    this.setData({
      showDetail: true,
      detailDevice: device,
      detailInfo: {
        currentValue: device.meta || '—',
        status: device.status === 'online' ? '在线' : device.status === 'fault' ? '故障' : '离线',
        onlineRate: '98.5%',
        lastReport: '刚刚'
      }
    })
  },

  onCloseDetail() {
    this.setData({ showDetail: false, detailDevice: null })
  },
})
