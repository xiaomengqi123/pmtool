# PMTool 代码审查报告

## 结论

本轮未发现 `v-html`、`eval`、硬编码凭据或未清理的全局事件监听。已修复两项权限入口问题，并将前端源码统一格式化，便于后续维护与审计。

## 已修复项

| 编号 | 类型 | 位置 | 处理结果 |
| --- | --- | --- | --- |
| PR-01 | FE-AQ 权限入口 | `frontend/src/views/ProjectView.vue` | 项目编辑列仅管理员/项目经理可见。 |
| PR-02 | FE-AQ 权限入口 | `frontend/src/views/ProjectDetailView.vue` | 成员仅能推进和编辑本人任务的进度；经理可维护任务元数据。 |
| PR-03 | FE-AQ 附件归属 | `backend/src/main/java/com/pmtool/ObjectStorage.java`、项目附件页面 | 附件响应返回 `uploaderId`；删除按钮仅对上传者或项目经理显示，后端仍作最终授权。 |
| PR-04 | 工程可维护性 | `frontend/src/**/*.{vue,ts}` | 使用 Prettier 完成一致格式化。 |

## 通过的检查

- 路由守卫在 `frontend/src/router/index.ts` 加载当前用户并限制管理员、项目经理路由。
- Axios 在 `frontend/src/api/http.ts` 统一注入 JWT、处理 401 和 API 错误响应。
- 后端任务与工时均以版本字段处理并发冲突；成员只能操作自己的任务，项目范围由服务端校验。
- 附件后端限制单文件不超过 2 MB，下载和删除都在项目访问权限范围内校验。

## 仍需关注

- 生产构建的主包约 1.09 MB，当前不影响功能；上线后可按访问数据再引入 Element Plus 按需组件与更细的 vendor 分包。
- 本地没有 Docker，因此 MySQL Flyway 迁移和 MinIO 实例需在部署环境完成一次实测。
