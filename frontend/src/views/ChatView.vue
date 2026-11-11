<script setup>
import { nextTick, ref, watch } from 'vue'
import ChatMessage from '../components/ChatMessage.vue'
import ChatComposer from '../components/ChatComposer.vue'
import { useChatStream } from '../composables/useChatStream'

const {
  messages,
  isStreaming,
  style,
  error,
  traceId,
  firstTokenMs,
  totalMs,
  send,
  stop,
  reset,
} = useChatStream()

const draft = ref('')
const listRef = ref(null)

const examples = [
  '写一份关于开展安全生产检查的通知，包含检查目的、范围、时间安排和工作要求。',
  '用正式风格起草一份部门季度工作总结的结尾部分。',
]

function handleSend(text) {
  const accepted = send(text)
  if (accepted) {
    draft.value = ''
  }
}

function useExample(index) {
  draft.value = examples[index] || examples[0]
}

function setStyle(value) {
  style.value = value
}

watch(
  messages,
  async () => {
    await nextTick()
    const list = listRef.value
    if (list) {
      list.scrollTop = list.scrollHeight
    }
  },
  { deep: true },
)
</script>

<template>
  <section class="chat-view">
    <div class="chat-toolbar">
      <div>
        <h2>流式对话</h2>
        <p class="subtitle">同一问题切换“正式/简洁”，观察 Prompt 变量对输出的影响。</p>
      </div>
      <button
        type="button"
        class="ghost-button"
        :disabled="isStreaming"
        @click="reset"
      >
        清空对话
      </button>
    </div>

    <div ref="listRef" class="chat-list" aria-live="polite">
      <ChatMessage v-for="message in messages" :key="message.id" :message="message" />
    </div>

    <div v-if="error" class="error-banner" role="alert">
      <strong>请求失败</strong>
      <span>{{ error.message }}</span>
      <span v-if="error.code">错误码 {{ error.code }}</span>
      <span v-if="error.traceId">traceId: {{ error.traceId }}</span>
    </div>

    <div class="prompt-hints">
      <button
        v-for="(example, index) in examples"
        :key="example"
        type="button"
        class="prompt-chip"
        @click="useExample(index)"
      >
        {{ index === 0 ? '安全生产检查' : '季度总结结尾' }}
      </button>
    </div>

    <ChatComposer
      v-model="draft"
      :style-value="style"
      :is-streaming="isStreaming"
      @update:style-value="setStyle"
      @send="handleSend"
      @stop="stop"
      @example="useExample(0)"
    />

    <div class="chat-meta">
      <span v-if="firstTokenMs">首字 {{ (firstTokenMs / 1000).toFixed(2) }}s</span>
      <span v-if="totalMs">总耗时 {{ (totalMs / 1000).toFixed(2) }}s</span>
      <span v-if="traceId">traceId: {{ traceId }}</span>
      <span v-if="!firstTokenMs && !traceId">等待发送…</span>
    </div>
  </section>
</template>
