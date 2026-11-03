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
