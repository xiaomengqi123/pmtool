INSERT IGNORE INTO roles (code, name, description) VALUES
  ('ADMIN', '管理员', '全局管理与审批权限'),
  ('PM', '项目经理', '负责项目的管理与审批权限'),
  ('MEMBER', '项目成员', '参与项目与本人任务执行权限');
INSERT IGNORE INTO permissions (code, name) VALUES
  ('system:manage', '系统管理'), ('project:manage', '项目管理'),
  ('task:update:self', '更新本人任务'), ('worklog:submit', '提交工时'),
  ('worklog:review', '审批工时');
