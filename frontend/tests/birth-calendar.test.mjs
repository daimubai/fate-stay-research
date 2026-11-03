import test from 'node:test'
import assert from 'node:assert/strict'
import { daysFor, monthsFor, normalizeParts, partsForDate, toSolarDate, lunarLabel, toBirthInput } from '../src/lib/birth-calendar.ts'

test('leap month choices include only actual leap months, in calendar order', () => {
  const months = monthsFor('LUNAR', 2023).map(m => m.value)
  assert.deepEqual(months.slice(0, 4), [1, 2, -2, 3])
  assert.equal(monthsFor('LUNAR', 2024).some(m => m.value < 0), false)
  assert.equal(daysFor('LUNAR', 2023, -2).length, 29)
  assert.equal(daysFor('LUNAR', 2023, 2).length, 30)
})

test('switching calendar preserves the solar date and explicit leap month', () => {
  const parts = partsForDate('2023-03-22', 'LUNAR')
  assert.deepEqual(parts, { year: 2023, month: -2, day: 1 })
  assert.equal(toSolarDate('LUNAR', parts), '2023-03-22')
  assert.equal(lunarLabel('2023-03-22'), '2023年闰二月初一')
  assert.equal(toSolarDate('LUNAR', { year: 2023, month: 2, day: 1 }), '2023-02-20')
})

test('changing year/month clamps day, missing leap month falls back to ordinary month', () => {
  assert.deepEqual(normalizeParts('SOLAR', { year: 2023, month: 2, day: 31 }), { year: 2023, month: 2, day: 28 })
  assert.deepEqual(normalizeParts('SOLAR', { year: 2024, month: 2, day: 31 }), { year: 2024, month: 2, day: 29 })
  assert.deepEqual(normalizeParts('LUNAR', { year: 2023, month: -2, day: 30 }), { year: 2023, month: -2, day: 29 })
  assert.deepEqual(normalizeParts('LUNAR', { year: 2024, month: -2, day: 30 }), { year: 2024, month: 2, day: 30 })
})

test('lunar choices at both range edges remain within supported solar dates', () => {
  for (const year of [1899, 2100]) {
    for (const month of monthsFor('LUNAR', year)) {
      for (const day of daysFor('LUNAR', year, month.value)) {
        const solar = toSolarDate('LUNAR', { year, month: month.value, day: day.value })
        assert.ok(solar >= '1900-01-01' && solar <= '2100-12-31')
      }
    }
  }
  for (const solar of ['1900-01-01', '2100-12-31']) {
    assert.equal(toSolarDate('LUNAR', partsForDate(solar, 'LUNAR')), solar)
  }
})

test('leap-month API payload uses positive month plus explicit leap flag', () => {
  assert.deepEqual(toBirthInput({ solarDate: '2023-03-22', time: '13:37', calendarType: 'LUNAR' }), {
    calendarType: 'LUNAR', birthTime: '13:37', lunarDate: { year: 2023, month: 2, day: 1, leapMonth: true },
  })
  assert.deepEqual(toBirthInput({ solarDate: '2023-03-22', time: '13:37', calendarType: 'SOLAR' }), {
    calendarType: 'SOLAR', birthDate: '2023-03-22', birthTime: '13:37',
  })
})
