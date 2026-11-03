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
  if (scroll) nextTick(() => document.getElementById('chart-result')?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
}
function newRecord() {
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
    if (revision === inputRevision) { chart.value = result; saveCurrentChart() }
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
  <main>
    <header><span class="mark">研</span><span>FateStay Research</span><span class="tag">未登录 · 本地模式</span></header>
    <section class="intro">
      <p class="eyebrow">命理研究平台</p>
      <h1>输入出生时间，查看四柱八字。</h1>
      <p>选择公历或农历出生日期，查看对应日期与年、月、日、时四柱。</p>
    </section>
    <LocalChartLibrary :library="library" :notice="storageNotice" :status="libraryStatus" :pending="!!pending" :has-chart="!!chart" @select="selectRecord" @new="newRecord" @remove="removeRecord" @rename="renameRecord" @replace="replaceRecord" @save="saveCurrentChart" @dismiss="dismissSave" />
    <section class="panel chart-panel" aria-labelledby="chart-title">
      <div class="panel-heading"><h2 id="chart-title">八字排盘</h2><span class="tag">北京时间 UTC+08:00</span></div>
      <form @submit.prevent="submitChart">
        <BirthPicker v-model="birth" />
        <fieldset class="gender-field"><legend>性别 <span>用于起运与大运顺逆规则</span></legend><div role="group" aria-label="性别"><button type="button" :aria-pressed="birth.gender === 'MALE'" @click="birth = { ...birth, gender: 'MALE' }">男</button><button type="button" :aria-pressed="birth.gender === 'FEMALE'" @click="birth = { ...birth, gender: 'FEMALE' }">女</button></div></fieldset>
        <div class="birth-submit-row"><p>北京时间 UTC+08:00 · 立春换年、节令换月、23 点换日。<br />不校正夏令时或真太阳时。</p><button type="submit" :disabled="calculating">{{ calculating ? '正在计算四柱…' : '开始排盘' }}<span aria-hidden="true"> →</span></button></div>
      </form>
      <p v-if="chartError" role="alert" class="chart-error">{{ chartError }}</p>
      <section v-if="chart" class="chart-result" id="chart-result" aria-label="四柱结果" aria-live="polite">
        <div class="saved-result-bar"><span>{{ currentRecord ? `本机记录 · ${currentRecord.name}` : '本次结果尚未保存' }}</span><a href="#local-library-title">{{ pending ? '记录已满，选择替换 →' : '管理本机记录 ↑' }}</a></div>
        <div v-if="library.records.length > 1" class="saved-result-tabs" role="group" aria-label="切换本机排盘"><button v-for="record in library.records" :key="record.id" type="button" :title="record.name" :aria-pressed="library.activeId === record.id" @click="selectRecord(record.id, false)">{{ record.name }}</button></div>
        <div class="calendar-results">
          <p><span>公历</span><strong>{{ chart.solarDate }} {{ chart.birthDateTime.slice(11, 16) }}</strong></p>
          <p><span>农历</span><strong>{{ chart.lunarDate.display }} {{ chart.birthDateTime.slice(11, 16) }}</strong></p>
        </div>
        <p class="result-input">输入历法：{{ chart.inputCalendar === 'SOLAR' ? '公历' : '农历' }} · {{ chart.timeZone }} · 日期按零点换日，四柱按 23 点换日</p>
        <BaziResult :chart="chart" />
        <FortuneResult v-if="chart.fortune && chart.gender" :key="`${chart.birthDateTime}-${chart.gender}`" :fortune="chart.fortune" :gender="chart.gender" />
        <p v-else class="legacy-fortune-note">此为旧排盘快照，尚未设置性别。<a href="#chart-title">补充性别并重新排盘</a>后可查看起运、大运与流年。</p>
        <p class="engine">引擎 {{ chart.engineVersion }} · 规则 {{ chart.ruleVersion }}</p>
      </section>
    </section>
    <section class="panel" aria-labelledby="connection-title">
      <div class="panel-heading"><h2 id="connection-title">服务连接</h2><span class="endpoint">GET /api/v1/health</span></div>
      <div role="status" aria-live="polite" class="connection">
        <span class="dot" :class="{ online: health, failed: error }"></span>
        <div>
          <strong>{{ loading ? '正在连接后端…' : health ? '后端连接正常' : '后端未连接' }}</strong>
          <p>{{ error || (health ? `${health.application} · ${health.status}` : '等待服务响应') }}</p>
        </div>
      </div>
      <div class="panel-footer"><span>{{ checkedAt ? `最近检查 ${checkedAt}` : '首次连接检查' }}</span><button :disabled="loading" @click="checkConnection">{{ loading ? '检查中…' : '重新检查' }}</button></div>
    </section>
    <p class="note">当前为未登录本地模式；账号登录与云端保存将在后续实现。</p>
  </main>
</template>
