<script setup lang="ts">
import { computed } from 'vue'
import type { BaziChart, ElementName } from '../api/bazi'

const props = defineProps<{ chart: BaziChart }>()
const tones: Record<ElementName, string> = { 木: 'wood', 火: 'fire', 土: 'earth', 金: 'metal', 水: 'water' }
const cards = computed(() => {
  const palaces = props.chart.auxiliaryPalaces
  if (!palaces) return []
  return [
    { key: 'fetalOrigin', label: '胎元', note: '月柱推导', source: `月柱 ${props.chart.pillars.month}`, value: palaces.fetalOrigin },
    { key: 'ownSign', label: '命宫', note: '年 · 月 · 时推导', source: `年柱 ${props.chart.pillars.year} · 月支 ${props.chart.pillarDetails[1]!.branch.name} · 时支 ${props.chart.pillarDetails[3]!.branch.name}`, value: palaces.ownSign },
    { key: 'bodySign', label: '身宫', note: '年 · 月 · 时推导', source: `年柱 ${props.chart.pillars.year} · 月支 ${props.chart.pillarDetails[1]!.branch.name} · 时支 ${props.chart.pillarDetails[3]!.branch.name}`, value: palaces.bodySign },
  ]
})
</script>

<template>
  <section class="auxiliary-section" aria-labelledby="auxiliary-title">
    <div class="auxiliary-heading"><div><p>四柱衍生</p><h4 id="auxiliary-title">胎元 · 命宫 · 身宫</h4></div><span>Tyme4j</span></div>
    <p class="auxiliary-guide">由本次出生四柱推导，十神以日干「{{ chart.dayMaster.name }}」为参照。作为附加信息单独展示，不计入八字表层五行的 8 字统计。</p>
    <template v-if="chart.auxiliaryPalaces">
      <div class="auxiliary-grid">
        <article v-for="card in cards" :key="card.key" class="auxiliary-card" :aria-label="`${card.label}详解`">
          <div class="auxiliary-card-heading"><h5>{{ card.label }}</h5><span>{{ card.note }}</span></div>
          <div class="auxiliary-characters" :aria-label="`${card.label}干支 ${card.value.ganZhi}`"><div :class="tones[card.value.stem.element]"><strong>{{ card.value.stem.name }}</strong><small>天干 · {{ card.value.stem.element }}</small></div><div :class="tones[card.value.branch.element]"><strong>{{ card.value.branch.name }}</strong><small>地支 · {{ card.value.branch.element }}</small></div></div>
          <dl><div><dt>天干十神</dt><dd>{{ card.value.stem.tenGod }}</dd></div><div><dt>纳音</dt><dd>{{ card.value.naYin }}</dd></div></dl>
          <p class="auxiliary-source">{{ card.source }}</p>
        </article>
      </div>
      <details class="auxiliary-help"><summary>查看胎元与两宫的计算口径</summary><dl><dt>胎元</dt><dd>直接使用 Tyme4j getFetalOrigin：月干顺进一位、月支顺进三位。这里展示干支推导结果，不推算实际受孕日期。</dd><dt>命宫与身宫</dt><dd>直接使用 Tyme4j getOwnSign、getBodySign，按出生年干、月支和时支推导。月柱沿用节令换月，年柱沿用立春换年；农历输入先转换到同一出生时刻再计算。</dd></dl><p>沿用北京时间 UTC+08:00、23 点换日，不校正夏令时或真太阳时。采用八字宫位口径，不切换为紫微斗数命身宫。</p><p>规则版本：{{ chart.auxiliaryPalaces.ruleVersion }}</p></details>
    </template>
    <p v-else class="auxiliary-legacy" role="status">这份历史快照尚无胎元、命宫和身宫。重新排盘后可补齐，并更新原本机记录。</p>
  </section>
</template>

<style scoped>
.auxiliary-section { margin-top: 24px; padding: 20px; border: 1px solid #dfe7dc; border-radius: 12px; color: #64765a; }
.auxiliary-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.auxiliary-heading p { font-size: 10px; letter-spacing: 2px; color: #829283; margin: 0 0 6px; }
.auxiliary-heading h4 { margin: 0; font-size: 16px; font-weight: 600; }
.auxiliary-heading > span { font-size: 10px; border-radius: 5px; background: #f2f6ef; padding: 5px 8px; }
.auxiliary-guide, .auxiliary-legacy { font-size: 12px; line-height: 1.9; margin: 14px 0 18px; color: #7a887c; }
.auxiliary-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.auxiliary-card { border: 1px solid #e3e9de; border-radius: 10px; padding: 16px; background: #fbfcf9; min-width: 0; }
.auxiliary-card-heading { display: flex; align-items: center; justify-content: space-between; gap: 8px; flex-wrap: wrap; }
.auxiliary-card h5 { margin: 0; font-size: 14px; font-weight: 550; }
.auxiliary-card-heading span { font-size: 10px; color: #8b9786; }
.auxiliary-characters { display: flex; gap: 28px; padding: 20px 0; }
.auxiliary-characters > div { display: grid; gap: 7px; }
.auxiliary-characters strong { font-size: 30px; line-height: 1.2; font-weight: 550; }
.auxiliary-characters small { font-size: 10px; }
.wood { color: #3c7357; } .fire { color: #b4503d; } .earth { color: #8d6d39; } .metal { color: #6d7185; } .water { color: #3e7799; }
.auxiliary-card dl { margin: 0; display: grid; gap: 10px; font-size: 12px; }
.auxiliary-card dl > div { display: flex; justify-content: space-between; gap: 8px; }
.auxiliary-card dt { color: #8b9786; } .auxiliary-card dd { margin: 0; }
.auxiliary-source { font-size: 10px; line-height: 1.8; border-top: 1px solid #e7ece2; margin: 15px 0 0; padding-top: 12px; color: #8b9786; overflow-wrap: anywhere; }
.auxiliary-help { font-size: 12px; line-height: 1.9; margin-top: 14px; overflow-wrap: anywhere; }
.auxiliary-help summary { cursor: pointer; padding: 10px 0; min-height: 44px; box-sizing: border-box; }
.auxiliary-help summary:focus-visible { outline: 2px solid #95b8a7; outline-offset: 3px; }
.auxiliary-help dt { font-weight: 600; } .auxiliary-help dd { margin: 3px 0 12px; }
@media (max-width: 767px) {
  .auxiliary-section { padding: 16px; }
  .auxiliary-heading h4 { font-size: 15px; }
  .auxiliary-grid { grid-template-columns: 1fr; }
  .auxiliary-card { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 14px; }
  .auxiliary-card-heading, .auxiliary-source { grid-column: 1 / -1; }
  .auxiliary-characters { gap: 16px; padding: 0; }
  .auxiliary-characters strong { font-size: 28px; }
  .auxiliary-card dl { align-content: center; }
  .auxiliary-card dl > div { display: block; }
  .auxiliary-card dt { font-size: 10px; }
  .auxiliary-card dd { margin-top: 5px; }
  .auxiliary-source { margin-top: 0; padding-top: 10px; }
}
</style>
