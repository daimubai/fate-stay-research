<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import axios from 'axios'
import { calculateBazi, type BaziChart } from './api/bazi'
import { getHealth, type HealthResponse } from './api/health'
import BirthPicker from './components/birth/BirthPicker.vue'
import BaziResult from './components/BaziResult.vue'
import FortuneResult from './components/FortuneResult.vue'
import LocalChartLibrary from './components/LocalChartLibrary.vue'
import { deleteChart, readLibrary, renameChart, saveChart, writeLibrary, type ChartLibrary, type LocalStoragePort } from './lib/local-charts'
import { toBirthInput, type BirthSelection } from './lib/birth-calendar'

let storage: LocalStoragePort | undefined
try { storage = window.localStorage } catch { /* Browsers may deny storage access. */ }
const initial = readLibrary(storage)
const library = ref<ChartLibrary>(initial.library)
const storageNotice = ref(initial.notice)
const libraryStatus = ref('')
const restored = initial.library.records.find(record => record.id === initial.library.activeId)
const birth = ref<BirthSelection>(restored ? { ...restored.birth } : { solarDate: '', time: '', calendarType: 'SOLAR', gender: '' })
const calculating = ref(false)
const chart = ref<BaziChart | null>(restored?.chart ?? null)
const chartError = ref('')
const editorOpen = ref(!restored || window.matchMedia('(min-width: 1024px)').matches)
function editBirth() { editorOpen.value = true; nextTick(() => document.getElementById('birth-editor')?.scrollIntoView({ behavior: 'smooth', block: 'start' })) }
const pending = ref<{ birth: BirthSelection; chart: BaziChart } | null>(null)
const currentRecord = computed(() => library.value.records.find(record => record.id === library.value.activeId))
if (restored) libraryStatus.value = `已从此浏览器恢复「${restored.name}」`

function persist(next: ChartLibrary) {
  library.value = next
  storageNotice.value = writeLibrary(storage, next)
}
function saveCurrentChart() {
  if (!chart.value) return
  try {
    const next = saveChart(library.value, birth.value, chart.value)
    if (next) {
      pending.value = null
      persist(next)
      libraryStatus.value = '当前出生日期和排盘结果已保留在本机记录中。'
    } else {
      pending.value = { birth: { ...birth.value }, chart: chart.value }
      libraryStatus.value = '本次结果未保存，请选择替换记录或暂不保存。'
    }
  } catch {
    storageNotice.value = '本次结果未能保存，仍可继续查看或重新排盘。'
  }
}
function selectRecord(id: string, scroll = true) {
  const record = library.value.records.find(item => item.id === id)
  if (!record) return
  birth.value = { ...record.birth }
  chart.value = record.chart
  chartError.value = ''
  pending.value = null
  persist({ ...library.value, activeId: id })
  libraryStatus.value = `正在查看「${record.name}」，无需重新计算。`
  if (window.matchMedia('(max-width: 767px)').matches) editorOpen.value = false
  if (scroll) nextTick(() => document.getElementById('chart-result')?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
}
function newRecord() {
  editorOpen.value = true
  birth.value = { solarDate: '', time: '', calendarType: 'SOLAR', gender: '' }
  chart.value = null
  pending.value = null
  persist({ ...library.value, activeId: null })
  libraryStatus.value = library.value.records.length === 3 ? '可继续排盘；保存新结果时需选择替换一个位置。' : '输入新的出生日期与时间，成功排盘后会自动保存。'
}
function removeRecord(id: string) {
  persist(deleteChart(library.value, id))
  libraryStatus.value = '已删除此本地记录；当前已显示的结果仍可继续查看。'
  if (pending.value && chart.value) saveCurrentChart()
}
function renameRecord(id: string, name: string) {
  persist(renameChart(library.value, id, name))
  libraryStatus.value = '记录名称已更新。'
}
function replaceRecord(id: string) {
  if (!pending.value) return
  const next = saveChart(library.value, pending.value.birth, pending.value.chart, id)
  if (!next) return
  persist(next)
  pending.value = null
  libraryStatus.value = '已替换所选记录，当前排盘已保存。'
}
function dismissSave() { pending.value = null; libraryStatus.value = '本次结果仅供查看，尚未保存到本机记录。' }

let inputRevision = 0
watch(birth, () => {
  inputRevision++
  chart.value = null
  chartError.value = ''
  pending.value = null
  libraryStatus.value = '出生信息已修改，请重新排盘。'
  if (library.value.activeId) persist({ ...library.value, activeId: null })
}, { flush: 'sync', deep: true })

async function submitChart() {
  if (!birth.value.solarDate || !birth.value.time) {
    chartError.value = '请选择完整的出生日期和时间，再开始排盘。'
    return
  }
  if (!birth.value.gender) { chartError.value = '请选择性别，用于计算起运与大运顺逆。'; return }
  const revision = inputRevision
  calculating.value = true
  chart.value = null
  pending.value = null
  chartError.value = ''
  try {
    const result = await calculateBazi(toBirthInput(birth.value))
    if (revision === inputRevision) {
      chart.value = result; saveCurrentChart()
      if (window.matchMedia('(max-width: 1023px)').matches) {
        editorOpen.value = false
        nextTick(() => document.getElementById('chart-result')?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
      }
    }
  } catch (error) {
    if (revision === inputRevision) {
      chartError.value = axios.isAxiosError(error) && error.response?.status === 400
        ? error.response.data.message
        : '排盘失败，请确认后端已启动后重试。'
    }
  } finally {
    calculating.value = false
  }
}

const loading = ref(false)
const health = ref<HealthResponse | null>(null)
const error = ref('')
const checkedAt = ref('')

async function checkConnection() {
  loading.value = true
  health.value = null
  error.value = ''
  try {
    health.value = await getHealth()
  } catch {
    error.value = '连接失败，请确认后端已在 8080 端口启动，再重新检查。'
  } finally {
    checkedAt.value = new Date().toLocaleTimeString('zh-CN')
    loading.value = false
  }
}

onMounted(checkConnection)
</script>

<template>
  <main class="workspace-page">
    <header class="workspace-header"><span class="mark">研</span><span>FateStay Research</span><span class="tag">未登录 · 本机保存</span></header>
    <div class="workspace-layout">
      <aside class="workspace-sidebar">
        <details id="birth-editor" class="panel editor-disclosure" :open="editorOpen" @toggle="editorOpen = ($event.target as HTMLDetailsElement).open">
          <summary><h2>出生信息</h2><span>{{ editorOpen ? '收起' : '修改 / 新建' }}</span></summary>
          <form @submit.prevent="submitChart">
            <BirthPicker v-model="birth" />
            <fieldset class="gender-field"><legend>性别</legend><div role="group" aria-label="性别"><button type="button" :aria-pressed="birth.gender === 'MALE'" @click="birth = { ...birth, gender: 'MALE' }">男</button><button type="button" :aria-pressed="birth.gender === 'FEMALE'" @click="birth = { ...birth, gender: 'FEMALE' }">女</button></div></fieldset>
            <div class="birth-submit-row"><button type="submit" :disabled="calculating">{{ calculating ? '正在排盘…' : '开始排盘' }}</button></div>
            <details class="birth-policy"><summary>计算口径</summary><p>北京时间 UTC+08:00 · 立春换年、节令换月、23 点换日。不校正夏令时或真太阳时；性别用于起运与大运顺逆。</p></details>
          </form>
          <p v-if="chartError" role="alert" class="chart-error">{{ chartError }}</p>
        </details>
        <LocalChartLibrary :library="library" :notice="storageNotice" :status="libraryStatus" :pending="!!pending" :has-chart="!!chart" @select="selectRecord" @new="newRecord" @remove="removeRecord" @rename="renameRecord" @replace="replaceRecord" @save="saveCurrentChart" @dismiss="dismissSave" />
        <details class="connection-disclosure"><summary><span class="dot" :class="{ online: health, failed: error }"></span>{{ loading ? '正在连接…' : health ? '本地服务已连接' : '本地服务未连接' }}</summary><p>{{ error || (health ? `${health.application} · ${health.status}` : '等待服务响应') }}</p><button type="button" :disabled="loading" @click="checkConnection">重新检查</button><small v-if="checkedAt">最近检查 {{ checkedAt }}</small></details>
      </aside>
      <div class="workspace-content">
        <section v-if="chart" class="chart-result" id="chart-result" aria-label="四柱结果">
          <div class="natal-panel panel">
            <div class="result-title"><div><h1>四柱命盘</h1><span>{{ currentRecord?.name ?? '本次排盘' }}</span></div><button type="button" class="text-button" @click="editBirth">修改出生信息</button></div>
            <div v-if="library.records.length > 1" class="saved-result-tabs" role="group" aria-label="切换本机排盘"><button v-for="record in library.records" :key="record.id" type="button" :title="record.name" :aria-pressed="library.activeId === record.id" @click="selectRecord(record.id, false)">{{ record.name }}</button></div>
            <div class="calendar-results"><span>公历 <b>{{ chart.solarDate }} {{ chart.birthDateTime.slice(11, 16) }}</b></span><span>农历 <b>{{ chart.lunarDate.display }}</b></span><span>{{ chart.gender === 'MALE' ? '男' : chart.gender === 'FEMALE' ? '女' : '性别待补' }}</span></div>
            <BaziResult :key="`${chart.birthDateTime}-${chart.gender}`" :chart="chart" />
            <p v-if="pending" class="unsaved-notice">本次结果尚未保存，<a href="#local-library-title">选择替换一个本机记录</a>。</p>
          </div>
          <FortuneResult v-if="chart.fortune && chart.gender" :key="`${chart.birthDateTime}-${chart.gender}`" :fortune="chart.fortune" :gender="chart.gender" :birth-date-time="chart.birthDateTime" />
          <p v-else class="legacy-fortune-note">旧快照尚未设置性别。<button type="button" class="text-button" @click="editBirth">补充性别并重新排盘</button>后可查看起运和运程。</p>
          <details class="engine-disclosure"><summary>排盘口径与引擎</summary><p>{{ chart.timeZone }} · {{ chart.engineVersion }} · {{ chart.ruleVersion }}。日期按零点换日，四柱按 23 点换日。输入历法：{{ chart.inputCalendar === 'SOLAR' ? '公历' : '农历' }}。</p></details>
        </section>
        <section v-else class="panel empty-workspace"><span class="empty-mark" aria-hidden="true">四柱</span><h1>{{ calculating ? '正在生成命盘' : '排一张命盘，开始研究。' }}</h1><p>输入出生日期、时间与性别，或选择已有本机记录。</p><button v-if="!editorOpen" type="button" @click="editBirth">填写出生信息</button><p v-if="chartError" role="alert" class="chart-error">{{ chartError }}</p></section>
      </div>
    </div>
    <footer class="workspace-footer">未登录 · 仅当前浏览器保存 · 最多 3 份</footer>
  </main>
</template>
