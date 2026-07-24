# PMTool 自检报告

> 本项目此前没有独立的 `wf-self-check-cases.md`；本报告依据已确认实施文档和代码实现进行回归核验。

## 自动化结果

| 检查项 | 结果 | 证据 |
| --- | --- | --- |
| 前端单元测试 | 通过 | `npm test`：Vitest 5/5 通过，覆盖工作台、项目、任务与工时角色入口。 |
| 前端类型检查与构建 | 通过 | `npm run build`：`vue-tsc -b` 与 Vite 构建通过。 |
| 后端单元/上下文/安全测试 | 通过 | `mvn test`：18/18 通过，含用户目录授权与软删除、任务负责人范围、客户资料与任务/工时项目范围、状态筛选分页、登录/JWT 与 OpenAPI 契约回归。 |
| 后端可发布包 | 通过 | `mvn package` 已生成 `backend/target/pmtool-1.0.0.jar`。 |

## 功能核验

| 编号 | 目标 | 结果 | 代码依据 |
| --- | --- | --- | --- |
| TC-AUTH-01 | JWT 登录、路由守卫与角色入口限制 | 通过 | `frontend/src/stores/auth.ts`、`frontend/src/router/index.ts`。 |
| TC-CRUD-01 | 客户、项目、任务、里程碑可由管理角色软删除 | 通过 | 删除入口带二次确认；任务后端校验项目管理权限并保留审计记录。 |
| TC-CRUD-02 | 管理员可软删除其他用户，不能删除当前登录账号 | 通过 | 用户删除保留审计日志并拒绝自删，前端带二次确认。 |
| TC-LIST-01 | 客户、项目、任务、人员、工时支持分页筛选 | 通过 | 服务端在数据范围授权后执行筛选与分页；对应列表均提供查询和分页控件。 |
| TC-PERM-01 | 管理员全局、项目经理项目范围、成员仅本人任务进度 | 通过 | `PmToolService.saveTask`、`updateTaskStatus` 与项目页面权限入口。 |
| TC-PERM-02 | 成员在全局任务页仅能保存本人任务状态和进度 | 通过 | `TaskView` 仅对负责人显示保存入口，后端仍进行最终项目范围与负责人校验。 |
| TC-PERM-03 | 客户、联系人与跟进记录仅管理角色可读取 | 通过 | 客户相关读取接口执行 `requireManager`，前端菜单与后端授权一致。 |
| TC-PERM-04 | 成员不能读取全量用户清单 | 通过 | `/users/all` 限制为管理角色，成员端不再请求该接口。 |
| TC-TASK-01 | 任务乐观锁冲突返回 409 | 通过 | `PmToolService.saveTask`、`updateTaskStatus` 的版本校验。 |
| TC-TASK-02 | 任务负责人必须属于项目成员或为项目经理 | 通过 | `PmToolService.saveTask` 校验负责人存在并校验项目成员关系。 |
| TC-ENUM-01 | 项目、任务、里程碑状态与任务优先级均限制为约定枚举 | 通过 | 服务端枚举白名单校验；前端里程碑完成状态统一为 `completed`。 |
| TC-WORKLOG-01 | 工时提交、审批/驳回、重提和审批后修改重置待审 | 通过 | `PmToolService.saveWorkLog` 与工时页面。 |
| TC-WORKLOG-02 | 项目经理只能审批自己负责项目的工时 | 通过 | `PmToolService.reviewWorkLog` 按工时所属任务和项目验证项目经理范围。 |
| TC-WORKLOG-03 | 非负责项目的项目经理不能修改他人工时或已审批工时 | 通过 | `PmToolService.saveWorkLog` 将项目管理权与普通成员自身工时权限分离。 |
| TC-WORKLOG-04 | 工时页面仅展示当前用户可管理项目的审批/修改入口 | 通过 | 工时接口返回 `canManage`，页面据此呈现操作入口；后端继续执行最终授权。 |
| TC-ATTACH-01 | 任意类型、单文件最大 2 MB、受控下载和删除 | 通过 | `ObjectStorage.java` 与项目附件页面。 |
| TC-AUDIT-01 | 操作日志与 180 天清理 | 通过 | `PmToolService.cleanLogs`。 |

## 待部署环境验证

- 使用 MySQL 8 执行 Flyway 全量迁移。
- 验证 MinIO 上传、下载和 2 MB 边界文件。
- 以管理员、项目经理、成员三个账号完成浏览器端核心闭环回归。

> 发布配置已为 multipart 请求边界预留 3 MB 总请求空间；后端仍以实际文件字节严格拒绝超过 2 MB 的单文件。
