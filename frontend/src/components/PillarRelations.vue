<script setup lang="ts">
import { computed, ref } from 'vue'
import type { BaziChart, PillarRelationHit, RelationKind } from '../api/bazi'
const props = defineProps<{ chart: BaziChart }>()
const emit = defineEmits<{ highlight: [keys: string[], part: 'stem' | 'branch' | null] }>()
const opened = ref(false), selectedId = ref('')
const filter = ref<RelationKind | 'ALL'>('ALL')
const groups = computed(() => props.chart.relations?.rules.map(rule => ({ ...rule, count: props.chart.relations!.hits.filter(h => h.kind === rule.kind).length })) ?? [])
const visible = computed(() => props.chart.relations?.hits.filter(h => filter.value === 'ALL' || h.kind === filter.value) ?? [])
const summary = computed(() => groups.value.filter(g => g.count).map(g => `${g.label.replace('天干', '').replace('地支', '')} ${g.count}`).join(' · ') || '未命中已启用关系')
const label = (hit: PillarRelationHit) => groups.value.find(g => g.kind === hit.kind)?.label ?? ''
function clear() { selectedId.value = ''; emit('highlight', [], null) }
function changeFilter(kind: RelationKind | 'ALL') { filter.value = kind; clear() }
function select(hit: PillarRelationHit) {
  if (selectedId.value === hit.id) { clear(); return }
  selectedId.value = hit.id
  emit('highlight', hit.participants.map(p => p.pillarKey), hit.kind === 'STEM_COMBINATION' ? 'stem' : 'branch')
}
function toggle(event: Event) { opened.value = (event.target as HTMLDetailsElement).open; if (!opened.value) clear() }
function formLabel(hit: PillarRelationHit) {
  if (hit.form === 'SELF_PAIR') return '自刑'
  if (hit.form === 'TRIPLE') return hit.kind === 'BRANCH_PUNISHMENT' ? '三刑齐见' : '三支齐见'
  return hit.kind === 'BRANCH_PUNISHMENT' && hit.pattern.length === 3 ? '两支相刑' : '两柱关系'
}
</script>
<template>
  <details class="relations-disclosure" @toggle="toggle">
    <summary><b>合冲刑破害</b><span>{{ chart.relations ? summary : '待重新排盘' }}</span></summary>
    <div v-if="opened" class="relations-content">
      <template v-if="chart.relations">
        <div class="relation-toolbar"><p>仅看出生四柱，点击关系可标记上方对应干支。</p><a v-if="selectedId" href="#natal-pillars">查看已标记柱位 ↑</a></div>
        <div class="relation-filters" role="group" aria-label="筛选四柱关系"><button type="button" :aria-pressed="filter === 'ALL'" @click="changeFilter('ALL')">全部 {{ chart.relations.hits.length }}</button><button v-for="g in groups" :key="g.kind" type="button" :aria-pressed="filter === g.kind" @click="changeFilter(g.kind)">{{ g.label.replace('天干','').replace('地支','') }} {{ g.count }}</button></div>
        <p v-if="!visible.length" class="relation-empty" role="status">{{ chart.relations.hits.length ? '当前四柱未命中此类关系。' : '当前四柱未命中已启用的五合、六合、三合、刑冲破害规则。' }}</p>
        <div v-else class="relation-list" aria-label="四柱关系明细"><button v-for="hit in visible" :key="hit.id" type="button" class="relation-row" :aria-label="`${label(hit)} ${hit.pattern} ${hit.participants.map(p => p.pillarLabel + p.value).join(hit.directed ? '刑' : '与')}`" :aria-pressed="selectedId === hit.id" @click="select(hit)"><div class="relation-type"><strong>{{ label(hit) }}</strong><small>{{ formLabel(hit) }}</small></div><div class="relation-evidence"><div class="relation-participants"><template v-for="(p,index) in hit.participants" :key="p.pillarKey"><span v-if="index" class="relation-connector">{{ hit.directed ? '→' : '＋' }}</span><span class="participant"><small>{{ p.pillarLabel }}</small><b>{{ p.value }}</b></span></template></div><p>{{ hit.basis }}</p></div><div class="relation-correspondence"><span v-if="hit.associatedElement">配属五行 <b>{{ hit.associatedElement }}</b></span><span v-else>{{ hit.pattern }}{{ hit.pattern.length === 3 && hit.form === 'PAIR' ? '组' : '' }}</span><small>{{ selectedId === hit.id ? '已标记 · 再点取消' : '点击标记柱位' }}</small></div></button></div>
        <details class="relations-help"><summary>计算范围与判定口径</summary><p>四柱天干、地支分别比较，含不相邻柱位；不包含藏干、胎元两宫或运程。同一柱位组合可同时出现多种关系，不互相抵消，也不据此判定吉凶。</p><p>五合、六合、冲、害使用 Tyme4j 原生关系；三合、刑、破为项目固定规则。三合只认三个不同地支齐见，不计半合或拱合。</p><p>刑：子卯互刑；寅→巳→申→寅、丑→戌→未→丑。仅两支时标出刑向，三支齐见时合并为三柱项，不再重复列该组两支项。辰午酉亥自刑须在不同柱位重复出现。</p><p>配属五行只描述传统对应，不代表已满足合化条件；四柱属性与 8 字五行统计不因此改变。</p><p>六破：子酉、卯午、辰丑、未戌、寅亥、巳申。重复字保留各自柱位；每个不同柱位组合只列一次。</p><p>规则版本 {{ chart.relations.ruleVersion }} · 出生四柱范围。</p></details>
      </template>
      <p v-else class="relation-legacy" role="status">这份历史快照尚无四柱关系。重新排盘后补齐，并更新原本机记录。</p>
    </div>
  </details>
</template>
<style scoped>
.relations-disclosure { margin-top: 9px; border: 1px solid #e0e7db; border-radius: 6px; color: #63785a; font-size: 12px; }
.relations-disclosure > summary { cursor: pointer; padding: 12px; min-height: 43px; }
.relations-disclosure > summary b { font-size: 12px; font-weight: 550; margin-right: 16px; }
.relations-disclosure > summary > span { color: #89967f; font-size: 11px; }
.relations-content { padding: 0 12px 14px; }
.relation-toolbar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.relation-toolbar p { font-size: 11px; color: #87977b; margin: 0; line-height: 1.9; }
.relation-toolbar a { font-size: 11px; color: #537a46; }
.relation-filters { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px; }
.relation-filters button { padding: 8px 10px; font-size: 11px; background: #f7f9f3; color: #7a8b6b; border: 1px solid #e0e7d9; min-height: 34px; border-radius: 5px; }
.relation-filters button:hover { background: #edf3e5; }
.relation-filters button[aria-pressed="true"] { background: #e8f0df; border-color: #9bb288; color: #456c35; }
.relation-list { display: grid; gap: 7px; }
.relation-row { display: grid; grid-template-columns: 110px minmax(0,1fr) 130px; align-items: center; gap: 14px; text-align: left; padding: 14px; background: #fbfcf9; color: #596e4e; border: 1px solid #e4eadd; border-radius: 7px; }
.relation-row:hover { background: #f4f8ef; }
.relation-row[aria-pressed="true"] { background: #eef5e7; border-color: #8ea97a; }
.relation-type strong { font-size: 12px; font-weight: 550; }
.relation-type > small, .relation-correspondence > small { display: block; font-size: 10px; margin-top: 6px; color: #94a186; }
.relation-participants { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.participant { display: flex; align-items: center; gap: 6px; }
.participant small { color: #87977b; font-size: 10px; }
.participant b { font-size: 20px; font-weight: 550; }
.relation-connector { color: #a1ac97; font-size: 11px; }
.relation-evidence p { margin: 7px 0 0; font-size: 10px; color: #8b997f; line-height: 1.8; }
.relation-correspondence { font-size: 11px; }
.relation-correspondence b { font-size: 14px; font-weight: 550; margin-left: 6px; }
.relations-help, .relation-empty, .relation-legacy { font-size: 11px; line-height: 1.9; color: #87977a; }
.relations-help { margin-top: 12px; }
.relations-help summary { cursor: pointer; padding: 8px 0; }
summary:focus-visible { outline: 2px solid #95b8a7; outline-offset: 2px; }
@media(max-width:767px) {
 .relations-disclosure > summary { padding: 12px 9px; }.relations-disclosure > summary b { font-size: 11px; margin-right: 8px; }.relations-disclosure > summary > span { font-size: 10px; }
 .relations-content { padding: 0 9px 12px; }.relation-filters button { min-height: 40px; padding: 8px 9px; font-size: 10px; }
 .relation-row { grid-template-columns: minmax(0,1fr); gap: 10px; padding: 12px; }.relation-type { display: flex; align-items: center; justify-content: space-between; gap: 8px; }.relation-type > small { margin: 0; }
 .relation-correspondence { display: flex; align-items: center; justify-content: space-between; gap: 8px; }.relation-correspondence > small { margin: 0; }
}
</style>
