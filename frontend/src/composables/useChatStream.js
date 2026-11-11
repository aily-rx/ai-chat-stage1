import { computed, ref } from 'vue'
import { streamChat } from '../api/chat'

function createWelcomeMessage() {
  return {
    id: 'welcome',
    role: 'assistant',
    content: '你好，我是公文写作助手。请描述你要写作的内容，或切换到“结构化抽取”处理一段公文。',
    status: 'done',
    traceId: '',
    firstTokenMs: null,
    error: null,
  }
}

export function useChatStream() {
  const messages = ref([createWelcomeMessage()])
  const isStreaming = ref(false)
  const style = ref('formal')
  const error = ref(null)
  const traceId = ref('')
  const firstTokenMs = ref(null)
  const totalMs = ref(null)

  let controller = null
  let sequence = 0

  const canSend = computed(() => !isStreaming.value)

  function nextId(prefix) {
    sequence += 1
    return `${prefix}-${Date.now()}-${sequence}`
  }

  async function send(rawMessage) {
    const message = String(rawMessage || '').trim()

    if (!message || isStreaming.value) {
      return false
    }

    error.value = null
    traceId.value = ''
    firstTokenMs.value = null
    totalMs.value = null

    messages.value.push({
      id: nextId('user'),
      role: 'user',
      content: message,
      status: 'done',
      traceId: '',
      firstTokenMs: null,
      error: null,
    })

    messages.value.push({
      id: nextId('assistant'),
      role: 'assistant',
      content: '',
      status: 'streaming',
      traceId: '',
      firstTokenMs: null,
      error: null,
    })

    const assistant = messages.value[messages.value.length - 1]
    const activeController = new AbortController()
    controller = activeController
    isStreaming.value = true

    const startedAt = performance.now()
    let firstDeltaReceived = false
    let terminalReceived = false

    try {
      await streamChat({
        message,
        style: style.value,
        signal: activeController.signal,
        onEvent: (event) => {
          if (event.event === 'message' && typeof event.data?.delta === 'string') {
            if (!firstDeltaReceived) {
              firstDeltaReceived = true
              assistant.firstTokenMs = Math.round(performance.now() - startedAt)
              firstTokenMs.value = assistant.firstTokenMs
            }

            assistant.content += event.data.delta
            return
          }

          if (event.event === 'done') {
            terminalReceived = true
            assistant.status = 'done'
            assistant.traceId = event.data?.traceId || ''
            traceId.value = assistant.traceId
            return
          }

          if (event.event === 'error') {
            terminalReceived = true
            assistant.status = 'error'
            assistant.error = {
              code: event.data?.code ?? 'UNKNOWN',
              message: event.data?.message || '生成失败',
              traceId: event.data?.traceId || '',
            }
            assistant.traceId = assistant.error.traceId
            error.value = assistant.error
            traceId.value = assistant.error.traceId
          }
        },
      })

      if (!terminalReceived && assistant.status === 'streaming') {
        assistant.status = 'error'
        assistant.error = {
          code: 'STREAM_INCOMPLETE',
          message: '连接提前结束，未收到完成事件',
          traceId: '',
        }
        error.value = assistant.error
      }
    } catch (caught) {
      if (caught.name === 'AbortError') {
        assistant.status = 'stopped'
      } else {
        assistant.status = 'error'
        assistant.error = {
          code: caught.code ?? 'NETWORK',
          message: caught.message || '请求失败',
          traceId: caught.traceId || '',
        }
        assistant.traceId = assistant.error.traceId
        error.value = assistant.error
        traceId.value = assistant.error.traceId
      }
    } finally {
      totalMs.value = Math.round(performance.now() - startedAt)
      isStreaming.value = false
      controller = null
    }

    return true
  }

  function stop() {
    controller?.abort()
  }

  function reset() {
    if (isStreaming.value) {
      return
    }

    messages.value = [createWelcomeMessage()]
    error.value = null
    traceId.value = ''
    firstTokenMs.value = null
    totalMs.value = null
  }

  return {
    messages,
    isStreaming,
    style,
    error,
    traceId,
    firstTokenMs,
    totalMs,
    canSend,
    send,
    stop,
    reset,
  }
}
