import axios from 'axios'

export interface BaziChart {
  inputCalendar: 'SOLAR' | 'LUNAR'
  solarDate: string
  lunarDate: { year: number; month: number; day: number; leapMonth: boolean; display: string }
  birthDateTime: string
  timeZone: string
  precision: string
  engineVersion: string
  ruleVersion: string
  pillars: { year: string; month: string; day: string; hour: string }
}

export type BirthInput =
  | { calendarType: 'SOLAR'; birthDate: string; birthTime: string }
  | { calendarType: 'LUNAR'; lunarDate: { year: number; month: number; day: number; leapMonth: boolean }; birthTime: string }

export async function calculateBazi(input: BirthInput): Promise<BaziChart> {
  const { data } = await axios.post<BaziChart>('/api/v1/bazi/charts', input, { timeout: 10000 })
  return data
}
