# PR 与评审记录

## PR #1：阶段一全栈链路

| 项目 | 内容 |
|---|---|
| 标题 | feat: 阶段一全栈链路（流式对话 + 结构化抽取 + 文档） |
| 链接 | https://github.com/aily-rx/ai-chat-stage1/pull/1 |
| 源分支 | `feat/s1-project-skeleton` |
| 目标分支 | `main` |
| 状态 | 已合并；合并 commit `1c96bb3`，随后打初始 tag `stage1-v1` |
| 评审人 | aily（单人仓库，按交叉评审清单自检；如后续有同伴可补充对方 PR 互评） |
| 评审时间 | 2026-11-11 |

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

## 回归验证

- `mvn -f backend/pom.xml test`：28 个测试，0 失败（最终收口复测）。
- `npm run build`：构建成功，产物正常生成。
- 干净目录克隆后按 README 启动，健康检查、流式对话、停止生成、结构化抽取均通过。
- CORS 预检：开发源返回 200，`Access-Control-Allow-Origin` 与 `X-Trace-Id` 暴露头正确；非开发源返回 403。
- 仓库与 Git 历史检查：未发现真实密钥；`.env` 未被跟踪。

## 合并与 tag

- PR #1 合并后建立初始里程碑 tag `stage1-v1`（commit `1c96bb3`）。
- PR #2–#4 已依次合并到 `main`；最终收口 PR 审批合并后建立最终交付 tag `stage1-v1.1`。
- 初始 tag 保留不动，最终交付以 `stage1-v1.1` 指向的 commit 为准。
