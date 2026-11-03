import axios from 'axios'

export type BirthGender = 'MALE' | 'FEMALE'
export interface AnnualYear { year: number; age: number; ganZhi: string; stem: StemDetail; branch: { name: string; element: ElementName }; naYin: string }
export interface DecadeFortune { index: number; ganZhi: string; startAge: number; endAge: number; startYear: number; endYear: number; stem: StemDetail; branch: { name: string; element: ElementName }; naYin: string; annualYears: AnnualYear[] }
export interface FortuneTimeline { ruleVersion: string; direction: 'FORWARD' | 'REVERSE'; startTime: string; startInterval: { years: number; months: number; days: number; hours: number; minutes: number }; ageConvention: 'YEAR_DIFF_PLUS_ONE'; decades: DecadeFortune[] }

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
  gender?: BirthGender | null
  fortune?: FortuneTimeline | null
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
  | { calendarType: 'SOLAR'; birthDate: string; birthTime: string; gender: BirthGender }
  | { calendarType: 'LUNAR'; lunarDate: { year: number; month: number; day: number; leapMonth: boolean }; birthTime: string; gender: BirthGender }

export async function calculateBazi(input: BirthInput): Promise<BaziChart> {
  const { data } = await axios.post<BaziChart>('/api/v1/bazi/charts', input, { timeout: 10000 })
  return data
}

export interface FlowMonth {
  index: number; name: string; term: string; startTime: string; endTime: string
  ganZhi: string; stem: StemDetail; branch: { name: string; element: ElementName }; naYin: string
}
export interface FlowDay {
  date: string; lunarDate: BaziChart['lunarDate']; startTime: string; endTime: string; partial: boolean
  ganZhi: string; stem: StemDetail; branch: { name: string; element: ElementName }; naYin: string
}
export interface FlowMonths { year: number; yearGanZhi: string; ruleVersion: string; dayMaster: BaziChart['dayMaster']; months: FlowMonth[] }
export interface FlowDays { year: number; monthIndex: number; ruleVersion: string; dayMaster: BaziChart['dayMaster']; month: FlowMonth; days: FlowDay[] }
export interface FlowInput { birthDate: string; birthTime: string; year: number }
export async function getFlowMonths(input: FlowInput, signal?: AbortSignal): Promise<FlowMonths> {
  return (await axios.post<FlowMonths>('/api/v1/bazi/flow-months', input, { timeout: 10000, signal })).data
}
export async function getFlowDays(input: FlowInput & { monthIndex: number }, signal?: AbortSignal): Promise<FlowDays> {
  return (await axios.post<FlowDays>('/api/v1/bazi/flow-days', input, { timeout: 10000, signal })).data
}

export interface FlowHour {
  index: number; name: string; nominalStartTime: string; nominalEndTime: string
  startTime: string | null; endTime: string | null; included: boolean; partial: boolean
  ganZhi: string; stem: StemDetail; branch: { name: string; element: ElementName }; naYin: string
}
export interface FlowHours {
  year: number; monthIndex: number; date: string; ruleVersion: string
  dayMaster: BaziChart['dayMaster']; day: FlowDay; hours: FlowHour[]
}
export async function getFlowHours(input: FlowInput & { monthIndex: number; date: string }, signal?: AbortSignal): Promise<FlowHours> {
  return (await axios.post<FlowHours>('/api/v1/bazi/flow-hours', input, { timeout: 10000, signal })).data
}
