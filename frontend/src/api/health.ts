import axios from 'axios'

export interface HealthResponse {
  status: string
  application: string
}

export async function getHealth(): Promise<HealthResponse> {
  const { data } = await axios.get<HealthResponse>('/api/v1/health', { timeout: 5000 })
  if (data.status !== 'UP' || data.application !== 'fate-stay-research') {
    throw new Error('Unexpected health response')
  }
  return data
}
