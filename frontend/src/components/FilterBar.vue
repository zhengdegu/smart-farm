<script setup>
import { ref, reactive } from 'vue'

const props = defineProps({
  filters: {
    type: Array,
    default: () => []
  },
  chips: {
    type: Array,
    default: () => []
  },
  actions: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['filter-change', 'chip-change'])

const filterValues = reactive({})

// Initialize filter values
props.filters.forEach((f) => {
  filterValues[f.key] = ''
})

const activeChip = ref(props.chips.length > 0 ? props.chips[0].value : '')

function onFilterChange() {
  emit('filter-change', { ...filterValues })
}

function onChipClick(value) {
  activeChip.value = value
  emit('chip-change', value)
}
</script>

<template>
  <div class="filter-bar">
    <!-- Dropdown filters -->
    <el-select
      v-for="filter in filters"
      :key="filter.key"
      v-model="filterValues[filter.key]"
      :placeholder="filter.label"
      class="filter-select"
      @change="onFilterChange"
    >
      <el-option
        v-for="opt in filter.options"
        :key="opt.value"
        :label="opt.label"
        :value="opt.value"
      />
    </el-select>

    <!-- Filter chips -->
    <span
      v-for="chip in chips"
      :key="chip.value"
      :class="['filter-chip', { active: activeChip === chip.value }]"
      :style="chip.color && activeChip === chip.value ? { background: chip.color, borderColor: chip.color } : {}"
      @click="onChipClick(chip.value)"
    >
      {{ chip.label }}
    </span>

    <!-- Spacer -->
    <span v-if="actions.length" class="filter-spacer" />

    <!-- Action buttons -->
    <el-button
      v-for="(action, idx) in actions"
      :key="idx"
      :type="action.type === 'primary' ? 'primary' : ''"
      :plain="action.type === 'outline'"
      @click="action.onClick"
    >
      <template v-if="action.icon">
        <span class="action-icon">{{ action.icon }}</span>
      </template>
      {{ action.label }}
    </el-button>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-select {
  width: auto;
  min-width: 140px;
}

.filter-select :deep(.el-input__wrapper) {
  padding: 4px 12px;
  border-radius: var(--sf-radius-sm);
  font-size: 13px;
}

.filter-chip {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid var(--sf-border);
  background: #fff;
  color: var(--sf-text-secondary);
  transition: all 0.2s;
  user-select: none;
}

.filter-chip:hover {
  border-color: var(--sf-primary);
  color: var(--sf-primary);
}

.filter-chip.active {
  background: var(--sf-primary);
  color: #fff;
  border-color: var(--sf-primary);
}

.filter-spacer {
  flex: 1;
}

.action-icon {
  margin-right: 4px;
}
</style>
