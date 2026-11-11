# Dify 本地部署记录

> 本文件只记录部署信息，不记录管理员密码、模型 Key、App API Key。

| 项目 | 值 |
|---|---|
| Dify 版本 | 1.19.0 |
| 编排引擎 | Graphon 0.8.0 |
| 源码目录 | `E:\Dify\dify` |
| Compose 目录 | `E:\Dify\dify\docker` |
| 启动命令 | `docker compose up -d`（在 Compose 目录执行） |
| 停止命令 | `docker compose down` |
| 访问地址 | 启动后以 `docker compose ps` 的端口映射为准 |
| 当前状态 | 2026-11-11：镜像已存在，容器未启动 |
| Chatbot 名称 | 待创建 |
| 测试问题与结果 | 待验证 |

## 复核命令

```powershell
docker images --format "table {{.Repository}}\t{{.Tag}}" | Select-String dify
docker exec <dify-api容器名> sh -c "grep -m1 '^version' /app/api/pyproject.toml"
```

## 备注

- 阶段一 Dify 只做独立部署与 Chatbot，不接入主链路。
- Dify 的模型供应商可使用与后端相同的 DeepSeek API。
