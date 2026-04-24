<script setup>
defineProps({
  icon: {
    type: String,
    required: true
  },
  iconBg: {
    type: String,
    required: true,
    validator: (val) => ['green', 'blue', 'orange', 'red'].includes(val)
  },
  label: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    required: true
  },
  unit: {
    type: String,
    default: ''
  },
  trend: {
    type: String,
    default: ''
  },
  trendType: {
    type: String,
    default: '',
    validator: (val) => val === '' || ['up', 'down'].includes(val)
  },
  valueColor: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <div class="stat-card">
    <div :class="['stat-icon-box', iconBg]">
      <span class="stat-icon">{{ icon }}</span>
    </div>
    <div class="stat-info">
      <div class="label">{{ label }}</div>
      <div class="value" :style="valueColor ? { color: valueColor } : {}">
        {{ value }}<span v-if="unit" class="unit">{{ unit }}</span>
      </div>
      <div v-if="trend" :class="['trend', trendType === 'up' ? 'trend-up' : trendType === 'down' ? 'trend-down' : '']">
        {{ trend }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.stat-card {
  background: var(--sf-card);
  border-radius: var(--sf-radius);
  padding: 20px;
  box-shadow: var(--sf-shadow);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: box-shadow 0.2s;
}

.stat-card:hover {
  box-shadow: var(--sf-shadow-hover);
}

.stat-icon-box {
  width: 48px;
  height: 48px;
  border-radius: var(--sf-radius);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.stat-icon-box.green {
  background: rgba(46, 125, 50, 0.1);
}

.stat-icon-box.blue {
  background: rgba(25, 118, 210, 0.1);
}

.stat-icon-box.orange {
  background: rgba(245, 124, 0, 0.1);
}

.stat-icon-box.red {
  background: rgba(211, 47, 47, 0.1);
}

.stat-info .label {
  font-size: 13px;
  color: var(--sf-text-secondary);
}

.stat-info .value {
  font-size: 28px;
  font-weight: 700;
  margin-top: 2px;
}

.stat-info .unit {
  font-size: 14px;
  font-weight: 400;
  margin-left: 2px;
}

.stat-info .trend {
  font-size: 12px;
  margin-top: 4px;
}

.trend-up {
  color: var(--sf-primary-light);
}

.trend-down {
  color: var(--sf-l1);
}
</style>
