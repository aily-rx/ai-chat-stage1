# PR 与评审记录

## PR #1：阶段一全栈链路

| 项目 | 内容 |
|---|---|
| 标题 | feat: 阶段一全栈链路（流式对话 + 结构化抽取 + 文档） |
| 链接 | https://github.com/aily-rx/ai-chat-stage1/pull/1 |
| 源分支 | `feat/s1-project-skeleton` |
| 目标分支 | `main` |
| 状态 | 已创建并完成评审；合并后打 tag `stage1-v1` |
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

## 回归验证

- `mvn -f backend/pom.xml test`：21 个测试，0 失败。
- `npm run build`：构建成功，产物正常生成。
- 干净目录克隆后按 README 启动，健康检查、流式对话、停止生成、结构化抽取均通过。
- 仓库与 Git 历史检查：未发现真实密钥；`.env` 未被跟踪。

## 合并与 tag

- PR 评审意见处理完成后合并到 `main`。
- 阶段一交付 tag：`stage1-v1`。
- 合并与 tag 的具体 commit 以 GitHub 仓库为准。
