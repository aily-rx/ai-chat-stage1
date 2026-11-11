<script setup>
import { computed } from 'vue'

const props = defineProps({
  activeTab: {
    type: String,
    required: true,
  },
  status: {
    type: String,
    default: 'checking',
  },
})

defineEmits(['update:activeTab', 'refresh'])

const statusLabel = computed(() => {
  const labels = {
    checking: '检查中…',
    online: '后端正常',
    offline: '后端离线',
    mock: '演示模式',
  }

  return labels[props.status] || '未知状态'
})
</script>

<template>
  <header class="app-header">
    <div class="brand">
      <span class="brand-mark">AI</span>
      <div>
        <h1>AI 公文助手</h1>
        <p>阶段一 · 流式对话与结构化抽取</p>
      </div>
    </div>

    <nav class="tabs" aria-label="功能切换">
      <button
        type="button"
        :class="['tab-button', { active: activeTab === 'chat' }]"
        :aria-pressed="activeTab === 'chat'"
        @click="$emit('update:activeTab', 'chat')"
      >
        流式对话
      </button>
      <button
        type="button"
        :class="['tab-button', { active: activeTab === 'extract' }]"
        :aria-pressed="activeTab === 'extract'"
        @click="$emit('update:activeTab', 'extract')"
      >
        结构化抽取
      </button>
    </nav>

    <button
      type="button"
      class="status-button"
      :title="`当前状态：${statusLabel}，点击刷新`"
      @click="$emit('refresh')"
    >
      <span :class="['status-dot', `status-dot--${status}`]"></span>
      <span>{{ statusLabel }}</span>
    </button>
  </header>
</template>
