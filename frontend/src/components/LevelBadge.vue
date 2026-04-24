<script setup>
defineProps({
  level: {
    type: String,
    required: true,
    validator: (val) => ['L1', 'L2', 'L3'].includes(val)
  }
})
</script>

<template>
  <span :class="['level-badge', level.toLowerCase()]">{{ level }}</span>
</template>

<style scoped>
.level-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
}

.level-badge.l1 {
  background: var(--sf-l1);
}

.level-badge.l2 {
  background: var(--sf-l2);
}

.level-badge.l3 {
  background: var(--sf-l3);
}
</style>
