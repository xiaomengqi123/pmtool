CREATE TABLE customer_contacts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  position_name VARCHAR(100) NULL,
  phone VARCHAR(50) NULL,
  email VARCHAR(150) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT NOT NULL DEFAULT b'0',
  INDEX idx_customer_contacts_customer (customer_id, deleted)
);
CREATE TABLE customer_follow_ups (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  content VARCHAR(1000) NOT NULL,
  follow_up_at DATETIME NOT NULL,
  creator_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT NOT NULL DEFAULT b'0',
  INDEX idx_customer_followups_customer (customer_id, deleted, follow_up_at)
);
CREATE INDEX idx_project_members_user ON project_members (user_id, project_id);
