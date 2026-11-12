# 阶段一接口文档

| 项目 | 内容 |
|---|---|
| 文档版本 | v1.0 |
| 适用范围 | W1–W2（阶段一） |
| 后端地址 | `http://localhost:8080` |
| 前端开发地址 | `http://localhost:5173` |
| 协议 | HTTP + JSON；流式接口使用 SSE（`text/event-stream`） |
| 主链路 | Vue3 → Spring Boot → DeepSeek API；Dify 不在主链路 |

> 本文档只定义阶段一接口。登录鉴权、会话历史、RAG、Dify 接入和私有 vLLM 等接口属于后续阶段，不在本文档范围内。

## 一、通用约定

### 1.1 请求格式

- 请求体统一使用 `Content-Type: application/json; charset=UTF-8`。
- 前端通过 Vite 代理访问 `/api`，开发环境不需要单独配置 CORS。
- 真实 DeepSeek Key 只保存在后端 `.env`，前端不得持有，也不得出现在日志和响应中。

### 1.2 非流式响应格式

除 SSE 接口外，统一返回：

```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "traceId": "a1b2c3d4"
}
```

字段说明：

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | integer | 业务码，`0` 表示成功 |
| `message` | string | 提示信息；失败时为可读原因 |
| `data` | object / string / null | 业务数据 |
| `traceId` | string | 本次请求的追踪 ID，同时写入响应头 `X-Trace-Id` |

### 1.3 错误码

| 业务码 | HTTP 状态 | 含义 | 前端处理 |
|---|---|---|---|
| `0` | 200 | 成功 | 正常展示 |
| `4000` | 400 | 参数校验失败 | 提示用户修改输入 |
| `5000` | 500 | 未处理的服务端异常 | 展示通用错误和 traceId |
| `5001` | 200 | 模型调用失败、超时或网络异常 | 展示“模型调用失败”和 traceId |
| `5003` | 200 | 结构化抽取结果无法解析，且重试一次后仍失败 | 展示“解析失败，请重试”和 traceId |

说明：

- 非流式接口的“业务失败”统一使用 HTTP 200，前端以 `code` 判断；参数格式错误使用 HTTP 400，未处理异常使用 HTTP 500。
- 流式接口一旦建立 SSE 连接，后续异常通过 `event: error` 返回，不再依赖 HTTP 状态码。

---

## 二、健康检查

### 2.1 基本信息

| 项目 | 内容 |
|---|---|
| 方法 | `GET` |
| 路径 | `/api/health` |
| 用途 | 检查后端是否启动，供演示和验收使用 |
| 鉴权 | 无 |

### 2.2 响应示例

```json
{
  "code": 0,
  "message": "ok",
  "data": "ok",
  "traceId": "a1b2c3d4"
}
```

### 2.3 验收

```bash
curl http://localhost:8080/api/health
```

返回 HTTP 200，且 `code=0`。

---

## 三、流式对话

### 3.1 基本信息

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| 路径 | `/api/chat/stream` |
| 用途 | 接收用户问题，流式返回模型增量内容 |
| 响应类型 | `text/event-stream` |
| 鉴权 | 阶段一为本地单用户，暂不鉴权 |

### 3.2 请求体

```json
{
  "message": "写一份关于开展安全生产检查的通知",
  "style": "formal"
}
```

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| `message` | string | 是 | 去除首尾空白后 1–2000 字符 | 用户问题或写作要求 |
| `style` | string | 是 | `formal` 或 `concise` | Prompt 变量，分别对应正式、简洁风格 |

校验失败时返回 HTTP 400：

```json
{
  "code": 4000,
  "message": "message 不能为空",
  "data": null,
  "traceId": "a1b2c3d4"
}
```

### 3.3 SSE 事件

连接建立后，服务端按顺序发送 `message` 增量事件，最后必须发送一个终止事件。

#### 增量事件

```text
event: message
data: {"delta":"关于"}

event: message
data: {"delta":"开展"}

event: message
data: {"delta":"安全生产检查"}

```

`delta` 为本次新增的文本片段，前端按到达顺序追加。

#### 正常结束

```text
event: done
data: {"traceId":"a1b2c3d4"}

```

#### 异常结束

```text
event: error
data: {"code":5001,"message":"模型调用失败","traceId":"a1b2c3d4"}

```

### 3.4 事件约定

| 项目 | 约定 |
|---|---|
| 事件终止 | 每次请求必须以 `done` 或 `error` 之一结束 |
| 增量事件 | 只使用 `message`，`data` 为 JSON |
| 空增量 | 可以不发送，但不得发送空字符串导致前端误判 |
| 错误信息 | 不返回上游原始堆栈、模型 Key 或内部地址 |
| traceId | `done` 和 `error` 必须携带，便于前后端排查 |
| 客户端中断 | 用户点击停止时前端取消请求；后端应停止向上游读取并释放资源 |

### 3.5 curl 示例

```bash
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message":"用三句话介绍RAG","style":"formal"}'
```

预期看到多条 `event: message`，最后收到 `event: done`。

### 3.6 前端接入方式

不能使用 `EventSource`，因为 `EventSource` 只支持 GET，而本接口需要 POST。使用 `fetch` + `ReadableStream`：

```js
const res = await fetch('/api/chat/stream', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ message, style }),
  signal
});

const reader = res.body.getReader();
const decoder = new TextDecoder();
let buffer = '';

while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  buffer += decoder.decode(value, { stream: true });

  const parts = buffer.split('\n\n');
  buffer = parts.pop() ?? '';
  for (const part of parts) handleSse(part);
}
```

前端需要：

- 按空行切分 SSE 事件；
- 解析 `event:` 和 `data:` 字段；
- 将 `delta` 追加到当前助手消息；
- 收到 `done` 结束生成状态；
- 收到 `error` 展示错误信息和 traceId；
- 使用 `AbortController` 支持停止生成；
- 记录从发送到第一个 `delta` 的耗时，用于验收首字 ≤3 秒。

---

## 四、结构化抽取

### 4.1 基本信息

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| 路径 | `/api/extract` |
| 用途 | 从公文文本中抽取固定字段并返回 JSON |
| 响应类型 | `application/json` |
| 鉴权 | 阶段一为本地单用户，暂不鉴权 |

### 4.2 请求体

```json
{
  "text": "……公文原文，长度 1–10000 字符……"
}
```

| 字段 | 类型 | 必填 | 约束 |
|---|---|---|---|
| `text` | string | 是 | 去除首尾空白后 1–10000 字符 |

### 4.3 成功响应

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "docType": "通知",
    "title": "关于加强安全生产检查的通知",
    "recipient": "各部门",
    "signature": "XX办公室",
    "date": "2026-11-01"
  },
  "traceId": "a1b2c3d4"
}
```

字段要求：

| 字段 | 类型 | 是否可为空 | 说明 |
|---|---|---|---|
| `docType` | string \| null | 是 | 文种，如通知、请示、报告 |
| `title` | string \| null | 是 | 公文标题 |
| `recipient` | string \| null | 是 | 主送单位 |
| `signature` | string \| null | 是 | 落款单位或个人 |
| `date` | string \| null | 是 | 成文日期，统一为 `YYYY-MM-DD` |

五个字段必须全部存在；无法识别的字段返回 `null`，不能省略字段名。

### 4.4 失败响应

模型输出无法解析，重试一次后仍失败：

```json
{
  "code": 5003,
  "message": "模型输出无法解析，已重试 1 次",
  "data": null,
  "traceId": "a1b2c3d4"
}
```

模型调用失败：

```json
{
  "code": 5001,
  "message": "模型调用失败",
  "data": null,
  "traceId": "a1b2c3d4"
}
```

### 4.5 后端处理要求

1. 调用 DeepSeek `stream=false` 的 `/chat/completions`。
2. 要求模型只输出 JSON，不输出解释文字。
3. 去除 Markdown 代码围栏，例如 ```json 和 ```。
4. 使用 Jackson 解析为固定字段对象。
5. 校验字段是否存在、类型是否正确、日期是否为 `YYYY-MM-DD`。
6. 解析失败时重试一次；仍失败返回 `code=5003`。
7. 原始模型输出只写日志，不返回给前端。
8. 不出现未处理的 HTTP 500 堆栈。

### 4.6 curl 示例

```bash
curl -X POST http://localhost:8080/api/extract \
  -H "Content-Type: application/json" \
  -d '{"text":"关于开展安全生产检查的通知\n各部门：\n……\nXX办公室\n2026年11月1日"}'
```

---

## 五、可选调试接口（阶段一未实现）

> 按 `02-阶段一全栈链路执行计划.md`，`POST /api/chat` 为可选接口，阶段一未实现。本文档仅保留契约占位；如后续启用，须先按计划变更流程确认并更新文档。

### 5.1 非流式对话（未实现）

| 项目 | 内容 |
|---|---|
| 状态 | **未实现**（阶段一不提供） |
| 方法 | `POST` |
| 路径 | `/api/chat` |
| 用途 | 调试 Prompt 和模型参数，不参与前端流式展示 |
| 请求体 | 与 `/api/chat/stream` 一致 |
| 响应 | `data` 为完整回答字符串 |

---

## 六、测试用例清单

### 6.1 健康检查

- [ ] 后端启动后 `GET /api/health` 返回 `code=0`。
- [ ] 响应头包含 `X-Trace-Id`。

### 6.2 流式对话

- [ ] 正常问题：收到多条 `message`，最后收到 `done`。
- [ ] 长回答：内容不截断，必须收到终止事件。
- [ ] `formal` 与 `concise` 输出风格差异明显。
- [ ] 空 `message` 返回 HTTP 400 和 `code=4000`。
- [ ] 非法 `style` 返回 HTTP 400 和 `code=4000`。
- [ ] 上游模型异常时收到 `error`，且不暴露 Key 和堆栈。
- [ ] 用户中断请求后，后端停止读取上游并释放连接。
- [ ] 连续 5 次测试记录首字耗时，最大值 ≤3 秒。

### 6.3 结构化抽取

- [ ] 正常公文返回五个固定字段。
- [ ] 字段缺失时返回 `null`，字段名仍存在。
- [ ] 模型输出带 Markdown 围栏时可正常去除并解析。
- [ ] 日期非法时置空或按约定处理，并在日志中记录。
- [ ] 解析失败时重试一次。
- [ ] 重试仍失败返回 `code=5003`，不出现未处理 500。
- [ ] 模型调用失败返回 `code=5001`。

---

## 七、后续阶段接口

以下接口不在阶段一实现，W3 起再补充：

- Dify App API 代理接口；
- RAG 知识库检索接口；
- 会话历史与消息持久化接口；
- 登录、鉴权与用户信息接口；
- 私有 vLLM 切换后的模型调用接口。

## 八、变更记录

| 版本 | 日期 | 变更 |
|---|---|---|
| v1.0 | 2026-11-11 | 归纳阶段一健康检查、流式对话、结构化抽取接口契约 |
