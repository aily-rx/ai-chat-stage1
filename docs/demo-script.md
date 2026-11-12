# 阶段一汇报演示脚本

> 目标：在 8–10 分钟内完成“Vue 页面 → Spring Boot → DeepSeek 流式返回”、Prompt 变量切换和结构化抽取演示；失败时按备用方案切换。

## 0. 演示前检查（提前 10 分钟）

```bash
cd ai-chat-stage1
git status
git log --oneline -3
git tag -l

java -version
mvn -version
node -v
npm -v
docker version
```

本地 `.env` 必须存在且包含 DeepSeek Key（不要投屏 Key 内容）：

```bash
cp .env.example .env
# 编辑 .env，填入 LLM_API_KEY
```

前端真实模式：

```bash
cd frontend
cp .env.example .env
# 确认 VITE_USE_MOCK=false（默认值，连接真实后端）
npm install
```

## 1. 启动后端（约 1 分钟）

```bash
cd ai-chat-stage1
mvn -f backend/pom.xml test
mvn -f backend/pom.xml spring-boot:run
```

另开终端验证：

```bash
curl -i http://localhost:8080/api/health
```

预期：HTTP 200，响应体 `code=0`，响应头包含 `X-Trace-Id`。

## 2. 启动前端（约 30 秒）

```bash
cd ai-chat-stage1/frontend
npm run dev
```

打开 http://localhost:5173，右上角状态应为“后端正常”。

## 3. 启动 Dify（独立交付物）

```bash
cd E:/Dify/dify/docker
docker compose up -d
docker compose ps
```

打开 http://localhost，进入 Chatbot「公文问答助手」。若 Docker Desktop 报 `unable to start`：

```powershell
wsl --shutdown
docker desktop restart
```

## 4. 演示步骤与话术

### 4.1 仓库与密钥检查（1 分钟）

```bash
git ls-files | grep -E "(^|/)\.env$" || echo "no .env tracked"
git log --all -p | grep -E "sk-[A-Za-z0-9]{20,}" | head
```

预期：没有 `.env` 被跟踪；没有真实 Key（只允许 `.env.example` 中的 `sk-xxxxxxxx-placeholder`）。

话术：模型 Key 只放在本地 `.env`，通过 `application.yml` 的 `optional:file` 加载，仓库和 Git 历史不包含密钥。

### 4.2 流式对话 + 首字延迟（2 分钟）

1. 在页面输入：`介绍一下RAG`。
2. 保持“正式”风格，点击发送。
3. 观察逐字输出、首字耗时、总耗时和 traceId。

预期：首字 < 3 秒；回答完整；Markdown 标题/列表正常；底部显示 traceId。

实测参考：正式风格首字 0.90 s，总耗时 2.41 s。

### 4.3 Prompt 变量切换（1 分钟）

1. 将“回答风格”从“正式”切换为“简洁”。
2. 再次发送同一个问题。
3. 对比两次输出长度和语气。

预期：简洁版明显更短、直入主题。实测参考：420 字符 vs 66 字符。

### 4.4 停止生成（30 秒）

1. 输入长任务：`写一份关于开展安全生产检查的通知，正文不少于800字。`
2. 点击“停止”。
3. 观察消息状态。

预期：停止后不再追加内容，显示“已停止生成”，不出现错误横幅。

### 4.5 结构化抽取（2 分钟，重点）

1. 点击顶部“结构化抽取”。
2. 点击“加载正常样例”，再点击“开始抽取”。
3. 展示五字段表格和原始 JSON。

预期：

```json
{
  "docType": "通知",
  "title": "关于开展安全生产检查的通知",
  "recipient": "各部门",
  "signature": "XX办公室",
  "date": "2026-11-01"
}
```

4. 点击“加载稀疏样例”或直接粘贴 `今天天气不错，适合户外活动。`，再次抽取。

预期：五个字段全部为 `null`，HTTP 200，不报 500。

5. 说明兜底逻辑：模型输出带 Markdown 围栏会先剥离；JSON 解析失败重试 1 次；两次失败返回 `5003`；模型调用失败返回 `5001`。

#### 备用验证命令（页面异常时）

```bash
curl -s -X POST http://localhost:8080/api/extract -H "Content-Type: application/json" --data-binary @docs/samples/extract-normal.json
```

预期返回 `code=0` 和五个字段。

### 4.6 traceId 与日志（30 秒）

在页面底部记录 traceId，然后查看后端终端日志：

```text
INFO [xxxxxxxx] [chat-stream-1] ... chat stream completed traceId=xxxxxxxx chars=...
```

说明：前端展示的 traceId 与后端日志一致，可按 traceId 串联一次请求。

### 4.7 Dify Chatbot（1 分钟）

1. 打开 http://localhost，进入「公文问答助手」。
2. 提问：`请示与报告的主要区别是什么？`
3. 展示返回的规范中文答案。

说明：Dify 是独立交付物，用于第一周成果演示，不接入主链路；主链路仍是 Vue3 → Spring Boot → DeepSeek。

## 5. 失败案例与备用方案

| 风险 | 备用方案 |
|---|---|
| DeepSeek 接口超时/限流 | 展示 `docs/streaming-test.md` 的历史记录；前端切换到 Mock 模式演示流式效果；说明错误码 5001 与 traceId 提示 |
| 前端无法访问后端 | 将 `frontend/.env` 的 `VITE_USE_MOCK` 改为 `true`，重启 Vite；演示 Mock 流式对话和 5003 失败样例 |
| 结构化抽取现场解析失败 | 运行 `mvn -f backend/pom.xml -Dtest=ExtractionServiceTest test`，展示重试与 5003 测试；或切到 Mock 失败样例 |
| Dify 无法登录/容器异常 | 展示 `docs/dify-setup.md` 与已记录的 3 个测试结果；执行 `docker compose ps` 说明版本和端口 |
| 端口被占用 | 后端默认 8080，前端严格使用 5173；先关闭占用进程，或按 README 修改 `SERVER_PORT` 与 Vite 代理 |

## 6. 演示后收尾

```bash
# 停止前端/后端：在对应终端按 Ctrl+C
cd E:/Dify/dify/docker
docker compose down
```
