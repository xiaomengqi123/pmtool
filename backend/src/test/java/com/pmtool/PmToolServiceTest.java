package com.pmtool;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
    void managerCannotPersistInvalidProjectOrTaskStatus() {
        PmToolService service = service();
        authenticate(1L, "PM");

        assertThatThrownBy(() -> service.saveProject(new ProjectInput("项目", "PM-001", null, null, "archived", null, null), null))
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

        assertThatThrownBy(() -> service.reviewWorkLog(30L, true, null))
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
