<template>
  <div class="filters-wrapper">
    <div class="filters-group">
      <!-- Date Range Picker -->
      <div class="date-range-fields">
        <input
          aria-label="Date de début"
          class="date-input"
          type="date"
          :disabled="!currentNetwork"
          :value="formatDateForInput(filters.dateRange[0])"
          @input="setDateRangeValue(0, $event)"
        >
        <span
          aria-hidden="true"
          class="date-range-separator"
        >-</span>
        <input
          aria-label="Date de fin"
          class="date-input"
          type="date"
          :disabled="!currentNetwork"
          :value="formatDateForInput(filters.dateRange[1])"
          @input="setDateRangeValue(1, $event)"
        >
      </div>

      <!-- Quick Date Filters -->
      <div class="quick-filters">
        <Button 
          v-for="filter in quickDateFilters" 
          :key="filter.value"
          :label="filter.label"
          :outlined="filters.quickDate !== filter.value"
          :severity="filters.quickDate === filter.value ? 'primary' : 'secondary'"
          size="small"
          :disabled="!currentNetwork"
          @click="selectQuickDate(filter.value)"
        />
      </div>

      <!-- Filters -->
      <MultiSelect
        v-model="filters.selectedFilters"
        :options="filterOptions"
        option-label="label"
        placeholder="Filtres"
        :max-selected-labels="3"
        :disabled="!currentNetwork"
      />

      <!-- Sort Options -->
      <Dropdown
        v-model="filters.sort"
        :options="sortOptions"
        option-label="label"
        placeholder="Trier par"
        :disabled="!currentNetwork"
      />

      <!-- Reset Button -->
      <Button 
        v-tooltip="$t('filters.reset_tooltip')" 
        icon="pi pi-filter-slash" 
        text
        severity="secondary"
        :disabled="!currentNetwork"
        @click="resetFilters"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { MenuItem } from '../types'

interface FilterOption {
  label: string
  value: string
}

interface Filters {
  dateRange: [Date | null, Date | null]
  quickDate: string | null
  selectedFilters: string[]
  sort: string | null
}

const props = defineProps<{
  currentNetwork: MenuItem | null
}>()

const filters = ref<Filters>({
  dateRange: [null, null],
  quickDate: null,
  selectedFilters: [],
  sort: null
})

const quickDateFilters: FilterOption[] = [
  { label: "Aujourd'hui", value: 'today' },
  { label: '7 jours', value: 'week' },
  { label: '30 jours', value: 'month' },
  { label: 'Cette année', value: 'year' }
]

const filterOptions: FilterOption[] = [
  { label: 'Publications', value: 'posts' },
  { label: 'Commentaires', value: 'comments' },
  { label: 'Mentions', value: 'mentions' },
  { label: 'Messages privés', value: 'dm' }
]

const sortOptions: FilterOption[] = [
  { label: 'Plus récent', value: 'newest' },
  { label: 'Plus ancien', value: 'oldest' },
  { label: 'Plus populaire', value: 'popular' },
  { label: 'Plus commentés', value: 'comments' }
]

const selectQuickDate = (value: string) => {
  filters.value.quickDate = value
  filters.value.dateRange = [null, null]
}

const formatDateForInput = (date: Date | null) => {
  if (!date) {
    return ''
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const parseDateInput = (value: string) => {
  if (!value) {
    return null
  }

  return new Date(`${value}T00:00:00`)
}

const setDateRangeValue = (index: 0 | 1, event: Event) => {
  const target = event.target as HTMLInputElement
  const nextRange: [Date | null, Date | null] = [...filters.value.dateRange]
  nextRange[index] = parseDateInput(target.value)
  filters.value.dateRange = nextRange
  filters.value.quickDate = null
}

const resetFilters = () => {
  filters.value = {
    dateRange: [null, null],
    quickDate: null,
    selectedFilters: [],
    sort: null
  }
}

const emit = defineEmits<{
  'filter-change': [filters: Filters]
}>()

watch(filters, (newFilters) => {
  emit('filter-change', newFilters)
}, { deep: true })
</script>

<style scoped>
.filters-wrapper {
  display: flex;
  align-items: center;
  width: 100%;
}

.filters-group {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 1;
}

.quick-filters {
  display: flex;
  gap: 0.5rem;
  flex-wrap: nowrap;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.quick-filters::-webkit-scrollbar {
  display: none;
}

:deep(.p-multiselect),
:deep(.p-dropdown) {
  min-width: unset;
  flex-shrink: 1;
}

.date-range-fields {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-shrink: 0;
}

.date-input {
  width: 8.25rem;
  min-height: 2.35rem;
  border: 1px solid var(--p-content-border-color, #d1d5db);
  border-radius: 6px;
  background: var(--p-content-background, #fff);
  color: var(--p-text-color, #111827);
  padding: 0.45rem 0.55rem;
  font: inherit;
}

.date-input:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.date-range-separator {
  color: var(--p-text-muted-color, #6b7280);
}

:deep(.p-multiselect) {
  width: 150px;
}

:deep(.p-dropdown) {
  width: 120px;
}

@media (max-width: 1200px) {
  .filters-wrapper {
    flex-direction: column;
    align-items: stretch;
  }

  .search-container {
    min-width: unset;
  }

  .filters-group {
    flex-wrap: wrap;
  }
}
</style> 
