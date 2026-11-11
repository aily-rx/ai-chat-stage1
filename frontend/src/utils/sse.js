export function parseSseBlock(block) {
  if (!block || !block.trim()) {
    return null
  }

  let event = 'message'
  const dataLines = []

  for (const rawLine of block.split(/\r?\n/)) {
    if (!rawLine || rawLine.startsWith(':')) {
      continue
    }

    const separator = rawLine.indexOf(':')
    const field = separator === -1 ? rawLine : rawLine.slice(0, separator)
    let value = separator === -1 ? '' : rawLine.slice(separator + 1)

    if (value.startsWith(' ')) {
      value = value.slice(1)
    }

    if (field === 'event') {
      event = value
    } else if (field === 'data') {
      dataLines.push(value)
    }
  }

  if (!dataLines.length) {
    return null
  }

  const dataText = dataLines.join('\n')
  let data

  try {
    data = JSON.parse(dataText)
  } catch {
    data = { raw: dataText }
  }

  return { event, data }
}
