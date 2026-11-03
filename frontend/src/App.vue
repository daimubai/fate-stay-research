<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getHealth, type HealthResponse } from './api/health'

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
      <h1>从基础开始，探索历法与八字。</h1>
      <p>开发入口已就绪。这里将逐步接入历法计算、八字排盘与研究工具。</p>
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
    <p class="note">当前为开发骨架，历法、排盘及账户功能尚未实现。</p>
  </main>
</template>
