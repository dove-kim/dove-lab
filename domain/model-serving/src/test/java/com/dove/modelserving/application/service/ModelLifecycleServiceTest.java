package com.dove.modelserving.application.service;

import com.dove.modelserving.application.exception.ModelActivationException;
import com.dove.modelserving.application.exception.ModelNotFoundException;
import com.dove.modelserving.application.exception.ModelScoringException;
import com.dove.modelserving.application.port.DryRunSampleSource;
import com.dove.modelserving.application.port.ModelScorer;
import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.domain.enums.ModelOutputType;
import com.dove.modelserving.domain.enums.ModelStatus;
import com.dove.modelserving.domain.meta.ModelMetaParser;
import com.dove.modelserving.domain.repository.MlModelRepository;
import com.dove.modelserving.infrastructure.repository.StockModelScoreRepositorySupport;
import com.dove.modelserving.infrastructure.scorer.ArtifactMaterializer;
import com.dove.modelserving.infrastructure.scorer.PredictRow;
import com.dove.modelserving.infrastructure.scorer.ScoredRow;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ModelLifecycleService")
@ExtendWith(MockitoExtension.class)
class ModelLifecycleServiceTest {

    private static final Long MODEL_ID = 7L;
    private static final String META_OK = "{\"features\":[\"rsi_14\"]}";
    private static final String META_UPPERCASE = "{\"features\":[\"RSI_14\"]}";

    @Mock
    private MlModelRepository modelRepository;
    @Mock
    private StockModelScoreRepositorySupport scoreSupport;
    @Mock
    private DryRunSampleSource sampleSource;
    @Mock
    private ModelScorer modelScorer;
    @Mock
    private ArtifactMaterializer artifactMaterializer;

    private ModelLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new ModelLifecycleService(modelRepository, scoreSupport,
                new ModelMetaParser(new ObjectMapper()), sampleSource, modelScorer, artifactMaterializer);
    }

    @Nested
    @DisplayName("activate")
    class Activate {

        @Test
        @DisplayName("드라이런 점수가 유효하면 모델을 ACTIVE로 전환한다")
        void shouldActivateWhenDryRunPasses() {
            MlModel model = model(META_OK);
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));
            when(sampleSource.sample(any(), anyInt())).thenReturn(List.of(row("rsi_14")));
            when(artifactMaterializer.materialize(any(), any())).thenReturn(Path.of("model.pkl"));
            when(modelScorer.score(any())).thenReturn(List.of(new ScoredRow("AAA", "2026-06-20", 0.62)));

            MlModel result = service.activate(MODEL_ID);

            assertThat(result.getStatus()).isEqualTo(ModelStatus.ACTIVE);
            verify(artifactMaterializer).cleanup(any());
        }

        @Test
        @DisplayName("모델이 없으면 거부한다")
        void shouldRejectWhenModelMissing() {
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.activate(MODEL_ID))
                    .isInstanceOf(ModelNotFoundException.class);

            verifyNoInteractions(sampleSource, modelScorer, artifactMaterializer);
        }

        @Test
        @DisplayName("표본이 없으면 DRY_RUN_NO_SAMPLE로 활성화를 차단한다")
        void shouldRejectWhenNoSample() {
            MlModel model = model(META_OK);
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));
            when(sampleSource.sample(any(), anyInt())).thenReturn(List.of());

            assertThatThrownBy(() -> service.activate(MODEL_ID))
                    .isInstanceOf(ModelActivationException.class)
                    .hasMessage(ModelActivationException.DRY_RUN_NO_SAMPLE);

            assertThat(model.getStatus()).isEqualTo(ModelStatus.INACTIVE);
            verifyNoInteractions(modelScorer, artifactMaterializer);
        }

        @Test
        @DisplayName("meta 피처명이 대문자여서 소문자 입력 키와 불일치하면 DRY_RUN_FEATURE_MISMATCH로 차단한다")
        void shouldRejectWhenFeatureCaseMismatch() {
            MlModel model = model(META_UPPERCASE);
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));
            when(sampleSource.sample(any(), anyInt())).thenReturn(List.of(row("rsi_14")));

            assertThatThrownBy(() -> service.activate(MODEL_ID))
                    .isInstanceOf(ModelActivationException.class)
                    .hasMessage(ModelActivationException.DRY_RUN_FEATURE_MISMATCH);

            assertThat(model.getStatus()).isEqualTo(ModelStatus.INACTIVE);
            // 피처 검증은 채점 전 단계 — 채점기·아티팩트를 건드리지 않는다
            verifyNoInteractions(modelScorer, artifactMaterializer);
        }

        @Test
        @DisplayName("점수가 [0,1]을 벗어나면 DRY_RUN_INVALID_OUTPUT로 차단한다")
        void shouldRejectWhenScoreOutOfRange() {
            MlModel model = model(META_OK);
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));
            when(sampleSource.sample(any(), anyInt())).thenReturn(List.of(row("rsi_14")));
            when(artifactMaterializer.materialize(any(), any())).thenReturn(Path.of("model.pkl"));
            when(modelScorer.score(any())).thenReturn(List.of(new ScoredRow("AAA", "2026-06-20", 1.5)));

            assertThatThrownBy(() -> service.activate(MODEL_ID))
                    .isInstanceOf(ModelActivationException.class)
                    .hasMessage(ModelActivationException.DRY_RUN_INVALID_OUTPUT);

            assertThat(model.getStatus()).isEqualTo(ModelStatus.INACTIVE);
            verify(artifactMaterializer).cleanup(any());
        }

        @Test
        @DisplayName("점수가 NaN이면 DRY_RUN_INVALID_OUTPUT로 차단한다")
        void shouldRejectWhenScoreNaN() {
            MlModel model = model(META_OK);
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));
            when(sampleSource.sample(any(), anyInt())).thenReturn(List.of(row("rsi_14")));
            when(artifactMaterializer.materialize(any(), any())).thenReturn(Path.of("model.pkl"));
            when(modelScorer.score(any())).thenReturn(List.of(new ScoredRow("AAA", "2026-06-20", Double.NaN)));

            assertThatThrownBy(() -> service.activate(MODEL_ID))
                    .isInstanceOf(ModelActivationException.class)
                    .hasMessage(ModelActivationException.DRY_RUN_INVALID_OUTPUT);

            assertThat(model.getStatus()).isEqualTo(ModelStatus.INACTIVE);
        }

        @Test
        @DisplayName("모든 점수가 결측이면 DRY_RUN_INVALID_OUTPUT로 차단한다")
        void shouldRejectWhenAllScoresNull() {
            MlModel model = model(META_OK);
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));
            when(sampleSource.sample(any(), anyInt())).thenReturn(List.of(row("rsi_14")));
            when(artifactMaterializer.materialize(any(), any())).thenReturn(Path.of("model.pkl"));
            when(modelScorer.score(any())).thenReturn(List.of(new ScoredRow("AAA", "2026-06-20", null)));

            assertThatThrownBy(() -> service.activate(MODEL_ID))
                    .isInstanceOf(ModelActivationException.class)
                    .hasMessage(ModelActivationException.DRY_RUN_INVALID_OUTPUT);

            assertThat(model.getStatus()).isEqualTo(ModelStatus.INACTIVE);
        }

        @Test
        @DisplayName("채점기 실행이 실패하면 원인을 보존해 활성화를 차단한다")
        void shouldRejectWhenScorerFails() {
            MlModel model = model(META_OK);
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));
            when(sampleSource.sample(any(), anyInt())).thenReturn(List.of(row("rsi_14")));
            when(artifactMaterializer.materialize(any(), any())).thenReturn(Path.of("model.pkl"));
            when(modelScorer.score(any()))
                    .thenThrow(new ModelScoringException("MODEL_LOAD_FAILED", "로드 실패"));

            assertThatThrownBy(() -> service.activate(MODEL_ID))
                    .isInstanceOf(ModelActivationException.class)
                    .hasMessageContaining(ModelActivationException.DRY_RUN_SCORING_FAILED)
                    .hasMessageContaining("로드 실패");

            assertThat(model.getStatus()).isEqualTo(ModelStatus.INACTIVE);
            verify(artifactMaterializer).cleanup(any());
        }
    }

    @Nested
    @DisplayName("deactivate")
    class Deactivate {

        @Test
        @DisplayName("ACTIVE 모델을 INACTIVE로 전환한다")
        void shouldDeactivate() {
            MlModel model = model(META_OK);
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
            MlModel model = model(META_OK);
            LocalDate to = LocalDate.of(2026, 6, 1);
            when(modelRepository.findById(MODEL_ID)).thenReturn(Optional.of(model));

            service.resetScoreCursor(MODEL_ID, to);

            verify(scoreSupport).deleteByModelAndDateRange(MODEL_ID, to.plusDays(1), null);
            assertThat(model.getScoreCursor()).isEqualTo(to);
        }

        @Test
        @DisplayName("null이면 미시작으로 되돌리고 전 점수를 삭제한다")
        void shouldResetToNullAndDeleteAll() {
            MlModel model = model(META_OK);
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
            MlModel model = model(META_OK);
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

    private static MlModel model(String metaJson) {
        return MlModel.register("swing_entry", "1.0.0", new byte[]{1}, metaJson,
                ModelOutputType.PROBABILITY, Set.of(StockExchange.KOSPI, StockExchange.KOSDAQ),
                PriceType.ADJUSTED, "root");
    }

    private static PredictRow row(String featureKey) {
        return new PredictRow("AAA", "2026-06-20", Map.of(featureKey, 55.0));
    }
}
