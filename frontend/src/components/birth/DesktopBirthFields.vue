<script setup lang="ts">
import { computed } from 'vue'
import { ElConfigProvider, ElDatePicker, ElTimePicker, ElSelect, ElOption } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/es/components/base/style/css'
import 'element-plus/es/components/date-picker/style/css'
import 'element-plus/es/components/time-picker/style/css'
import 'element-plus/es/components/select/style/css'
import 'element-plus/es/components/option/style/css'
import { daysFor, monthsFor, normalizeParts, partsForDate, solarToLunar, toSolarDate, yearsFor } from '../../lib/birth-calendar'
import type { BirthSelection, DateParts } from '../../lib/birth-calendar'
const model = defineModel<BirthSelection>({ required: true })
const parts = computed(() => model.value.solarDate ? partsForDate(model.value.solarDate, 'LUNAR') : undefined)
const currentYear = computed(() => parts.value?.year ?? 1990)
const currentMonth = computed(() => parts.value?.month ?? monthsFor('LUNAR', currentYear.value)[0].value)
function patch(value: Partial<BirthSelection>) { model.value = { ...model.value, ...value } }
function changeLunar(field: keyof DateParts, value: unknown) {
  const base = parts.value ?? { year: 1990, month: 1, day: 1 }
  const next = normalizeParts('LUNAR', { ...base, [field]: Number(value) })
  patch({ solarDate: toSolarDate('LUNAR', next) })
}
function setSolar(value: unknown) { patch({ solarDate: typeof value === 'string' ? value : '' }) }
function setTime(value: unknown) { patch({ time: typeof value === 'string' ? value : '' }) }
function disabledDate(date: Date) { const y = date.getFullYear(); return y < 1900 || y > 2100 }
function cellLunar(date: Date) { const lunar = solarToLunar(`${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`); return lunar.getDay() === 1 ? lunar.getLunarMonth().getName() : lunar.getName() }
</script>
<template>
    <ElConfigProvider :locale="zhCn">
      <div class="desktop-birth-fields">
        <div class="date-field"><label class="field-caption" :for="model.calendarType === 'SOLAR' ? 'birth-solar-date' : 'birth-lunar-year'">{{ model.calendarType === 'SOLAR' ? '公历日期' : '农历日期' }}</label>
          <ElDatePicker v-if="model.calendarType === 'SOLAR'" id="birth-solar-date" :model-value="model.solarDate" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" placeholder="选择或输入出生日期" :default-value="new Date(1990, 0, 1)" :disabled-date="disabledDate" :clearable="false" size="large" popper-class="birth-date-popper" @update:model-value="setSolar">
            <template #default="cell"><div class="dual-date-cell"><span>{{ cell.text }}</span><small>{{ cellLunar(cell.date) }}</small></div></template>
          </ElDatePicker>
          <div v-else class="lunar-desktop-row">
            <ElSelect id="birth-lunar-year" :model-value="parts?.year" filterable placeholder="搜索年份" size="large" aria-label="农历年份" @update:model-value="changeLunar('year', $event)"><ElOption v-for="year in yearsFor('LUNAR')" :key="year.value" :label="year.text" :value="year.value" /></ElSelect>
            <ElSelect :model-value="parts?.month" placeholder="月份 / 闰月" size="large" aria-label="农历月份" @update:model-value="changeLunar('month', $event)"><ElOption v-for="month in monthsFor('LUNAR', currentYear)" :key="month.value" :label="month.text" :value="month.value" /></ElSelect>
            <ElSelect :model-value="parts?.day" placeholder="日期" size="large" aria-label="农历日期" @update:model-value="changeLunar('day', $event)"><ElOption v-for="day in daysFor('LUNAR', currentYear, currentMonth)" :key="day.value" :label="day.text" :value="day.value" /></ElSelect>
          </div>
        </div>
        <div class="time-field"><label class="field-caption" for="birth-clock">出生时间 · 24 小时制</label><ElTimePicker id="birth-clock" :model-value="model.time" format="HH:mm" value-format="HH:mm" placeholder="选择或输入时分" :default-value="new Date(2000, 0, 1, 12, 0)" :clearable="false" size="large" @update:model-value="setTime" /></div>
      </div>
    </ElConfigProvider>

</template>
