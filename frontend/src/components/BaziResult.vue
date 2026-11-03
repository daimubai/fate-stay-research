<script setup lang="ts">
import { computed } from 'vue'
import type { BaziChart, ElementName } from '../api/bazi'

const props = defineProps<{ chart: BaziChart }>()
const hitsFor = (key: string) => props.chart.shenSha?.hits.filter(hit => hit.pillarKey === key) ?? []
const elements: ElementName[] = ['木', '火', '土', '金', '水']
const tones: Record<ElementName, string> = { 木: 'wood', 火: 'fire', 土: 'earth', 金: 'metal', 水: 'water' }
const counts = computed(() => elements.map(name => ({ name, count: props.chart.elementCounts[name] })))
</script>

<template>
  <div class="bazi-details">
    <div class="details-heading"><div><p class="details-eyebrow">四柱详解</p><h3>十神 · 长生 · 神煞</h3></div><div class="day-master"><span>日主</span><strong :class="['element-text', tones[chart.dayMaster.element]]">{{ chart.dayMaster.name }}<small>{{ chart.dayMaster.element }}</small></strong></div></div>
    <p class="details-guide">十神均以日干「{{ chart.dayMaster.name }}」为参照；日干自身的关系为比肩，标记为日主。</p>

    <table class="pillar-table" aria-label="四柱详细对照">
      <thead><tr><th scope="col">项目</th><th v-for="pillar in chart.pillarDetails" :key="pillar.key" scope="col" :class="{'day-column': pillar.key === 'day'}">{{ pillar.label }}<small v-if="pillar.key === 'day'">日主所在</small></th></tr></thead>
      <tbody>
        <tr class="ten-god-row"><th scope="row">天干十神</th><td v-for="pillar in chart.pillarDetails" :key="pillar.key"><span class="ten-god-badge">{{ pillar.stem.tenGod }}</span></td></tr>
        <tr class="character-row"><th scope="row">天干</th><td v-for="pillar in chart.pillarDetails" :key="pillar.key"><strong :class="['element-text', tones[pillar.stem.element]]">{{ pillar.stem.name }}</strong><small :class="['element-label', tones[pillar.stem.element]]">{{ pillar.stem.element }}</small></td></tr>
        <tr class="character-row"><th scope="row">地支</th><td v-for="pillar in chart.pillarDetails" :key="pillar.key"><strong :class="['element-text', tones[pillar.branch.element]]">{{ pillar.branch.name }}</strong><small :class="['element-label', tones[pillar.branch.element]]">{{ pillar.branch.element }}</small></td></tr>
        <tr class="hidden-row"><th scope="row">地支藏干<small>五行 / 十神</small></th><td v-for="pillar in chart.pillarDetails" :key="pillar.key"><div v-for="hidden in pillar.hiddenStems" :key="hidden.qi" class="hidden-item"><span class="qi-label">{{ hidden.qi }}</span><strong :class="['element-text', tones[hidden.element]]">{{ hidden.name }}<small>{{ hidden.element }}</small></strong><span>{{ hidden.tenGod }}</span></div></td></tr>
        <tr><th scope="row">十二长生<small>日干对四支</small></th><td v-for="pillar in chart.pillarDetails" :key="pillar.key">{{ pillar.terrain ?? '待重新排盘' }}</td></tr>
        <tr><th scope="row">自坐长生<small>本干对本支</small></th><td v-for="pillar in chart.pillarDetails" :key="pillar.key">{{ pillar.selfTerrain ?? '待重新排盘' }}</td></tr>
        <tr><th scope="row">神煞</th><td v-for="pillar in chart.pillarDetails" :key="pillar.key"><div class="shen-tags"><span v-for="hit in hitsFor(pillar.key)" :key="hit.id">{{ hit.name }}</span><small v-if="!hitsFor(pillar.key).length">{{ chart.shenSha ? '未命中已启用规则' : '待重新排盘' }}</small></div></td></tr>
        <tr class="nayin-row"><th scope="row">纳音</th><td v-for="pillar in chart.pillarDetails" :key="pillar.key">{{ pillar.naYin }}</td></tr>
      </tbody>
    </table>

    <div class="mobile-pillar-cards">
      <article v-for="pillar in chart.pillarDetails" :key="pillar.key" class="detail-card" :aria-label="`${pillar.label}详解`">
        <div class="detail-card-heading"><h4>{{ pillar.label }}</h4><span v-if="pillar.key === 'day'" class="day-badge">日主</span><span class="ten-god-badge">{{ pillar.stem.tenGod }}</span></div>
        <div class="card-characters"><div><span>天干</span><strong :class="['element-text', tones[pillar.stem.element]]">{{ pillar.stem.name }}</strong><small>{{ pillar.stem.element }}</small></div><div><span>地支</span><strong :class="['element-text', tones[pillar.branch.element]]">{{ pillar.branch.name }}</strong><small>{{ pillar.branch.element }}</small></div><p><span>纳音</span><b>{{ pillar.naYin }}</b></p></div>
        <div class="card-traits"><p>十二长生 <strong>{{ pillar.terrain ?? '待重新排盘' }}</strong><small>日干对本支</small></p><p>自坐长生 <strong>{{ pillar.selfTerrain ?? '待重新排盘' }}</strong><small>本干对本支</small></p><div class="shen-tags"><span v-for="hit in hitsFor(pillar.key)" :key="hit.id">{{ hit.name }}</span><small v-if="!hitsFor(pillar.key).length">{{ chart.shenSha ? '未命中已启用规则' : '待重新排盘' }}</small></div></div>
        <div class="card-hidden"><p>藏干 <small>本气 → 中气 → 余气</small></p><div v-for="hidden in pillar.hiddenStems" :key="hidden.qi" class="hidden-item"><span class="qi-label">{{ hidden.qi }}</span><strong :class="['element-text', tones[hidden.element]]">{{ hidden.name }}<small>{{ hidden.element }}</small></strong><span>{{ hidden.tenGod }}</span></div></div>
      </article>
    </div>

    <section class="shen-section" aria-label="长生与神煞规则">
      <h4>长生与神煞 · 计算依据</h4>
      <p>十二长生直接采用 Tyme4j：日干对四支与各柱自坐分开展示，阳干顺行、阴干逆行，戊随丙、己随丁。</p>
      <template v-if="chart.shenSha">
        <p>已启用 {{ chart.shenSha.rules.length }} 项神煞：{{ chart.shenSha.rules.map(rule => rule.name).join('、') }}。这是基于 Tyme4j 四柱的项目规则层，不使用黄历吉凶神；不代表完整神煞体系。</p>
        <details><summary>查看命中依据与规则口径（{{ chart.shenSha.hits.length }} 项）</summary>
          <ul class="shen-evidence"><li v-for="hit in chart.shenSha.hits" :key="`${hit.id}-${hit.pillarKey}`"><b>{{ hit.pillarLabel }} · {{ hit.name }}</b><span v-for="basis in hit.bases" :key="basis.reference">{{ basis.reference }}「{{ basis.sourceValue }}」查 {{ basis.targetValues.join('、') }} → 命中{{ hit.targetBranch }}</span></li></ul>
          <p v-if="!chart.shenSha.hits.length">当前四柱未命中这七项规则。</p>
          <p>天乙贵人仅按日干，采用甲戊庚丑未、乙己子申、丙丁亥酉、辛寅午、壬癸卯巳；禄神取日干临官。驿马、桃花、华盖、将星分别按年支和日支查四支，含参照柱自身，同柱同项合并依据。旬空仅按日柱所在旬查四支。</p>
          <p>规则版本：{{ chart.shenSha.ruleVersion }}。神煞为传统分类信息，不单独作吉凶判断。</p>
        </details>
      </template>
      <p v-else role="status">这份历史快照尚无长生与神煞数据，点击重新排盘即可补齐，仍使用原缓存位置。</p>
    </section>
    <section class="element-summary" aria-labelledby="element-summary-title"><div class="element-summary-heading"><h4 id="element-summary-title">八字表层五行</h4><span>共 8 字</span></div><div class="element-counts"><div v-for="item in counts" :key="item.name" :class="['element-count', tones[item.name]]"><span>{{ item.name }}</span><strong>{{ item.count }}<small>个</small></strong><div class="element-track" aria-hidden="true"><i :style="{ width: `${item.count / 8 * 100}%` }"></i></div></div></div><p>四个天干、四个地支各计 1 次，不含藏干，也未按月令或旺衰加权。数量为 0 不代表完整命局缺失该五行，不能直接据此判断喜用神。</p></section>
    <details class="result-help"><summary>这些信息怎么看？</summary><dl><dt>十神</dt><dd>以日主天干为参照，按五行生克及阴阳关系，描述其他天干与日主的关系。藏干也采用同一参照。</dd><dt>藏干</dt><dd>地支所含的天干，按 Tyme4j 的本气、中气、余气顺序展示；没有的项不填补、不计权重。</dd><dt>五行</dt><dd>分别标注天干与地支的木、火、土、金、水属性。表层计数与藏干属性分开展示。</dd><dt>纳音</dt><dd>六十甲子的传统分类名称，与天干、地支自身的五行属性不同。</dd></dl></details>
  </div>
</template>

<style scoped>
.shen-section { margin-top: 20px; padding: 18px; border: 1px solid #dfe7dc; border-radius: 12px; font-size: 12px; line-height: 1.9; color: #64765a; overflow-wrap: anywhere; }
.shen-section h4 { margin: 0; font-size: 14px; }
.shen-section summary { cursor: pointer; padding: 8px 0; }
.shen-section summary:focus-visible { outline: 2px solid #95b8a7; }
.shen-evidence { padding-left: 18px; }
.shen-evidence li { margin-bottom: 10px; }
.shen-evidence span { display: block; }
.shen-tags { display: flex; justify-content: center; flex-wrap: wrap; gap: 5px; }
.shen-tags > span { padding: 3px 7px; border-radius: 5px; background: #f2efe6; color: #8d6d39; font-size: 12px; }
.shen-tags > small { color: #899381; font-size: 10px; }
.pillar-table th small { display: block; font-size: 10px; margin-top: 5px; }
.card-traits { padding: 12px 16px; border-top: 1px solid #edf1e8; font-size: 12px; }
.card-traits p { display: flex; gap: 12px; margin: 0 0 10px; color: #72836b; }
.card-traits small { margin-left: auto; color: #97a18f; font-size: 10px; }
.card-traits .shen-tags { justify-content: flex-start; }
.details-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin: 25px 0 12px; }
.details-eyebrow { margin: 0 0 8px; font-size: 11px; color: #829283; letter-spacing: 2px; }
.details-heading h3 { font-size: 18px; margin: 0; font-weight: 600; }
.day-master { display: flex; align-items: center; gap: 12px; border: 1px solid #dce7dd; background: #f3f7f1; border-radius: 10px; padding: 10px 15px; flex-shrink: 0; }
.day-master > span { color: #738672; font-size: 12px; }
.day-master strong { font-size: 26px; }
.day-master small { font-size: 12px; margin-left: 8px; font-weight: 400; }
.details-guide { margin: 0 0 21px; font-size: 12px; color: #7a887c; line-height: 1.8; }
.element-text.wood, .element-label.wood { color: #3c7357; }
.element-text.fire, .element-label.fire { color: #b4503d; }
.element-text.earth, .element-label.earth { color: #8d6d39; }
.element-text.metal, .element-label.metal { color: #6d7185; }
.element-text.water, .element-label.water { color: #3e7799; }
.pillar-table { width: 100%; border-spacing: 0; border-collapse: separate; border: 1px solid #e0e7df; border-radius: 12px; overflow: hidden; table-layout: fixed; font-size: 13px; }
.pillar-table th { font-weight: 500; }
.pillar-table thead th { padding: 18px 8px; background: #f2f6ef; border-bottom: 1px solid #e0e7df; }
.pillar-table thead th:first-child { width: 110px; }
.pillar-table thead small { display: block; font-size: 10px; color: #55794f; margin-top: 5px; }
.pillar-table thead .day-column { background: #e9f1e4; }
.pillar-table tbody th { padding: 16px 12px; text-align: left; color: #7b897b; background: #fbfcf9; }
.pillar-table td { padding: 16px 8px; text-align: center; border-left: 1px solid #edf0e9; }
.pillar-table tbody tr + tr > * { border-top: 1px solid #edf0e9; }
.ten-god-badge { display: inline-block; background: #f1f5ee; color: #60745a; border-radius: 6px; padding: 4px 9px; font-size: 12px; line-height: 1.4; }
.character-row strong { font-size: 32px; font-weight: 550; }
.element-label { font-size: 11px; margin-left: 8px; }
.hidden-row th small { display: block; margin-top: 8px; font-size: 10px; font-weight: 400; }
.hidden-row td { vertical-align: top; padding-top: 18px; padding-bottom: 18px; }
.hidden-item { display: flex; align-items: center; justify-content: center; gap: 7px; font-size: 12px; line-height: 1.8; min-height: 28px; }
.hidden-item strong { font-size: 15px; font-weight: 550; white-space: nowrap; }
.hidden-item strong small { font-size: 10px; font-weight: 400; margin-left: 3px; }
.qi-label { color: #96a08f; font-size: 10px; }
.nayin-row td { font-size: 13px; color: #5a6e56; }
.mobile-pillar-cards { display: none; }
.element-summary { margin-top: 24px; border: 1px solid #e3e9de; border-radius: 12px; padding: 20px; }
.element-summary-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px; }
.element-summary-heading h4 { margin: 0; font-size: 14px; font-weight: 550; }
.element-summary-heading > span { font-size: 11px; color: #8b9786; }
.element-counts { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; }
.element-count { padding: 12px; background: #f7f9f4; border-radius: 8px; color: #627859; }
.element-count.fire { color: #b4503d; background: #fcf5f1; }
.element-count.earth { color: #8d6d39; background: #faf7f0; }
.element-count.metal { color: #6d7185; background: #f5f5f8; }
.element-count.water { color: #3e7799; background: #f1f6f9; }
.element-count > span { font-size: 13px; }
.element-count > strong { display: block; font-size: 24px; font-weight: 500; margin: 8px 0 12px; }
.element-count > strong small { font-size: 10px; font-weight: 400; margin-left: 4px; }
.element-track { height: 3px; background: #1727150d; border-radius: 3px; overflow: hidden; }
.element-track i { display: block; height: 100%; background: currentColor; }
.element-summary > p { margin: 16px 0 0; color: #899381; font-size: 11px; line-height: 1.9; }
.result-help { margin-top: 18px; font-size: 12px; line-height: 1.9; color: #71826a; }
.result-help summary { cursor: pointer; padding: 8px 0; }
.result-help summary:focus-visible { outline: 2px solid #95b8a7; outline-offset: 3px; }
.result-help dl { margin: 8px 0 0; padding: 16px 18px; background: #f7f9f4; border-radius: 8px; }
.result-help dt { font-weight: 600; }
.result-help dd { margin: 3px 0 12px; }
.result-help dd:last-child { margin-bottom: 0; }
@media (max-width: 767px) {
  .pillar-table { display: none; }
  .details-heading h3 { font-size: 15px; line-height: 1.5; }
  .day-master { padding: 9px 11px; gap: 8px; }
  .day-master strong { font-size: 23px; }
  .mobile-pillar-cards { display: grid; gap: 14px; }
  .detail-card { border: 1px solid #dfe7dc; border-radius: 12px; overflow: hidden; }
  .detail-card-heading { display: flex; align-items: center; padding: 14px 16px; background: #f5f8f2; gap: 10px; }
  .detail-card-heading h4 { margin: 0; font-size: 14px; font-weight: 550; }
  .detail-card-heading .ten-god-badge { margin-left: auto; }
  .day-badge { font-size: 10px; color: #55754b; border: 1px solid #cadbc3; border-radius: 4px; padding: 2px 6px; }
  .card-characters { display: flex; padding: 18px 16px; gap: 22px; align-items: center; }
  .card-characters > div { display: grid; justify-items: center; gap: 6px; }
  .card-characters span { font-size: 10px; color: #899580; }
  .card-characters strong { font-size: 30px; font-weight: 550; }
  .card-characters small { font-size: 11px; color: #7a8972; }
  .card-characters p { margin: 0 0 0 auto; display: grid; gap: 9px; text-align: right; }
  .card-characters b { font-size: 14px; font-weight: 500; color: #64765a; }
  .card-hidden { border-top: 1px solid #edf1e8; padding: 12px 16px 15px; }
  .card-hidden > p { font-size: 12px; margin: 0 0 10px; color: #72836b; }
  .card-hidden > p small { display: block; font-size: 10px; color: #97a18f; margin-top: 5px; }
  .card-hidden .hidden-item { justify-content: flex-start; gap: 16px; }
  .element-summary { padding: 16px; }
  .element-counts { gap: 5px; }
  .element-count { padding: 9px 5px; text-align: center; }
  .element-count > strong { font-size: 22px; }
  .element-count > strong small { display: none; }
}
</style>
