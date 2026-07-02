package com.dove.modelserving.infrastructure.repository;

import com.dove.jpa.QuerydslConfiguration;
import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.domain.enums.ModelOutputType;
import com.dove.modelserving.domain.repository.MlModelRepository;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MlModelRepositorySupport 채점 커서 CAS 전진 테스트.
 */
@DataJpaTest
@Import({MlModelRepositorySupport.class, QuerydslConfiguration.class})
class MlModelRepositorySupportTest {

    @Autowired MlModelRepositorySupport support;
    @Autowired MlModelRepository repository;
    @Autowired TestEntityManager em;

    private Long seedModel() {
        MlModel model = new MlModel("swing_entry", "1.0.0", new byte[]{1}, "{}",
                ModelOutputType.PROBABILITY, Set.of(StockExchange.KOSPI, StockExchange.KOSDAQ),
                PriceType.ADJUSTED, "root");
        return repository.save(model).getId();
    }

    /** CAS는 QueryDSL 벌크 update라 영속성 컨텍스트를 우회 → DB 값을 보려면 flush 후 clear. */
    private LocalDate cursorOf(Long id) {
        em.flush();
        em.clear();
        return repository.findById(id).orElseThrow().getScoreCursor();
    }

    @Nested
    @DisplayName("advanceScoreCursorIfMatches")
    class AdvanceScoreCursorIfMatches {

        @Test
        @DisplayName("커서가 null인 모델은 expected=null로 전진한다")
        void shouldAdvanceWhenCursorNull() {
            Long id = seedModel();

            long updated = support.advanceScoreCursorIfMatches(id, null, LocalDate.of(2026, 6, 1));

            assertThat(updated).isEqualTo(1);
            assertThat(cursorOf(id)).isEqualTo(LocalDate.of(2026, 6, 1));
        }

        @Test
        @DisplayName("expected와 일치하면 toDate로 전진한다")
        void shouldAdvanceWhenExpectedMatches() {
            Long id = seedModel();
            support.advanceScoreCursorIfMatches(id, null, LocalDate.of(2026, 6, 1));

            long updated = support.advanceScoreCursorIfMatches(id,
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2));

            assertThat(updated).isEqualTo(1);
            assertThat(cursorOf(id)).isEqualTo(LocalDate.of(2026, 6, 2));
        }

        @Test
        @DisplayName("expected가 실제 커서와 다르면 전진하지 않고 0행을 반환한다")
        void shouldNotAdvanceWhenExpectedMismatch() {
            Long id = seedModel();
            support.advanceScoreCursorIfMatches(id, null, LocalDate.of(2026, 6, 1));

            long updated = support.advanceScoreCursorIfMatches(id,
                    LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 2));

            assertThat(updated).isEqualTo(0);
            assertThat(cursorOf(id)).isEqualTo(LocalDate.of(2026, 6, 1));
        }

        @Test
        @DisplayName("커서가 이미 전진했는데 expected=null이면 전진하지 않는다")
        void shouldNotAdvanceWhenExpectedNullButCursorPresent() {
            Long id = seedModel();
            support.advanceScoreCursorIfMatches(id, null, LocalDate.of(2026, 6, 1));

            long updated = support.advanceScoreCursorIfMatches(id, null, LocalDate.of(2026, 6, 2));

            assertThat(updated).isEqualTo(0);
            assertThat(cursorOf(id)).isEqualTo(LocalDate.of(2026, 6, 1));
        }
    }
}
