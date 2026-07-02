package com.dove.modelserving.domain.entity;

import com.dove.modelserving.domain.enums.ModelOutputType;
import com.dove.modelserving.domain.enums.ModelStatus;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MlModel 엔티티 동작 테스트.
 */
class MlModelTest {

    private static final LocalDate EARLIEST = LocalDate.of(1985, 1, 1);

    private static final Set<StockExchange> KRX_EXCHANGES =
            Set.of(StockExchange.KOSPI, StockExchange.KOSDAQ);

    private MlModel newModel(Set<StockExchange> exchanges, PriceType priceType) {
        return new MlModel("swing_entry", "1.0.0", new byte[]{1, 2, 3}, "{}",
                ModelOutputType.PROBABILITY, exchanges, priceType, "root");
    }

    @Nested
    @DisplayName("생성")
    class Construct {

        @Test
        @DisplayName("새 모델은 ACTIVE 상태이고 채점 커서가 없다")
        void shouldStartActiveWithNoCursor() {
            MlModel model = newModel(KRX_EXCHANGES, PriceType.ADJUSTED);

            assertThat(model.getStatus()).isEqualTo(ModelStatus.ACTIVE);
            assertThat(model.isActive()).isTrue();
            assertThat(model.getScoreCursor()).isNull();
        }

        @Test
        @DisplayName("거래소·주가유형이 null이면 {KOSPI,KOSDAQ}·ADJUSTED 기본값을 쓴다")
        void shouldDefaultExchangesAndPriceTypeWhenNull() {
            MlModel model = newModel(null, null);

            assertThat(model.getScoreExchanges())
                    .containsExactlyInAnyOrder(StockExchange.KOSPI, StockExchange.KOSDAQ);
            assertThat(model.getScorePriceType()).isEqualTo(PriceType.ADJUSTED);
        }

        @Test
        @DisplayName("거래소가 빈 집합이면 {KOSPI,KOSDAQ} 기본값을 쓴다")
        void shouldDefaultExchangesWhenEmpty() {
            MlModel model = newModel(Set.of(), PriceType.ADJUSTED);

            assertThat(model.getScoreExchanges())
                    .containsExactlyInAnyOrder(StockExchange.KOSPI, StockExchange.KOSDAQ);
        }

        @Test
        @DisplayName("지정한 거래소 집합을 그대로 보존한다")
        void shouldKeepGivenExchanges() {
            MlModel model = newModel(Set.of(StockExchange.KONEX, StockExchange.NXT), PriceType.ADJUSTED);

            assertThat(model.getScoreExchanges())
                    .containsExactlyInAnyOrder(StockExchange.KONEX, StockExchange.NXT);
        }
    }

    @Nested
    @DisplayName("firstScoreDate")
    class FirstScoreDate {

        @Test
        @DisplayName("커서가 null이면 1985-01-01을 반환한다")
        void shouldReturnEarliestWhenCursorNull() {
            assertThat(MlModel.firstScoreDate(null)).isEqualTo(EARLIEST);
        }

        @Test
        @DisplayName("커서가 있으면 그 다음 날을 반환한다")
        void shouldReturnNextDayWhenCursorPresent() {
            assertThat(MlModel.firstScoreDate(LocalDate.of(2026, 6, 25)))
                    .isEqualTo(LocalDate.of(2026, 6, 26));
        }
    }

    @Nested
    @DisplayName("advanceScoreCursor")
    class AdvanceScoreCursor {

        @Test
        @DisplayName("채점 커서를 지정 거래일로 전진시킨다")
        void shouldAdvanceCursorToDate() {
            MlModel model = newModel(KRX_EXCHANGES, PriceType.ADJUSTED);

            model.advanceScoreCursor(LocalDate.of(2026, 6, 26));

            assertThat(model.getScoreCursor()).isEqualTo(LocalDate.of(2026, 6, 26));
        }
    }

    @Nested
    @DisplayName("deactivate")
    class Deactivate {

        @Test
        @DisplayName("상태를 INACTIVE로 바꾼다")
        void shouldSetStatusInactive() {
            MlModel model = newModel(KRX_EXCHANGES, PriceType.ADJUSTED);

            model.deactivate();

            assertThat(model.getStatus()).isEqualTo(ModelStatus.INACTIVE);
            assertThat(model.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("recordScoreSuccess")
    class RecordScoreSuccess {

        @Test
        @DisplayName("성공 일시를 기록하고 실패 사유를 초기화한다")
        void shouldSetScoredAtAndClearError() {
            MlModel model = newModel(KRX_EXCHANGES, PriceType.ADJUSTED);
            model.recordScoreFailure("SCORING_ERROR: boom");

            model.recordScoreSuccess();

            assertThat(model.getLastScoredAt()).isNotNull();
            assertThat(model.getLastError()).isNull();
        }
    }

    @Nested
    @DisplayName("recordScoreFailure")
    class RecordScoreFailure {

        @Test
        @DisplayName("실패 사유를 기록하고 성공 일시는 유지한다")
        void shouldSetErrorAndKeepScoredAt() {
            MlModel model = newModel(KRX_EXCHANGES, PriceType.ADJUSTED);
            model.recordScoreSuccess();

            model.recordScoreFailure("SCORING_ERROR: boom");

            assertThat(model.getLastError()).isEqualTo("SCORING_ERROR: boom");
            assertThat(model.getLastScoredAt()).isNotNull();
        }

        @Test
        @DisplayName("500자를 초과하는 사유는 500자로 잘라 저장한다")
        void shouldTruncateErrorTo500() {
            MlModel model = newModel(KRX_EXCHANGES, PriceType.ADJUSTED);

            model.recordScoreFailure("x".repeat(600));

            assertThat(model.getLastError()).hasSize(500);
        }
    }
}
