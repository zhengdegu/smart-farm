const api = require('../../utils/api')

Page({
  data: {
    activeTab: 'irrigation',
    tabs: [
      { key: 'irrigation', label: '灌溉统计' },
      { key: 'water', label: '节水率' },
      { key: 'sensor', label: '传感器' },
      { key: 'crop', label: '作物' },
    ],

    // 灌溉统计数据
    irrigationBars: [
      { day: '周一', height: 45 },
      { day: '周二', height: 60 },
      { day: '周三', height: 35 },
      { day: '周四', height: 80 },
      { day: '周五', height: 55 },
      { day: '周六', height: 70 },
      { day: '周日', height: 50 },
    ],

    // 节水率数据
    waterSaving: {
      rate: '32',
      totalSaved: '12.8',
      change: '+8.3%',
    },
    waterBars: [
      { month: '1月', height: 30 },
      { month: '2月', height: 35 },
      { month: '3月', height: 28 },
      { month: '4月', height: 40 },
      { month: '5月', height: 38 },
      { month: '6月', height: 32 },
    ],

    // 传感器趋势数据
    tempBars: [
      { height: 50 }, { height: 55 }, { height: 60 }, { height: 65 },
      { height: 70 }, { height: 68 }, { height: 62 }, { height: 58 },
    ],
    humidityBars: [
      { height: 70 }, { height: 65 }, { height: 55 }, { height: 45 },
      { height: 40 }, { height: 38 }, { height: 35 }, { height: 33 },
    ],

    // 作物数据
    crops: [
      {
        name: '🍅 番茄',
        stage: '开花坐果期',
        plantDate: '2026-03-01',
        days: 58,
        stageIndex: 2,
        stages: ['苗期', '生长期', '开花期', '果期', '采收期'],
        params: [
          { value: '50-75%', label: '湿度范围' },
          { value: '25min', label: '建议灌溉时长' },
          { value: '20-28°C', label: '最佳温度' },
          { value: '2次/天', label: '灌溉频次' },
        ]
      },
      {
        name: '🥒 黄瓜',
        stage: '生长期',
        plantDate: '2026-03-15',
        days: 44,
        stageIndex: 1,
        stages: ['苗期', '生长期', '开花期', '果期', '采收期'],
        params: [
          { value: '60-80%', label: '湿度范围' },
          { value: '15min', label: '建议灌溉时长' },
          { value: '22-30°C', label: '最佳温度' },
          { value: '3次/天', label: '灌溉频次' },
        ]
      }
    ],
  },

  onLoad() {
    this.loadData()
  },

  onPullDownRefresh() {
    this.loadData().then(() => wx.stopPullDownRefresh())
  },

  async loadData() {
    try {
      if (this.data.activeTab === 'irrigation') {
        const stats = await api.getDailyStats({ days: 7 })
        // Map API data if available
      } else if (this.data.activeTab === 'water') {
        const saving = await api.getWaterSaving()
        // Map API data if available
      } else if (this.data.activeTab === 'sensor') {
        const trend = await api.getSensorTrend({ hours: 24 })
        // Map API data if available
      } else if (this.data.activeTab === 'crop') {
        const plantings = await api.getCropPlantings()
        // Map API data if available
      }
    } catch (e) {
      console.error('Failed to load data:', e)
    }
  },

  onTabSwitch(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ activeTab: tab })
    this.loadData()
  },
})
