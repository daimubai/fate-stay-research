<script setup lang="ts">
import { ref } from 'vue'
import type { ChartLibrary } from '../lib/local-charts'

defineProps<{ library: ChartLibrary; notice: string; status: string; pending: boolean; hasChart: boolean }>()
const emit = defineEmits<{ select: [id: string]; remove: [id: string]; rename: [id: string, name: string]; replace: [id: string]; new: []; save: []; dismiss: [] }>()
const editingId = ref<string | null>(null)
const deleteId = ref<string | null>(null)
const name = ref('')
function rename(id: string) { if (name.value.trim()) { emit('rename', id, name.value); editingId.value = null } }
</script>

<template>
  <section class="panel local-library" aria-labelledby="local-library-title">
    <div class="library-heading"><div><p>未登录 · 本地模式</p><h2 id="local-library-title">我的本机排盘 <span>{{ library.records.length }} / 3</span></h2></div><button type="button" class="new-local-chart" @click="emit('new')">＋ 新增排盘</button></div>
    <p class="library-description">出生信息和排盘结果仅保存在当前浏览器，不会保存到账号。最多 3 份，清除网站数据后会丢失。</p>
    <div v-if="library.records.length" class="local-records" aria-label="已保存的排盘">
      <article v-for="record in library.records" :key="record.id" class="local-record" :class="{active: library.activeId === record.id}">
        <button type="button" class="record-select" :aria-label="`切换至${record.name}`" :aria-pressed="library.activeId === record.id" @click="emit('select', record.id)"><span class="record-name">{{ record.name }}<small v-if="library.activeId === record.id">当前</small></span><span class="record-date">{{ record.birth.solarDate }} · {{ record.birth.time }}</span><span class="record-pillars">{{ record.chart.pillars.year }}　{{ record.chart.pillars.month }}　{{ record.chart.pillars.day }}　{{ record.chart.pillars.hour }}</span><span class="record-meta">{{ record.birth.calendarType === 'LUNAR' ? '农历输入' : '公历输入' }} · 日主{{ record.chart.dayMaster.name }}{{ record.chart.dayMaster.element }}</span></button>
        <form v-if="editingId === record.id" class="record-rename" @submit.prevent="rename(record.id)"><label class="sr-only" :for="`name-${record.id}`">记录名称</label><input :id="`name-${record.id}`" v-model="name" maxlength="20" aria-label="记录名称" required /><button type="submit">保存名称</button><button type="button" @click="editingId = null">取消</button></form>
        <div v-else-if="deleteId === record.id" class="record-delete-confirm"><span>删除此本地记录？</span><button type="button" @click="emit('remove', record.id); deleteId = null">确认删除</button><button type="button" @click="deleteId = null">取消</button></div>
        <div v-else class="record-actions"><button type="button" :aria-label="`重命名${record.name}`" @click="editingId = record.id; name = record.name; deleteId = null">重命名</button><button type="button" :aria-label="`删除${record.name}`" @click="deleteId = record.id; editingId = null">删除</button></div>
      </article>
    </div>
    <div v-else class="library-empty">还没有本地记录。完成第一次排盘后会自动保存，之后可在这里切换。</div>
    <div v-if="pending" class="library-replace" role="region" aria-label="选择替换记录"><strong>3 个位置已满，本次结果尚未保存</strong><p>选择要替换的记录；不替换也可以继续查看本次结果。</p><div><button v-for="record in library.records" :key="record.id" type="button" @click="emit('replace', record.id)">替换「{{ record.name }}」</button><button type="button" class="keep-unsaved" @click="emit('dismiss')">暂不保存</button></div></div>
    <div class="library-footer"><span role="status" aria-live="polite">{{ status || (library.records.length ? '选择记录即可恢复出生日期和完整排盘，无需重新计算。' : '同一出生时刻重复排盘会更新记录，不占用新位置。') }}</span><button v-if="hasChart && !library.activeId && !pending" type="button" @click="emit('save')">保存当前排盘</button></div>
    <p v-if="notice" role="alert" class="library-notice">{{ notice }}</p>
  </section>
</template>

<style scoped>
.local-library { margin-bottom: 24px; padding: 25px 28px 18px; }
.library-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.library-heading p { margin: 0 0 8px; color: #7d8b78; font-size: 11px; }
.library-heading h2 { font-size: 16px; }
.library-heading h2 span { color: #8c9a84; font-size: 12px; font-weight: 400; margin-left: 10px; }
.new-local-chart { color: #466a42; background: #eff4ea; border: 1px solid #dce6d4; min-height: 38px; white-space: nowrap; }
.library-description { font-size: 12px; color: #84917c; line-height: 1.8; margin: 18px 0; }
.local-records { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.local-record { border: 1px solid #e0e7da; border-radius: 10px; overflow: hidden; background: #fbfcf9; }
.local-record.active { border-color: #77946d; box-shadow: 0 0 0 1px #77946d; background: #f3f7ee; }
.record-select { display: flex; flex-direction: column; width: 100%; text-align: left; background: transparent; color: #344b31; padding: 16px 14px 12px; border-radius: 0; gap: 9px; }
.record-select:hover { background: #f0f5e9; }
.record-name { display: flex; width: 100%; align-items: center; gap: 8px; font-size: 14px; font-weight: 550; overflow-wrap: anywhere; }
.record-name small { font-size: 10px; margin-left: auto; color: #5b8050; background: #e6eedf; padding: 3px 6px; border-radius: 4px; flex-shrink: 0; }
.record-date { font-size: 12px; color: #73816b; }
.record-pillars { font-size: 13px; line-height: 1.6; }
.record-meta { color: #8b9683; font-size: 11px; }
.record-actions, .record-rename, .record-delete-confirm { border-top: 1px solid #e6ecdf; display: flex; flex-wrap: wrap; align-items: center; gap: 10px; padding: 8px 14px; }
.record-actions button, .record-rename button, .record-delete-confirm button { background: transparent; color: #75856b; padding: 5px 0; font-size: 11px; }
.record-actions button:last-child { margin-left: auto; }
.record-rename input { width: 100%; min-width: 0; border: 1px solid #cddbc2; border-radius: 6px; padding: 8px; font: inherit; font-size: 13px; color: #405738; }
.record-delete-confirm span { font-size: 11px; color: #8b6447; width: 100%; }
.record-delete-confirm button:first-of-type { color: #a0583c; }
.library-empty { border: 1px dashed #dce6d3; border-radius: 9px; background: #f9fbf6; padding: 20px; font-size: 13px; color: #8b9881; line-height: 1.8; }
.library-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 16px; }
.library-footer > span { font-size: 11px; color: #819074; line-height: 1.8; }
.library-footer button { background: #eff4e9; color: #476a3c; padding: 8px 12px; font-size: 11px; flex-shrink: 0; }
.library-notice { font-size: 12px; line-height: 1.8; color: #986c36; background: #faf5e9; padding: 10px 12px; border-radius: 7px; margin: 12px 0 0; }
.library-replace { background: #faf7ef; border: 1px solid #ebe0c8; border-radius: 9px; padding: 16px; margin-top: 16px; }
.library-replace strong { font-size: 13px; color: #886838; }
.library-replace p { font-size: 12px; color: #96866b; line-height: 1.7; margin: 9px 0 13px; }
.library-replace > div { display: flex; flex-wrap: wrap; gap: 8px; }
.library-replace button { font-size: 11px; background: #ece3d1; color: #80643d; padding: 8px 10px; overflow-wrap: anywhere; }
.library-replace .keep-unsaved { background: transparent; color: #8c846f; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0,0,0,0); }
@media (max-width: 767px) { .local-library { padding: 22px 20px 16px; } .local-records { grid-template-columns: 1fr; } .record-select { gap: 8px; } .record-name { font-size: 14px; } .record-actions { padding-top: 4px; padding-bottom: 4px; } .library-footer { align-items: flex-start; } }
</style>
