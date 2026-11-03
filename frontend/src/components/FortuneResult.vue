<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { BirthGender, FortuneTimeline } from '../api/bazi'

const props = defineProps<{ fortune: FortuneTimeline; gender: BirthGender }>()
const currentYear = new Date(Date.now() + 8 * 3600_000).getUTCFullYear()
const selectedIndex = ref(0)
const selectedYear = ref(0)
const decade = computed(() => props.fortune.decades[selectedIndex.value]!)
const annual = computed(() => decade.value.annualYears.find(year => year.year === selectedYear.value) ?? decade.value.annualYears[0]!)
const interval = computed(() => {
  const i = props.fortune.startInterval
  return `${i.years} 年 ${i.months} 月 ${i.days} 天 ${i.hours} 小时 ${i.minutes} 分钟`
})
function selectDecade(index: number) { selectedIndex.value = index; selectedYear.value = decade.value.annualYears.find(year => year.year === currentYear)?.year ?? decade.value.startYear }
watch(() => props.fortune, value => { const current = value.decades.findIndex(d => d.startYear <= currentYear && d.endYear >= currentYear); selectDecade(current >= 0 ? current : 0) }, { immediate: true })
</script>

<template>
  <section class="fortune-result" aria-labelledby="fortune-title">
    <div class="fortune-heading"><div><p>起运与运程</p><h3 id="fortune-title">大运 · 流年</h3></div><span>{{ gender === 'MALE' ? '男' : '女' }} · {{ fortune.direction === 'FORWARD' ? '顺排' : '逆排' }}</span></div>
    <div class="start-fortune"><div><span>起运公历时间 · UTC+08:00</span><strong>{{ fortune.startTime.replace('T', ' ').slice(0, 16) }}</strong></div><div><span>出生后起运间隔</span><strong>{{ interval }}</strong></div></div>
    <p class="fortune-policy">Tyme4j 默认起运法：阳男阴女顺排，阴男阳女逆排；按节令间隔以三天折一年。下列年龄为年序虚岁（年份减出生公历年加 1），不是周岁。</p>
    <div class="decade-strip" role="group" aria-label="选择大运"><button v-for="(item, index) in fortune.decades" :key="item.index" type="button" :aria-pressed="selectedIndex === index" :aria-label="`第${item.index}步大运 ${item.ganZhi} ${item.startYear}至${item.endYear}年`" @click="selectDecade(index)"><span>第 {{ item.index }} 步</span><strong>{{ item.ganZhi }}</strong><small>{{ item.startAge }}—{{ item.endAge }} 岁</small><small>{{ item.startYear }}—{{ item.endYear }}</small><em v-if="item.startYear <= currentYear && item.endYear >= currentYear">含今年</em></button></div>
    <div class="decade-focus" role="status"><h4>第 {{ decade.index }} 步 · {{ decade.ganZhi }}大运</h4><p>{{ decade.startYear }}—{{ decade.endYear }} 年 · {{ decade.startAge }}—{{ decade.endAge }} 岁（年序虚岁）</p><div><span>天干十神 <b>{{ decade.stem.tenGod }}</b></span><span>五行 <b>{{ decade.stem.element }}{{ decade.branch.element }}</b></span><span>纳音 <b>{{ decade.naYin }}</b></span></div></div>
    <div class="annual-heading"><h4>对应流年</h4><span>点选年份查看详情</span></div>
    <div class="annual-years" role="group" aria-label="选择流年"><button v-for="year in decade.annualYears" :key="year.year" type="button" :aria-pressed="annual.year === year.year" :aria-label="`${year.year}年 ${year.ganZhi}流年`" @click="selectedYear = year.year"><span>{{ year.year }}<small v-if="year.year === currentYear">今年</small></span><strong>{{ year.ganZhi }}</strong><small>{{ year.age }} 岁 · {{ year.stem.tenGod }}</small></button></div>
    <div class="annual-detail" aria-live="polite" aria-label="流年详情"><strong>{{ annual.year }} 年 · {{ annual.ganZhi }}</strong><span>{{ annual.age }} 岁（年序虚岁）</span><dl><div><dt>天干十神</dt><dd>{{ annual.stem.tenGod }}</dd></div><div><dt>干支五行</dt><dd>{{ annual.stem.name }}{{ annual.stem.element }} · {{ annual.branch.name }}{{ annual.branch.element }}</dd></div><div><dt>纳音</dt><dd>{{ annual.naYin }}</dd></div></dl></div>
    <p class="fortune-boundary">展示起运后的前 10 步大运，每步 10 个流年。流年按干支年（立春起）标注；列表为年份分组，起运、交运当年须结合具体交运时刻区分，不代表整年都属同一步大运。起运前童限及小运暂未展开。</p>
  </section>
</template>

<style scoped>
.fortune-result { margin-top: 30px; padding-top: 27px; border-top: 1px solid #e5ebdf; }
.fortune-heading { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.fortune-heading p { color: #829178; font-size: 11px; margin: 0 0 8px; letter-spacing: 2px; }
.fortune-heading h3 { margin: 0; font-size: 19px; font-weight: 600; }
.fortune-heading > span { background: #edf3e7; color: #63804e; font-size: 12px; padding: 7px 13px; border-radius: 20px; }
.start-fortune { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.start-fortune > div { border: 1px solid #e1e8d9; background: #f5f8f1; border-radius: 10px; padding: 17px; display: grid; gap: 10px; }
.start-fortune span { color: #809072; font-size: 11px; }
.start-fortune strong { font-size: 15px; font-weight: 550; line-height: 1.7; }
.fortune-policy, .fortune-boundary { color: #7e8d72; font-size: 11px; line-height: 1.9; margin: 15px 0 20px; }
.decade-strip { display: grid; grid-template-columns: repeat(5, 1fr); gap: 9px; }
.decade-strip button { position: relative; display: flex; flex-direction: column; align-items: center; gap: 8px; color: #667e54; background: #fafbf7; border: 1px solid #e1e8d9; padding: 15px 7px; border-radius: 9px; }
.decade-strip button[aria-pressed="true"] { background: #edf4e4; border-color: #829e68; box-shadow: 0 0 0 1px #829e68; }
.decade-strip span { font-size: 10px; color: #8c9a7e; }
.decade-strip strong { font-size: 23px; font-weight: 550; }
.decade-strip small { font-size: 10px; white-space: nowrap; }
.decade-strip em { font-size: 9px; color: #8d9d71; font-style: normal; }
.decade-focus { margin: 17px 0 25px; padding: 19px; background: #f5f8f0; border-radius: 10px; }
.decade-focus h4 { margin: 0; font-size: 16px; font-weight: 550; }
.decade-focus p { font-size: 12px; color: #819172; margin: 10px 0 14px; }
.decade-focus > div { display: flex; flex-wrap: wrap; gap: 9px 24px; font-size: 11px; color: #829274; }
.decade-focus b { color: #587143; margin-left: 7px; font-weight: 500; }
.annual-heading { display: flex; justify-content: space-between; align-items: center; margin-bottom: 13px; }
.annual-heading h4 { font-size: 14px; font-weight: 550; margin: 0; }
.annual-heading span { font-size: 11px; color: #8a977d; }
.annual-years { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 9px; }
.annual-years button { display: grid; gap: 10px; justify-items: start; background: #fcfdf9; border: 1px solid #e4eadf; color: #687f55; border-radius: 8px; padding: 12px; text-align: left; }
.annual-years button[aria-pressed="true"] { background: #eef4e7; border-color: #8aa475; }
.annual-years span { font-size: 12px; }
.annual-years span small { margin-left: 4px; font-size: 9px; color: #9b925d; }
.annual-years strong { font-size: 19px; font-weight: 550; }
.annual-years > button > small { font-size: 10px; color: #89967a; }
.annual-detail { margin-top: 15px; border: 1px solid #dfe8d6; border-radius: 10px; padding: 20px; }
.annual-detail > strong { display: inline-block; font-size: 16px; font-weight: 550; margin-right: 15px; }
.annual-detail > span { font-size: 11px; color: #8a967c; }
.annual-detail dl { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin: 19px 0 0; }
.annual-detail dt { font-size: 10px; color: #89967c; }
.annual-detail dd { margin: 9px 0 0; font-size: 13px; color: #5e7850; }
.fortune-boundary { margin-bottom: 0; }
@media (max-width: 767px) { .start-fortune { grid-template-columns: 1fr; gap: 10px; } .decade-strip { display: flex; overflow-x: auto; padding: 2px 2px 12px; scroll-snap-type: x proximity; } .decade-strip button { flex: 0 0 104px; scroll-snap-align: start; } .annual-years { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; } .annual-years button { padding: 14px; } .annual-detail { padding: 16px; } .annual-detail > span { display: block; margin-top: 9px; } .annual-detail dl { gap: 9px; } .annual-detail dd { font-size: 12px; line-height: 1.7; } .decade-focus > div { gap: 10px 16px; } }
</style>
