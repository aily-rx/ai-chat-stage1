# PR 与评审记录

## PR #1：阶段一全栈链路

| 项目 | 内容 |
|---|---|
| 标题 | feat: 阶段一全栈链路（流式对话 + 结构化抽取 + 文档） |
| 链接 | https://github.com/aily-rx/ai-chat-stage1/pull/1 |
| 源分支 | `feat/s1-project-skeleton` |
| 目标分支 | `main` |
| 状态 | 已合并；合并 commit `1c96bb3`，随后打初始 tag `stage1-v1` |
| 评审人 | aily（自检）；zhongBY526（合并后补充 3 条行内评审意见） |
| 评审时间 | 2026-11-11（自检）；2026-11-12（协作人补充） |

## 三条可执行评审意见与处理

| # | 文件 | 评审意见 | 处理结果 |
|---|---|---|---|
| 1 | `frontend/src/composables/useChatStream.js` | 收到 `done` 事件后，后续读取流的网络异常不应把已完成消息改成 error，否则界面会同时显示完整答案和错误提示。建议在 catch 中先判断 `terminalReceived`。 | 已采纳。catch 中增加 `terminalReceived` 判断，保留已完成状态；见 commit `754e0f8`。 |
| 2 | `backend/src/main/java/com/ailyrx/aichat/llm/LlmClient.java` | 流式请求也应用了 `timeoutSeconds` 总超时，长回答可能在第 60 秒被截断并触发 5001。建议仅对非流式 `complete` 设置总超时。 | 已采纳。仅非流式请求设置整体超时，流式请求依赖 connectTimeout 和客户端取消；见 commit `754e0f8`。 |
| 3 | `backend/src/main/java/com/ailyrx/aichat/chat/ChatService.java` | `5001` 在 ChatService 和 ExtractController 中硬编码，接口契约调整时容易漏改。建议提取 `ErrorCodes` 常量类并统一引用。 | 已采纳。新增 `common/ErrorCodes.java`，统一 `MODEL_CALL_FAILED=5001`、`EXTRACTION_PARSE_FAILED=5003`；见 commit `754e0f8`。 |

评论链接：

- https://github.com/aily-rx/ai-chat-stage1/pull/1#discussion_r4533639368
- https://github.com/aily-rx/ai-chat-stage1/pull/1#discussion_r4533639391
- https://github.com/aily-rx/ai-chat-stage1/pull/1#discussion_r4533639402

## PR #2：Prompt 外置与样例补全

| 项目 | 内容 |
|---|---|
| 标题 | fix(prompt): 外置 Prompt 模板并补全样例说明 |
| 链接 | https://github.com/aily-rx/ai-chat-stage1/pull/2 |
| 源分支 | `fix/s1-prompt-externalization` |
| 目标分支 | `main` |
| 合并 commit | `24e7fd0` |
| 状态 | 已合并 |
| 评审方式 | 单人仓库，按评审清单自检；无第二评审人 |

主要变更：Prompt 从 Java 字符串外置到 classpath 资源，返回稳定 `id/version`，补充 `formal`、`concise`、`extract-fields` 三份可审阅样例，并增加 `PromptServiceTest`。

## PR #3：真实后端配置与验收证据收口

| 项目 | 内容 |
|---|---|
| 标题 | docs(release): 收口真实后端配置与验收证据 |
| 链接 | https://github.com/aily-rx/ai-chat-stage1/pull/3 |
| 源分支 | `chore/s1-baseline-docs` |
| 目标分支 | `main` |
| 合并 commit | `ce3fbe2` |
| 状态 | 已合并 |
| 评审方式 | 单人仓库，按文档一致性清单自检；无第二评审人 |

主要变更：前端默认切回真实后端，统一 README 与接口文档口径，标注未实现的 `/api/chat`，补充干净克隆测试样例。

## PR #4：长回答与长文本抽取验收

| 项目 | 内容 |
|---|---|
| 标题 | test(acceptance): 补全长回答与长文本抽取验证 |
| 链接 | https://github.com/aily-rx/ai-chat-stage1/pull/4 |
| 源分支 | `test/s1-long-content` |
| 目标分支 | `main` |
| 合并 commit | `f816180` |
| 状态 | 已合并 |
| 评审方式 | 单人仓库，按验收清单自检；无第二评审人 |

主要变更：补充 2102 字符长回答不截断证据、9367 字符长文本抽取证据、10001 字符边界测试及对应测试用例。

## PR #5：阶段一最终收口

| 项目 | 内容 |
|---|---|
| 标题 | chore(release): 完成阶段一最终收口 |
| 链接 | https://github.com/aily-rx/ai-chat-stage1/pull/5 |
| 源分支 | `chore/s1-final-closeout` |
| 目标分支 | `main` |
| 状态 | 已合并；合并后建立 tag `stage1-v1.1`（后续由 `stage1-v1.3` 取代为最终交付） |
| 评审方式 | 单人仓库，按发布清单自检；无第二评审人 |

主要变更：新增仅开发源可用的 CORS 配置及正反两个预检用例；同步测试数、接口文档、汇报单和 Dify 记录；补充 Dify 重启后 Web App 通道复测样例。最终测试总数 28，`npm run build` 成功。

## 协作人补充评审（PR #1，2026-11-12）

PR #1 合并后，`zhongBY526` 补充 3 条行内评审意见，已在 `9ae78fb` 处理并逐条回复：

| # | 文件 | 评审意见 | 处理结果 |
|---|---|---|---|
| 1 | `backend/src/main/java/com/ailyrx/aichat/llm/LlmClient.java` | 结束判断只认 `done`，DeepSeek 触达 `max_tokens` 时先返回 `finish_reason=length` 再返回 `done`，截断与正常完成无法区分。 | `LlmClient` 检查 `finish_reason`，截断时抛 `LlmOutputTruncatedException`；`ChatService` 返回 `event: error` 和 `code=5001`，不再发送 `done`；非流式抽取同步处理；新增 `LlmClientTest` 和接口截断场景测试。 |
| 2 | `backend/src/main/java/com/ailyrx/aichat/chat/ChatController.java` | 流式接口未设置 `X-Accel-Buffering: no` 与 `Cache-Control: no-cache, no-transform`，经过 Nginx 或网关时可能一次性输出。 | 已补充两个响应头，并在 `ChatControllerTest` 中断言。 |
| 3 | `backend/src/main/java/com/ailyrx/aichat/llm/LlmClient.java` | 结构化抽取未设置 `temperature`，模型使用默认采样，稳定性完全依赖提示词。 | 抽取请求固定 `temperature=0.0`，支持 `LLM_EXTRACT_TEMPERATURE` 配置；仅抽取 payload 携带该参数；补充 payload 测试并同步文档。 |

原评论与回复：

- [截断判定](https://github.com/aily-rx/ai-chat-stage1/pull/1#discussion_r4547721711) → [回复](https://github.com/aily-rx/ai-chat-stage1/pull/1#discussion_r4548107324)
- [SSE 反缓冲](https://github.com/aily-rx/ai-chat-stage1/pull/1#discussion_r4547762749) → [回复](https://github.com/aily-rx/ai-chat-stage1/pull/1#discussion_r4548107724)
- [抽取温度](https://github.com/aily-rx/ai-chat-stage1/pull/1#discussion_r4547844719) → [回复](https://github.com/aily-rx/ai-chat-stage1/pull/1#discussion_r4548108481)

对应修复提交：`9ae78fb`（`fix(chat): 处理 PR #1 评审意见`）。

## 交叉评审：协作人 PR #4（2026-11-12）

对协作人仓库 `zhongBY526/-` 的 PR #4「首字可感知、流式截断识别与异常日志」提交 `COMMENT` review，未使用 `Approve` / `Request changes`。

| 项目 | 内容 |
|---|---|
| 仓库 | https://github.com/zhongBY526/- |
| PR | https://github.com/zhongBY526/-/pull/4 |
| 状态 | 已合并/关闭；合并 commit `2d0920b` |
| 评审身份 | `aily-rx` |
| 评审方式 | `COMMENT` review，3 条行内意见 |
| 评审结论 | 无阻塞性功能问题；以下 3 条作为后续迭代建议 |

| # | 文件 | 意见 | 处理建议 |
|---|---|---|---|
| 1 | `front/src/views/ChatViews.vue` | `done` / `error` 终态可能被后续读取异常覆盖，导致页面展示二次异常而不是服务端真实错误。 | catch 中增加 `!ended` 判断，保证终态优先。 |
| 2 | `front/src/views/ChatViews.vue` | `waitTimer` 与未结束的流只在 `send()` 的 finally 清理；切换 tab 卸载组件时可能继续运行。 | 增加 `onBeforeUnmount(() => { stopWaitTimer(); controller?.abort() })`。 |
| 3 | `backed/src/main/java/com/intern/backed/chat/DeepseekStreamChat.java` | `resp.finishReason() == null` 被映射为 `"stop"`，会把缺失结束原因误报为正常完成。 | null 映射为 `"unknown"` 或保留 null，并补充 null / stop / length 测试。 |

评审与评论链接：

- [评审汇总](https://github.com/zhongBY526/-/pull/4#pullrequestreview-5871804250)
- [评论 1：终态覆盖](https://github.com/zhongBY526/-/pull/4#discussion_r4548385496)
- [评论 2：卸载清理](https://github.com/zhongBY526/-/pull/4#discussion_r4548385507)
- [评论 3：finishReason 语义](https://github.com/zhongBY526/-/pull/4#discussion_r4548385508)

## 回归验证

- `mvn -f backend/pom.xml test`：34 个测试，0 失败（2026-11-12 补充评审修复后复测）。
- `npm run build`：构建成功，产物正常生成。
- 干净目录克隆后按 README 启动，健康检查、流式对话、停止生成、结构化抽取均通过。
- CORS 预检：开发源返回 200，`Access-Control-Allow-Origin` 与 `X-Trace-Id` 暴露头正确；非开发源返回 403。
- 仓库与 Git 历史检查：未发现真实密钥；`.env` 未被跟踪。

## 合并与 tag

- PR #1 合并后建立初始里程碑 tag `stage1-v1`（commit `1c96bb3`）。
- PR #2–#4 已依次合并到 `main`；PR #5 完成阶段性收口并建立 tag `stage1-v1.1`。
- README 重构后建立 tag `stage1-v1.2`（commit `235ff63`）。
- 2026-11-12 在 `main` 处理 PR #1 协作评审意见（commit `9ae78fb`），随后建立最终交付 tag `stage1-v1.3`；最终交付以 `stage1-v1.3` 指向的 commit 为准。
