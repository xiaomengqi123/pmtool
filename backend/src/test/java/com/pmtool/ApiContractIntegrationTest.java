package com.pmtool;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
}
