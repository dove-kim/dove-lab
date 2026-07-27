package com.dove.api.portfolio.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.portfolio.application.service.PortfolioRebalancePlanService;
import com.dove.portfolio.domain.value.RebalancePlanConfig;
import com.dove.portfolio.domain.value.RebalancePlanEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 리밸런싱 계획 API 통합 테스트.
 */
@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class PortfolioRebalancePlanControllerTest {

    private static final long MEMBER_ID = 1L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PortfolioRebalancePlanService service;

    private static final String BODY =
            "{\"name\":\"공격형\",\"config\":{\"slots\":8,\"partRate\":10,\"positions\":[{\"symbol\":\"삼성전자\",\"account\":\"국내\",\"currency\":\"KRW\",\"targetPct\":50}],\"cash\":[]}}";

    private static RebalancePlanConfig config(RebalancePlanEntry... positions) {
        return new RebalancePlanConfig(8, 10, java.util.List.of(positions), java.util.List.of());
    }

    @Nested
    @DisplayName("목록 조회")
    class List {
        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/portfolio/rebalance-plans")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser
        @DisplayName("PORTFOLIO_REBALANCE 권한 없으면 403")
        void shouldReturn403WhenMissingCapability() throws Exception {
            mockMvc.perform(get("/portfolio/rebalance-plans")).andExpect(status().isForbidden());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_REBALANCE"})
        @DisplayName("본인 계획만 반환")
        void shouldReturnOwn() throws Exception {
            service.save(MEMBER_ID, "내계획", config(new RebalancePlanEntry("A", "국내", "KRW", 100)), "tester");
            service.save(999L, "남계획", config(new RebalancePlanEntry("B", "국내", "KRW", 100)), "other");

            mockMvc.perform(get("/portfolio/rebalance-plans"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("내계획"));
        }
    }

    @Nested
    @DisplayName("저장(upsert)")
    class Save {
        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_REBALANCE"})
        @DisplayName("유효하면 201")
        void shouldCreate() throws Exception {
            mockMvc.perform(post("/portfolio/rebalance-plans").contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("공격형"))
                    .andExpect(jsonPath("$.config.positions.length()").value(1));
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_REBALANCE"})
        @DisplayName("같은 이름 재저장은 갱신되어 중복 생성되지 않는다")
        void shouldUpsert() throws Exception {
            mockMvc.perform(post("/portfolio/rebalance-plans").contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isCreated());
            mockMvc.perform(post("/portfolio/rebalance-plans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"공격형\",\"config\":{\"slots\":8,\"partRate\":10,\"positions\":[],\"cash\":[]}}"))
                    .andExpect(status().isCreated());

            assertThat(service.findByOwner(MEMBER_ID)).singleElement()
                    .matches(p -> p.getName().equals("공격형") && p.getConfig().positions().isEmpty());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_REBALANCE"})
        @DisplayName("이름 비면 400")
        void shouldReturn400WhenNameBlank() throws Exception {
            mockMvc.perform(post("/portfolio/rebalance-plans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\",\"config\":{\"slots\":8,\"partRate\":10,\"positions\":[],\"cash\":[]}}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {
        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_REBALANCE"})
        @DisplayName("삭제 204")
        void shouldDelete() throws Exception {
            var p = service.save(MEMBER_ID, "삭제대상", config(new RebalancePlanEntry("A", "국내", "KRW", 100)), "tester");
            mockMvc.perform(delete("/portfolio/rebalance-plans/" + p.getId())).andExpect(status().isNoContent());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"PORTFOLIO_REBALANCE"})
        @DisplayName("남의 계획 삭제는 404")
        void shouldReturn404WhenDeletingOthers() throws Exception {
            var p = service.save(999L, "남계획", config(new RebalancePlanEntry("B", "국내", "KRW", 100)), "other");
            mockMvc.perform(delete("/portfolio/rebalance-plans/" + p.getId())).andExpect(status().isNotFound());
        }
    }
}
