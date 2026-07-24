# PMTool 自检报告

> 本项目此前没有独立的 `wf-self-check-cases.md`；本报告依据已确认实施文档和代码实现进行回归核验。

## 自动化结果

| 检查项 | 结果 | 证据 |
| --- | --- | --- |
| 前端单元测试 | 通过 | `npm test`：Vitest 1/1 通过。 |
| 前端类型检查与构建 | 通过 | `npm run build`：`vue-tsc -b` 与 Vite 构建通过。 |
| 后端单元/上下文/安全测试 | 通过 | `mvn test`：4/4 通过。 |

## 功能核验

| 编号 | 目标 | 结果 | 代码依据 |
| --- | --- | --- | --- |
| TC-AUTH-01 | JWT 登录、路由守卫与角色入口限制 | 通过 | `frontend/src/stores/auth.ts`、`frontend/src/router/index.ts`。 |
| TC-PERM-01 | 管理员全局、项目经理项目范围、成员仅本人任务进度 | 通过 | `PmToolService.saveTask`、`updateTaskStatus` 与项目页面权限入口。 |
| TC-TASK-01 | 任务乐观锁冲突返回 409 | 通过 | `PmToolService.saveTask`、`updateTaskStatus` 的版本校验。 |
| TC-WORKLOG-01 | 工时提交、审批/驳回、重提和审批后修改重置待审 | 通过 | `PmToolService.saveWorkLog` 与工时页面。 |
| TC-ATTACH-01 | 任意类型、单文件最大 2 MB、受控下载和删除 | 通过 | `ObjectStorage.java` 与项目附件页面。 |
| TC-AUDIT-01 | 操作日志与 180 天清理 | 通过 | `PmToolService.cleanLogs`。 |

## 待部署环境验证

- 使用 MySQL 8 执行 Flyway 全量迁移。
- 验证 MinIO 上传、下载和 2 MB 边界文件。
- 以管理员、项目经理、成员三个账号完成浏览器端核心闭环回归。
