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
export const chartId = (birth: BirthSelection) => `${birth.solarDate}T${birth.time}${birth.gender ? `|${birth.gender}` : ''}`

function validBirth(value: unknown): value is BirthSelection {
  if (!isObject(value) || !/^\d{4}-\d{2}-\d{2}$/.test(value.solarDate) || !/^([01]\d|2[0-3]):[0-5]\d$/.test(value.time)) return false
  if (value.gender !== undefined && value.gender !== '' && !['MALE', 'FEMALE'].includes(value.gender)) return false
  if (!['SOLAR', 'LUNAR'].includes(value.calendarType) || value.solarDate < '1900-01-01' || value.solarDate > '2100-12-31') return false
  const [year, month, day] = value.solarDate.split('-').map(Number)
  const date = new Date(Date.UTC(year, month - 1, day))
  return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day
}

/** Reject old or incomplete snapshots before they can reach the result renderer. */
function validChart(value: unknown, birth: BirthSelection): value is BaziChart {
  if (!isObject(value) || value.solarDate !== birth.solarDate || value.birthDateTime !== `${birth.solarDate}T${birth.time}:00`
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
  const terrains = ['长生', '沐浴', '冠带', '临官', '帝旺', '衰', '病', '死', '墓', '绝', '胎', '养']
  const hasTraits = value.shenSha !== undefined || value.pillarDetails.some((p: any) => p.terrain !== undefined || p.selfTerrain !== undefined)
  if (hasTraits) {
    if (!value.pillarDetails.every((p: any) => terrains.includes(p.terrain) && terrains.includes(p.selfTerrain))) return false
    const shen = value.shenSha
    const ids = ['TIAN_YI', 'LU_SHEN', 'YI_MA', 'TAO_HUA', 'HUA_GAI', 'JIANG_XING', 'XUN_KONG']
    const branch = (v: unknown) => typeof v === 'string' && v.length === 1 && '子丑寅卯辰巳午未申酉戌亥'.includes(v)
    if (!isObject(shen) || shen.provider !== 'PROJECT_RULES_ON_TYME4J' || shen.ruleVersion !== 'common-seven-day-year-v1'
      || !Array.isArray(shen.rules) || shen.rules.length !== 7 || !shen.rules.every((r: any, i: number) => isObject(r) && r.id === ids[i] && text(r.name, 8) && text(r.reference, 12))
      || !Array.isArray(shen.hits) || shen.hits.length > 28) return false
    const seen = new Set<string>()
    for (const h of shen.hits) {
      if (!isObject(h) || !ids.includes(h.id) || !keys.includes(h.pillarKey) || !text(h.pillarLabel, 8)
        || h.name !== shen.rules.find((r: any) => r.id === h.id)?.name
        || h.targetBranch !== value.pillarDetails.find((p: any) => p.key === h.pillarKey)?.branch.name
        || !Array.isArray(h.bases) || h.bases.length < 1 || h.bases.length > 2 || seen.has(`${h.id}:${h.pillarKey}`)) return false
      seen.add(`${h.id}:${h.pillarKey}`)
      if (!h.bases.every((b: any) => isObject(b) && ['日干', '日支', '年支', '日柱'].includes(b.reference) && text(b.sourceValue, 2)
        && Array.isArray(b.targetValues) && b.targetValues.length > 0 && b.targetValues.length <= 2
        && b.targetValues.every(branch) && b.targetValues.includes(h.targetBranch))) return false
    }
  }
  if (value.dayMaster.name !== value.pillarDetails[2].stem.name || value.dayMaster.element !== value.pillarDetails[2].stem.element) return false
  if (!isObject(value.elementCounts)) return false
  const expected = Object.fromEntries(elements.map(name => [name, 0]))
  for (const p of value.pillarDetails) { expected[p.stem.element]++; expected[p.branch.element]++ }
  if (!elements.every(name => value.elementCounts[name] === expected[name])) return false
  if (value.auxiliaryPalaces !== undefined) {
    const palaces = value.auxiliaryPalaces
    if (!isObject(palaces) || palaces.ruleVersion !== 'tyme-eight-char-palaces-v1') return false
    for (const key of ['fetalOrigin', 'ownSign', 'bodySign']) {
      const p = palaces[key]
      if (!isObject(p) || !isObject(p.stem) || !isObject(p.branch)
        || !text(p.ganZhi, 2) || p.ganZhi.length !== 2 || p.ganZhi !== `${p.stem.name}${p.branch.name}`
        || !text(p.stem.name, 1) || !'甲乙丙丁戊己庚辛壬癸'.includes(p.stem.name)
        || !text(p.branch.name, 1) || !'子丑寅卯辰巳午未申酉戌亥'.includes(p.branch.name)
        || !element(p.stem.element) || !element(p.branch.element) || !text(p.stem.tenGod, 8) || !text(p.naYin, 12)) return false
    }
  }
  if (!birth.gender) return value.gender == null && value.fortune == null // legacy snapshots remain viewable
  if (value.gender !== birth.gender || !isObject(value.fortune)) return false
  const f = value.fortune
  if (!['FORWARD', 'REVERSE'].includes(f.direction) || !text(f.ruleVersion) || !text(f.startTime)
      || !Number.isFinite(Date.parse(f.startTime)) || f.ageConvention !== 'YEAR_DIFF_PLUS_ONE'
      || !isObject(f.startInterval) || !['years','months','days','hours','minutes'].every(k => Number.isInteger(f.startInterval[k]) && f.startInterval[k] >= 0)
      || !Array.isArray(f.decades) || f.decades.length !== 10) return false
  const cycle = (item: unknown) => isObject(item) && text(item.ganZhi, 2) && isObject(item.stem)
    && text(item.stem.name, 1) && element(item.stem.element) && text(item.stem.tenGod, 8)
    && isObject(item.branch) && text(item.branch.name, 1) && element(item.branch.element) && text(item.naYin, 12)
  return f.decades.every((decade: any, index: number) => cycle(decade) && decade.index === index + 1
    && Number.isInteger(decade.startYear) && decade.startYear === f.decades[0].startYear + index * 10
    && decade.endYear === decade.startYear + 9 && Number.isInteger(decade.startAge) && decade.startAge > 0
    && decade.startAge === f.decades[0].startAge + index * 10 && decade.endAge === decade.startAge + 9
    && Array.isArray(decade.annualYears) && decade.annualYears.length === 10
    && decade.annualYears.every((year: any, j: number) => cycle(year) && year.year === decade.startYear + j && year.age === decade.startAge + j))
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

/** Identity is the solar minute plus gender; equivalent solar/lunar input updates one slot. */
export function saveChart(library: ChartLibrary, birth: BirthSelection, chart: BaziChart, replaceId?: string, now = new Date().toISOString()): ChartLibrary | null {
  if (!validBirth(birth) || !validChart(chart, birth)) throw new Error('无法保存不完整的排盘结果，请重新排盘。')
  const id = chartId(birth)
  let existing = library.records.findIndex(record => record.id === id)
  // Explicit recalculation with a chosen gender upgrades the corresponding legacy slot.
  if (existing < 0 && birth.gender) existing = library.records.findIndex(record => !record.birth.gender
    && record.birth.solarDate === birth.solarDate && record.birth.time === birth.time)
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
