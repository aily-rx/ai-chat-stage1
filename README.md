# ai-chat-stage1

阶段一实践项目：Vue3 + Spring Boot + DeepSeek API 的流式对话应用，包含公文结构化抽取接口。Dify 作为独立交付物部署，不接入主链路。

## 仓库

- GitHub（私有）：https://github.com/aily-rx/ai-chat-stage1

## 当前进度

- [x] S0 环境与凭证
- [ ] S1 项目骨架
- [ ] S2 后端 SSE 接口
- [ ] S3 前端流式页面
- [ ] S4 Prompt 变量与首字延迟
- [ ] S5 结构化抽取接口
- [ ] S6 工程化收尾
- [ ] S7 文档与样例
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

- [Dify 部署记录](docs/dify-setup.md)

启动步骤将在 S1 完成后补充。
