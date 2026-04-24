<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: {
    type: String,
    required: true,
    validator: (val) => ['online', 'offline', 'fault', 'pending', 'resolved'].includes(val)
  },
  text: {
    type: String,
    default: ''
  }
})

const defaultTextMap = {
  online: '在线',
  offline: '离线',
  fault: '故障',
  pending: '待处理',
  resolved: '已处理'
}

const displayText = computed(() => props.text || defaultTextMap[props.status] || props.status)
</script>

<template>
  <span :class="['status-tag', status]">{{ displayText }}</span>
</template>

<style scoped>
.status-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.online {
  background: #E8F5E9;
  color: #2E7D32;
}

.status-tag.offline {
  background: #F5F5F5;
  color: #999;
}

.status-tag.fault {
  background: #FFEBEE;
  color: #D32F2F;
}

.status-tag.pending {
  background: #FFF3E0;
  color: #F57C00;
}

.status-tag.resolved {
  background: #E8F5E9;
  color: #2E7D32;
}
</style>
