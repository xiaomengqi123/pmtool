package com.pmtool;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

interface UserRepository extends JpaRepository<UserAccount, Long> { Optional<UserAccount> findByUsernameAndDeletedFalse(String username); Page<UserAccount> findByDeletedFalse(Pageable pageable); }
interface DepartmentRepository extends JpaRepository<Department, Long> { List<Department> findByDeletedFalseOrderByNameAsc(); }
interface RoleRepository extends JpaRepository<Role, Long> { List<Role> findAllByOrderByIdAsc(); @Query(value="select p.code from permissions p join role_permissions rp on p.id=rp.permission_id where rp.role_id=?1 order by p.code",nativeQuery=true) List<String> findPermissionCodes(Long roleId); }
interface CustomerRepository extends JpaRepository<Customer, Long> { Page<Customer> findByDeletedFalse(Pageable pageable); }
interface CustomerContactRepository extends JpaRepository<CustomerContact, Long> { List<CustomerContact> findByCustomerIdAndDeletedFalse(Long customerId); }
interface CustomerFollowUpRepository extends JpaRepository<CustomerFollowUp, Long> { List<CustomerFollowUp> findByCustomerIdAndDeletedFalseOrderByFollowUpAtDesc(Long customerId); }
interface ProjectRepository extends JpaRepository<Project, Long> { Page<Project> findByDeletedFalse(Pageable pageable); List<Project> findByManagerIdAndDeletedFalse(Long managerId); }
interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> { boolean existsByIdProjectIdAndIdUserId(Long projectId, Long userId); List<ProjectMember> findByIdProjectId(Long projectId); }
interface MilestoneRepository extends JpaRepository<Milestone, Long> { List<Milestone> findByProjectIdAndDeletedFalseOrderByDueDateAsc(Long projectId); List<Milestone> findByDueDateBetweenAndDeletedFalse(LocalDateTime from, LocalDateTime to); }
interface TaskRepository extends JpaRepository<TaskItem, Long> { Page<TaskItem> findByDeletedFalse(Pageable pageable); List<TaskItem> findByProjectIdAndDeletedFalseOrderBySortOrderAsc(Long projectId); List<TaskItem> findByProjectIdAndDeletedFalse(Long projectId); }
interface TaskDependencyRepository extends JpaRepository<TaskDependency, TaskDependencyId> { List<TaskDependency> findByIdTaskId(Long taskId); }
interface WorkLogRepository extends JpaRepository<WorkLog, Long> { Page<WorkLog> findByDeletedFalse(Pageable pageable); }
interface NotificationRepository extends JpaRepository<NotificationItem, Long> { List<NotificationItem> findByUserIdOrderByCreatedAtDesc(Long userId); long countByUserIdAndReadFalse(Long userId); boolean existsByUserIdAndTitle(Long userId,String title); }
interface AttachmentRepository extends JpaRepository<Attachment, Long> { List<Attachment> findByTargetTypeAndTargetIdAndDeletedFalse(String targetType, Long targetId); }
interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Long> { List<ProjectDocument> findByProjectIdAndDeletedFalse(Long projectId); }
interface ProjectRiskRepository extends JpaRepository<ProjectRisk, Long> { List<ProjectRisk> findByProjectIdAndDeletedFalse(Long projectId); }
interface OperationLogRepository extends JpaRepository<OperationLog, Long> { long deleteByCreatedAtBefore(LocalDateTime before); Page<OperationLog> findAllByOrderByCreatedAtDesc(Pageable pageable); }
