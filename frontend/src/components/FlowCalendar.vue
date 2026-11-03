<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import FlowHours from './FlowHours.vue'
import { getFlowDays, getFlowMonths, type FlowDays, type FlowMonths, type FlowHours as FlowHoursData } from '../api/bazi'

const props = defineProps<{ year: number; birthDateTime: string }>()
const months = ref<FlowMonths | null>(null)
const days = ref<FlowDays | null>(null)
const monthIndex = ref(1)
const selectedDate = ref('')
const monthLoading = ref(false)
const dayLoading = ref(false)
const monthError = ref('')
const dayError = ref('')
const dayPanel = ref<HTMLElement>()
const dayDetail = ref<HTMLElement>()
const month = computed(() => months.value?.months.find(item => item.index === monthIndex.value))
const day = computed(() => days.value?.days.find(item => item.date === selectedDate.value) ?? days.value?.days[0])
const monthCache = new Map<string, FlowMonths>()
const dayCache = new Map<string, FlowDays>()
const hourCache = new Map<string, FlowHoursData>()
let monthRequest = 0
let dayRequest = 0
let monthAbort: AbortController | undefined
let dayAbort: AbortController | undefined
const timeLabel = (time: string) => time.replace('T', ' ')
function remember<T>(cache: Map<string, T>, key: string, value: T) { if (cache.size >= 24 && !cache.has(key)) cache.delete(cache.keys().next().value!); cache.set(key, value) }
function input() { return { birthDate: props.birthDateTime.slice(0, 10), birthTime: props.birthDateTime.slice(11, 16), year: props.year } }
function scrollOnMobile(target?: HTMLElement) { if (window.matchMedia('(max-width: 767px)').matches) nextTick(() => target?.scrollIntoView({ behavior: 'smooth', block: 'start' })) }
async function loadMonths() {
  const request = ++monthRequest
  dayRequest++
  monthAbort?.abort(); dayAbort?.abort()
  months.value = null; days.value = null; monthError.value = ''; dayError.value = ''; dayLoading.value = false
  monthLoading.value = true
  monthAbort = new AbortController()
  const params = input()
  const key = `${props.birthDateTime}-${params.year}`
  try {
    const result = monthCache.get(key) ?? await getFlowMonths(params, monthAbort.signal)
    if (request !== monthRequest) return
    remember(monthCache, key, result)
    months.value = result
    const now = new Date(Date.now() + 8 * 3600_000).toISOString().slice(0, 19)
    monthIndex.value = result.months.find(item => item.startTime <= now && item.endTime > now)?.index ?? 1
    await loadDays()
  } catch {
    if (request === monthRequest) monthError.value = '流月加载失败，请确认本地后端已启动后重试。'
  } finally { if (request === monthRequest) monthLoading.value = false }
}
async function loadDays() {
  const request = ++dayRequest
  dayAbort?.abort(); dayAbort = new AbortController()
  days.value = null; selectedDate.value = ''; dayError.value = ''; dayLoading.value = true
  const params = { ...input(), monthIndex: monthIndex.value }
  const key = `${props.birthDateTime}-${params.year}-${params.monthIndex}`
  try {
    const result = dayCache.get(key) ?? await getFlowDays(params, dayAbort.signal)
    if (request !== dayRequest) return
    remember(dayCache, key, result)
    days.value = result
    const now = new Date(Date.now() + 8 * 3600_000).toISOString().slice(0, 19)
    selectedDate.value = result.days.find(item => item.startTime <= now && item.endTime > now)?.date ?? result.days[0]?.date ?? ''
  } catch {
    if (request === dayRequest) dayError.value = '流日加载失败，请确认本地后端已启动后重试。'
  } finally { if (request === dayRequest) dayLoading.value = false }
}
function selectMonth(index: number) { monthIndex.value = index; loadDays(); scrollOnMobile(dayPanel.value) }
function selectDay(date: string) { selectedDate.value = date; scrollOnMobile(dayDetail.value) }
watch(() => [props.year, props.birthDateTime] as const, loadMonths, { immediate: true })
onBeforeUnmount(() => { monthRequest++; dayRequest++; monthAbort?.abort(); dayAbort?.abort() })
</script>

<template>
  <section class="flow-calendar" aria-labelledby="flow-months-title">
    <div class="flow-heading"><div><p>流年 → 流月 → 流日 → 流时</p><h4 id="flow-months-title">{{ year }}{{ months ? ` ${months.yearGanZhi}` : '' }}年 · 流月</h4></div><span>按节令换月</span></div>
    <p class="flow-hint">寅月从立春开始，依次至丑月跨入下一公历年；不是公历月或农历月。十神沿用出生日干为参照。</p>
    <div v-if="monthLoading && !months" role="status" class="flow-loading">正在加载 {{ year }} 年流月…</div>
    <div v-if="monthError" class="flow-error" role="alert">{{ monthError }} <button type="button" @click="loadMonths">重试流月</button></div>
    <template v-if="months && month">
      <div class="flow-months" role="group" aria-label="选择流月"><button v-for="item in months.months" :key="item.index" type="button" :aria-pressed="monthIndex === item.index" :aria-label="`${item.name} ${item.ganZhi} ${item.term}`" @click="selectMonth(item.index)"><span>{{ item.name }}<small>{{ item.term }}</small></span><strong>{{ item.ganZhi }}</strong><small>{{ item.startTime.slice(0,4) === String(year) ? item.startTime.slice(5,10) : item.startTime.slice(0,10) }} 起 · {{ item.stem.tenGod }}</small></button></div>
      <div class="flow-month-detail"><strong>{{ year }}年 · {{ month.name }} {{ month.ganZhi }}</strong><span>{{ month.stem.tenGod }} · {{ month.stem.element }}{{ month.branch.element }} · {{ month.naYin }}</span><p><b>{{ month.term }}起（含）</b> {{ timeLabel(month.startTime) }}<br /><b>下一节令（不含）</b> {{ timeLabel(month.endTime) }}</p></div>
      <section ref="dayPanel" id="flow-days-panel" class="flow-days-panel" aria-labelledby="flow-days-title">
        <div class="flow-heading day-heading"><h4 id="flow-days-title">{{ month.name }}对应流日</h4><a href="#flow-months-title">返回流月 ↑</a></div>
        <p class="flow-hint">流日于前一日 23:00 开始，日期标记其主要覆盖的公历日；节令边界处仅展示该月内的部分时段。点击查看精确起止。</p>
        <div v-if="dayLoading" role="status" class="flow-loading">正在加载 {{ month.name }}流日…</div>
        <div v-if="dayError" class="flow-error" role="alert">{{ dayError }} <button type="button" @click="loadDays">重试流日</button></div>
        <template v-if="days && day">
          <div class="flow-days" role="group" aria-label="选择流日"><button v-for="item in days.days" :key="item.date" type="button" :aria-pressed="day.date === item.date" :aria-label="`${item.date} ${item.ganZhi}流日`" @click="selectDay(item.date)"><span>{{ item.date.slice(5) }}</span><strong>{{ item.ganZhi }}</strong><small>{{ item.stem.tenGod }}</small><em v-if="item.partial">部分时段</em></button></div>
          <div ref="dayDetail" class="flow-day-detail" aria-label="流日详情" aria-live="polite"><div><h4>{{ day.date }} · {{ day.ganZhi }}流日</h4><span v-if="day.partial" class="partial-tag">节令截断时段</span></div><p>农历 {{ day.lunarDate.display }}</p><dl><div><dt>天干十神</dt><dd>{{ day.stem.tenGod }}</dd></div><div><dt>干支五行</dt><dd>{{ day.stem.name }}{{ day.stem.element }} · {{ day.branch.name }}{{ day.branch.element }}</dd></div><div><dt>纳音</dt><dd>{{ day.naYin }}</dd></div></dl><p class="flow-time-range">开始（含） {{ timeLabel(day.startTime) }}<br />结束（不含） {{ timeLabel(day.endTime) }}<br />北京时间 UTC+08:00</p><a href="#flow-days-title">返回流日列表 ↑</a> <a href="#flow-hours-title" class="view-hours-link">查看十二时辰 ↓</a></div>
          <FlowHours :year="year" :month-index="monthIndex" :date="day.date" :birth-date-time="birthDateTime" :response-cache="hourCache" />
        </template>
      </section>
    </template>
    <p class="flow-footnote">流月、流日、流时按需从本地后端加载，已加载内容可在本页重复查看；不写入本机排盘快照，离线时未加载的明细需恢复后端连接后重试。</p>
  </section>
</template>

<style scoped>
.flow-calendar { margin-top: 25px; border-top: 1px solid #e3eadc; padding-top: 25px; scroll-margin-top: 20px; }
.flow-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.flow-heading p { margin: 0 0 8px; font-size: 10px; color: #839576; letter-spacing: 1px; }
.flow-heading h4 { margin: 0; font-size: 16px; font-weight: 550; }
.flow-heading > span, .flow-heading a { font-size: 11px; color: #829372; text-decoration: none; }
.flow-hint, .flow-footnote { font-size: 11px; color: #7f8f73; line-height: 1.9; margin: 0 0 17px; }
.flow-footnote { margin-top: 18px; margin-bottom: 0; }
.flow-loading { padding: 20px; background: #f5f8f0; color: #809570; border-radius: 8px; font-size: 12px; }
.flow-error { background: #fcf5ed; color: #9f7048; padding: 16px; font-size: 12px; line-height: 1.8; border-radius: 8px; }
.flow-error button { font-size: 11px; padding: 6px 10px; background: #ede3d5; color: #8d6a43; margin-left: 7px; }
.flow-months { display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 9px; }
.flow-months button { background: #fafcf6; border: 1px solid #e0e9d7; color: #637e50; padding: 14px 12px; border-radius: 9px; display: grid; gap: 9px; text-align: left; }
.flow-months button[aria-pressed="true"], .flow-days button[aria-pressed="true"] { background: #edf4e3; border-color: #8ba774; box-shadow: 0 0 0 1px #8ba774; }
.flow-months button > span { font-size: 12px; }
.flow-months button > span small { font-size: 10px; margin-left: 10px; color: #92a080; }
.flow-months strong { font-size: 21px; font-weight: 550; }
.flow-months button > small { font-size: 10px; color: #889b77; }
.flow-month-detail { padding: 18px; background: #f5f8ef; border-radius: 9px; margin: 16px 0 24px; }
.flow-month-detail strong { font-size: 15px; font-weight: 550; display: block; }
.flow-month-detail > span { display: block; margin-top: 10px; font-size: 12px; color: #789164; }
.flow-month-detail p { font-size: 11px; color: #829571; line-height: 1.9; margin: 13px 0 0; }
.flow-month-detail b { font-weight: 400; color: #6e875b; }
.flow-days-panel, .flow-day-detail, #flow-months-title, #flow-days-title { scroll-margin-top: 20px; }
.flow-days { display: grid; grid-template-columns: repeat(7,minmax(0,1fr)); gap: 7px; }
.flow-days button { padding: 11px 5px; background: #fbfcf8; color: #688553; border: 1px solid #e2ead9; border-radius: 8px; display: grid; gap: 8px; justify-items: center; }
.flow-days button > span { font-size: 10px; color: #8b9b7c; }
.flow-days strong { font-size: 18px; font-weight: 550; }
.flow-days small { font-size: 10px; }
.flow-days em { font-size: 9px; font-style: normal; color: #ac9568; }
.flow-day-detail { padding: 19px; border: 1px solid #dfe8d4; border-radius: 10px; margin-top: 16px; }
.flow-day-detail > div:first-child { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.flow-day-detail h4 { margin: 0; font-size: 15px; font-weight: 550; }
.partial-tag { padding: 4px 7px; background: #f8f0df; color: #9f8557; font-size: 10px; border-radius: 5px; }
.flow-day-detail > p { font-size: 12px; color: #839473; line-height: 1.9; }
.flow-day-detail dl { display: grid; grid-template-columns: repeat(3,1fr); gap: 12px; margin: 20px 0; }
.flow-day-detail dt { font-size: 10px; color: #94a184; }
.flow-day-detail dd { font-size: 13px; color: #6b8557; margin: 8px 0 0; }
.flow-day-detail .flow-time-range { font-size: 11px; padding-top: 14px; border-top: 1px solid #edf1e6; }
.view-hours-link { margin-left: 18px; }
.flow-day-detail > a { font-size: 11px; color: #8a9d75; text-decoration: none; }
@media (max-width:767px) { .flow-months { grid-template-columns: repeat(2,minmax(0,1fr)); gap: 8px; } .flow-months button { padding: 13px 11px; } .flow-days { grid-template-columns: repeat(4,minmax(0,1fr)); gap: 6px; } .flow-day-detail { padding: 16px; } .flow-day-detail dl { gap: 8px; } .flow-day-detail dd { font-size: 12px; line-height: 1.8; } .flow-heading h4 { font-size: 15px; } }
</style>
