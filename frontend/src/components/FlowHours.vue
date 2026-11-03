<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { getFlowHours, type FlowHours, type FlowHour } from '../api/bazi'

const props = defineProps<{ year: number; monthIndex: number; date: string; birthDateTime: string; responseCache?: Map<string, FlowHours> }>()
const data = ref<FlowHours | null>(null)
const loading = ref(false)
const error = ref('')
const selectedIndex = ref(1)
const detail = ref<HTMLElement>()
const selected = computed(() => data.value?.hours.find(hour => hour.index === selectedIndex.value))
const cache = props.responseCache ?? new Map<string, FlowHours>()
let revision = 0
let abort: AbortController | undefined
const fullTime = (time: string) => time.replace('T', ' ')
function clockRange(hour: FlowHour) { return `${hour.nominalStartTime.slice(0, 10) !== props.date ? '前日 ' : ''}${hour.nominalStartTime.slice(11,16)}—${hour.nominalEndTime.slice(11,16)}` }
async function load() {
  const request = ++revision
  abort?.abort(); abort = new AbortController()
  data.value = null; error.value = ''; loading.value = true
  const input = { birthDate: props.birthDateTime.slice(0,10), birthTime: props.birthDateTime.slice(11,16), year: props.year, monthIndex: props.monthIndex, date: props.date }
  const key = `${props.birthDateTime}-${props.year}-${props.monthIndex}-${props.date}`
  try {
    const result = cache.get(key) ?? await getFlowHours(input, abort.signal)
    if (request !== revision) return
    if (cache.size >= 24 && !cache.has(key)) cache.delete(cache.keys().next().value!)
    cache.set(key, result)
    data.value = result
    const now = new Date(Date.now() + 8 * 3600_000).toISOString().slice(0,19)
    selectedIndex.value = result.hours.find(hour => hour.included && hour.startTime! <= now && hour.endTime! > now)?.index
      ?? result.hours.find(hour => hour.included)?.index ?? 1
  } catch { if (request === revision) error.value = '流时加载失败，请确认本地后端已启动后重试。' }
  finally { if (request === revision) loading.value = false }
}
function selectHour(index: number) {
  selectedIndex.value = index
  if (window.matchMedia('(max-width:767px)').matches) nextTick(() => detail.value?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
}
watch(() => [props.year, props.monthIndex, props.date, props.birthDateTime] as const, load, { immediate: true })
onBeforeUnmount(() => { revision++; abort?.abort() })
</script>

<template>
  <section class="flow-hours-panel" aria-labelledby="flow-hours-title">
    <div class="hour-heading"><div><p>流日 → 流时</p><h4 id="flow-hours-title">{{ date }} · 十二时辰</h4></div><a href="#flow-days-title">返回流日 ↑</a></div>
    <p class="hour-hint">子时从前一日 23:00 开始，亥时至当日 23:00 结束。十神以出生时日干为参照；节令截断的流日会标注部分覆盖或区间外，区间外时辰仅供对照。</p>
    <p v-if="loading" class="hour-loading" role="status">正在加载十二时辰…</p>
    <div v-if="error" class="hour-error" role="alert">{{ error }} <button type="button" @click="load">重试流时</button></div>
    <template v-if="data && selected">
      <div class="hour-grid" role="group" aria-label="选择流时"><button v-for="hour in data.hours" :key="hour.index" type="button" :aria-label="`${hour.name} ${hour.ganZhi}流时`" :aria-pressed="selectedIndex === hour.index" :class="{outside: !hour.included}" @click="selectHour(hour.index)"><span class="hour-name">{{ hour.name }}<em v-if="!hour.included">区间外</em><em v-else-if="hour.partial" class="clipped">部分覆盖</em></span><span class="hour-clock">{{ clockRange(hour) }}</span><strong>{{ hour.ganZhi }}</strong><span class="hour-attributes">{{ hour.stem.tenGod }} · {{ hour.stem.element }}{{ hour.branch.element }}</span><small>纳音 {{ hour.naYin }}</small></button></div>
      <div ref="detail" class="hour-detail" aria-label="流时详情" aria-live="polite"><div><h4>{{ date }} · {{ selected.name }} {{ selected.ganZhi }}</h4><span v-if="!selected.included" class="hour-status">区间外 · 参考时辰</span><span v-else-if="selected.partial" class="hour-status">节令截断 · 部分覆盖</span></div><dl><div><dt>天干十神</dt><dd>{{ selected.stem.tenGod }}</dd></div><div><dt>干支五行</dt><dd>{{ selected.stem.name }}{{ selected.stem.element }} · {{ selected.branch.name }}{{ selected.branch.element }}</dd></div><div><dt>纳音</dt><dd>{{ selected.naYin }}</dd></div></dl><p>标准时辰：<br />{{ fullTime(selected.nominalStartTime) }} 至<br />{{ fullTime(selected.nominalEndTime) }}</p><p v-if="selected.included && selected.startTime && selected.endTime" class="hour-actual">所选流日内实际覆盖：<br />开始（含） {{ fullTime(selected.startTime) }}<br />结束（不含） {{ fullTime(selected.endTime) }}</p><p v-else class="hour-actual outside-note">该时辰与所选流日区间无交集，不计入该流日的实际覆盖时段。</p><p class="hour-zone">北京时间 UTC+08:00 · 仍按 23 点换日</p><a href="#flow-hours-title">返回十二时辰 ↑</a></div>
    </template>
    <p class="hour-footnote">流时按需加载并在本页有限缓存，不写入本机排盘快照。</p>
  </section>
</template>

<style scoped>
.flow-hours-panel { margin-top: 25px; padding-top: 24px; border-top: 1px solid #e2e9d9; }
.hour-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 13px; }
.hour-heading p { margin: 0 0 8px; color: #849777; font-size: 10px; letter-spacing: 1px; }
.hour-heading h4 { margin: 0; font-size: 16px; font-weight: 550; }
.hour-heading a, .hour-detail a { color: #869b74; font-size: 11px; text-decoration: none; }
.hour-hint, .hour-footnote { font-size: 11px; line-height: 1.9; color: #809474; margin: 0 0 17px; }
.hour-footnote { margin: 16px 0 0; }
.hour-loading { background: #f5f8f0; border-radius: 8px; padding: 20px; font-size: 12px; color: #7e946e; }
.hour-error { background: #fcf5ed; color: #9f7048; padding: 16px; font-size: 12px; line-height: 1.8; border-radius: 8px; }
.hour-error button { padding: 6px 10px; background: #ede3d5; color: #8d6a43; font-size: 11px; margin-left: 7px; }
.hour-grid { display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 9px; }
.hour-grid button { display: flex; flex-direction: column; align-items: flex-start; gap: 8px; text-align: left; border: 1px solid #e0e9d8; background: #fafcf6; color: #688654; border-radius: 9px; padding: 14px 12px; }
.hour-grid button[aria-pressed="true"] { background: #edf4e3; border-color: #89a571; box-shadow: 0 0 0 1px #89a571; }
.hour-grid button.outside { border-style: dashed; background: #f9faf7; color: #83917b; }
.hour-name { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; font-size: 12px; }
.hour-name em { font-style: normal; font-size: 9px; color: #909b87; padding: 2px 5px; border-radius: 4px; background: #eef1e9; }
.hour-name em.clipped { color: #a18b60; background: #f7efdf; }
.hour-clock { font-size: 10px; color: #8a9c7a; line-height: 1.6; }
.hour-grid strong { font-size: 22px; font-weight: 550; }
.hour-attributes, .hour-grid small { font-size: 11px; line-height: 1.6; }
.hour-grid small { color: #8c9e7b; }
.hour-detail { margin-top: 17px; border: 1px solid #dfe8d5; border-radius: 10px; padding: 20px; }
.hour-detail > div:first-child { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.hour-detail h4 { font-size: 15px; font-weight: 550; margin: 0; }
.hour-status { background: #f7f1e5; color: #9a865e; font-size: 10px; padding: 4px 7px; border-radius: 5px; }
.hour-detail dl { display: grid; grid-template-columns: repeat(3,1fr); gap: 12px; margin: 20px 0; }
.hour-detail dt { font-size: 10px; color: #8b9d7a; }
.hour-detail dd { margin: 8px 0 0; font-size: 13px; color: #6a8556; }
.hour-detail p { font-size: 11px; line-height: 1.9; color: #859b75; }
.hour-actual { border-top: 1px solid #edf1e7; padding-top: 14px; }
.hour-detail .outside-note { color: #9a8763; }
.hour-detail .hour-zone { font-size: 10px; }
#flow-hours-title, .hour-detail { scroll-margin-top: 20px; }
@media(max-width:767px) { .hour-grid { grid-template-columns: repeat(2,minmax(0,1fr)); gap: 8px; } .hour-grid button { padding: 13px 10px; } .hour-heading h4 { font-size: 15px; } .hour-heading a { flex-shrink: 0; } .hour-detail { padding: 16px; } .hour-detail dl { gap: 8px; } .hour-detail dd { font-size: 12px; line-height: 1.8; } }
</style>
