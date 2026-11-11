<script setup>
import { ref } from 'vue'
import ExtractResult from '../components/ExtractResult.vue'
import { extractFields } from '../api/extract'

const normalSample = `关于开展安全生产检查的通知

各部门：

为进一步压实安全生产责任，防范化解安全风险，经研究，决定开展安全生产专项检查。现将有关事项通知如下：

一、检查范围
办公区域、机房、仓库及外包作业现场。

二、检查内容
消防设施、用电安全、设备运行、应急预案和值班记录。

三、时间安排
2026年11月10日至11月20日。

四、工作要求
各部门于11月8日前完成自查，发现问题立即整改。

XX办公室
2026年11月1日`

const failureSample = `关于开展安全生产检查的通知

各部门：

请各单位于11月8日前完成自查，发现问题立即整改。

[[fail]]

XX办公室
2026年11月1日`

const text = ref(normalSample)
const loading = ref(false)
const result = ref(null)
const error = ref(null)
const rawJson = ref('')

function clearOutput() {
  result.value = null
  error.value = null
  rawJson.value = ''
}

function loadSample(kind) {
  text.value = kind === 'fail' ? failureSample : normalSample
  clearOutput()
}

function clearAll() {
  if (loading.value) {
    return
  }

  text.value = ''
  clearOutput()
}

async function runExtract() {
  if (loading.value) {
    return
  }

  const value = text.value.trim()

  if (!value) {
    error.value = {
      code: 4000,
      message: '请输入公文文本',
      traceId: '',
    }
    result.value = null
    rawJson.value = ''
    return
  }

  if (value.length > 10000) {
    error.value = {
      code: 4000,
      message: '公文文本不能超过 10000 字符',
      traceId: '',
    }
    result.value = null
    rawJson.value = ''
    return
  }

  loading.value = true
  clearOutput()

  try {
    const data = await extractFields(value)
    result.value = data
    rawJson.value = JSON.stringify(data, null, 2)
  } catch (caught) {
    error.value = {
      code: caught.code ?? 'NETWORK',
      message: caught.message || '抽取失败',
      traceId: caught.traceId || '',
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="extract-view">
    <div class="extract-toolbar">
      <div>
        <h2>结构化抽取</h2>
        <p class="subtitle">粘贴公文文本，抽取文种、标题、主送、落款和日期五个固定字段。</p>
      </div>
      <button
        type="button"
        class="ghost-button"
        :disabled="loading"
        @click="clearAll"
      >
        清空
      </button>
    </div>

    <div class="extract-layout">
      <div class="panel">
        <div class="panel__header">
          <div>
            <h3>公文文本</h3>
            <p class="panel-hint">支持粘贴通知、请示、报告等公文片段。</p>
          </div>
          <span :class="['char-count', { 'char-count--over': text.length > 10000 }]">
            {{ text.length }} / 10000
          </span>
        </div>

        <textarea
          v-model="text"
          class="extract-textarea"
          rows="12"
          placeholder="在这里粘贴公文原文……"
          aria-label="公文文本"
        ></textarea>

        <div class="toolbar-row">
          <button type="button" class="ghost-button" @click="loadSample('normal')">
            加载正常样例
          </button>
          <button type="button" class="ghost-button" @click="loadSample('fail')">
            加载失败样例
          </button>
          <button
            type="button"
            class="primary-button"
            :disabled="loading"
            @click="runExtract"
          >
            {{ loading ? '抽取中…' : '开始抽取' }}
          </button>
        </div>

        <p class="panel-hint">
          失败样例包含 <code>[[fail]]</code>。演示模式会返回 5003；连接真实后端后，以模型实际解析结果为准。
        </p>
      </div>

      <ExtractResult
        :result="result"
        :raw-json="rawJson"
        :error="error"
        :loading="loading"
      />
    </div>
  </section>
</template>
