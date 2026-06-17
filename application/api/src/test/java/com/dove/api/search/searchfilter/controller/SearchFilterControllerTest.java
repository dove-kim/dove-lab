package com.dove.api.search.searchfilter.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.screening.application.service.SearchFilterCommandService;
import com.dove.screening.domain.enums.DateRule;
import com.dove.screening.domain.value.FilterExpression;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.enums.PriceType;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class SearchFilterControllerTest {

    private static final long MEMBER_ID = 1L;

    private static final String SIMPLE_EXPR =
            "{\"nodeType\":\"CONDITION\",\"conditionType\":\"PRICE_VALUE\",\"priceField\":\"CLOSE\",\"operator\":\"GT\",\"value\":0}";

    @Autowired MockMvc mockMvc;
    @Autowired SearchFilterCommandService searchFilterCommandService;

    private void seedFilter(String name, DateRule dateRule) {
        searchFilterCommandService.create(MEMBER_ID, name, dateRule,
                List.of(MarketType.KOSPI), PriceType.RAW, FilterExpression.parse(SIMPLE_EXPR), null);
    }

    @Nested
    @DisplayName("GET /filters")
    class ListFilters {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/filters"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("빈 목록 반환")
        void shouldReturnEmptyListWhenNoFilters() throws Exception {
            mockMvc.perform(get("/filters"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("생성한 필터 목록에 포함")
        void shouldReturnCreatedFilterWhenExists() throws Exception {
            seedFilter("내필터", DateRule.LATEST);

            mockMvc.perform(get("/filters"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("내필터"))
                    .andExpect(jsonPath("$[0].dateRule").value("LATEST"));
        }
    }

    @Nested
    @DisplayName("POST /filters")
    class CreateFilter {

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("필터 생성 201")
        void shouldCreateFilterWhenValid() throws Exception {
            String body = """
                    {"name":"신규필터","dateRule":"LATEST","markets":["KOSPI"],
                     "expression":%s}
                    """.formatted(quoted(SIMPLE_EXPR));

            mockMvc.perform(post("/filters")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("신규필터"))
                    .andExpect(jsonPath("$.markets[0]").value("KOSPI"));
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("name 누락 시 400")
        void shouldReturn400WhenNameMissing() throws Exception {
            String body = """
                    {"dateRule":"LATEST","markets":["KOSPI"],"expression":%s}
                    """.formatted(quoted(SIMPLE_EXPR));

            mockMvc.perform(post("/filters")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("이름 중복 시 409")
        void shouldReturn409WhenNameDuplicate() throws Exception {
            seedFilter("중복필터", DateRule.LATEST);

            String body = """
                    {"name":"중복필터","dateRule":"LATEST","markets":["KOSPI"],
                     "expression":%s}
                    """.formatted(quoted(SIMPLE_EXPR));

            mockMvc.perform(post("/filters")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PUT /filters/{id}")
    class UpdateFilter {

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("없는 id면 404")
        void shouldReturn404WhenUpdatingNonExistent() throws Exception {
            String body = """
                    {"name":"없는필터","dateRule":"LATEST","markets":["KOSPI"],
                     "expression":%s}
                    """.formatted(quoted(SIMPLE_EXPR));

            mockMvc.perform(put("/filters/99999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("필터 수정")
        void shouldUpdateFilterWhenExists() throws Exception {
            var filter = searchFilterCommandService.create(MEMBER_ID, "원래이름", DateRule.LATEST,
                    List.of(MarketType.KOSPI), PriceType.RAW, FilterExpression.parse(SIMPLE_EXPR), null);

            String body = """
                    {"name":"바뀐이름","dateRule":"PREV_1D","markets":["KOSDAQ"],
                     "expression":%s}
                    """.formatted(quoted(SIMPLE_EXPR));

            mockMvc.perform(put("/filters/" + filter.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("바뀐이름"))
                    .andExpect(jsonPath("$.dateRule").value("PREV_1D"));
        }
    }

    @Nested
    @DisplayName("DELETE /filters/{id}")
    class DeleteFilter {

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("없는 id면 404")
        void shouldReturn404WhenDeletingNonExistent() throws Exception {
            mockMvc.perform(delete("/filters/99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("필터 삭제 204")
        void shouldDeleteFilterWhenExists() throws Exception {
            var filter = searchFilterCommandService.create(MEMBER_ID, "삭제할필터", DateRule.LATEST,
                    List.of(MarketType.KOSPI), PriceType.RAW, FilterExpression.parse(SIMPLE_EXPR), null);

            mockMvc.perform(delete("/filters/" + filter.getId()))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("PATCH /filters/reorder")
    class ReorderFilters {

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("순서 변경 204")
        void shouldReorderFiltersWhenValid() throws Exception {
            var f1 = searchFilterCommandService.create(MEMBER_ID, "첫번째", DateRule.LATEST,
                    List.of(MarketType.KOSPI), PriceType.RAW, FilterExpression.parse(SIMPLE_EXPR), null);
            var f2 = searchFilterCommandService.create(MEMBER_ID, "두번째", DateRule.LATEST,
                    List.of(MarketType.KOSPI), PriceType.RAW, FilterExpression.parse(SIMPLE_EXPR), null);

            String body = "{\"ids\":[" + f2.getId() + "," + f1.getId() + "]}";

            mockMvc.perform(patch("/filters/reorder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /filters/{id}/execute")
    class ExecuteFilter {

        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(post("/filters/1/execute"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("없는 id면 404")
        void shouldReturn404WhenExecutingNonExistent() throws Exception {
            mockMvc.perform(post("/filters/99999/execute")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithApiUser(memberId = MEMBER_ID, capabilities = {"STOCK_SEARCH"})
        @DisplayName("결과 반환 (빈 종목 → matchCount=0)")
        void shouldExecuteFilterWhenExists() throws Exception {
            // SPECIFIC_DATE: referenceDate=null 이면 LocalDate.now()를 기준일로 사용 → 빈 결과 반환
            var filter = searchFilterCommandService.create(MEMBER_ID, "실행필터", DateRule.SPECIFIC_DATE,
                    List.of(MarketType.KOSPI), PriceType.RAW, FilterExpression.parse(SIMPLE_EXPR), null);

            mockMvc.perform(post("/filters/" + filter.getId() + "/execute")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matchCount").value(0));
        }
    }

    /** JSON 문자열을 JSON 내 문자열 값으로 이스케이프 */
    private String quoted(String json) {
        return "\"" + json.replace("\"", "\\\"") + "\"";
    }
}
