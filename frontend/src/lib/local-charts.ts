import type { BaziChart, ElementName } from '../api/bazi'
import type { BirthSelection } from './birth-calendar'

export const LOCAL_CHARTS_KEY = 'fatestay.guest-charts.v1'
export const MAX_LOCAL_CHARTS = 3
export interface SavedChart { id: string; name: string; birth: BirthSelection; chart: BaziChart; savedAt: string }
export interface ChartLibrary { version: 1; records: SavedChart[]; activeId: string | null }
export interface LocalStoragePort { getItem(key: string): string | null; setItem(key: string, value: string): void }
export const emptyLibrary = (): ChartLibrary => ({ version: 1, records: [], activeId: null })
const elements: ElementName[] = ['木', '火', '土', '金', '水']
const keys = ['year', 'month', 'day', 'hour'] as const
const isObject = (value: unknown): value is Record<string, any> => typeof value === 'object' && value !== null && !Array.isArray(value)
const text = (value: unknown, max = 80): value is string => typeof value === 'string' && value.length > 0 && value.length <= max
const element = (value: unknown) => elements.includes(value as ElementName)
export const chartId = (birth: BirthSelection) => `${birth.solarDate}T${birth.time}`

function validBirth(value: unknown): value is BirthSelection {
  if (!isObject(value) || !/^\d{4}-\d{2}-\d{2}$/.test(value.solarDate) || !/^([01]\d|2[0-3]):[0-5]\d$/.test(value.time)) return false
  if (!['SOLAR', 'LUNAR'].includes(value.calendarType) || value.solarDate < '1900-01-01' || value.solarDate > '2100-12-31') return false
  const [year, month, day] = value.solarDate.split('-').map(Number)
  const date = new Date(Date.UTC(year, month - 1, day))
  return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day
}

/** Reject old or incomplete snapshots before they can reach the result renderer. */
function validChart(value: unknown, birth: BirthSelection): value is BaziChart {
  if (!isObject(value) || value.solarDate !== birth.solarDate || value.birthDateTime !== `${chartId(birth)}:00`
      || value.inputCalendar !== birth.calendarType || value.timeZone !== 'UTC+08:00' || value.precision !== 'MINUTE'
      || !text(value.engineVersion) || !text(value.ruleVersion)) return false
  if (!isObject(value.dayMaster) || !text(value.dayMaster.name, 1) || !element(value.dayMaster.element)
      || !isObject(value.lunarDate) || !Number.isInteger(value.lunarDate.year)
      || !Number.isInteger(value.lunarDate.month) || value.lunarDate.month < 1 || value.lunarDate.month > 12
      || !Number.isInteger(value.lunarDate.day) || value.lunarDate.day < 1 || value.lunarDate.day > 30
      || typeof value.lunarDate.leapMonth !== 'boolean' || !text(value.lunarDate.display)) return false
  if (!isObject(value.pillars) || !Array.isArray(value.pillarDetails) || value.pillarDetails.length !== 4) return false
  for (const [index, key] of keys.entries()) {
    const p = value.pillarDetails[index]
    if (!isObject(p) || p.key !== key || !text(p.label, 8) || !text(value.pillars[key], 2) || p.ganZhi !== value.pillars[key]
        || !isObject(p.stem) || !text(p.stem.name, 1) || !element(p.stem.element) || !text(p.stem.tenGod, 8)
        || !isObject(p.branch) || !text(p.branch.name, 1) || !element(p.branch.element) || !text(p.naYin, 12)
        || !Array.isArray(p.hiddenStems) || p.hiddenStems.length < 1 || p.hiddenStems.length > 3) return false
    let lastQi = -1
    for (const hidden of p.hiddenStems) {
      if (!isObject(hidden) || !text(hidden.name, 1) || !element(hidden.element) || !text(hidden.tenGod, 8)) return false
      const qi = ['本气', '中气', '余气'].indexOf(hidden.qi)
      if (qi <= lastQi || (lastQi === -1 && qi !== 0)) return false
      lastQi = qi
    }
  }
  if (value.dayMaster.name !== value.pillarDetails[2].stem.name || value.dayMaster.element !== value.pillarDetails[2].stem.element) return false
  if (!isObject(value.elementCounts)) return false
  const expected = Object.fromEntries(elements.map(name => [name, 0]))
  for (const p of value.pillarDetails) { expected[p.stem.element]++; expected[p.branch.element]++ }
  return elements.every(name => value.elementCounts[name] === expected[name])
}

function validRecord(value: unknown): value is SavedChart {
  return isObject(value) && validBirth(value.birth) && value.id === chartId(value.birth)
    && text(value.name, 20) && text(value.savedAt) && Number.isFinite(Date.parse(value.savedAt)) && validChart(value.chart, value.birth)
}

export function readLibrary(storage?: LocalStoragePort): { library: ChartLibrary; notice: string } {
  if (!storage) return { library: emptyLibrary(), notice: '此浏览器不允许本地保存，记录仅在本次页面中保留。' }
  try {
    const raw = storage.getItem(LOCAL_CHARTS_KEY)
    if (!raw) return { library: emptyLibrary(), notice: '' }
    if (raw.length > 256_000) throw new Error('oversized')
    const parsed: unknown = JSON.parse(raw)
    if (!isObject(parsed) || parsed.version !== 1 || !Array.isArray(parsed.records)) throw new Error('schema')
    const records: SavedChart[] = []
    for (const record of parsed.records) {
      if (validRecord(record) && !records.some(item => item.id === record.id) && records.length < MAX_LOCAL_CHARTS) records.push(record)
    }
    const activeId = records.some(record => record.id === parsed.activeId) ? parsed.activeId : null
    return { library: { version: 1, records, activeId }, notice: records.length !== parsed.records.length ? '部分旧记录不完整或已损坏，已跳过；可重新排盘保存。' : '' }
  } catch {
    return { library: emptyLibrary(), notice: '本地记录无法读取，当前仍可正常排盘并重新保存。' }
  }
}

export function writeLibrary(storage: LocalStoragePort | undefined, library: ChartLibrary): string {
  try {
    if (!storage) throw new Error('unavailable')
    storage.setItem(LOCAL_CHARTS_KEY, JSON.stringify(library))
    return ''
  } catch {
    return '本地保存失败（存储已满或被禁用），本次变更仅在当前页面生效，刷新可能丢失。'
  }
}

/** Identity is the solar minute, so equivalent solar/lunar input updates one slot. */
export function saveChart(library: ChartLibrary, birth: BirthSelection, chart: BaziChart, replaceId?: string, now = new Date().toISOString()): ChartLibrary | null {
  if (!validBirth(birth) || !validChart(chart, birth)) throw new Error('无法保存不完整的排盘结果，请重新排盘。')
  const id = chartId(birth)
  const existing = library.records.findIndex(record => record.id === id)
  const replacing = replaceId ? library.records.findIndex(record => record.id === replaceId) : -1
  if (existing === -1 && library.records.length >= MAX_LOCAL_CHARTS && replacing === -1) return null
  const index = existing >= 0 ? existing : replacing >= 0 ? replacing : library.records.length
  const usedNames = library.records.filter((_, i) => i !== replacing).map(record => record.name)
  const defaultName = [index + 1, 1, 2, 3].map(n => `出生记录 ${n}`).find(name => !usedNames.includes(name)) ?? `出生记录 ${index + 1}`
  const record: SavedChart = { id, name: existing >= 0 ? library.records[existing].name : defaultName,
    birth: { ...birth }, chart: JSON.parse(JSON.stringify(chart)) as BaziChart, savedAt: now }
  const records = [...library.records]
  records[index] = record
  return { version: 1, records, activeId: id }
}

export function renameChart(library: ChartLibrary, id: string, name: string): ChartLibrary {
  const normalized = name.trim().slice(0, 20)
  if (!normalized) return library
  return { ...library, records: library.records.map(record => record.id === id ? { ...record, name: normalized } : record) }
}
export function deleteChart(library: ChartLibrary, id: string): ChartLibrary {
  return { ...library, records: library.records.filter(record => record.id !== id), activeId: library.activeId === id ? null : library.activeId }
}
