<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import axios from 'axios'
import { calculateBazi, type BaziChart } from './api/bazi'
import { getHealth, type HealthResponse } from './api/health'
import BirthPicker from './components/birth/BirthPicker.vue'
import { toBirthInput, type BirthSelection } from './lib/birth-calendar'

const birth = ref<BirthSelection>({ solarDate: '', time: '', calendarType: 'SOLAR' })
const calculating = ref(false)
const chart = ref<BaziChart | null>(null)
const chartError = ref('')
let inputRevision = 0
watch(birth, () => {
  inputRevision++
  chart.value = null
  chartError.value = ''
}, { flush: 'sync', deep: true })

async function submitChart() {
  if (!birth.value.solarDate || !birth.value.time) {
    chartError.value = '请选择完整的出生日期和时间，再开始排盘。'
    return
  }
  const revision = inputRevision
  calculating.value = true
  chart.value = null
  chartError.value = ''
  try {
    const result = await calculateBazi(toBirthInput(birth.value))
    if (revision === inputRevision) chart.value = result
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
    <header><span class="mark">研</span><span>FateStay Research</span><span class="tag">本地开发</span></header>
    <section class="intro">
      <p class="eyebrow">命理研究平台</p>
      <h1>输入出生时间，查看四柱八字。</h1>
      <p>选择公历或农历出生日期，查看对应日期与年、月、日、时四柱。</p>
    </section>
    <section class="panel chart-panel" aria-labelledby="chart-title">
      <div class="panel-heading"><h2 id="chart-title">八字排盘</h2><span class="tag">北京时间 UTC+08:00</span></div>
      <form @submit.prevent="submitChart">
        <BirthPicker v-model="birth" />
        <div class="birth-submit-row"><p>北京时间 UTC+08:00 · 立春换年、节令换月、23 点换日。<br />不校正夏令时或真太阳时。</p><button type="submit" :disabled="calculating">{{ calculating ? '正在计算四柱…' : '开始排盘' }}<span aria-hidden="true"> →</span></button></div>
      </form>
      <p v-if="chartError" role="alert" class="chart-error">{{ chartError }}</p>
      <section v-if="chart" class="chart-result" aria-label="四柱结果" aria-live="polite">
        <div class="calendar-results">
          <p><span>公历</span><strong>{{ chart.solarDate }} {{ chart.birthDateTime.slice(11, 16) }}</strong></p>
          <p><span>农历</span><strong>{{ chart.lunarDate.display }} {{ chart.birthDateTime.slice(11, 16) }}</strong></p>
        </div>
        <p class="result-input">输入历法：{{ chart.inputCalendar === 'SOLAR' ? '公历' : '农历' }} · {{ chart.timeZone }} · 日期按零点换日，四柱按 23 点换日</p>
        <div class="pillars">
          <div v-for="(label, key) in { year: '年柱', month: '月柱', day: '日柱', hour: '时柱' }" :key="key" class="pillar">
            <h3>{{ label }}</h3><strong>{{ chart.pillars[key] }}</strong>
          </div>
        </div>
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
    <p class="note">当前提供基础四柱排盘，大运、流年、账户与排盘保存尚未实现。</p>
  </main>
</template>
