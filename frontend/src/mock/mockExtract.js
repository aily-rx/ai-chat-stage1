function createTraceId() {
  return `mock-${Math.random().toString(16).slice(2, 10)}`
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export async function mockExtract(text) {
  await sleep(650)

  if (!text || !text.trim()) {
    const error = new Error('text 不能为空')
    error.code = 4000
    error.traceId = createTraceId()
    throw error
  }

  if (text.includes('[[fail]]')) {
    const error = new Error('模型输出无法解析，已重试 1 次')
    error.code = 5003
    error.traceId = createTraceId()
    throw error
  }

  return {
    docType: '通知',
    title: '关于开展安全生产检查的通知',
    recipient: '各部门',
    signature: 'XX办公室',
    date: '2026-11-01',
  }
}
