<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Picker as VanPicker, Popup as VanPopup, TimePicker as VanTimePicker } from 'vant'
import type { PickerConfirmEventParams } from 'vant'
import 'vant/lib/picker/style'
import 'vant/lib/popup/style'
import 'vant/lib/time-picker/style'
import { daysFor, lunarLabel, monthsFor, normalizeParts, partsForDate, toSolarDate, yearsFor } from '../../lib/birth-calendar'
import type { BirthSelection, CalendarType, DateParts } from '../../lib/birth-calendar'
const model = defineModel<BirthSelection>({ required: true })
const formatted = computed(() => model.value.solarDate ? (model.value.calendarType === 'LUNAR' ? lunarLabel(model.value.solarDate) : model.value.solarDate.replaceAll('-', ' / ')) : '')
const showDate = ref(false)
const showTime = ref(false)
const triggerDate = ref<HTMLButtonElement>()
const triggerTime = ref<HTMLButtonElement>()
const draftType = ref<CalendarType>('SOLAR')
const draftParts = ref<DateParts>({ year: 1990, month: 1, day: 1 })
const draftTime = ref(['12', '00'])
const jumpYear = ref('1990')
const jumpError = ref('')
watch(() => draftParts.value.year, year => { jumpYear.value = String(year); jumpError.value = '' })
function applyYear() {
  const year = Number(jumpYear.value)
  const minimum = draftType.value === 'LUNAR' ? 1899 : 1900
  if (!/^\d{4}$/.test(jumpYear.value) || year < minimum || year > 2100) {
    jumpError.value = `请输入 ${minimum}–2100 年`
    return
  }
  draftParts.value = normalizeParts(draftType.value, { ...draftParts.value, year })
  jumpError.value = ''
}

const draftSolar = computed(() => toSolarDate(draftType.value, draftParts.value))
const draftPreview = computed(() => `${draftSolar.value} · ${lunarLabel(draftSolar.value)}`)
const columns = computed(() => [yearsFor(draftType.value), monthsFor(draftType.value, draftParts.value.year), daysFor(draftType.value, draftParts.value.year, draftParts.value.month)])
const pickerValues = computed({
  get: () => [draftParts.value.year, draftParts.value.month, draftParts.value.day],
  set: values => { draftParts.value = normalizeParts(draftType.value, { year: Number(values[0]), month: Number(values[1]), day: Number(values[2]) }) },
})

function patch(value: Partial<BirthSelection>) { model.value = { ...model.value, ...value } }
function openDate() {
  draftType.value = model.value.calendarType
  draftParts.value = partsForDate(model.value.solarDate || '1990-01-01', draftType.value)
  jumpYear.value = String(draftParts.value.year)
  jumpError.value = ''
  showDate.value = true
}
function switchDraft(type: CalendarType) {
  const solar = draftSolar.value
  draftType.value = type
  draftParts.value = partsForDate(solar, type)
}
function confirmDate(event: PickerConfirmEventParams) {
  const pendingYear = jumpYear.value !== String(draftParts.value.year)
  if (pendingYear) {
    applyYear()
    if (jumpError.value) return
  }
  const [year, month, day] = event.selectedValues.map(Number)
  const next = pendingYear ? draftParts.value : normalizeParts(draftType.value, { year, month, day })
  patch({ solarDate: toSolarDate(draftType.value, next), calendarType: draftType.value })
  showDate.value = false
}
function openTime() { draftTime.value = (model.value.time || '12:00').split(':'); showTime.value = true }
function confirmTime({ selectedValues }: PickerConfirmEventParams) { patch({ time: selectedValues.map(v => String(v).padStart(2,'0')).join(':') }); showTime.value = false }
</script>
<template>
    <div class="mobile-birth-fields">
      <button ref="triggerDate" type="button" class="picker-trigger" aria-label="选择出生日期" aria-haspopup="dialog" :aria-expanded="showDate" @click="openDate"><span><small>{{ model.calendarType === 'SOLAR' ? '公历日期' : '农历日期' }}</small><strong :class="{placeholder: !formatted}">{{ formatted || '请选择出生日期' }}</strong></span><span aria-hidden="true" class="picker-chevron">›</span></button>
      <button ref="triggerTime" type="button" class="picker-trigger" aria-label="选择出生时间" aria-haspopup="dialog" :aria-expanded="showTime" @click="openTime"><span><small>出生时间 · 24 小时制</small><strong :class="{placeholder: !model.time}">{{ model.time || '请选择小时与分钟' }}</strong></span><span aria-hidden="true" class="picker-chevron">›</span></button>
    </div>
    <VanPopup v-model:show="showDate" round position="bottom" teleport="body" safe-area-inset-bottom close-on-popstate class="birth-sheet" role="dialog" aria-label="出生日期选择" @keydown.esc.prevent="showDate = false" @closed="triggerDate?.focus()">
      <div class="sheet-handle" aria-hidden="true"></div><div class="sheet-heading"><h3>选择出生日期</h3><p>对应公历 1900—2100 年</p></div>
      <div class="calendar-tabs sheet-tabs" role="group" aria-label="弹层历法"><button type="button" :aria-pressed="draftType === 'SOLAR'" @click="switchDraft('SOLAR')">公历（阳历）</button><button type="button" :aria-pressed="draftType === 'LUNAR'" @click="switchDraft('LUNAR')">农历（阴历）</button></div>
      <div class="year-jump"><label for="picker-jump-year">直达年份</label><input id="picker-jump-year" v-model="jumpYear" type="text" inputmode="numeric" maxlength="4" aria-label="直达年份" :aria-invalid="!!jumpError" @keydown.enter.prevent="applyYear" /><button type="button" @click="applyYear">跳转</button></div>
      <p v-if="jumpError" class="year-jump-error" role="alert">{{ jumpError }}</p>
      <p class="sheet-preview" aria-live="polite">{{ draftPreview }}</p>
      <VanPicker v-model="pickerValues" :columns="columns" :swipe-duration="500" :visible-option-num="5" :option-height="48" confirm-button-text="确认日期" cancel-button-text="取消" @confirm="confirmDate" @cancel="showDate = false" />
      <p class="sheet-note">上下滑动选择；切换历法保留同一天，确认后生效</p>
    </VanPopup>
    <VanPopup v-model:show="showTime" round position="bottom" teleport="body" safe-area-inset-bottom close-on-popstate class="birth-sheet" role="dialog" aria-label="出生时间选择" @keydown.esc.prevent="showTime = false" @closed="triggerTime?.focus()">
      <div class="sheet-handle" aria-hidden="true"></div><div class="sheet-heading"><h3>选择出生时间</h3><p>北京时间 UTC+08:00 · 精确到分钟</p></div>
      <VanTimePicker v-model="draftTime" :columns-type="['hour', 'minute']" :swipe-duration="500" :visible-option-num="5" :option-height="48" confirm-button-text="确认时间" cancel-button-text="取消" @confirm="confirmTime" @cancel="showTime = false" />
      <p class="sheet-note">24 小时制 · 23:00 起按次日日柱排盘</p>
    </VanPopup>

</template>
