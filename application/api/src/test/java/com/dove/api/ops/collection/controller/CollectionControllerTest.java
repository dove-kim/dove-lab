package com.dove.api.ops.collection.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.application.service.CollectionLauncher;
import com.dove.stockcollection.application.service.CollectionTaskService;
import com.dove.stockcollection.domain.entity.CollectionTask;
import com.dove.stockcollection.domain.enums.CollectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class CollectionControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean CollectionLauncher launcher;
    @MockitoBean CollectionTaskService taskService;

    @BeforeEach
    void setUp() {
        given(launcher.launchPriceCollection(any(StockExchange.class), any(), any(), any(), anyLong()))
                .willReturn(11L);
        given(launcher.launchEventCollection(any(), any(), anyLong())).willReturn(22L);
        given(launcher.launchStockCollection(any(), any(), anyLong())).willReturn(33L);
        given(launcher.launchInvestorCollection(any(), any(), anyLong())).willReturn(55L);
        given(launcher.relaunch(anyLong(), anyLong())).willReturn(44L);

        CollectionTask task = new CollectionTask(CollectionType.PRICE, StockExchange.KOSPI,
                LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31), 1L);
        given(taskService.findRecent(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(task)));
        given(taskService.find(eq(7L))).willReturn(Optional.of(task));
    }

    @Nested
    @DisplayName("POST /admin/ops/collection/price")
    class StartPriceCollection {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(post("/admin/ops/collection/price")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"exchange":"KOSPI","from":"2020-01-01","to":"2020-01-31"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 권한이면 주가 재조회 시작 202 + 작업ID")
        void shouldStartPriceCollectionWhenRoot() throws Exception {
            mockMvc.perform(post("/admin/ops/collection/price")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"exchange":"KOSPI","from":"2020-01-01","to":"2020-01-31"}
                                    """))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.taskId").value(11));
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("exchange 누락 시 400 (@Valid)")
        void shouldReturn400WhenExchangeMissing() throws Exception {
            mockMvc.perform(post("/admin/ops/collection/price")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"from":"2020-01-01","to":"2020-01-31"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("유효하지 않은 exchange 값이면 400 + INVALID_EXCHANGE")
        void shouldReturn400WhenExchangeInvalid() throws Exception {
            mockMvc.perform(post("/admin/ops/collection/price")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"exchange":"NASDAQ","from":"2020-01-01","to":"2020-01-31"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("INVALID_EXCHANGE"));
        }
    }

    @Nested
    @DisplayName("POST /admin/ops/collection/event")
    class StartEventCollection {

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 권한이면 이벤트 재조회 시작 202 + 작업ID")
        void shouldStartEventCollectionWhenRoot() throws Exception {
            mockMvc.perform(post("/admin/ops/collection/event")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"from":"2020-01-01","to":"2020-01-31"}
                                    """))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.taskId").value(22));
        }
    }

    @Nested
    @DisplayName("POST /admin/ops/collection/investor")
    class StartInvestorCollection {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(post("/admin/ops/collection/investor")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"from":"2026-01-01","to":"2026-01-31"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 권한이면 투자자동향 재조회 시작 202 + 작업ID")
        void shouldStartInvestorCollectionWhenRoot() throws Exception {
            mockMvc.perform(post("/admin/ops/collection/investor")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"from":"2026-01-01","to":"2026-01-31"}
                                    """))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.taskId").value(55));
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("from 누락 시 400 (@Valid)")
        void shouldReturn400WhenFromMissing() throws Exception {
            mockMvc.perform(post("/admin/ops/collection/investor")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"to":"2026-01-31"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /admin/ops/collection/tasks")
    class GetTasks {

        @Test
        @DisplayName("USER 권한이면 403")
        @WithApiUser(role = "USER")
        void shouldReturn403WhenUser() throws Exception {
            mockMvc.perform(get("/admin/ops/collection/tasks"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 권한이면 작업 목록 조회 200")
        void shouldReturnTasksWhenRoot() throws Exception {
            mockMvc.perform(get("/admin/ops/collection/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].type").value("PRICE"));
        }
    }

    @Nested
    @DisplayName("GET /admin/ops/collection/tasks/{id}")
    class GetTask {

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 권한이면 단일 작업 상태 조회 200")
        void shouldReturnSingleTaskWhenRoot() throws Exception {
            mockMvc.perform(get("/admin/ops/collection/tasks/7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("PRICE"))
                    .andExpect(jsonPath("$.scope").value("PRICE/KOSPI/2020-01-01~2020-01-31"));
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("미존재 작업 조회 시 404")
        void shouldReturn404WhenTaskNotFound() throws Exception {
            mockMvc.perform(get("/admin/ops/collection/tasks/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /admin/ops/collection/tasks/{id}/retry")
    class RetryTask {

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("ROOT 권한이면 재시도 202 + 새 작업ID")
        void shouldRetryTaskWhenRoot() throws Exception {
            mockMvc.perform(post("/admin/ops/collection/tasks/7/retry"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.taskId").value(44));
        }

        @Test
        @WithApiUser(role = "ROOT")
        @DisplayName("미존재 작업 재시도 시 404")
        void shouldReturn404WhenRetryTaskNotFound() throws Exception {
            mockMvc.perform(post("/admin/ops/collection/tasks/999/retry"))
                    .andExpect(status().isNotFound());
        }
    }
}
