# Dify 本地部署记录

> 本文件只记录部署与演示信息，不记录管理员密码、模型 Key、App API Key。

## 部署信息

| 项目 | 值 |
|---|---|
| Dify 版本 | 1.19.0 |
| 编排引擎 | Graphon 0.8.0（不是 LangGraph） |
| 源码目录 | `E:\Dify\dify` |
| Compose 目录 | `E:\Dify\dify\docker` |
| 启动命令 | `docker compose up -d`（在 Compose 目录执行） |
| 停止命令 | `docker compose down` |
| 访问地址 | http://localhost/（nginx 映射 80 端口） |
| 当前状态 | 2026-11-11：已启动，15 个容器运行中，`/console/api/setup` 返回 `finished` |
| 模型供应商 | DeepSeek（Dify 插件 `langgenius/deepseek/deepseek`） |
| 演示用模型 | `deepseek-flash` |
| Chatbot 名称 | 公文问答助手 |
| Chatbot 模式 | Chatbot（`mode=chat`，非 Agent） |
| Chatbot App ID | `6bbaaa07-e9ab-4d20-9ebe-3ca61e5d1574` |
| Chatbot 用途 | 阶段一独立交付物：公文写作与行政办公业务问答 |

## 部署与验证命令

```powershell
cd E:\Dify\dify\docker
docker compose up -d
docker compose ps
```

本次启动后关键容器状态：

- `docker-nginx-1`：`0.0.0.0:80->80/tcp`
- `docker-api-1`：`healthy`
- `docker-db_postgres-1`、`docker-redis-1`、`docker-weaviate-1`：运行中
- `docker-plugin_daemon-1`：`5003` 端口

版本复核：

```powershell
docker images --format "table {{.Repository}}	{{.Tag}}" | Select-String dify
docker exec docker-api-1 sh -c "grep -m1 '^version' /app/api/pyproject.toml"
```

## Chatbot 配置

- 系统提示词：公文写作与行政办公助手，回答准确、简洁、可执行，使用规范中文，不编造制度依据、单位或数据。
- 开场白：`您好，我是公文问答助手，可以为您解答公文写作与行政办公相关问题。`
- 推荐问题：
  1. 公文的结构包括哪些部分？
  2. 如何写一份会议通知？
  3. 请示和报告有什么区别？
- 未创建 Agent，未接入主链路；主链路的问答仍由 Vue3 + Spring Boot + DeepSeek 完成。

## 联调测试记录（2026-11-11）

测试方式：调用 Dify 控制台调试接口 `POST /console/api/apps/{app_id}/chat-messages`，使用已保存的 Chatbot 配置，`response_mode=blocking`。三次请求均返回 HTTP 200。

| # | 提问 | HTTP | 耗时 | 结果摘要 |
|---|---|---|---|---|
| 1 | 公文正文一般包括哪些部分？ | 200 | 4.70 s | 正确给出“开头（缘由）—主体（事项）—结尾（结语）”结构，并补充附件说明与格式提示 |
| 2 | 如何写一份会议通知？请给出关键要点。 | 200 | 7.91 s | 给出会议要素、基本结构、写作要点、简要模板和发出前检查清单 |
| 3 | 请示与报告的主要区别是什么？ | 200 | 7.32 s | 用“请示要批、报告只要知”概括，并给出目的、是否回复、行文时限、一文一事等对比表 |

结论：

- Dify 1.19.0 本地部署可用，Chatbot 能正常调用 DeepSeek 模型完成业务问答。
- 回答内容符合公文场景，能够支撑第一周成果演示。
- Chatbot 属于独立交付物，不参与前端 → 后端 → DeepSeek 的主链路。

## 常见问题

- 若 Docker Desktop 报 `Docker Desktop is unable to start` 或日志提示 `WSL update required`，先执行：
  ```powershell
  wsl --shutdown
  docker desktop restart
  ```
  引擎恢复后再执行 `docker compose up -d`。
- 若 `/console/api/setup` 返回 502，通常是 API 容器还在启动，等待 `docker-api-1` 变为 `healthy` 后重试。
