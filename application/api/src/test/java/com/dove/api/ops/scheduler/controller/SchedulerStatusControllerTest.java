package com.dove.api.ops.scheduler.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.jobstatus.JobState;
import com.dove.jobstatus.JobStatus;
import com.dove.jobstatus.JobStatusRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class SchedulerStatusControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean JobStatusRegistry jobStatusRegistry;

    @BeforeEach
    void setUp() {
        JobStatus status = new JobStatus("PRICE_DAILY", JobState.RUNNING, 100L, 40L, 0L, 0L, null);
        given(jobStatusRegistry.all()).willReturn(List.of(status));
    }

    @Nested
    @DisplayName("GET /admin/ops/scheduler/status")
    class GetStatus {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/admin/ops/scheduler/status"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(role = "USER")
        @DisplayName("USER 권한이면 403")
        void shouldReturn403WhenUser() throws Exception {
            mockMvc.perform(get("/admin/ops/scheduler/status"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 권한이면 진행 상태 목록 조회 200")
        void shouldReturnStatusListWhenRoot() throws Exception {
            mockMvc.perform(get("/admin/ops/scheduler/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].name").value("PRICE_DAILY"))
                    .andExpect(jsonPath("$[0].state").value("RUNNING"));
        }
    }
}
