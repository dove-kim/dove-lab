package com.dove.portfolio.domain.converter;

import com.dove.portfolio.domain.value.RebalancePlanCash;
import com.dove.portfolio.domain.value.RebalancePlanConfig;
import com.dove.portfolio.domain.value.RebalancePlanEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ENTRIES JSON ↔ RebalancePlanConfig 변환과 구형 배열 흡수를 검증한다.
 */
class RebalancePlanConfigConverterTest {

    private final RebalancePlanConfigConverter converter = new RebalancePlanConfigConverter();

    @Nested
    @DisplayName("구형 배열 읽기")
    class LegacyArray {
        @Test
        @DisplayName("센티넬을 설정으로 분리하고 나머지는 종목으로 남긴다")
        void shouldAbsorbSentinels() {
            String legacy = "["
                    + "{\"symbol\":\"삼성전자\",\"account\":\"국내\",\"currency\":\"KRW\",\"targetPct\":60},"
                    + "{\"symbol\":\"__SLOTS__\",\"account\":\"\",\"currency\":\"\",\"targetPct\":6},"
                    + "{\"symbol\":\"__PARTRATE__\",\"account\":\"\",\"currency\":\"\",\"targetPct\":5},"
                    + "{\"symbol\":\"__CASH__\",\"account\":\"CMA\",\"currency\":\"USD\",\"targetPct\":40}"
                    + "]";

            RebalancePlanConfig config = converter.convertToEntityAttribute(legacy);

            assertThat(config.slots()).isEqualTo(6);
            assertThat(config.partRate()).isEqualTo(5);
            assertThat(config.positions()).singleElement()
                    .isEqualTo(new RebalancePlanEntry("삼성전자", "국내", "KRW", 60));
            assertThat(config.cash()).singleElement()
                    .isEqualTo(new RebalancePlanCash("CMA", "USD", 40));
        }

        @Test
        @DisplayName("센티넬이 없으면 슬롯·참여율은 기본값(8·10)")
        void shouldDefaultWhenNoSentinel() {
            RebalancePlanConfig config = converter.convertToEntityAttribute(
                    "[{\"symbol\":\"A\",\"account\":\"국내\",\"currency\":\"KRW\",\"targetPct\":100}]");

            assertThat(config.slots()).isEqualTo(8);
            assertThat(config.partRate()).isEqualTo(10);
            assertThat(config.positions()).hasSize(1);
            assertThat(config.cash()).isEmpty();
        }
    }

    @Nested
    @DisplayName("신형 객체")
    class NewObject {
        @Test
        @DisplayName("객체 JSON 왕복이 값 그대로 보존된다")
        void shouldRoundTrip() {
            RebalancePlanConfig original = new RebalancePlanConfig(5, 7.5,
                    List.of(new RebalancePlanEntry("A", "국내", "KRW", 100)),
                    List.of(new RebalancePlanCash("CMA", "KRW", 0)));

            RebalancePlanConfig back = converter.convertToEntityAttribute(converter.convertToDatabaseColumn(original));

            assertThat(back).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("빈 값")
    class Empty {
        @Test
        @DisplayName("null은 기본 설정으로 읽는다")
        void shouldReturnDefaults() {
            RebalancePlanConfig config = converter.convertToEntityAttribute(null);

            assertThat(config.slots()).isEqualTo(8);
            assertThat(config.partRate()).isEqualTo(10);
            assertThat(config.positions()).isEmpty();
            assertThat(config.cash()).isEmpty();
        }
    }
}
