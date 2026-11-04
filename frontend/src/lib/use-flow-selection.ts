import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { getFlowMonths, getFlowDays, getFlowHours, type FlowMonths, type FlowDays, type FlowHours } from '../api/bazi'

/** Each tier is fetched only when its picker is opened; parent changes invalidate all descendants. */
export function useFlowSelection(birth: () => string, year: () => number) {
  const months = ref<FlowMonths | null>(null), days = ref<FlowDays | null>(null), hours = ref<FlowHours | null>(null)
  const monthIndex = ref(''), date = ref(''), hourIndex = ref('')
  const level = ref<'year' | 'month' | 'day' | 'hour'>('year')
  const loading = ref({ month: false, day: false, hour: false })
  const errors = ref({ month: '', day: '', hour: '' })
  const month = computed(() => months.value?.months.find(m => String(m.index) === monthIndex.value))
  const day = computed(() => days.value?.days.find(d => d.date === date.value))
  const hour = computed(() => hours.value?.hours.find(h => String(h.index) === hourIndex.value))
  type Tier = 'month' | 'day' | 'hour'
  const caches = { month: new Map<string, FlowMonths>(), day: new Map<string, FlowDays>(), hour: new Map<string, FlowHours>() }
  const revisions = { month: 0, day: 0, hour: 0 }
  const aborts: Partial<Record<Tier, AbortController>> = {}
  function cancel(tiers: Tier[]) {
    for (const tier of tiers) { revisions[tier]++; aborts[tier]?.abort(); loading.value[tier] = false; errors.value[tier] = '' }
  }
  function clearDays() { cancel(['day', 'hour']); days.value = null; hours.value = null; date.value = ''; hourIndex.value = '' }
  function selectMonth(value: string) {
    if (value !== monthIndex.value) { clearDays(); monthIndex.value = value }
    level.value = 'month'
  }
  function selectDay(value: string) {
    if (value !== date.value) { cancel(['hour']); hours.value = null; hourIndex.value = ''; date.value = value }
    level.value = 'day'
  }
  function selectHour(value: string) { hourIndex.value = value; level.value = 'hour' }
  async function load(tier: Tier) {
    if (loading.value[tier] || (tier === 'day' && !month.value) || (tier === 'hour' && !day.value)) return
    if ((tier === 'month' && months.value) || (tier === 'day' && days.value) || (tier === 'hour' && hours.value)) return
    const revision = ++revisions[tier]
    aborts[tier]?.abort(); const controller = new AbortController(); aborts[tier] = controller
    loading.value[tier] = true; errors.value[tier] = ''
    const base = { birthDate: birth().slice(0,10), birthTime: birth().slice(11,16), year: year() }
    const key = `${birth()}|${year()}|${tier === 'month' ? '' : monthIndex.value}|${tier === 'hour' ? date.value : ''}`
    try {
      const remember = <T>(cache: Map<string,T>, result: T) => {
        if (cache.size >= 24 && !cache.has(key)) cache.delete(cache.keys().next().value!)
        cache.set(key, result)
        return result
      }
      if (tier === 'month') {
        const result = caches.month.get(key) ?? await getFlowMonths(base, controller.signal)
        if (revision === revisions.month) months.value = remember(caches.month, result)
      } else if (tier === 'day') {
        const result = caches.day.get(key) ?? await getFlowDays({ ...base, monthIndex: Number(monthIndex.value) }, controller.signal)
        if (revision === revisions.day) days.value = remember(caches.day, result)
      } else {
        const result = caches.hour.get(key) ?? await getFlowHours({ ...base, monthIndex: Number(monthIndex.value), date: date.value }, controller.signal)
        if (revision === revisions.hour) hours.value = remember(caches.hour, result)
      }
    } catch {
      if (revision === revisions[tier]) errors.value[tier] = '加载失败，请确认本地后端连接后重试。'
    } finally { if (revision === revisions[tier]) loading.value[tier] = false }
  }
  watch(() => [birth(), year()], () => {
    cancel(['month','day','hour']); months.value = null; monthIndex.value = ''; clearDays(); level.value = 'year'
  }, { flush: 'sync' })
  onBeforeUnmount(() => cancel(['month','day','hour']))
  return { months, days, hours, monthIndex, date, hourIndex, month, day, hour, level, loading, errors, selectMonth, selectDay, selectHour, load }
}
