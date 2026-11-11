# ai-chat-stage1

阶段一实践项目：Vue3 + Spring Boot + DeepSeek API 的流式对话应用，包含公文结构化抽取接口。Dify 作为独立交付物部署，不接入主链路。

## 仓库

- GitHub（私有）：https://github.com/aily-rx/ai-chat-stage1

## 当前进度

- [x] S0 环境与凭证
- [x] S1 项目骨架
- [x] S2 后端 SSE 接口
- [x] S3 前端流式页面
- [x] S4 Prompt 变量与首字延迟
- [x] S5 结构化抽取接口
- [x] S6 工程化收尾
- [x] S7 文档与样例
- [ ] S8 Dify + PR + 汇报材料

## 环境要求

- JDK 17
- Maven 3.9+
- Node.js 18+
- npm
- Git

## 配置

```bash
cp .env.example .env
# 在 .env 中填入自己的 DeepSeek Key
```

`.env` 已被 `.gitignore` 忽略，禁止提交真实 Key。

## 前端运行（演示模式）

```bash
cd frontend
npm install
npm run dev
```

默认 `VITE_USE_MOCK=true`，无需后端即可演示流式对话、停止生成、正式/简洁切换和结构化抽取。页面地址：http://127.0.0.1:5173

后端就绪后，将 `frontend/.env` 中的 `VITE_USE_MOCK` 改为 `false`，即可通过 Vite 代理访问 `http://localhost:8080/api`。

## 后端运行

在项目根目录执行，`application.yml` 会自动读取根目录或 `backend/` 下的 `.env`：

```bash
# 运行测试
mvn -f backend/pom.xml test

# 启动后端
mvn -f backend/pom.xml spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/api/health
```

预期响应：

```json
{"code":0,"message":"ok","data":"ok","traceId":"a1b2c3d4"}
```

响应头同时包含 `X-Trace-Id`。技术栈：Spring Boot 3.3.5、Java 17、Maven。

## 接口验证

流式对话：

```bash
curl -N -X POST http://localhost:8080/api/chat/stream   -H "Content-Type: application/json"   -d '{"message":"用三句话介绍RAG","style":"formal"}'
```

预期收到多条 `event: message`，并以 `event: done` 结束。将 `style` 改为 `concise` 可对比输出风格。

结构化抽取：

```bash
curl -X POST http://localhost:8080/api/extract   -H "Content-Type: application/json"   -d '{"text":"关于开展安全生产检查的通知
各部门：
……
XX办公室
2026年11月1日"}'
```

完整请求、SSE 事件和错误码说明见 [接口文档](docs/api.md)。五个抽取字段固定为 `docType`、`title`、`recipient`、`signature`、`date`；无法识别时返回 `null`。

## 提交规范

- 提交信息统一使用中文。
- 标题格式：`类型(范围): 摘要`，例如 `feat(chat): 增加流式对话接口`。
- 正文必须说明本次提交的变更，至少包含以下两节：
  - `修改：` 已有文件、功能或配置的调整
  - `新增：` 新增的文件、功能或配置
- 没有修改或新增时写“无”，不要省略。
- 示例：

```text
feat(chat): 增加 SSE 流式对话接口

修改：
- ChatController 增加 /api/chat/stream 接口

新增：
- LlmClient 封装 DeepSeek 流式调用
```

## 计划目录

```text
ai-chat-stage1/
├─ frontend/          # Vue3 + Vite
├─ backend/           # Spring Boot 3 + JDK17
├─ prompts/           # Prompt 样例
├─ docs/              # 接口、测试、Dify、PR 记录
├─ notes/             # 学习笔记
├─ .env.example
├─ .gitignore
└─ README.md
```

## 文档

- [阶段一接口文档](docs/api.md)
- [前端原型设计](docs/frontend-prototype.md)
- [联调与性能测试记录](docs/streaming-test.md)
- [Dify 部署记录](docs/dify-setup.md)
- [Prompt 样例](prompts/chat-formal.md)
