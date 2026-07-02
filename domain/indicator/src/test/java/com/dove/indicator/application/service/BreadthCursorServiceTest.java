package com.dove.indicator.application.service;

import com.dove.indicator.application.exception.BreadthCursorRewoundException;
import com.dove.indicator.domain.breadth.entity.BreadthCursor;
import com.dove.indicator.domain.breadth.repository.BreadthCursorRepository;
import com.dove.indicator.infrastructure.repository.BreadthCursorRepositorySupport;
import com.dove.jpa.QuerydslConfiguration;
import com.dove.stock.domain.enums.MarketUniverse;
import com.dove.stock.domain.enums.PriceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({BreadthCursorService.class, BreadthCursorRepositorySupport.class, QuerydslConfiguration.class})
class BreadthCursorServiceTest {

    private static final MarketUniverse EX = MarketUniverse.KRX;
    private static final PriceType PT = PriceType.RAW;

    @Autowired BreadthCursorService cursorService;
    @Autowired BreadthCursorRepository repository;
    @Autowired TestEntityManager em;

    @Nested
    @DisplayName("advanceForwardCas")
    class AdvanceForwardCas {

        @Test
        @DisplayName("커서 없으면(cursorExists=false) 생성 후 전진한다")
        void shouldCreateWhenAbsent() {
            cursorService.advanceForwardCas(EX, PT, null, false, LocalDate.of(2024, 5, 30));

            assertThat(cursorDate()).isEqualTo(LocalDate.of(2024, 5, 30));
        }

        @Test
        @DisplayName("expected와 일치하면 전진한다")
        void shouldAdvanceWhenExpectedMatches() {
            seedCursor(LocalDate.of(2024, 1, 1));

            cursorService.advanceForwardCas(EX, PT, LocalDate.of(2024, 1, 1), true, LocalDate.of(2024, 6, 1));

            assertThat(cursorDate()).isEqualTo(LocalDate.of(2024, 6, 1));
        }

        @Test
        @DisplayName("cursorDate가 null인 커서는 expected=null로 전진한다")
        void shouldAdvanceWhenNullCursor() {
            repository.save(new BreadthCursor(EX, PT)); // cursorDate=null
            em.flush();
            em.clear();

            cursorService.advanceForwardCas(EX, PT, null, true, LocalDate.of(2024, 6, 1));

            assertThat(cursorDate()).isEqualTo(LocalDate.of(2024, 6, 1));
        }

        @Test
        @DisplayName("그 사이 커서가 달라지면(expected 불일치) 예외로 거부한다")
        void shouldThrowWhenMismatch() {
            seedCursor(LocalDate.of(2024, 12, 31));

            assertThatThrownBy(() ->
                    cursorService.advanceForwardCas(EX, PT, LocalDate.of(2024, 1, 1), true,
                            LocalDate.of(2025, 1, 31)))
                    .isInstanceOf(BreadthCursorRewoundException.class);

            assertThat(cursorDate()).isEqualTo(LocalDate.of(2024, 12, 31)); // 변경 없음
        }
    }

    private void seedCursor(LocalDate date) {
        cursorService.advanceForwardCas(EX, PT, null, false, date);
        em.flush();
        em.clear();
    }

    /** CAS는 QueryDSL 벌크 update라 영속성 컨텍스트를 우회 → DB 값을 보려면 flush 후 clear. */
    private LocalDate cursorDate() {
        em.flush();
        em.clear();
        return repository.findByUniverseAndPriceType(EX, PT).orElseThrow().getCursorDate();
    }
}
