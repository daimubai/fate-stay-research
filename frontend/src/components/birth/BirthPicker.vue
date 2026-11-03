<script setup lang="ts">
import { defineAsyncComponent, onBeforeUnmount, onMounted, ref } from 'vue'
import { lunarLabel } from '../../lib/birth-calendar'
import type { BirthSelection, CalendarType } from '../../lib/birth-calendar'
const DesktopBirthFields = defineAsyncComponent(() => import('./DesktopBirthFields.vue'))
const MobileBirthFields = defineAsyncComponent(() => import('./MobileBirthFields.vue'))
const model = defineModel<BirthSelection>({ required: true })
const media = window.matchMedia('(max-width: 767px)')
const mobile = ref(media.matches)
function updateDevice() { mobile.value = media.matches }
onMounted(() => media.addEventListener('change', updateDevice))
onBeforeUnmount(() => media.removeEventListener('change', updateDevice))
function changeCalendar(type: CalendarType) { model.value = { ...model.value, calendarType: type } }
</script>
<template>
  <div class="birth-picker">
    <div class="birth-picker-top"><span class="field-caption">出生日期与时间</span><div class="calendar-tabs" role="group" aria-label="输入历法">
      <button type="button" :aria-pressed="model.calendarType === 'SOLAR'" @click="changeCalendar('SOLAR')">公历（阳历）</button>
      <button type="button" :aria-pressed="model.calendarType === 'LUNAR'" @click="changeCalendar('LUNAR')">农历（阴历）</button>
    </div></div>
    <MobileBirthFields v-if="mobile" v-model="model" />
    <DesktopBirthFields v-else v-model="model" />
    <div class="birth-preview" aria-live="polite"><template v-if="model.solarDate"><span>公历 <b>{{ model.solarDate }}</b></span><span>农历 <b>{{ lunarLabel(model.solarDate) }}</b></span></template><span v-else>选定日期后，同时预览对应的公历和农历</span></div>
    <p class="picker-hint">{{ model.calendarType === 'LUNAR' ? '仅显示当年有效月份与天数，闰月已合并到月份列表。' : mobile ? '点按日期或时间打开选择面板，确认后生效。' : '可直接输入 YYYY-MM-DD，也可点击日历顶部快速选择出生年份。' }}</p>
  </div>
</template>
