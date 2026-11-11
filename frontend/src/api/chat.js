import { API_BASE, USE_MOCK } from './config'
import { parseSseBlock } from '../utils/sse'
import { mockStreamChat } from '../mock/mockChat'

export async function streamChat({ message, style, signal, onEvent }) {
  if (USE_MOCK) {
    return mockStreamChat({ message, style, signal, onEvent })
  }

  const response = await fetch(`${API_BASE}/chat/stream`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, style }),
    signal,
  })

  if (!response.ok) {
    let payload = null
    try {
      payload = await response.json()
    } catch {
      payload = null
    }

    const error = new Error(payload?.message || `请求失败（HTTP ${response.status}）`)
    error.code = payload?.code ?? response.status
    error.traceId = payload?.traceId
    throw error
  }

  if (!response.body) {
    throw new Error('当前浏览器不支持流式响应')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }

      buffer += decoder.decode(value, { stream: true })
      const blocks = buffer.split(/\r?\n\r?\n/)
      buffer = blocks.pop() ?? ''

      for (const block of blocks) {
        const event = parseSseBlock(block)
        if (event) {
          onEvent(event)
        }
      }
    }

    if (buffer.trim()) {
      const event = parseSseBlock(buffer)
      if (event) {
        onEvent(event)
      }
    }
  } finally {
    reader.releaseLock()
  }
}
