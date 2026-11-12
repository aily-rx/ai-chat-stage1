# Prompt 样例 03：公文要素结构化抽取

| 项目 | 内容 |
|---|---|
| Prompt ID | `extract-fields` |
| Prompt 版本 | `v1.0` |
| 实际模板 | `backend/src/main/resources/prompts/extract-fields.txt` |
| 调用接口 | `POST /api/extract` |
| 模型 | `deepseek-chat`（可通过 `LLM_MODEL` 配置） |
| 适用场景 | 从公文原文中抽取文种、标题、主送单位、落款和日期 |

## 1. 场景与目标

输入一段公文文本，输出固定五字段 JSON。目标是：

- 五个字段始终存在，无法识别时返回 `null`。
- 不编造单位、人员、日期。
- 日期统一为 `YYYY-MM-DD`。
- 模型输出异常时，后端有清洗、校验、重试和业务错误码兜底。

该 Prompt 是第一次汇报要求现场演示的结构化抽取能力。

## 2. 系统提示词

```text
你是公文信息抽取器。请从用户提供的公文文本中抽取指定字段。
只输出一个 JSON 对象，不要输出 Markdown 围栏、解释文字或任何额外内容。
JSON 结构必须为：
{"docType":"文种或 null","title":"标题或 null","recipient":"主送单位或 null","signature":"落款单位或个人或 null","date":"成文日期或 null"}
规则：
1. 五个字段必须全部输出，无法识别的字段填 null；
2. 不要编造不存在的单位、人员或日期；
3. date 必须统一为 YYYY-MM-DD；无法确定为 null。
```

## 3. 变量与字段说明

阶段一没有把公文正文直接用 `{{document_text}}` 拼进 system prompt，而是通过 OpenAI 兼容协议的 `user` role 传入，原因是大段公文可能包含类似占位符的字符，直接字符串替换容易产生注入和转义问题。

| 名称 | 来源 | 说明 |
|---|---|---|
| `text` | `POST /api/extract` 请求体 | 用户提供的公文原文，长度 1–10000 字符 |
| `docType` | 模型输出 | 文种，如通知、请示、报告；无法识别为 `null` |
| `title` | 模型输出 | 公文标题；无法识别为 `null` |
| `recipient` | 模型输出 | 主送单位；无法识别为 `null` |
| `signature` | 模型输出 | 落款单位或个人；无法识别为 `null` |
| `date` | 模型输出 | 成文日期，由后端规范化为 `YYYY-MM-DD` |
| `extract.version` | `prompt-catalog.properties` | Prompt 版本，写入日志 |

固定输出结构：

```json
{
  "docType": "文种或 null",
  "title": "标题或 null",
  "recipient": "主送单位或 null",
  "signature": "落款单位或个人或 null",
  "date": "成文日期或 null"
}
```

## 4. 模型参数与后端兜底

| 参数或行为 | 当前值 | 说明 |
|---|---|---|
| `model` | `deepseek-chat` | 可通过 `LLM_MODEL` 覆盖 |
| `stream` | `false` | 抽取使用非流式 `complete` 调用 |
| `temperature` | 未显式设置 | 使用 API 默认值 |
| `max_tokens` | 未显式设置 | 使用模型默认值 |
| 非流式总超时 | 60 s | 由 `LLM_TIMEOUT_SECONDS` 控制 |
| 解析重试 | 最多 2 次 | 第一次无法解析时重新请求一次 |
| 最终失败码 | `5003` | 两次都无法解析时返回业务错误 |
| 模型调用失败码 | `5001` | 上游错误、超时或空响应 |

后端还会：

- 去除 Markdown 代码围栏。
- 截取第一个 `{` 到最后一个 `}`。
- 校验结果至少包含一个约定字段。
- 将中文日期转换为 ISO 日期。
- 非法日期按 `null` 处理并记录日志。

## 5. 示例输入与输出

### 5.1 示例一：完整公文

输入（节选，完整样例见 `docs/samples/extract-normal.json`）：

```text
关于开展安全生产检查的通知

各部门：

为进一步压实安全生产责任，防范化解安全风险，经研究，决定开展安全生产专项检查。现将有关事项通知如下：
……
XX办公室
2026年11月1日
```

真实接口返回：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "docType": "通知",
    "title": "关于开展安全生产检查的通知",
    "recipient": "各部门",
    "signature": "XX办公室",
    "date": "2026-11-01"
  },
  "traceId": "3511366c"
}
```

说明：`date` 在原文中是“2026年11月1日”，模型返回后由后端/模型约束统一为 `2026-11-01`。

### 5.2 示例二：信息不足的文本

输入：

```json
{
  "text": "今天天气不错，适合户外活动。"
}
```

真实接口返回：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "docType": null,
    "title": null,
    "recipient": null,
    "signature": null,
    "date": null
  },
  "traceId": "c27b9b73"
}
```

这说明：文本不是公文时，接口不报错，而是返回五个固定字段的 `null`，前端可以正常展示“未识别”结果。

## 6. 设计理由

1. **固定 JSON 结构**：前端表格和测试都按五字段读取，字段名不能随模型输出变化。
2. **只输出 JSON**：减少无关文字和 Markdown 围栏，降低解析成本。
3. **明确 `null` 语义**：信息不足不能被模型编造成看似合理的内容。
4. **日期单独约束**：公文里常见“2026年11月1日”，需要统一成接口契约要求的格式。
5. **Prompt 不是唯一防线**：后端仍做围栏清洗、字段校验、日期校验和一次重试，避免把模型输出直接当可信数据。

## 7. 失败案例与改进

### 7.1 失败现象

自动化测试覆盖过以下模型输出：

````text
```json
{"docType":"通知","title":"测试通知","recipient":null,"signature":"XX办公室","date":"2026年11月1日"}
```
````

这段输出如果直接按 JSON 解析会因 Markdown 围栏失败；日期格式也不符合接口契约。

另一个无效输出是空 JSON：

```json
{}
```

它虽然能被 JSON 解析，但没有一个约定字段，不能作为有效抽取结果。

### 7.2 改进版本

当前方案从两侧处理：

**Prompt 侧**增加：

```text
只输出一个 JSON 对象，不要输出 Markdown 围栏、解释文字或任何额外内容。
五个字段必须全部输出，无法识别的字段填 null。
date 必须统一为 YYYY-MM-DD；无法确定为 null。
```

**后端侧**增加：

- `stripFences()` 去除 Markdown 围栏并截取 JSON 对象。
- `containsExtractField()` 拒绝 `{}` 这类无字段结果。
- 解析失败自动重试一次。
- 连续失败返回 `5003`，模型调用失败返回 `5001`。
- 非法日期记录告警并返回 `date: null`。

相关自动化测试位于 `ExtractionServiceTest`，真实接口结果记录于 `docs/streaming-test.md`。

## 8. 输出约定

- 五个字段固定为 `docType`、`title`、`recipient`、`signature`、`date`。
- 后端会去除 Markdown 围栏、抽取 JSON 对象、校验字段类型和日期格式。
- 第一次解析失败后自动重试一次；仍失败返回业务码 `5003`。
- 模型调用失败返回业务码 `5001`。
- 日志中记录 `promptId=extract-fields` 与 `promptVersion`，便于追踪不同版本的效果。

## 9. 版本变更规则

- 修改 `extract-fields.txt` 后更新 `extract.version`。
- 如果调整字段、日期格式或输出 JSON 结构，必须同步更新接口文档、前端表格和测试。
- 如果仅调整抽取效果，应在 `docs/streaming-test.md` 记录新的真实样例和失败率。
