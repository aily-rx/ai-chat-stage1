import { API_BASE, USE_MOCK } from './config'
import { mockExtract } from '../mock/mockExtract'

export async function extractFields(text) {
  if (USE_MOCK) {
    return mockExtract(text)
  }

  const response = await fetch(`${API_BASE}/extract`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text }),
  })

  let payload = null
  try {
    payload = await response.json()
  } catch {
    payload = null
  }

  if (!response.ok) {
    const error = new Error(payload?.message || `请求失败（HTTP ${response.status}）`)
    error.code = payload?.code ?? response.status
    error.traceId = payload?.traceId
    throw error
  }

  if (!payload || payload.code !== 0) {
    const error = new Error(payload?.message || '抽取失败')
    error.code = payload?.code
    error.traceId = payload?.traceId
    throw error
  }

  return payload.data
}
