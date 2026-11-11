<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  styleValue: {
    type: String,
    default: 'formal',
  },
  isStreaming: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'update:modelValue',
  'update:styleValue',
  'send',
  'stop',
  'example',
])

const localValue = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const canSubmit = computed(() => (
  localValue.value.trim().length > 0
  && localValue.value.length <= 2000
  && !props.isStreaming
))

function submit() {
  if (!canSubmit.value) {
    return
  }

  emit('send', localValue.value.trim())
}

function handleKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
    event.preventDefault()
    submit()
  }
}
</script>

<template>
  <div class="chat-composer">
    <div class="composer__top">
      <label class="field-label">
        <span>回答风格</span>
        <select
          :value="styleValue"
          class="select-control"
          @change="$emit('update:styleValue', $event.target.value)"
        >
          <option value="formal">正式</option>
          <option value="concise">简洁</option>
        </select>
      </label>

      <button type="button" class="link-button" @click="$emit('example')">
        填入示例
      </button>

      <span :class="['char-count', { 'char-count--over': modelValue.length > 2000 }]">
        {{ modelValue.length }} / 2000
      </span>
    </div>

    <div class="composer__row">
      <textarea
        v-model="localValue"
        class="chat-input"
        rows="3"
        placeholder="输入写作要求，例如：写一份关于开展安全生产检查的通知……"
        aria-label="输入写作要求"
        @keydown="handleKeydown"
      ></textarea>

      <button
        v-if="!isStreaming"
        type="button"
        class="primary-button"
        :disabled="!canSubmit"
        @click="submit"
      >
        发送
      </button>
      <button
        v-else
        type="button"
        class="danger-button"
        @click="$emit('stop')"
      >
        停止
      </button>
    </div>

    <div class="composer__hint">
      <span>Enter 发送，Shift + Enter 换行</span>
      <span v-if="modelValue.length > 2000" class="hint-error">超出 2000 字符</span>
    </div>
  </div>
</template>
