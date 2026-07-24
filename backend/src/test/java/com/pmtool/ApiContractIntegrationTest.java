package com.pmtool;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ApiContractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired NotificationRepository notifications;

    @Test
    void loginReturnsStandardResponseAndJwtCanReadCurrentUser() throws Exception {
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test-admin\",\"password\":\"test-password-123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.message").value("ok"))
            .andExpect(jsonPath("$.data.token").isString())
            .andExpect(jsonPath("$.data.user.roleCode").value("ADMIN"))
            .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.token");

        mvc.perform(get("/api/v1/auth/info").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.username").value("test-admin"));
    }

    @Test
    void traceIdIsReturnedAndStoredWithTheAuditLog() throws Exception {
        String traceId = "audit-trace-123";
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                .header("X-Trace-Id", traceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test-admin\",\"password\":\"test-password-123\"}"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Trace-Id", traceId))
            .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.token");

        mvc.perform(get("/api/v1/operation-logs").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[?(@.traceId == 'audit-trace-123')]").isNotEmpty());
    }

    @Test
    void protectedEndpointRejectsUnauthenticatedRequest() throws Exception {
        mvc.perform(get("/api/v1/auth/info"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledUsersTokenCannotAccessProtectedEndpoint() throws Exception {
        UserAccount disabled = new UserAccount("disabled-user", "hash", "已停用用户", "MEMBER");
        disabled.enabled = false;
        users.save(disabled);

        mvc.perform(get("/api/v1/auth/info").header("Authorization", "Bearer " + jwt.create(disabled)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void membersCannotReadAuditLogsOrModifyAnotherUsersNotification() throws Exception {
        UserAccount member = new UserAccount("notification-member", "hash", "成员", "MEMBER");
        users.save(member);
        UserAccount other = new UserAccount("notification-owner", "hash", "其他成员", "MEMBER");
        users.save(other);
        NotificationItem notification = notifications.save(new NotificationItem(other.id, "测试通知", "内容", "test"));
        String token = jwt.create(member);

        mvc.perform(get("/api/v1/operation-logs").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/notifications/" + notification.id + "/read").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void openApiDocumentExposesAuthenticationContract() throws Exception {
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openapi").isString())
            .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists());
    }

    @Test
    void coreListEndpointsAcceptFilterAndPaginationParameters() throws Exception {
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test-admin\",\"password\":\"test-password-123\"}"))
            .andExpect(status().isOk())
            .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.token");

        for (String endpoint : new String[] { "/api/v1/customers", "/api/v1/projects", "/api/v1/tasks" }) {
            mvc.perform(get(endpoint)
                    .param("page", "1")
                    .param("pageSize", "20")
                    .param("keyword", "none")
                    .param("status", "planning")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.page").value(1));
        }
        mvc.perform(get("/api/v1/users")
                .param("page", "1")
                .param("pageSize", "20")
                .param("keyword", "none")
                .param("roleCode", "MEMBER")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items").isArray());
        mvc.perform(get("/api/v1/work-logs")
                .param("page", "1")
                .param("pageSize", "20")
                .param("status", "pending")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void projectDeliveryFlowCoversRolesTasksAndWorkLogReview() throws Exception {
        String adminToken = login("test-admin", "test-password-123");
        Long managerId = createUser(adminToken, "e2e-manager", "项目经理", "PM");
        Long memberId = createUser(adminToken, "e2e-member", "项目成员", "MEMBER");
        String managerToken = login("e2e-manager", "e2e-password-123");
        String memberToken = login("e2e-member", "e2e-password-123");

        MvcResult project = mvc.perform(post("/api/v1/projects")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"端到端项目\",\"code\":\"E2E-001\",\"description\":\"回归项目\",\"status\":\"planning\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.managerId").value(managerId))
            .andReturn();
        Long projectId = readId(project, "$.data.id");

        mvc.perform(post("/api/v1/projects/" + projectId + "/members")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + memberId + ",\"roleCode\":\"MEMBER\"}"))
            .andExpect(status().isOk());

        MvcResult task = mvc.perform(post("/api/v1/tasks")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\":" + projectId + ",\"title\":\"端到端任务\",\"assigneeId\":" + memberId + ",\"status\":\"todo\",\"priority\":\"high\",\"estimatedHours\":8,\"progress\":0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.assigneeId").value(memberId))
            .andReturn();
        Long taskId = readId(task, "$.data.id");

        mvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"in_progress\"}"))
            .andExpect(status().isOk());

        MvcResult workLog = mvc.perform(post("/api/v1/work-logs")
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskId\":" + taskId + ",\"hours\":2.5,\"workDate\":\"2026-07-24\",\"description\":\"实现任务\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("pending"))
            .andReturn();
        Long workLogId = readId(workLog, "$.data.id");

        mvc.perform(post("/api/v1/work-logs/" + workLogId + "/reject")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"请补充说明\"}"))
            .andExpect(status().isOk());

        MvcResult resubmitted = mvc.perform(put("/api/v1/work-logs/" + workLogId)
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskId\":" + taskId + ",\"hours\":3,\"workDate\":\"2026-07-24\",\"description\":\"补充说明后重提\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("pending"))
            .andReturn();

        mvc.perform(post("/api/v1/work-logs/" + workLogId + "/approve")
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isOk());

        mvc.perform(put("/api/v1/work-logs/" + workLogId)
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskId\":" + taskId + ",\"hours\":3,\"workDate\":\"2026-07-24\",\"description\":\"成员无权改已审批\"}"))
            .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/work-logs/" + workLogId)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskId\":" + taskId + ",\"hours\":3.5,\"workDate\":\"2026-07-24\",\"description\":\"经理修改后重新审批\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("pending"));

        mvc.perform(get("/api/v1/work-logs").header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[?(@.id == " + workLogId + ")].canManage").value(true));
        org.assertj.core.api.Assertions.assertThat(readId(resubmitted, "$.data.id")).isEqualTo(workLogId);
    }

    @Test
    void projectUpdateRejectsStaleVersion() throws Exception {
        String adminToken = login("test-admin", "test-password-123");
        MvcResult created = mvc.perform(post("/api/v1/projects")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"并发项目\",\"code\":\"VERSION-001\",\"status\":\"planning\"}"))
            .andExpect(status().isOk())
            .andReturn();
        Long projectId = readId(created, "$.data.id");
        Number version = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.data.version");
        String payload = "{\"name\":\"并发项目已更新\",\"code\":\"VERSION-001\",\"status\":\"in_progress\",\"version\":" + version.longValue() + "}";

        mvc.perform(put("/api/v1/projects/" + projectId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());
        mvc.perform(put("/api/v1/projects/" + projectId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(40900));
    }

    @Test
    void reorderRejectsStaleTaskVersions() throws Exception {
        String adminToken = login("test-admin", "test-password-123");
        MvcResult project = mvc.perform(post("/api/v1/projects")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"排序项目\",\"code\":\"REORDER-001\",\"status\":\"planning\"}"))
            .andExpect(status().isOk())
            .andReturn();
        Long projectId = readId(project, "$.data.id");
        Long firstTaskId = createTask(adminToken, projectId, "任务一");
        Long secondTaskId = createTask(adminToken, projectId, "任务二");
        String order = "{\"tasks\":[{\"taskId\":" + secondTaskId + ",\"version\":0},{\"taskId\":" + firstTaskId + ",\"version\":0}]}";

        mvc.perform(post("/api/v1/tasks/project/" + projectId + "/reorder")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(order))
            .andExpect(status().isOk());
        mvc.perform(post("/api/v1/tasks/project/" + projectId + "/reorder")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(order))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(40900));
    }

    private String login(String username, String password) throws Exception {
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        return com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.token");
    }

    private Long createUser(String token, String username, String displayName, String roleCode) throws Exception {
        MvcResult user = mvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"e2e-password-123\",\"displayName\":\"" + displayName + "\",\"roleCode\":\"" + roleCode + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        return readId(user, "$.data.id");
    }

    private Long createTask(String token, Long projectId, String title) throws Exception {
        MvcResult task = mvc.perform(post("/api/v1/tasks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\":" + projectId + ",\"title\":\"" + title + "\",\"status\":\"todo\",\"priority\":\"medium\",\"progress\":0}"))
            .andExpect(status().isOk())
            .andReturn();
        return readId(task, "$.data.id");
    }

    private Long readId(MvcResult result, String path) throws Exception {
        Number id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), path);
        return id.longValue();
    }
}
