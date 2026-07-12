package com.dove.screening.domain.pipeline;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 검색 파이프라인 파싱 단위 테스트.
 */
class SearchPipelineTest {

    @Nested
    @DisplayName("parse")
    class Parse {

        @Test
        @DisplayName("null·빈 문자열이면 빈 목록을 반환한다")
        void shouldReturnEmptyWhenNullOrBlank() {
            assertThat(SearchPipeline.parse((String) null)).isEmpty();
            assertThat(SearchPipeline.parse("")).isEmpty();
            assertThat(SearchPipeline.parse("   ")).isEmpty();
        }

        @Test
        @DisplayName("배열이 아니면 빈 목록을 반환한다")
        void shouldReturnEmptyWhenNotArray() {
            assertThat(SearchPipeline.parse("{\"type\":\"RANK\"}")).isEmpty();
        }

        @Test
        @DisplayName("FILTER 단계를 FilterStage로 파싱한다")
        void shouldParseFilterStage() {
            String json = "[{\"type\":\"FILTER\",\"expression\":"
                    + "{\"conditionType\":\"VOLUME_VALUE\",\"operator\":\"GT\",\"value\":1000}}]";

            List<PipelineStage> stages = SearchPipeline.parse(json);

            assertThat(stages).hasSize(1);
            assertThat(stages.get(0)).isInstanceOf(FilterStage.class);
            assertThat(((FilterStage) stages.get(0)).filter()).isNotNull();
        }

        @Test
        @DisplayName("expression 없는 FILTER 단계는 건너뛴다")
        void shouldSkipFilterWithoutExpression() {
            assertThat(SearchPipeline.parse("[{\"type\":\"FILTER\"}]")).isEmpty();
        }

        @Test
        @DisplayName("RANK 단계를 정렬 키·limit과 함께 파싱한다")
        void shouldParseRankStageWithSortAndLimit() {
            String json = "[{\"type\":\"RANK\",\"sort\":["
                    + "{\"field\":\"CHANGE_RATE\",\"direction\":\"DESC\"},"
                    + "{\"field\":\"VOLUME\",\"direction\":\"ASC\"}],\"limit\":20}]";

            List<PipelineStage> stages = SearchPipeline.parse(json);

            assertThat(stages).hasSize(1);
            RankStage rank = (RankStage) stages.get(0);
            assertThat(rank.limit()).isEqualTo(20);
            assertThat(rank.sortKeys()).containsExactly(
                    new SortKey(SortField.CHANGE_RATE, SortDirection.DESC),
                    new SortKey(SortField.VOLUME, SortDirection.ASC));
        }

        @Test
        @DisplayName("limit 없는 RANK 단계는 limit이 null이다")
        void shouldParseRankWithoutLimit() {
            String json = "[{\"type\":\"RANK\",\"sort\":[{\"field\":\"MARKET_CAP\",\"direction\":\"DESC\"}]}]";

            RankStage rank = (RankStage) SearchPipeline.parse(json).get(0);

            assertThat(rank.limit()).isNull();
            assertThat(rank.sortKeys()).containsExactly(new SortKey(SortField.MARKET_CAP, SortDirection.DESC));
        }

        @Test
        @DisplayName("알 수 없는 정렬 필드·방향 키는 건너뛴다")
        void shouldSkipUnknownSortKeys() {
            String json = "[{\"type\":\"RANK\",\"sort\":["
                    + "{\"field\":\"NOPE\",\"direction\":\"DESC\"},"
                    + "{\"field\":\"VOLUME\",\"direction\":\"SIDEWAYS\"},"
                    + "{\"field\":\"VOLUME\",\"direction\":\"ASC\"}]}]";

            RankStage rank = (RankStage) SearchPipeline.parse(json).get(0);

            assertThat(rank.sortKeys()).containsExactly(new SortKey(SortField.VOLUME, SortDirection.ASC));
        }

        @Test
        @DisplayName("알 수 없는 type 단계는 건너뛰고 나머지는 순서대로 유지한다")
        void shouldSkipUnknownStageTypeAndKeepOrder() {
            String json = "[{\"type\":\"WAT\"},"
                    + "{\"type\":\"RANK\",\"sort\":[{\"field\":\"MARKET_CAP\",\"direction\":\"DESC\"}],\"limit\":100},"
                    + "{\"type\":\"FILTER\",\"expression\":"
                    + "{\"conditionType\":\"VOLUME_VALUE\",\"operator\":\"GT\",\"value\":1000}}]";

            List<PipelineStage> stages = SearchPipeline.parse(json);

            assertThat(stages).hasSize(2);
            assertThat(stages.get(0)).isInstanceOf(RankStage.class);
            assertThat(stages.get(1)).isInstanceOf(FilterStage.class);
        }
    }
}
