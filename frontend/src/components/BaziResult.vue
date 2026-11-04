<script setup lang="ts">
import { ref } from 'vue'
import AuxiliaryPalaces from './AuxiliaryPalaces.vue'
import type { BaziChart, ElementName } from '../api/bazi'
const props = defineProps<{ chart: BaziChart }>()
const elements: ElementName[] = ['木', '火', '土', '金', '水']
const tones: Record<ElementName, string> = { 木: 'wood', 火: 'fire', 土: 'earth', 金: 'metal', 水: 'water' }
const hitsFor = (key: string) => props.chart.shenSha?.hits.filter(h => h.pillarKey === key) ?? []
const auxiliaryOpen = ref(false)
</script>
<template>
  <div class="bazi-details">
    <p class="day-master-line">日主 <b :class="tones[chart.dayMaster.element]">{{ chart.dayMaster.name }}{{ chart.dayMaster.element }}</b><span>十神以日干为参照</span></p>
    <table class="pillar-table" aria-label="四柱详细对照"><thead><tr><th scope="col">项目</th><th v-for="p in chart.pillarDetails" :key="p.key" scope="col" :class="{'day-column':p.key === 'day'}">{{ p.label }}</th></tr></thead><tbody>
      <tr><th scope="row">天干十神</th><td v-for="p in chart.pillarDetails" :key="p.key"><span class="ten-god-badge">{{ p.key === 'day' ? '日主' : p.stem.tenGod }}</span></td></tr>
      <tr class="character-row"><th scope="row">天干</th><td v-for="p in chart.pillarDetails" :key="p.key"><strong :class="tones[p.stem.element]">{{ p.stem.name }}</strong><small :class="tones[p.stem.element]">{{ p.stem.element }}</small></td></tr>
      <tr class="character-row"><th scope="row">地支</th><td v-for="p in chart.pillarDetails" :key="p.key"><strong :class="tones[p.branch.element]">{{ p.branch.name }}</strong><small :class="tones[p.branch.element]">{{ p.branch.element }}</small></td></tr>
      <tr class="hidden-row"><th scope="row">藏干</th><td v-for="p in chart.pillarDetails" :key="p.key"><div v-for="hidden in p.hiddenStems" :key="hidden.qi" class="hidden-item"><span class="qi-label">{{ hidden.qi }}</span><b :class="tones[hidden.element]">{{ hidden.name }}</b><span>{{ hidden.tenGod }}</span></div></td></tr>
      <tr class="nayin-row"><th scope="row">纳音</th><td v-for="p in chart.pillarDetails" :key="p.key">{{ p.naYin }}</td></tr>
    </tbody></table>
    <details class="chart-disclosure"><summary><b>长生与神煞</b><span>查看明细</span></summary><div class="disclosure-content">
      <table class="traits-table" aria-label="长生与神煞对照"><thead><tr><th>项目</th><th v-for="p in chart.pillarDetails" :key="p.key">{{ p.label }}</th></tr></thead><tbody><tr><th>十二长生<small>日干对四支</small></th><td v-for="p in chart.pillarDetails" :key="p.key">{{ p.terrain ?? '待重排' }}</td></tr><tr><th>自坐长生<small>本干对本支</small></th><td v-for="p in chart.pillarDetails" :key="p.key">{{ p.selfTerrain ?? '待重排' }}</td></tr><tr><th>神煞</th><td v-for="p in chart.pillarDetails" :key="p.key"><div class="shen-tags"><span v-for="h in hitsFor(p.key)" :key="h.id">{{ h.name }}</span><small v-if="!hitsFor(p.key).length">{{ chart.shenSha ? '未命中' : '待重排' }}</small></div></td></tr></tbody></table>
      <p>十二长生取自 Tyme4j，阳干顺行、阴干逆行，戊随丙、己随丁。神煞采用项目七项规则，未命中不代表完整神煞体系无其他项。</p>
      <details v-if="chart.shenSha" class="rule-help"><summary>查看七项规则与命中依据</summary><p>{{ chart.shenSha.rules.map(r => r.name).join('、') }}</p><ul class="shen-evidence"><li v-for="hit in chart.shenSha.hits" :key="`${hit.id}-${hit.pillarKey}`"><b>{{ hit.pillarLabel }} · {{ hit.name }}</b><span v-for="basis in hit.bases" :key="basis.reference">{{ basis.reference }}「{{ basis.sourceValue }}」查 {{ basis.targetValues.join('、') }} → 命中{{ hit.targetBranch }}</span></li></ul><p v-if="!chart.shenSha.hits.length">当前四柱未命中已启用的七项规则。</p><p>天乙仅按日干，甲戊庚丑未、乙己子申、丙丁亥酉、辛寅午、壬癸卯巳；禄神取日干临官。驿马、桃花、华盖、将星按年支和日支查四支，含参照柱自身，合并同柱同项依据。旬空按日柱所在旬查四支。</p><p>项目规则层 · {{ chart.shenSha.ruleVersion }} · 不单独作吉凶判断。</p></details>
      <p v-else>旧快照缺少新字段，重新排盘后补齐。</p>
    </div></details>
    <details class="chart-disclosure" @toggle="auxiliaryOpen = ($event.target as HTMLDetailsElement).open"><summary><b>胎元与两宫</b><span v-if="chart.auxiliaryPalaces" class="palace-preview">胎元 {{ chart.auxiliaryPalaces.fetalOrigin.ganZhi }} · 命宫 {{ chart.auxiliaryPalaces.ownSign.ganZhi }} · 身宫 {{ chart.auxiliaryPalaces.bodySign.ganZhi }}</span><span v-else>待重新排盘</span></summary><AuxiliaryPalaces v-if="auxiliaryOpen" :chart="chart" /></details>
    <details class="chart-disclosure"><summary><b>五行分布</b><span class="inline-elements"><span v-for="name in elements" :key="name" :class="tones[name]">{{ name }}<strong>{{ chart.elementCounts[name] }}</strong></span></span></summary><p class="disclosure-content">仅统计四干四支共 8 字，不含藏干，不按月令或旺衰加权。数量为 0 不代表完整命局缺失该五行，不能直接判断喜用神。</p></details>
    <details class="result-help"><summary>十神、藏干与纳音说明</summary><p>十神以出生日干为参照，按五行生克和阴阳区分；日干自身为比肩，盘面标记为日主。藏干按本气、中气、余气顺序展示。纳音为六十甲子传统分类，与干支自身五行分别展示。</p><dl class="hidden-reference"><div v-for="p in chart.pillarDetails" :key="p.key"><dt>{{ p.label }} · {{ p.stem.name }}{{ p.stem.element }} / {{ p.branch.name }}{{ p.branch.element }}</dt><dd v-for="h in p.hiddenStems" :key="h.qi">{{ h.qi }} {{ h.name }}{{ h.element }} · {{ h.tenGod }}</dd></div></dl></details>
  </div>
</template>
<style scoped>
.day-master-line { font-size: 11px; color: #84927e; margin: 10px 0; }
.day-master-line b { font-size: 13px; margin: 0 12px 0 4px; }
.day-master-line span { color: #98a18e; }
.wood { color: #3c7357; }.fire { color: #b4503d; }.earth { color: #8d6d39; }.metal { color: #6d7185; }.water { color: #3e7799; }
.pillar-table,.traits-table { width: 100%; border-collapse: separate; border-spacing: 0; border: 1px solid #e0e7df; border-radius: 7px; overflow: hidden; table-layout: fixed; font-size: 12px; text-align: center; }
.pillar-table th,.traits-table th { font-weight: 500; }
.pillar-table th:first-child,.traits-table th:first-child { width: 92px; }
.pillar-table thead th,.traits-table thead th { background: #f2f6ef; padding: 10px 6px; }
.pillar-table thead .day-column { background: #e6f0e0; }
.pillar-table tbody th,.traits-table tbody th { background: #fafcf8; color: #85917e; font-size: 11px; }
.pillar-table td { padding: 9px 6px; border-left: 1px solid #eaf0e6; }
.pillar-table tbody tr > *, .traits-table tbody tr > * { border-top: 1px solid #eaf0e6; }
.character-row td { padding-top: 8px; padding-bottom: 8px; }
.character-row strong { font-size: 30px; font-weight: 550; }
.character-row small { font-size: 10px; margin-left: 8px; }
.ten-god-badge { color: #647b56; font-size: 12px; }
.hidden-row td { vertical-align: top; }
.hidden-item { display: flex; justify-content: center; align-items: center; gap: 7px; min-height: 20px; font-size: 11px; }
.hidden-item b { font-size: 14px; font-weight: 500; }
.qi-label { color: #97a18e; font-size: 9px; }
.nayin-row td { color: #5e7352; font-size: 12px; }
.chart-disclosure { margin-top: 9px; border: 1px solid #e0e7db; border-radius: 6px; font-size: 12px; color: #63785a; }
.chart-disclosure > summary { cursor: pointer; padding: 12px; min-height: 43px; }
.chart-disclosure summary b { font-size: 12px; font-weight: 550; margin-right: 16px; }
.chart-disclosure > summary > span { font-size: 11px; color: #89967f; }
.disclosure-content { padding: 12px; font-size: 11px; line-height: 1.9; overflow-wrap: anywhere; }
.disclosure-content > p { color: #87977b; }
.traits-table td { padding: 12px 4px; }
.traits-table th small { display: block; margin-top: 5px; font-size: 9px; }
.shen-tags { display: flex; justify-content: center; flex-wrap: wrap; gap: 4px; }
.shen-tags span { padding: 2px 5px; border-radius: 4px; background: #f5f0e4; color: #8d6d39; font-size: 10px; }
.shen-tags small { font-size: 10px; color: #9aa58e; }
.shen-evidence { padding-left: 18px; }
.shen-evidence li { margin-bottom: 8px; }
.shen-evidence span { display: block; }
.inline-elements { display: inline-flex; gap: 14px; }
.inline-elements strong { margin-left: 4px; font-weight: 500; }
.result-help { margin-top: 10px; font-size: 10px; color: #92a086; line-height: 1.9; }
.hidden-reference { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 12px; padding: 12px; background: #f7f9f3; }
.hidden-reference dt { color: #607954; margin-bottom: 5px; }
.hidden-reference dd { margin: 0; }
.result-help summary,.rule-help summary { cursor: pointer; padding: 5px 0; }
summary:focus-visible { outline: 2px solid #95b8a7; outline-offset: 2px; }
@media(max-width:767px) {
  .pillar-table { font-size: 11px; }.pillar-table th:first-child,.traits-table th:first-child { width: 48px; }
  .pillar-table thead th { padding: 10px 2px; }.pillar-table td { padding: 10px 2px; }.pillar-table tbody th { font-size: 9px; }
  .character-row strong { font-size: 27px; }.character-row small,.qi-label { display: none; }
  .hidden-item { gap: 3px; font-size: 9px; min-height: 20px; }.hidden-item b { font-size: 12px; }
  .nayin-row td { font-size: 10px; }.chart-disclosure > summary { padding: 12px 9px; }
  .chart-disclosure summary b { margin-right: 8px; font-size: 11px; }.chart-disclosure > summary > span { font-size: 10px; }
  .palace-preview { display: block; margin: 7px 0 0 14px; }.inline-elements { gap: 8px; }
  .traits-table { font-size: 10px; }.traits-table tbody th { font-size: 9px; }.disclosure-content { padding: 9px; }
}
</style>
