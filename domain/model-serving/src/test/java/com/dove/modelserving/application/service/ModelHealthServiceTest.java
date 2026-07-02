package com.dove.modelserving.application.service;

import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.domain.enums.ModelOutputType;
import com.dove.modelserving.domain.repository.MlModelRepository;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("ModelHealthService")
@ExtendWith(MockitoExtension.class)
class ModelHealthServiceTest {

    private static final Long MODEL_ID = 9L;

    @Mock
    private MlModelRepository modelRepository;

    @InjectMocks
    private ModelHealthService service;

    private MlModel model;

    @BeforeEach
    void setUp() {
        model = MlModel.register("swing_entry", "1.0.0", new byte[]{1}, "{}",
                ModelOutputType.PROBABILITY, Set.of(StockExchange.KOSPI, StockExchange.KOSDAQ),
                PriceType.ADJUSTED, "root");
    }

    @Nested
    @DisplayName("recordSuccess")
    class RecordSuccess {

        @Test
        @DisplayName("성공 일시를 세팅하고 실패 사유를 비운다")
        void shouldSetScoredAtAndClearError() {
            model.recordScoreFailure("SCORING_ERROR: boom");
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));

            service.recordSuccess(MODEL_ID);

            assertThat(model.getLastScoredAt()).isNotNull();
            assertThat(model.getLastError()).isNull();
        }

        @Test
        @DisplayName("모델이 없으면 조용히 무시한다")
        void shouldIgnoreWhenModelMissing() {
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.empty());

            service.recordSuccess(MODEL_ID);
        }
    }

    @Nested
    @DisplayName("recordFailure")
    class RecordFailure {

        @Test
        @DisplayName("실패 사유를 세팅한다")
        void shouldSetError() {
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));

            service.recordFailure(MODEL_ID, "SCORING_ERROR: boom");

            assertThat(model.getLastError()).isEqualTo("SCORING_ERROR: boom");
        }

        @Test
        @DisplayName("모델이 없으면 조용히 무시한다")
        void shouldIgnoreWhenModelMissing() {
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.empty());

            service.recordFailure(MODEL_ID, "SCORING_ERROR: boom");
        }
    }
}
