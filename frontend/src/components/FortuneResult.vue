<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import TimeSelect from './TimeSelect.vue'
import { useFlowSelection } from '../lib/use-flow-selection'
import type { BirthGender, FortuneTimeline } from '../api/bazi'

const props = defineProps<{ fortune: FortuneTimeline; gender: BirthGender; birthDateTime: string }>()
const currentYear = new Date(Date.now() + 8 * 3600_000).getUTCFullYear()
const selectedIndex = ref('0'), selectedYear = ref('0')
const decade = computed(() => props.fortune.decades[Number(selectedIndex.value)]!)
const annual = computed(() => decade.value.annualYears.find(y => String(y.year) === selectedYear.value) ?? decade.value.annualYears[0]!)
function selectDecade(value: string) {
  selectedIndex.value = value
  selectedYear.value = String(decade.value.annualYears.find(y => y.year === currentYear)?.year ?? decade.value.startYear)
}
watch(() => props.fortune, value => {
  const index = value.decades.findIndex(d => d.startYear <= currentYear && d.endYear >= currentYear)
  selectDecade(String(index >= 0 ? index : 0))
}, { immediate: true })
const flow = useFlowSelection(() => props.birthDateTime, () => annual.value.year)
const { months, days, hours, monthIndex, date, hourIndex, month, day, hour, level, loading, errors } = flow
const decadeOptions = computed(() => props.fortune.decades.map((d,i) => ({ value: String(i), label: `${d.ganZhi} · 第${d.index}步`, description: `${d.startYear}—${d.endYear} · ${d.startAge}—${d.endAge}岁`, annotation: d.startYear <= currentYear && d.endYear >= currentYear ? '包含今年' : '' })))
const yearOptions = computed(() => decade.value.annualYears.map(y => ({ value: String(y.year), label: `${y.year}年 ${y.ganZhi}`, description: `${y.age}岁 · ${y.stem.tenGod}`, annotation: y.year === currentYear ? '今年' : '' })))
const monthOptions = computed(() => months.value?.months.map(m => ({ value: String(m.index), label: `${m.name} ${m.ganZhi}`, description: `${m.term} · ${m.startTime.slice(0,10)}起`, annotation: `${m.stem.tenGod} · ${m.naYin}` })) ?? [])
const dayOptions = computed(() => days.value?.days.map(d => ({ value: d.date, label: `${d.date.slice(5)} ${d.ganZhi}`, description: d.lunarDate.display, annotation: `${d.stem.tenGod}${d.partial ? ' · 节令截断' : ''}` })) ?? [])
const hourOptions = computed(() => hours.value?.hours.map(h => ({ value: String(h.index), label: `${h.name} ${h.ganZhi}`, description: `${h.nominalStartTime.slice(0,10) !== date.value ? '前日 ' : ''}${h.nominalStartTime.slice(11,16)}—${h.nominalEndTime.slice(11,16)}`, annotation: !h.included ? '区间外 · 参考时辰' : h.partial ? '部分覆盖' : `${h.stem.tenGod} · ${h.naYin}` })) ?? [])
const focus = computed(() => level.value === 'hour' && hour.value ? hour.value : level.value === 'day' && day.value ? day.value : level.value === 'month' && month.value ? month.value : annual.value)
const focusTitle = computed(() => level.value === 'hour' && hour.value ? `${date.value} ${hour.value.name}` : level.value === 'day' && day.value ? day.value.date : level.value === 'month' && month.value ? `${annual.value.year}年 ${month.value.name}` : `${annual.value.year}年 流年`)
const interval = computed(() => { const i = props.fortune.startInterval; return `${i.years}年 ${i.months}月 ${i.days}天 ${i.hours}小时 ${i.minutes}分钟` })
const timeLabel = (value: string) => value.replace('T',' ')
</script>
<template>
  <section class="fortune-result" aria-labelledby="fortune-title">
    <div class="fortune-heading"><h3 id="fortune-title">运程与时间</h3><span>{{ gender === 'MALE' ? '男' : '女' }} · {{ fortune.direction === 'FORWARD' ? '顺排' : '逆排' }} <small>点选切换</small></span></div>
    <div class="time-navigation">
      <TimeSelect label="大运" :model-value="selectedIndex" :display="`${decade.ganZhi} ${decade.startYear}—${decade.endYear}`" :options="decadeOptions" @update:model-value="selectDecade" />
      <TimeSelect label="流年" v-model="selectedYear" :options="yearOptions" :hint="`第${decade.index}步大运 · ${decade.startYear}—${decade.endYear}年`" @update:model-value="level = 'year'" />
      <TimeSelect label="流月" :model-value="monthIndex" :options="monthOptions" :loading="loading.month" :error="errors.month" hint="按节令换月，寅月从立春起；丑月跨入下一公历年。" @open="flow.load('month')" @retry="flow.load('month')" @update:model-value="flow.selectMonth" />
      <TimeSelect label="流日" :model-value="date" :display="day ? `${day.date.slice(5)} ${day.ganZhi}` : ''" :disabled="!month" :options="dayOptions" :loading="loading.day" :error="errors.day" hint="先选流月，再选该月内的流日；日界为前一日 23:00。" @open="flow.load('day')" @retry="flow.load('day')" @update:model-value="flow.selectDay" />
      <TimeSelect label="流时" :model-value="hourIndex" :disabled="!day" :options="hourOptions" :loading="loading.hour" :error="errors.hour" hint="十二时辰包含参考时段，节令边界处会标注区间外或部分覆盖。" @open="flow.load('hour')" @retry="flow.load('hour')" @update:model-value="flow.selectHour" />
    </div>
    <div class="selected-time" aria-live="polite" aria-label="当前运程选择"><span>当前选择</span><b>{{ decade.ganZhi }}大运</b><button type="button" @click="level = 'year'">{{ annual.year }} {{ annual.ganZhi }}</button><button v-if="month" type="button" @click="level = 'month'">{{ month.name }} {{ month.ganZhi }}</button><button v-if="day" type="button" @click="level = 'day'">{{ day.date.slice(5) }} {{ day.ganZhi }}</button><button v-if="hour" type="button" @click="level = 'hour'">{{ hour.name }} {{ hour.ganZhi }}</button></div>
    <div class="time-focus" aria-label="所选时间详情"><strong>{{ focusTitle }} · {{ focus.ganZhi }}</strong><span>十神 {{ focus.stem.tenGod }}</span><span>五行 {{ focus.stem.element }}{{ focus.branch.element }}</span><span>纳音 {{ focus.naYin }}</span></div>
    <details v-if="level !== 'year'" class="time-boundaries"><summary>查看精确起止与历法说明</summary>
      <template v-if="level === 'month' && month"><p>{{ month.term }}起（含）：{{ timeLabel(month.startTime) }}<br />下一节令（不含）：{{ timeLabel(month.endTime) }}</p></template>
      <template v-else-if="level === 'day' && day"><p>农历 {{ day.lunarDate.display }}<br />开始（含）：{{ timeLabel(day.startTime) }}<br />结束（不含）：{{ timeLabel(day.endTime) }}</p><p v-if="day.partial">节令截断时段：仅覆盖所选流月内的部分流日。</p></template>
      <template v-else-if="level === 'hour' && hour"><p>标准时辰：{{ timeLabel(hour.nominalStartTime) }} 至 {{ timeLabel(hour.nominalEndTime) }}</p><p v-if="hour.included && hour.startTime && hour.endTime">实际覆盖{{ hour.partial ? '（部分时段）' : '' }}：{{ timeLabel(hour.startTime) }} 至 {{ timeLabel(hour.endTime) }}</p><p v-else>区间外：该参考时辰与所选流日无交集。</p></template>
      <p>北京时间 UTC+08:00 · 前一日 23:00 换日，区间均含开始、不含结束。十神以出生日干为参照。</p>
    </details>
    <details class="start-disclosure"><summary><span>起运 {{ fortune.startTime.replace('T',' ').slice(0,16) }}</span><span>查看起运与大运详情</span></summary><div class="start-details"><p>出生后 {{ interval }}起运。{{ fortune.direction === 'FORWARD' ? '顺排' : '逆排' }}，采用 Tyme4j 默认童限规则。</p><p>第 {{ decade.index }} 步大运 {{ decade.ganZhi }}：{{ decade.startYear }}—{{ decade.endYear }}年，{{ decade.startAge }}—{{ decade.endAge }}岁；十神 {{ decade.stem.tenGod }}，五行 {{ decade.stem.element }}{{ decade.branch.element }}，纳音 {{ decade.naYin }}。</p><p>年龄为年序虚岁；列表展示起运后前十步大运。流年立春起，交运当年须结合精确交运时刻区分。流月、日、时在打开选择器时加载，已加载内容在本页有限缓存，不写入本机快照。</p></div></details>
  </section>
</template>
<style scoped>
.fortune-result { margin-top: 18px; background: #fff; border: 1px solid #dfe6dc; border-radius: 10px; padding: 22px; }
.fortune-heading { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 16px; }
.fortune-heading h3 { margin: 0; font-size: 18px; }
.fortune-heading > span { font-size: 11px; color: #74876b; }
.fortune-heading small { margin-left: 10px; color: #92a08c; }
.time-navigation { display: grid; grid-template-columns: 1.25fr 1fr 1fr 1fr 1fr; gap: 10px; }
.selected-time { display: flex; align-items: center; flex-wrap: wrap; gap: 7px; padding: 14px 0 8px; font-size: 11px; color: #7e8d76; }
.selected-time b { color: #48633f; font-weight: 500; }
.selected-time button { color: #54764a; background: #f0f5eb; font-size: 11px; padding: 5px 8px; border-radius: 4px; }
.selected-time button:hover { background: #e1ecd8; }
.time-focus { display: flex; align-items: center; flex-wrap: wrap; gap: 8px 18px; background: #f6f9f3; border-radius: 6px; padding: 13px; font-size: 11px; color: #74896a; }
.time-focus strong { font-size: 13px; font-weight: 550; color: #425f37; }
.start-disclosure, .time-boundaries { font-size: 11px; color: #798d6e; line-height: 1.9; }
.start-disclosure summary { padding-top: 12px; cursor: pointer; }
.start-disclosure summary > span + span { margin-left: 18px; color: #557d47; }
.time-boundaries summary { padding: 10px 0 0; cursor: pointer; }
.start-details { border-top: 1px solid #e5ebdf; margin-top: 12px; }
@media(max-width:767px) { .fortune-result { padding: 16px 12px; } .time-navigation { grid-template-columns: repeat(2,minmax(0,1fr)); } .time-navigation > :first-child { grid-column: 1 / -1; } .fortune-heading h3 { font-size: 16px; } .start-disclosure summary > span { display: block; } .start-disclosure summary > span + span { margin-left: 0; } .time-focus { gap: 9px; } }
</style>
