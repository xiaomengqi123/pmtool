package com.pmtool;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@MappedSuperclass
abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false) LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false) LocalDateTime updatedAt = LocalDateTime.now();
    @Column(nullable = false) boolean deleted = false;
    @PreUpdate void touch() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
}

@Entity @Table(name="departments")
class Department extends BaseEntity {
    @Column(nullable=false) String name; @Column(name="parent_id") Long parentId;
    protected Department() {}
}

@Entity @Table(name="roles")
class Role {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false,unique=true) String code; @Column(nullable=false) String name; String description;
    protected Role() {}
}

@Entity @Table(name = "users")
class UserAccount extends BaseEntity {
    @Column(nullable = false, unique = true) String username;
    @Column(name = "password_hash", nullable = false) String passwordHash;
    @Column(name = "display_name", nullable = false) String displayName;
    @Column(name = "role_code", nullable = false) String roleCode;
    @Column(name = "department_id") Long departmentId;
    @Column(nullable = false) boolean enabled = true;
    protected UserAccount() {}
    UserAccount(String username, String passwordHash, String displayName, String roleCode) { this.username=username; this.passwordHash=passwordHash; this.displayName=displayName; this.roleCode=roleCode; }
}

@Entity @Table(name = "customers")
class Customer extends BaseEntity {
    @Column(nullable=false) String name; @Column(nullable=false) String level="normal"; @Column(nullable=false) String status="active";
    @Column(name="contact_name") String contactName; String phone;
    protected Customer() {}
}

@Entity @Table(name = "customer_contacts")
class CustomerContact extends BaseEntity {
    @Column(name="customer_id", nullable=false) Long customerId;
    @Column(nullable=false) String name;
    @Column(name="position_name") String positionName;
    String phone; String email;
    protected CustomerContact() {}
}

@Entity @Table(name = "customer_follow_ups")
class CustomerFollowUp extends BaseEntity {
    @Column(name="customer_id", nullable=false) Long customerId;
    @Column(nullable=false, length=1000) String content;
    @Column(name="follow_up_at", nullable=false) LocalDateTime followUpAt;
    @Column(name="creator_id", nullable=false) Long creatorId;
    protected CustomerFollowUp() {}
}

@Entity @Table(name = "projects")
class Project extends BaseEntity {
    @Column(nullable=false) String name; @Column(nullable=false, unique=true) String code;
    @Column(name="customer_id") Long customerId; @Column(name="manager_id", nullable=false) Long managerId;
    @Column(nullable=false) String status="planning"; @Column(columnDefinition="TEXT") String description;
    @Column(name="start_date") LocalDateTime startDate; @Column(name="end_date") LocalDateTime endDate;
    @Column(nullable=false) BigDecimal progress=BigDecimal.ZERO; @Version Long version;
    protected Project() {}
}

@Entity @Table(name = "milestones")
class Milestone {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="project_id",nullable=false) Long projectId;
    @Column(nullable=false) String name;
    @Column(name="due_date") LocalDateTime dueDate;
    @Column(nullable=false) String status="pending";
    @Column(nullable=false) boolean deleted=false;
    protected Milestone() {}
}

@Entity @Table(name = "project_members")
class ProjectMember {
    @EmbeddedId ProjectMemberId id; @Column(name="role_code", nullable=false) String roleCode="MEMBER";
    protected ProjectMember() {}
    ProjectMember(Long projectId, Long userId, String roleCode) { this.id=new ProjectMemberId(projectId,userId); this.roleCode=roleCode; }
}
@Embeddable
class ProjectMemberId implements java.io.Serializable {
    @Column(name="project_id") Long projectId; @Column(name="user_id") Long userId;
    protected ProjectMemberId() {} ProjectMemberId(Long projectId,Long userId){this.projectId=projectId;this.userId=userId;}
    @Override public boolean equals(Object o){return o instanceof ProjectMemberId x && java.util.Objects.equals(projectId,x.projectId)&&java.util.Objects.equals(userId,x.userId);}
    @Override public int hashCode(){return java.util.Objects.hash(projectId,userId);}
}

@Entity @Table(name = "project_tasks")
class TaskItem extends BaseEntity {
    @Column(name="project_id", nullable=false) Long projectId; @Column(nullable=false) String title;
    @Column(columnDefinition="TEXT") String description; @Column(name="assignee_id") Long assigneeId;
    @Column(nullable=false) String status="todo"; @Column(nullable=false) String priority="medium";
    @Column(name="estimated_hours") BigDecimal estimatedHours; @Column(nullable=false) BigDecimal progress=BigDecimal.ZERO;
    @Column(name="sort_order", nullable=false) Integer sortOrder=0; @Column(name="start_date") LocalDateTime startDate; @Column(name="due_date") LocalDateTime dueDate;
    @Version Long version;
    protected TaskItem() {}
}

@Entity @Table(name = "task_dependencies")
class TaskDependency {
    @EmbeddedId TaskDependencyId id;
    protected TaskDependency() {}
    TaskDependency(Long taskId,Long dependsOnTaskId){this.id=new TaskDependencyId(taskId,dependsOnTaskId);}
}
@Embeddable
class TaskDependencyId implements java.io.Serializable {
    @Column(name="task_id") Long taskId; @Column(name="depends_on_task_id") Long dependsOnTaskId;
    protected TaskDependencyId() {} TaskDependencyId(Long taskId,Long dependsOnTaskId){this.taskId=taskId;this.dependsOnTaskId=dependsOnTaskId;}
    @Override public boolean equals(Object o){return o instanceof TaskDependencyId x&&java.util.Objects.equals(taskId,x.taskId)&&java.util.Objects.equals(dependsOnTaskId,x.dependsOnTaskId);}
    @Override public int hashCode(){return java.util.Objects.hash(taskId,dependsOnTaskId);}
}

@Entity @Table(name = "work_logs")
class WorkLog extends BaseEntity {
    @Column(name="task_id", nullable=false) Long taskId; @Column(name="user_id", nullable=false) Long userId;
    @Column(nullable=false) BigDecimal hours; @Column(name="work_date", nullable=false) LocalDate workDate;
    String description; @Column(nullable=false) String status="pending"; @Column(name="reviewer_id") Long reviewerId; @Column(name="review_comment") String reviewComment;
    @Version Long version;
    protected WorkLog() {}
}

@Entity @Table(name = "notifications")
class NotificationItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="user_id", nullable=false) Long userId; @Column(nullable=false) String title; @Column(nullable=false) String content; @Column(nullable=false) String type;
    @Column(name="is_read", nullable=false) boolean read=false; @Column(name="created_at", nullable=false) LocalDateTime createdAt=LocalDateTime.now();
    protected NotificationItem() {}
    NotificationItem(Long userId, String title, String content, String type) { this.userId=userId; this.title=title; this.content=content; this.type=type; }
}

@Entity @Table(name = "attachments")
class Attachment extends BaseEntity {
    @Column(name="target_type",nullable=false) String targetType; @Column(name="target_id",nullable=false) Long targetId;
    @Column(name="original_name",nullable=false) String originalName; @Column(name="object_key",nullable=false) String objectKey; @Column(name="content_type") String contentType;
    @Column(name="size_bytes",nullable=false) Long sizeBytes; @Column(name="uploader_id",nullable=false) Long uploaderId;
    protected Attachment() {}
}

@Entity @Table(name = "project_documents")
class ProjectDocument {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(name="project_id",nullable=false) Long projectId; @Column(nullable=false) String title; @Column(columnDefinition="TEXT") String content; @Column(name="created_at",nullable=false) LocalDateTime createdAt=LocalDateTime.now(); @Column(nullable=false) boolean deleted=false;
    protected ProjectDocument() {}
}
@Entity @Table(name = "project_risks")
class ProjectRisk {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(name="project_id",nullable=false) Long projectId; @Column(nullable=false) String title; @Column(nullable=false) String level="medium"; @Column(nullable=false) String status="open"; @Column(name="owner_id") Long ownerId; @Column(name="created_at",nullable=false) LocalDateTime createdAt=LocalDateTime.now(); @Column(nullable=false) boolean deleted=false;
    protected ProjectRisk() {}
}

@Entity @Table(name = "operation_logs")
class OperationLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(name="user_id") Long userId;
    @Column(nullable=false) String action; @Column(name="resource_type",nullable=false) String resourceType; @Column(name="resource_id") Long resourceId; String detail; @Column(name="trace_id") String traceId;
    @Column(name="created_at",nullable=false) LocalDateTime createdAt=LocalDateTime.now();
    protected OperationLog() {}
    OperationLog(Long userId,String action,String resourceType,Long resourceId,String detail,String traceId){this.userId=userId;this.action=action;this.resourceType=resourceType;this.resourceId=resourceId;this.detail=detail;this.traceId=traceId;}
}
