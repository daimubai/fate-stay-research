import type { BirthInput, BirthGender } from '../api/bazi'
import { LunarDay, LunarMonth, LunarYear, SolarDay, SolarMonth } from 'tyme4ts'

export type CalendarType = 'SOLAR' | 'LUNAR'
export interface BirthSelection { solarDate: string; time: string; calendarType: CalendarType; gender?: BirthGender | '' }
export interface DateParts { year: number; month: number; day: number }
export interface Choice { value: number; text: string }
export const MIN_DATE = '1900-01-01'
export const MAX_DATE = '2100-12-31'
const pad = (value: number) => String(value).padStart(2, '0')
export const isoDate = (year: number, month: number, day: number) => `${year}-${pad(month)}-${pad(day)}`

export function solarToLunar(date: string) {
  const [year, month, day] = date.split('-').map(Number)
  return SolarDay.fromYmd(year, month, day).getLunarDay()
}

export function partsForDate(date: string, type: CalendarType): DateParts {
  if (type === 'LUNAR') {
    const lunar = solarToLunar(date)
    return { year: lunar.getYear(), month: lunar.getLunarMonth().getMonthWithLeap(), day: lunar.getDay() }
  }
  const [year, month, day] = date.split('-').map(Number)
  return { year, month, day }
}

export function toSolarDate(type: CalendarType, parts: DateParts): string {
  if (type === 'SOLAR') return isoDate(parts.year, parts.month, parts.day)
  const solar = LunarDay.fromYmd(parts.year, parts.month, parts.day).getSolarDay()
  return isoDate(solar.getYear(), solar.getMonth(), solar.getDay())
}

export function lunarLabel(date: string): string {
  const lunar = solarToLunar(date)
  return `${lunar.getYear()}年${lunar.getLunarMonth().getName()}${lunar.getName()}`
}

export const yearsFor = (type: CalendarType): Choice[] => Array.from(
  { length: type === 'LUNAR' ? 202 : 201 },
  (_, index) => ({ value: (type === 'LUNAR' ? 1899 : 1900) + index, text: `${(type === 'LUNAR' ? 1899 : 1900) + index}年` }),
)

const daysCache = new Map<string, Choice[]>()
export function daysFor(type: CalendarType, year: number, month: number): Choice[] {
  const key = `${type}-${year}-${month}`
  if (daysCache.has(key)) return daysCache.get(key)!
  const count = type === 'LUNAR' ? LunarMonth.fromYm(year, month).getDayCount() : SolarMonth.fromYm(year, month).getDayCount()
  const choices = Array.from({ length: count }, (_, i) => {
    const day = i + 1
    return { value: day, text: type === 'LUNAR' ? LunarDay.NAMES[i] : `${day}日` }
  }).filter(option => {
    const solar = toSolarDate(type, { year, month, day: option.value })
    return solar >= MIN_DATE && solar <= MAX_DATE
  })
  daysCache.set(key, choices)
  return choices
}

export function monthsFor(type: CalendarType, year: number): Choice[] {
  const months = type === 'LUNAR'
    ? LunarYear.fromYear(year).getMonths().map(month => ({ value: month.getMonthWithLeap(), text: month.getName() }))
    : Array.from({ length: 12 }, (_, i) => ({ value: i + 1, text: `${i + 1}月` }))
  return months.filter(month => daysFor(type, year, month.value).length > 0)
}

/** Preserve the day when possible; clamp after year/month changes (including missing leap months). */
export function normalizeParts(type: CalendarType, parts: DateParts): DateParts {
  const months = monthsFor(type, parts.year)
  const month = months.some(option => option.value === parts.month) ? parts.month
    : months.find(option => option.value === Math.abs(parts.month))?.value ?? months[0].value
  const days = daysFor(type, parts.year, month)
  const day = Math.min(Math.max(Number.isFinite(parts.day) ? parts.day : days[0].value, days[0].value), days[days.length - 1].value)
  return { year: parts.year, month, day }
}

export function toBirthInput(selection: BirthSelection): BirthInput {
  if (!selection.gender) throw new Error('请选择性别以计算起运与大运')
  if (selection.calendarType === 'SOLAR') {
    return { calendarType: 'SOLAR', birthDate: selection.solarDate, birthTime: selection.time, gender: selection.gender }
  }
  const lunar = solarToLunar(selection.solarDate)
  return { calendarType: 'LUNAR', birthTime: selection.time, gender: selection.gender, lunarDate: {
    year: lunar.getYear(), month: lunar.getLunarMonth().getMonth(),
    day: lunar.getDay(), leapMonth: lunar.getLunarMonth().isLeap(),
  } }
}
