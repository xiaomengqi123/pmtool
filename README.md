# PMTool

项目管理系统：Vue 3 + Java 17 + Spring Boot + MySQL + MinIO。

## 本地启动

1. 复制 `deploy/.env.example` 为本地环境变量文件并填入实际值。
2. 执行 `docker compose -f docker-compose.dev.yml up -d` 启动 MySQL 8 和 MinIO；数据库为 `pmtool_db`。
3. 后端：`cd backend && mvn spring-boot:run`（端口 5959）。
4. 前端：`cd frontend && npm install && npm run dev`（端口 8989）。

## 构建与验证

- 后端：`cd backend && mvn test && mvn package`
- 前端：`cd frontend && npm run build`
- 健康检查：`curl http://127.0.0.1:5959/actuator/health`

部署前后的执行清单见 [deploy/上线检查清单.md](deploy/上线检查清单.md)。

生产密钥、数据库密码和对象存储凭据不得提交到仓库。
