import { API_BASE, USE_MOCK } from './config'

export async function checkHealth() {
  if (USE_MOCK) {
    return { mode: 'mock' }
  }

  const response = await fetch(`${API_BASE}/health`, {
    headers: { Accept: 'application/json' },
  })

  let payload = null
  try {
    payload = await response.json()
  } catch {
    payload = null
  }

  if (!response.ok) {
    const error = new Error(payload?.message || `健康检查失败（HTTP ${response.status}）`)
    error.code = payload?.code ?? response.status
    error.traceId = payload?.traceId
    throw error
  }

  if (!payload || payload.code !== 0) {
    const error = new Error(payload?.message || '健康检查失败')
    error.code = payload?.code
    error.traceId = payload?.traceId
    throw error
  }

  return { mode: 'real', traceId: payload.traceId }
}
