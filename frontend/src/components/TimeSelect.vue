<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
export interface TimeOption { value: string; label: string; description?: string; annotation?: string }
const props = defineProps<{ label: string; modelValue: string; options: TimeOption[]; display?: string; placeholder?: string; disabled?: boolean; loading?: boolean; error?: string; hint?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string]; open: []; retry: [] }>()
const dialog = ref<HTMLDialogElement>()
const opened = ref(false)
const caption = computed(() => props.display || props.options.find(item => item.value === props.modelValue)?.label || props.placeholder || `选择${props.label}`)
function open() {
  if (props.disabled || !dialog.value) return
  dialog.value.showModal(); opened.value = true; emit('open')
}
function closed() { opened.value = false }
function choose(value: string) { emit('update:modelValue', value); dialog.value?.close() }
onBeforeUnmount(() => { if (opened.value) { dialog.value?.close(); closed() } })
</script>
<template>
  <div class="time-select">
    <span class="time-select-label">{{ label }}</span>
    <button type="button" class="time-select-trigger" :disabled="disabled" :aria-label="`选择${label}：${caption}`" aria-haspopup="dialog" :aria-expanded="opened" @click="open"><span>{{ caption }}</span><svg viewBox="0 0 16 16" aria-hidden="true"><path d="m4 6 4 4 4-4" /></svg></button>
    <Teleport to="body">
      <dialog ref="dialog" class="time-dialog" :aria-label="`选择${label}`" @close="closed" @click="($event.target === $event.currentTarget) && dialog?.close()">
        <div class="time-dialog-content"><div class="time-dialog-heading"><h3>选择{{ label }}</h3><button type="button" class="dialog-close" :aria-label="`关闭${label}选择`" @click="dialog?.close()"><svg viewBox="0 0 20 20" aria-hidden="true"><path d="m5 5 10 10M15 5 5 15" /></svg></button></div>
          <p v-if="hint" class="time-dialog-hint">{{ hint }}</p>
          <p v-if="loading" class="time-dialog-status" role="status">正在加载{{ label }}…</p>
          <div v-else-if="error" class="time-dialog-error" role="alert">{{ error }}<button type="button" @click="emit('retry')">重试{{ label }}</button></div>
          <div v-else-if="options.length" class="time-options" :aria-label="`${label}选项`"><button v-for="item in options" :key="item.value" type="button" :aria-pressed="modelValue === item.value" @click="choose(item.value)"><strong>{{ item.label }}</strong><span v-if="item.description">{{ item.description }}</span><small v-if="item.annotation">{{ item.annotation }}</small><em v-if="modelValue === item.value">已选</em></button></div>
          <p v-else class="time-dialog-status">暂无可选{{ label }}。</p>
        </div>
      </dialog>
    </Teleport>
  </div>
</template>
<style>
body:has(.time-dialog[open]) { overflow: hidden; }
.time-select { min-width: 0; }
.time-select-label { display: block; font-size: 11px; color: #7b8d7d; margin-bottom: 7px; }
.time-select-trigger { width: 100%; min-height: 44px; padding: 10px; background: #fff; color: #345446; border: 1px solid #dae3da; border-radius: 6px; display: flex; justify-content: space-between; align-items: center; gap: 5px; text-align: left; font-size: 12px; }
.time-select-trigger:hover { background: #f4f8f1; }
.time-select-trigger span { overflow-wrap: anywhere; }
.time-select-trigger:disabled { cursor: not-allowed; color: #95a194; background: #f8faf6; }
.time-select svg { width: 14px; height: 14px; flex-shrink: 0; fill: none; stroke: currentColor; stroke-width: 1.5; }
.time-dialog { width: min(620px, calc(100% - 32px)); max-height: min(760px, 86dvh); padding: 0; border: 1px solid #dce4da; border-radius: 12px; color: #30473c; box-shadow: 0 24px 80px #1b302b30; }
.time-dialog::backdrop { background: #162d284a; }
.time-dialog-content { padding: 22px; }
.time-dialog-heading { display: flex; justify-content: space-between; align-items: center; gap: 16px; position: sticky; top: -1px; z-index: 1; background: white; padding-bottom: 12px; }
.time-dialog h3 { font-size: 18px; margin: 0; }
.dialog-close { background: #f1f5ef; color: #5b7162; width: 44px; height: 44px; display: grid; place-items: center; padding: 10px; }
.dialog-close svg { width: 20px; height: 20px; fill: none; stroke: currentColor; stroke-width: 1.5; }
.time-dialog-hint, .time-dialog-status { font-size: 12px; color: #7e8e7a; line-height: 1.8; margin: 0 0 16px; }
.time-dialog-status { padding: 24px 0; }
.time-options { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.time-options button { position: relative; display: grid; align-content: start; gap: 8px; min-height: 90px; background: #fafcf8; color: #456044; text-align: left; border: 1px solid #e0e7dc; border-radius: 8px; padding: 15px 12px; }
.time-options button:hover { background: #f0f6e9; border-color: #9bae90; }
.dialog-close:hover { background: #e8eee3; }
.time-options button[aria-pressed="true"] { border-color: #6f9161; background: #eef5e9; }
.time-options strong { font-size: 14px; font-weight: 550; }
.time-options span, .time-options small { font-size: 11px; line-height: 1.7; color: #7d8e73; }
.time-options em { font-size: 9px; font-style: normal; color: #4b7446; }
.time-dialog-error { background: #faf3e9; color: #986c40; padding: 18px; font-size: 12px; line-height: 1.8; }
.time-dialog-error button { margin-top: 12px; display: block; }
@media(max-width:767px) { .time-dialog { width: 100%; max-width: 100%; max-height: 85dvh; margin: auto 0 0; border-radius: 16px 16px 0 0; padding-bottom: env(safe-area-inset-bottom); } .time-dialog-content { padding: 18px 16px; } .time-options { grid-template-columns: repeat(2,minmax(0,1fr)); } }
</style>
