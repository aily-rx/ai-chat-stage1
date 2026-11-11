<script setup>
import { computed } from 'vue'

const props = defineProps({
  result: {
    type: Object,
    default: null,
  },
  rawJson: {
    type: String,
    default: '',
  },
  error: {
    type: Object,
    default: null,
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const fields = [
  { key: 'docType', label: '文种' },
  { key: 'title', label: '标题' },
  { key: 'recipient', label: '主送单位' },
  { key: 'signature', label: '落款' },
  { key: 'date', label: '成文日期' },
]

const statusBadge = computed(() => {
  if (props.loading) return '抽取中'
  if (props.error) return '抽取失败'
  if (props.result) return '抽取成功'
  return '等待抽取'
})
</script>

<template>
  <div class="panel extract-result-panel" aria-live="polite">
    <div class="panel__header">
      <div>
        <h3>抽取结果</h3>
        <p class="panel-hint">固定字段：docType / title / recipient / signature / date</p>
      </div>
      <span
        :class="[
          'badge',
          loading ? 'badge--pending' : '',
          error ? 'badge--danger' : '',
          result ? 'badge--success' : '',
        ]"
      >
        {{ statusBadge }}
      </span>
    </div>

    <div v-if="loading" class="empty-state">
      正在调用模型抽取，请稍候…
    </div>

    <div v-else-if="error" class="error-card" role="alert">
      <strong>错误码 {{ error.code }}</strong>
      <p>{{ error.message }}</p>
      <span v-if="error.traceId" class="mono">traceId: {{ error.traceId }}</span>
    </div>

    <div v-else-if="result" class="result-content">
      <table class="result-table">
        <tbody>
          <tr v-for="field in fields" :key="field.key">
            <th>{{ field.label }}</th>
            <td>{{ result[field.key] || '未识别' }}</td>
          </tr>
        </tbody>
      </table>

      <details class="raw-json" open>
        <summary>原始 JSON</summary>
        <pre>{{ rawJson }}</pre>
      </details>
    </div>

    <div v-else class="empty-state">
      粘贴公文后点击“开始抽取”，五个固定字段会显示在这里。
    </div>
  </div>
</template>
