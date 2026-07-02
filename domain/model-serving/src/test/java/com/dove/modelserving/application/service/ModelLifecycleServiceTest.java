package com.dove.modelserving.application.service;

import com.dove.modelserving.application.exception.ModelNotFoundException;
import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.domain.enums.ModelOutputType;
import com.dove.modelserving.domain.enums.ModelStatus;
import com.dove.modelserving.domain.repository.MlModelRepository;
import com.dove.modelserving.infrastructure.repository.StockModelScoreRepositorySupport;
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

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ModelLifecycleService")
@ExtendWith(MockitoExtension.class)
class ModelLifecycleServiceTest {

    private static final Long MODEL_ID = 7L;

    @Mock
    private MlModelRepository modelRepository;
    @Mock
    private StockModelScoreRepositorySupport scoreSupport;

    @InjectMocks
    private ModelLifecycleService service;

    private MlModel model;

    @BeforeEach
    void setUp() {
        model = MlModel.register("swing_entry", "1.0.0", new byte[]{1}, "{}",
                ModelOutputType.PROBABILITY, Set.of(StockExchange.KOSPI, StockExchange.KOSDAQ),
                PriceType.ADJUSTED, "root");
    }

    @Nested
    @DisplayName("activate")
    class Activate {

        @Test
        @DisplayName("모델을 ACTIVE로 전환한다")
        void shouldActivateWhenModelExists() {
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));

            MlModel result = service.activate(MODEL_ID);

            assertThat(result.getStatus()).isEqualTo(ModelStatus.ACTIVE);
        }

        @Test
        @DisplayName("모델이 없으면 거부한다")
        void shouldRejectWhenModelMissing() {
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.activate(MODEL_ID))
                    .isInstanceOf(ModelNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deactivate")
    class Deactivate {

        @Test
        @DisplayName("ACTIVE 모델을 INACTIVE로 전환한다")
        void shouldDeactivate() {
            model.activate();
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));

            MlModel result = service.deactivate(MODEL_ID);

            assertThat(result.getStatus()).isEqualTo(ModelStatus.INACTIVE);
        }
    }

    @Nested
    @DisplayName("resetScoreCursor")
    class ResetScoreCursor {

        @Test
        @DisplayName("지정일로 되돌리고 그 이후 점수를 삭제한다")
        void shouldResetToDateAndDeleteAfter() {
            LocalDate to = LocalDate.of(2026, 6, 1);
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));

            service.resetScoreCursor(MODEL_ID, to);

            verify(scoreSupport).deleteByModelAndDateRange(MODEL_ID, to.plusDays(1), null);
            assertThat(model.getScoreCursor()).isEqualTo(to);
        }

        @Test
        @DisplayName("null이면 미시작으로 되돌리고 전 점수를 삭제한다")
        void shouldResetToNullAndDeleteAll() {
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));

            service.resetScoreCursor(MODEL_ID, null);

            verify(scoreSupport).deleteByModelAndDateRange(MODEL_ID, null, null);
            assertThat(model.getScoreCursor()).isNull();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("점수를 먼저 지우고 모델을 삭제한다")
        void shouldDeleteScoresThenModel() {
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));

            service.delete(MODEL_ID);

            verify(scoreSupport).deleteByModel(MODEL_ID);
            verify(modelRepository).delete(model);
        }

        @Test
        @DisplayName("모델이 없으면 점수도 지우지 않는다")
        void shouldNotDeleteWhenModelMissing() {
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(MODEL_ID))
                    .isInstanceOf(ModelNotFoundException.class);

            verify(scoreSupport, never()).deleteByModel(any());
            verify(modelRepository, never()).delete(any());
        }
    }
}
