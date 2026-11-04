import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { LOCAL_CHARTS_KEY, emptyLibrary, chartId, readLibrary, writeLibrary, saveChart, renameChart, deleteChart } from '../src/lib/local-charts.ts'

const fixture = JSON.parse(readFileSync(new URL('./fixtures/bazi-chart.json', import.meta.url), 'utf8'))
const now = '2026-11-03T08:00:00.000Z'
function data(minute = 37, calendarType = 'SOLAR') {
  const time = `13:${minute}`
  const birth = { solarDate: '1986-05-29', time, calendarType }
  const chart = structuredClone(fixture)
  chart.birthDateTime = `${birth.solarDate}T${time}:00`
  chart.inputCalendar = calendarType
  return { birth, chart }
}
function add(library, minute = 37, type = 'SOLAR', replacing) {
  const { birth, chart } = data(minute, type)
  return saveChart(library, birth, chart, replacing, now)
}
function memoryStorage(value = null) {
  return { value, getItem(key) { assert.equal(key, LOCAL_CHARTS_KEY); return this.value }, setItem(key, value) { assert.equal(key, LOCAL_CHARTS_KEY); this.value = value } }
}

test('stores a complete independent snapshot and canonical minute identity', () => {
  const input = data()
  const library = saveChart(emptyLibrary(), input.birth, input.chart, undefined, now)
  assert.equal(library.records.length, 1)
  assert.equal(library.activeId, chartId(input.birth))
  assert.equal(library.records[0].name, '出生记录 1')
  input.birth.time = '00:00'
  input.chart.pillarDetails[0].naYin = 'changed'
  assert.equal(library.records[0].birth.time, '13:37')
  assert.equal(library.records[0].chart.pillarDetails[0].naYin, '炉中火')
})

test('equivalent lunar input updates one slot and preserves its custom name', () => {
  const first = add(emptyLibrary())
  const named = renameChart(first, first.activeId, '我的排盘')
  const updated = add(named, 37, 'LUNAR')
  assert.equal(updated.records.length, 1)
  assert.equal(updated.records[0].name, '我的排盘')
  assert.equal(updated.records[0].birth.calendarType, 'LUNAR')
  assert.equal(updated.records[0].chart.inputCalendar, 'LUNAR')
})

test('three slots require an explicit replacement, never silently evict', () => {
  const library = add(add(add(emptyLibrary(), 37), 38), 39)
  assert.equal(library.records.length, 3)
  assert.equal(add(library, 40), null)
  assert.equal(add(library, 40, 'SOLAR', 'missing'), null)
  const next = add(library, 40, 'SOLAR', library.records[1].id)
  assert.equal(next.records.length, 3)
  assert.equal(next.records[0].birth.time, '13:37')
  assert.equal(next.records[1].birth.time, '13:40')
  assert.equal(next.records[2].birth.time, '13:39')
  assert.equal(next.activeId, next.records[1].id)
})

test('storage round-trip restores the active birth date and detailed result', () => {
  const library = add(add(emptyLibrary(), 37), 38)
  const storage = memoryStorage()
  assert.equal(writeLibrary(storage, library), '')
  assert.deepEqual(readLibrary(storage), { library, notice: '' })
  const draft = { ...library, activeId: null }
  writeLibrary(storage, draft)
  assert.equal(readLibrary(storage).library.activeId, null)
})

test('deleting an active record does not select another birth date or remove other data', () => {
  const library = add(add(emptyLibrary(), 37), 38)
  const next = deleteChart(library, library.activeId)
  assert.equal(next.activeId, null)
  assert.equal(next.records.length, 1)
  assert.equal(next.records[0].birth.time, '13:37')
  assert.equal(add(next, 39).records[1].name, '出生记录 2')
})

test('renaming trims and bounds the name, blank names do not erase it', () => {
  const library = add(emptyLibrary())
  const named = renameChart(library, library.activeId, '  家人  ')
  assert.equal(named.records[0].name, '家人')
  assert.equal(renameChart(named, named.activeId, '   '), named)
  assert.equal(renameChart(named, named.activeId, '名'.repeat(30)).records[0].name.length, 20)
})

test('corrupted, oversized and unsupported storage cannot break the page', () => {
  for (const raw of ['{broken', 'null', JSON.stringify({ version: 2, records: [] }), 'x'.repeat(256001)]) {
    const restored = readLibrary(memoryStorage(raw))
    assert.deepEqual(restored.library, emptyLibrary())
    assert.ok(restored.notice)
  }
})

test('skips incomplete, duplicated and extra slots while recovering valid records', () => {
  const one = add(emptyLibrary(), 37).records[0]
  const two = add(emptyLibrary(), 38).records[0]
  const three = add(emptyLibrary(), 39).records[0]
  const four = add(emptyLibrary(), 40).records[0]
  const result = readLibrary(memoryStorage(JSON.stringify({ version: 1, records: [{ bad: true }, one, one, two, three, four], activeId: 'missing' })))
  assert.equal(result.library.records.length, 3)
  assert.equal(result.library.activeId, null)
  assert.ok(result.notice)
})

test('rejects incomplete result schemas, mixed birth times and invalid detail values', () => {
  const good = add(emptyLibrary()).records[0]
  const cases = [
    record => { delete record.chart.pillarDetails },
    record => { record.chart.solarDate = '2000-01-01' },
    record => { record.birth.solarDate = '1986-02-30' },
    record => { record.chart.dayMaster.name = '甲' },
    record => { record.chart.pillarDetails[0].hiddenStems[0].qi = '余气' },
    record => { record.chart.elementCounts.木 = 99 },
    record => { record.chart.pillarDetails[0].branch.element = 'unknown' },
  ]
  for (const mutate of cases) {
    const record = structuredClone(good)
    mutate(record)
    const result = readLibrary(memoryStorage(JSON.stringify({ version: 1, records: [record], activeId: record.id })))
    assert.equal(result.library.records.length, 0)
    assert.ok(result.notice)
  }
})

test('disabled or full storage leaves calculation and in-memory records usable', () => {
  assert.ok(readLibrary().notice)
  const denied = { getItem() { throw new Error('SecurityError') }, setItem() { throw new Error('QuotaExceededError') } }
  assert.ok(readLibrary(denied).notice)
  const library = add(emptyLibrary())
  assert.ok(writeLibrary(denied, library))
  assert.ok(writeLibrary(undefined, library))
  assert.equal(library.records[0].chart.dayMaster.name, '癸')
})

test('can save reactive-style proxy snapshots without structuredClone failures', () => {
  const { birth, chart } = data()
  const proxy = new Proxy(chart, {})
  assert.equal(saveChart(emptyLibrary(), birth, proxy).records.length, 1)
})

const maleFortune = JSON.parse(readFileSync(new URL('./fixtures/bazi-fortune-male.json', import.meta.url), 'utf8'))
const femaleFortune = JSON.parse(readFileSync(new URL('./fixtures/bazi-fortune-female.json', import.meta.url), 'utf8'))
function genderInput(gender) {
  return { solarDate: '1986-05-29', time: '13:37', calendarType: 'SOLAR', gender }
}

test('same birth minute with different genders occupies distinct slots and survives reload', () => {
  const male = saveChart(emptyLibrary(), genderInput('MALE'), maleFortune)
  const both = saveChart(male, genderInput('FEMALE'), femaleFortune)
  assert.equal(both.records.length, 2)
  assert.notEqual(both.records[0].id, both.records[1].id)
  assert.equal(both.records[0].chart.fortune.direction, 'FORWARD')
  assert.equal(both.records[1].chart.fortune.direction, 'REVERSE')
  const storage = memoryStorage()
  assert.equal(writeLibrary(storage, both), '')
  assert.deepEqual(readLibrary(storage).library, both)
  assert.equal(readLibrary(storage).notice, '')
})

test('legacy snapshot remains available and explicit gender recalculation upgrades the same slot', () => {
  const old = add(emptyLibrary())
  const named = renameChart(old, old.activeId, '原记录')
  const storage = memoryStorage()
  writeLibrary(storage, named)
  const legacy = readLibrary(storage).library
  assert.equal(legacy.records[0].birth.gender, undefined)
  const upgraded = saveChart(legacy, genderInput('MALE'), maleFortune)
  assert.equal(upgraded.records.length, 1)
  assert.equal(upgraded.records[0].name, '原记录')
  assert.equal(upgraded.records[0].birth.gender, 'MALE')
  assert.equal(upgraded.activeId, chartId(genderInput('MALE')))
})

test('cached fortune rejects wrong gender, incomplete decades or broken annual chronology', () => {
  for (const mutate of [
    chart => { chart.gender = 'FEMALE' },
    chart => { chart.gender = false },
    chart => { chart.fortune = null },
    chart => { chart.fortune.decades.pop() },
    chart => { chart.fortune.decades[0].annualYears[0].year++ },
    chart => { chart.fortune.decades[0].annualYears[0].stem.element = 'unknown' },
  ]) {
    const chart = structuredClone(maleFortune)
    mutate(chart)
    assert.throws(() => saveChart(emptyLibrary(), genderInput('MALE'), chart), /不完整/)
  }
})

test('new traits round trip, upgrade the same slot, and reject damaged evidence', () => {
  const traits = JSON.parse(readFileSync(new URL('./fixtures/bazi-traits.json', import.meta.url), 'utf8'))
  const old = add(emptyLibrary())
  const { birth, chart } = data()
  Object.assign(chart, traits)
  const upgraded = saveChart(old, birth, chart, undefined, now)
  assert.equal(upgraded.records.length, 1)
  assert.equal(upgraded.activeId, old.activeId)
  const storage = memoryStorage()
  writeLibrary(storage, upgraded)
  assert.deepEqual(readLibrary(storage).library.records[0].chart.shenSha, traits.shenSha)
  for (const mutate of [c => c.pillarDetails[0].terrain = '无效', c => c.shenSha.hits[0].bases = [], c => c.shenSha.hits[0].targetBranch = '子', c => delete c.shenSha]) {
    const invalid = structuredClone(chart)
    mutate(invalid)
    assert.throws(() => saveChart(emptyLibrary(), birth, invalid, undefined, now))
  }
})

test('auxiliary palaces upgrade old snapshots in place, preserve names and round trip independently', () => {
  const palaces = JSON.parse(readFileSync(new URL('./fixtures/bazi-palaces.json', import.meta.url), 'utf8'))
  const old = renameChart(add(emptyLibrary()), chartId(data().birth), '我的排盘')
  const { birth, chart } = data()
  chart.auxiliaryPalaces = structuredClone(palaces)
  const updated = saveChart(old, birth, chart, undefined, now)
  assert.equal(updated.records.length, 1)
  assert.equal(updated.activeId, old.activeId)
  assert.equal(updated.records[0].name, '我的排盘')
  chart.auxiliaryPalaces.fetalOrigin.naYin = '修改'
  assert.deepEqual(updated.records[0].chart.auxiliaryPalaces, palaces)
  const storage = memoryStorage()
  writeLibrary(storage, updated)
  assert.deepEqual(readLibrary(storage).library.records[0].chart.auxiliaryPalaces, palaces)
  assert.equal(readLibrary(memoryStorage(JSON.stringify(old))).library.records.length, 1)
  assert.equal(readLibrary(memoryStorage(JSON.stringify(old))).library.records[0].chart.auxiliaryPalaces, undefined)
})

test('damaged auxiliary palaces are rejected at save and read time', () => {
  const palaces = JSON.parse(readFileSync(new URL('./fixtures/bazi-palaces.json', import.meta.url), 'utf8'))
  for (const mutate of [
    p => p.ruleVersion = 'unknown', p => delete p.ownSign, p => p.fetalOrigin = null,
    p => p.bodySign.ganZhi = '甲子', p => p.bodySign.branch.element = '未知',
    p => p.ownSign.stem.name = '错', p => p.ownSign.stem.tenGod = '', p => p.bodySign.naYin = '',
  ]) {
    const { birth, chart } = data()
    chart.auxiliaryPalaces = structuredClone(palaces)
    mutate(chart.auxiliaryPalaces)
    assert.throws(() => saveChart(emptyLibrary(), birth, chart), /不完整/)
    const raw = { version: 1, records: [{ id: chartId(birth), name: '损坏', birth, chart, savedAt: now }], activeId: chartId(birth) }
    const loaded = readLibrary(memoryStorage(JSON.stringify(raw)))
    assert.equal(loaded.library.records.length, 0)
    assert.match(loaded.notice, /损坏/)
  }
})
