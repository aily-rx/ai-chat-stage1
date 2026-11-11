<script setup>
import { computed } from 'vue'
import { renderMarkdown } from '../utils/markdown'

const props = defineProps({
  message: {
    type: Object,
    required: true,
  },
})

const isUser = computed(() => props.message.role === 'user')
const rendered = computed(() => renderMarkdown(props.message.content))
const hasBody = computed(() => Boolean(props.message.content) || props.message.status === 'streaming')

const statusText = computed(() => {
  if (props.message.status === 'streaming') return '生成中…'
  if (props.message.status === 'stopped') return '已停止'
  if (props.message.status === 'error') return '生成失败'
  return ''
})
</script>

<template>
  <article :class="['message', `message--${message.role}`, `message--${message.status}`]">
    <div class="message__meta">
      <span>{{ isUser ? '你' : '助手' }}</span>
      <span v-if="statusText" class="message__status">{{ statusText }}</span>
    </div>

    <div v-if="hasBody" class="message__bubble" :class="{ 'message__markdown': !isUser }">
      <template v-if="isUser">{{ message.content }}</template>
      <div v-else class="message__content" v-html="rendered"></div>
      <span
        v-if="message.status === 'streaming'"
        class="stream-cursor"
        aria-hidden="true"
      ></span>
    </div>

    <div v-if="message.error" class="message__error" role="alert">
      <strong>错误码 {{ message.error.code }}</strong>
      <span>{{ message.error.message }}</span>
      <span v-if="message.error.traceId">traceId: {{ message.error.traceId }}</span>
    </div>

    <div v-if="!isUser" class="message__footer">
      <span v-if="message.firstTokenMs">首字 {{ (message.firstTokenMs / 1000).toFixed(2) }}s</span>
      <span v-if="message.traceId">traceId: {{ message.traceId }}</span>
      <span v-if="message.status === 'stopped'">已停止生成</span>
    </div>
  </article>
</template>
