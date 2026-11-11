function createTraceId() {
  return `mock-${Math.random().toString(16).slice(2, 10)}`
}

function sleep(ms, signal) {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(new DOMException('Aborted', 'AbortError'))
      return
    }

    const onAbort = () => {
      clearTimeout(timer)
      reject(new DOMException('Aborted', 'AbortError'))
    }

    const timer = setTimeout(() => {
      signal?.removeEventListener('abort', onAbort)
      resolve()
    }, ms)

    signal?.addEventListener('abort', onAbort, { once: true })
  })
}

function formalAnswer() {
  return `## 关于开展安全生产检查的通知

各部门：

为进一步压实安全生产责任，防范化解安全风险，经研究，决定开展安全生产专项检查。现将有关事项通知如下：

1. **检查范围**：办公区域、机房、仓库及外包作业现场。
2. **检查内容**：消防设施、用电安全、设备运行、应急预案和值班记录。
3. **时间安排**：11 月 10 日至 11 月 20 日，分部门自查和集中检查两个阶段。
4. **工作要求**：各部门于 11 月 8 日前完成自查，发现问题立即整改；重大隐患及时上报。

请各部门高度重视，指定专人负责，确保检查取得实效。

**XX办公室**  
2026 年 11 月 1 日`
}

function conciseAnswer() {
  return `本次安全生产检查覆盖办公区域、机房、仓库和外包现场，重点检查消防、用电、设备及应急预案。各部门须于 11 月 8 日前完成自查，11 月 10 日至 20 日接受集中检查，发现问题立即整改并上报。`
}

export async function mockStreamChat({ message, style, signal, onEvent }) {
  const traceId = createTraceId()
  const answer = style === 'concise' ? conciseAnswer() : formalAnswer()
  const chunks = []
  const characters = Array.from(answer)

  for (let index = 0; index < characters.length; index += 8) {
    chunks.push(characters.slice(index, index + 8).join(''))
  }

  await sleep(300 + Math.random() * 150, signal)

  for (let index = 0; index < chunks.length; index += 1) {
    if (signal?.aborted) {
      throw new DOMException('Aborted', 'AbortError')
    }

    onEvent({ event: 'message', data: { delta: chunks[index] } })

    if (message.includes('[[error]]') && index === 2) {
      onEvent({
        event: 'error',
        data: {
          code: 5001,
          message: '模型调用失败（演示）',
          traceId,
        },
      })
      return
    }

    await sleep(28 + Math.random() * 34, signal)
  }

  onEvent({ event: 'done', data: { traceId } })
}
