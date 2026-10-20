package com.dove.modelserving.application.service;

import com.dove.modelserving.application.exception.ScoreCursorRewoundException;
import com.dove.modelserving.domain.entity.StockModelScore;
import com.dove.modelserving.domain.entity.StockModelScoreId;
import com.dove.modelserving.domain.repository.StockModelScoreRepository;
import com.dove.modelserving.infrastructure.repository.MlModelRepositorySupport;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

/**
 * ScoreDateCommitService 의 거래일 단위 저장·커서 전진 행위를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScoreDateCommitService")
class ScoreDateCommitServiceTest {

    private static final Long MODEL_ID = 2L;
    private static final LocalDate PREV = LocalDate.of(2026, 10, 6);
    private static final LocalDate DATE = LocalDate.of(2026, 10, 7);

    @Mock
    private StockModelScoreRepository scoreRepository;
    @Mock
    private MlModelRepositorySupport modelRepositorySupport;

    @InjectMocks
    private ScoreDateCommitService service;

    private StockModelScore score(String ticker) {
        return new StockModelScore(
                new StockModelScoreId(ticker, StockExchange.KOSPI, PriceType.ADJUSTED, DATE, MODEL_ID),
                0.99, LocalDateTime.of(2026, 10, 7, 21, 0));
    }

    @Nested
    @DisplayName("commit: 거래일 단위 멱등 저장")
    class Commit {

        @Test
        @DisplayName("저장 전에 그 거래일의 기존 점수를 지워 진입존에서 빠진 행이 남지 않게 한다")
        void shouldDeleteExistingScoresOfDateBeforeSaving() {
            given(modelRepositorySupport.advanceScoreCursorIfMatches(MODEL_ID, PREV, DATE)).willReturn(1L);

            service.commit(MODEL_ID, List.of(score("005930")), PREV, DATE);

            var order = inOrder(scoreRepository);
            order.verify(scoreRepository).deleteByIdModelIdAndIdTradeDate(MODEL_ID, DATE);
            order.verify(scoreRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("채점 결과가 비어도 그 거래일의 기존 점수를 지운다")
        void shouldDeleteExistingScoresWhenNoRowsQualify() {
            given(modelRepositorySupport.advanceScoreCursorIfMatches(MODEL_ID, PREV, DATE)).willReturn(1L);

            service.commit(MODEL_ID, List.of(), PREV, DATE);

            then(scoreRepository).should().deleteByIdModelIdAndIdTradeDate(MODEL_ID, DATE);
            then(scoreRepository).should(never()).saveAll(anyList());
        }

        @Test
        @DisplayName("커서가 기대값과 다르면 예외를 던진다")
        void shouldThrowWhenCursorMoved() {
            given(modelRepositorySupport.advanceScoreCursorIfMatches(MODEL_ID, PREV, DATE)).willReturn(0L);

            assertThatThrownBy(() -> service.commit(MODEL_ID, List.of(score("005930")), PREV, DATE))
                    .isInstanceOf(ScoreCursorRewoundException.class);
        }
    }
}
