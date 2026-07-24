package com.pmtool;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PmToolServiceTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void administratorCannotCreateUserWithInvalidRoleOrWeakPassword() {
        PmToolService service = service();
        authenticate(1L, "ADMIN");

        assertThatThrownBy(() -> service.createUser(new UserInput("member", "12345678", "成员", "OWNER", null)))
            .isInstanceOf(BusinessException.class)
            .hasMessage("角色无效");
        assertThatThrownBy(() -> service.createUser(new UserInput("member", "123", "成员", "MEMBER", null)))
            .isInstanceOf(BusinessException.class)
            .hasMessage("初始密码至少 8 位");
    }

    @Test
    void managerCannotPersistBlankCustomerOrProjectNames() {
        PmToolService service = service();
        authenticate(1L, "PM");

        assertThatThrownBy(() -> service.saveCustomer(new CustomerInput(" ", null, null, null, null), null))
            .isInstanceOf(BusinessException.class)
            .hasMessage("客户名称不能为空");
        assertThatThrownBy(() -> service.saveProject(new ProjectInput("项目", " ", null, null, null, null, null, null), null))
            .isInstanceOf(BusinessException.class)
            .hasMessage("项目名称和编码不能为空");
        assertThatThrownBy(() -> service.saveProject(new ProjectInput("项目", "PM-001", null, null, null, LocalDateTime.of(2026, 7, 2, 0, 0), LocalDateTime.of(2026, 7, 1, 0, 0), null), null))
            .isInstanceOf(BusinessException.class)
            .hasMessage("项目开始时间不能晚于结束时间");
    }

    @Test
    void memberCannotReadGlobalUserDirectory() {
        PmToolService service = service();
        authenticate(2L, "MEMBER");

        assertThatThrownBy(service::allUsers)
            .isInstanceOf(BusinessException.class)
            .extracting(error -> ((BusinessException) error).status)
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void administratorCanSoftDeleteAnotherUserButNotThemselves() {
        UserRepository users = mock(UserRepository.class);
        UserAccount member = new UserAccount("member", "hash", "成员", "MEMBER");
        member.id = 2L;
        when(users.findById(2L)).thenReturn(Optional.of(member));
        PmToolService service = new PmToolService(users, mock(CustomerRepository.class), mock(ProjectRepository.class), mock(ProjectMemberRepository.class), mock(MilestoneRepository.class), mock(TaskRepository.class), mock(TaskDependencyRepository.class), mock(WorkLogRepository.class), mock(NotificationRepository.class), mock(OperationLogRepository.class), mock(PasswordEncoder.class), mock(JwtService.class));
        authenticate(1L, "ADMIN");

        service.deleteUser(2L);

        assertThat(member.deleted).isTrue();
        assertThatThrownBy(() -> service.deleteUser(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("不能删除当前登录账号");
    }

    @Test
    void memberCannotUpdateAnotherMembersTask() {
        UserRepository users = mock(UserRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectMemberRepository members = mock(ProjectMemberRepository.class);
        MilestoneRepository milestones = mock(MilestoneRepository.class);
        TaskRepository tasks = mock(TaskRepository.class);
        TaskDependencyRepository dependencies = mock(TaskDependencyRepository.class);
        WorkLogRepository logs = mock(WorkLogRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        OperationLogRepository operationLogs = mock(OperationLogRepository.class);
        PmToolService service = new PmToolService(users, customers, projects, members, milestones, tasks, dependencies, logs, notifications, operationLogs, mock(PasswordEncoder.class), mock(JwtService.class));
        Project project = new Project();
        project.id = 10L;
        project.managerId = 1L;
        TaskItem task = new TaskItem();
        task.id = 20L;
        task.projectId = 10L;
        task.assigneeId = 3L;
        task.version = 1L;
        task.progress = BigDecimal.ZERO;
        when(tasks.findById(20L)).thenReturn(Optional.of(task));
        when(projects.findById(10L)).thenReturn(Optional.of(project));
        when(members.existsByIdProjectIdAndIdUserId(10L, 2L)).thenReturn(true);
        authenticate(2L, "MEMBER");

        assertThatThrownBy(() -> service.updateTaskStatus(20L, "done", 1L))
            .isInstanceOf(BusinessException.class)
            .extracting(error -> ((BusinessException) error).status)
            .isEqualTo(HttpStatus.FORBIDDEN);
        authenticate(2L, "PM");
        assertThatThrownBy(() -> service.updateTaskStatus(20L, "done", 1L))
            .isInstanceOf(BusinessException.class)
            .extracting(error -> ((BusinessException) error).status)
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void managerCannotAssignTaskToNonProjectMember() {
        UserRepository users = mock(UserRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectMemberRepository members = mock(ProjectMemberRepository.class);
        TaskRepository tasks = mock(TaskRepository.class);
        PmToolService service = new PmToolService(users, customers, projects, members, mock(MilestoneRepository.class), tasks, mock(TaskDependencyRepository.class), mock(WorkLogRepository.class), mock(NotificationRepository.class), mock(OperationLogRepository.class), mock(PasswordEncoder.class), mock(JwtService.class));
        Project project = new Project();
        project.id = 10L;
        project.managerId = 1L;
        UserAccount user = new UserAccount("member", "hash", "成员", "MEMBER");
        user.id = 2L;
        when(projects.findById(10L)).thenReturn(Optional.of(project));
        when(users.findById(2L)).thenReturn(Optional.of(user));
        when(members.existsByIdProjectIdAndIdUserId(10L, 2L)).thenReturn(false);
        authenticate(1L, "PM");

        assertThatThrownBy(() -> service.saveTask(new TaskInput(10L, "任务", null, 2L, "todo", "medium", null, BigDecimal.ZERO, null, null, null), null))
            .isInstanceOf(BusinessException.class)
            .hasMessage("负责人必须是项目成员");
    }

    @Test
    void projectScopedOwnerMustBeProjectMemberOrManager() {
        UserRepository users = mock(UserRepository.class);
        ProjectMemberRepository members = mock(ProjectMemberRepository.class);
        PmToolService service = new PmToolService(users, mock(CustomerRepository.class), mock(ProjectRepository.class), members, mock(MilestoneRepository.class), mock(TaskRepository.class), mock(TaskDependencyRepository.class), mock(WorkLogRepository.class), mock(NotificationRepository.class), mock(OperationLogRepository.class), mock(PasswordEncoder.class), mock(JwtService.class));
        Project project = new Project();
        project.id = 10L;
        project.managerId = 1L;
        UserAccount owner = new UserAccount("outside", "hash", "项目外成员", "MEMBER");
        owner.id = 2L;
        when(users.findById(2L)).thenReturn(Optional.of(owner));
        when(members.existsByIdProjectIdAndIdUserId(10L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> service.ensureProjectParticipant(project, 2L, "风险负责人必须是项目成员"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("风险负责人必须是项目成员");
    }

    @Test
    void projectMembersMustBeActiveAndUseValidProjectRoles() {
        UserRepository users = mock(UserRepository.class);
        ProjectRepository projects = mock(ProjectRepository.class);
        PmToolService service = new PmToolService(users, mock(CustomerRepository.class), projects, mock(ProjectMemberRepository.class), mock(MilestoneRepository.class), mock(TaskRepository.class), mock(TaskDependencyRepository.class), mock(WorkLogRepository.class), mock(NotificationRepository.class), mock(OperationLogRepository.class), mock(PasswordEncoder.class), mock(JwtService.class));
        Project project = new Project();
        project.id = 10L;
        project.managerId = 1L;
        UserAccount member = new UserAccount("member", "hash", "成员", "MEMBER");
        member.id = 2L;
        when(projects.findById(10L)).thenReturn(Optional.of(project));
        when(users.findById(2L)).thenReturn(Optional.of(member));
        authenticate(1L, "PM");

        member.enabled = false;
        assertThatThrownBy(() -> service.addMember(10L, 2L, "MEMBER"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("用户已停用");
        member.enabled = true;
        assertThatThrownBy(() -> service.addMember(10L, 2L, "ADMIN"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("项目成员角色无效");
    }

    @Test
    void projectManagerSoftDeletesTaskAndRecalculatesProjectProgress() {
        UserRepository users = mock(UserRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectMemberRepository members = mock(ProjectMemberRepository.class);
        MilestoneRepository milestones = mock(MilestoneRepository.class);
        TaskRepository tasks = mock(TaskRepository.class);
        TaskDependencyRepository dependencies = mock(TaskDependencyRepository.class);
        WorkLogRepository logs = mock(WorkLogRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        OperationLogRepository operationLogs = mock(OperationLogRepository.class);
        PmToolService service = new PmToolService(users, customers, projects, members, milestones, tasks, dependencies, logs, notifications, operationLogs, mock(PasswordEncoder.class), mock(JwtService.class));
        Project project = new Project();
        project.id = 10L;
        project.managerId = 1L;
        TaskItem task = new TaskItem();
        task.id = 20L;
        task.projectId = 10L;
        task.title = "待删除任务";
        task.progress = BigDecimal.ZERO;
        when(tasks.findById(20L)).thenReturn(Optional.of(task));
        when(projects.findById(10L)).thenReturn(Optional.of(project));
        when(tasks.findByProjectIdAndDeletedFalse(10L)).thenReturn(java.util.List.of());
        authenticate(1L, "PM");

        service.deleteTask(20L);

        assertThat(task.deleted).isTrue();
        verify(tasks).save(task);
        verify(projects).save(project);
    }

    @Test
    void projectProgressUsesTaskAverageWhenEstimatesAreZeroOrMissing() {
        TaskRepository tasks = mock(TaskRepository.class);
        ProjectRepository projects = mock(ProjectRepository.class);
        PmToolService service = new PmToolService(mock(UserRepository.class), mock(CustomerRepository.class), projects, mock(ProjectMemberRepository.class), mock(MilestoneRepository.class), tasks, mock(TaskDependencyRepository.class), mock(WorkLogRepository.class), mock(NotificationRepository.class), mock(OperationLogRepository.class), mock(PasswordEncoder.class), mock(JwtService.class));
        Project project = new Project();
        project.id = 10L;
        TaskItem first = new TaskItem();
        first.progress = BigDecimal.valueOf(20);
        first.estimatedHours = BigDecimal.ZERO;
        TaskItem second = new TaskItem();
        second.progress = BigDecimal.valueOf(80);
        second.estimatedHours = null;
        when(tasks.findByProjectIdAndDeletedFalse(10L)).thenReturn(java.util.List.of(first, second));

        service.recalculateProgress(project);

        assertThat(project.progress).isEqualByComparingTo("50.00");
        verify(projects).save(project);
    }

    @Test
    void managerCannotPersistInvalidProjectOrTaskStatus() {
        PmToolService service = service();
        authenticate(1L, "PM");

        assertThatThrownBy(() -> service.saveProject(new ProjectInput("项目", "PM-001", null, null, "archived", null, null, null), null))
            .isInstanceOf(BusinessException.class)
            .hasMessage("项目状态无效");

        UserRepository users = mock(UserRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectMemberRepository members = mock(ProjectMemberRepository.class);
        MilestoneRepository milestones = mock(MilestoneRepository.class);
        TaskRepository tasks = mock(TaskRepository.class);
        TaskDependencyRepository dependencies = mock(TaskDependencyRepository.class);
        WorkLogRepository logs = mock(WorkLogRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        OperationLogRepository operationLogs = mock(OperationLogRepository.class);
        PmToolService taskService = new PmToolService(users, customers, projects, members, milestones, tasks, dependencies, logs, notifications, operationLogs, mock(PasswordEncoder.class), mock(JwtService.class));
        Project project = new Project();
        project.id = 10L;
        project.managerId = 1L;
        TaskItem task = new TaskItem();
        task.id = 20L;
        task.projectId = 10L;
        task.progress = BigDecimal.ZERO;
        when(tasks.findById(20L)).thenReturn(Optional.of(task));
        when(projects.findById(10L)).thenReturn(Optional.of(project));
        authenticate(1L, "PM");

        assertThatThrownBy(() -> taskService.updateTaskStatus(20L, "archived", null))
            .isInstanceOf(BusinessException.class)
            .hasMessage("任务状态无效");
    }

    @Test
    void projectManagerCannotReviewWorkLogFromAnotherProject() {
        UserRepository users = mock(UserRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectMemberRepository members = mock(ProjectMemberRepository.class);
        MilestoneRepository milestones = mock(MilestoneRepository.class);
        TaskRepository tasks = mock(TaskRepository.class);
        TaskDependencyRepository dependencies = mock(TaskDependencyRepository.class);
        WorkLogRepository logs = mock(WorkLogRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        OperationLogRepository operationLogs = mock(OperationLogRepository.class);
        PmToolService service = new PmToolService(users, customers, projects, members, milestones, tasks, dependencies, logs, notifications, operationLogs, mock(PasswordEncoder.class), mock(JwtService.class));
        WorkLog workLog = new WorkLog();
        workLog.id = 30L;
        workLog.taskId = 20L;
        TaskItem task = new TaskItem();
        task.id = 20L;
        task.projectId = 10L;
        Project project = new Project();
        project.id = 10L;
        project.managerId = 1L;
        when(logs.findById(30L)).thenReturn(Optional.of(workLog));
        when(tasks.findById(20L)).thenReturn(Optional.of(task));
        when(projects.findById(10L)).thenReturn(Optional.of(project));
        authenticate(2L, "PM");

        assertThat(service.canManageWorkLog(workLog)).isFalse();
        assertThatThrownBy(() -> service.reviewWorkLog(30L, true, null))
            .isInstanceOf(BusinessException.class)
            .extracting(error -> ((BusinessException) error).status)
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void projectManagerCannotModifyAnotherMembersWorkLogOutsideManagedProject() {
        UserRepository users = mock(UserRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectMemberRepository members = mock(ProjectMemberRepository.class);
        MilestoneRepository milestones = mock(MilestoneRepository.class);
        TaskRepository tasks = mock(TaskRepository.class);
        TaskDependencyRepository dependencies = mock(TaskDependencyRepository.class);
        WorkLogRepository logs = mock(WorkLogRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        OperationLogRepository operationLogs = mock(OperationLogRepository.class);
        PmToolService service = new PmToolService(users, customers, projects, members, milestones, tasks, dependencies, logs, notifications, operationLogs, mock(PasswordEncoder.class), mock(JwtService.class));
        WorkLog workLog = new WorkLog();
        workLog.id = 30L;
        workLog.taskId = 20L;
        workLog.userId = 3L;
        workLog.status = "pending";
        TaskItem task = new TaskItem();
        task.id = 20L;
        task.projectId = 10L;
        Project project = new Project();
        project.id = 10L;
        project.managerId = 1L;
        when(tasks.findById(20L)).thenReturn(Optional.of(task));
        when(projects.findById(10L)).thenReturn(Optional.of(project));
        when(members.existsByIdProjectIdAndIdUserId(10L, 2L)).thenReturn(true);
        when(logs.findById(30L)).thenReturn(Optional.of(workLog));
        authenticate(2L, "PM");

        assertThatThrownBy(() -> service.saveWorkLog(new WorkLogInput(20L, BigDecimal.ONE, LocalDate.now(), "修改", null), 30L))
            .isInstanceOf(BusinessException.class)
            .extracting(error -> ((BusinessException) error).status)
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private PmToolService service() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByUsernameAndDeletedFalse(any())).thenReturn(Optional.empty());
        return new PmToolService(
            users,
            mock(CustomerRepository.class),
            mock(ProjectRepository.class),
            mock(ProjectMemberRepository.class),
            mock(MilestoneRepository.class),
            mock(TaskRepository.class),
            mock(TaskDependencyRepository.class),
            mock(WorkLogRepository.class),
            mock(NotificationRepository.class),
            mock(OperationLogRepository.class),
            mock(PasswordEncoder.class),
            mock(JwtService.class)
        );
    }

    private void authenticate(Long id, String role) {
        CurrentUser user = new CurrentUser(id, "test-user", role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }
}
