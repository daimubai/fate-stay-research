import axios from 'axios'

export type ElementName = '木' | '火' | '土' | '金' | '水'
export interface StemDetail { name: string; element: ElementName; tenGod: string }
export interface HiddenStem extends StemDetail { qi: '本气' | '中气' | '余气' }
export interface PillarDetail {
  key: 'year' | 'month' | 'day' | 'hour'
  label: string
  ganZhi: string
  stem: StemDetail
  branch: { name: string; element: ElementName }
  hiddenStems: HiddenStem[]
  naYin: string
}

export interface BaziChart {
  dayMaster: { name: string; element: ElementName }
  pillarDetails: PillarDetail[]
  elementCounts: Record<ElementName, number>
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
