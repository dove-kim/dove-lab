package com.dove.modelserving.application.service;

import com.dove.modelserving.application.exception.InvalidModelMetaException;
import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.domain.enums.ModelStatus;
import com.dove.modelserving.domain.feature.FeatureResolver;
import com.dove.modelserving.domain.meta.FeatureHasher;
import com.dove.modelserving.domain.meta.ModelMetaParser;
import com.dove.modelserving.domain.repository.MlModelRepository;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ModelRegistrationService")
@ExtendWith(MockitoExtension.class)
class ModelRegistrationServiceTest {

    private static final List<String> VALID_FEATURES = List.of("rsi_14", "macd_histogram", "rank_turnover");
    private static final byte[] ARTIFACT = {1, 2, 3};
    private static final String CREATED_BY = "42";
    private static final Set<StockExchange> KRX_EXCHANGES =
            Set.of(StockExchange.KOSPI, StockExchange.KOSDAQ);

    @Mock
    private MlModelRepository modelRepository;

    private ModelRegistrationService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new ModelRegistrationService(
                new ModelMetaParser(objectMapper),
                new FeatureResolver(),
                modelRepository,
                objectMapper);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("검증을 통과하면 INACTIVE로 저장한다")
        void shouldSaveInactiveWhenValid() {
            String meta = metaJson(VALID_FEATURES, FeatureHasher.hash(VALID_FEATURES));
            when(modelRepository.save(any(MlModel.class))).thenAnswer(inv -> inv.getArgument(0));

            MlModel saved = service.register(ARTIFACT, meta, KRX_EXCHANGES, PriceType.ADJUSTED, CREATED_BY);

            ArgumentCaptor<MlModel> captor = ArgumentCaptor.forClass(MlModel.class);
            verify(modelRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ModelStatus.INACTIVE);
            assertThat(captor.getValue().getScoreExchanges())
                    .containsExactlyInAnyOrder(StockExchange.KOSPI, StockExchange.KOSDAQ);
            assertThat(saved.getStatus()).isEqualTo(ModelStatus.INACTIVE);
        }

        @Test
        @DisplayName("오버라이드된 이름·버전·진입존을 적용해 저장한다")
        void shouldApplyOverrides() {
            String meta = metaJson(VALID_FEATURES, FeatureHasher.hash(VALID_FEATURES));
            when(modelRepository.save(any(MlModel.class))).thenAnswer(inv -> inv.getArgument(0));
            ModelRegistrationOverrides overrides = new ModelRegistrationOverrides(
                    "my_swing", "2.0.0", "내 진입존", List.of("rsi_14>=50", "macd_histogram>0"));

            service.register(ARTIFACT, meta, overrides, KRX_EXCHANGES, PriceType.ADJUSTED, CREATED_BY);

            ArgumentCaptor<MlModel> captor = ArgumentCaptor.forClass(MlModel.class);
            verify(modelRepository).save(captor.capture());
            MlModel saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("my_swing");
            assertThat(saved.getVersion()).isEqualTo("2.0.0");
            assertThat(saved.getMetaJson()).contains("\"version\":\"2.0.0\"")
                    .contains("내 진입존").contains("rsi_14>=50").contains("macd_histogram>0");
        }

        @Test
        @DisplayName("빈 오버라이드면 meta의 이름·버전을 유지한다")
        void shouldKeepMetaWhenOverrideBlank() {
            String meta = metaJson(VALID_FEATURES, FeatureHasher.hash(VALID_FEATURES));
            when(modelRepository.save(any(MlModel.class))).thenAnswer(inv -> inv.getArgument(0));

            service.register(ARTIFACT, meta, ModelRegistrationOverrides.none(),
                    KRX_EXCHANGES, PriceType.ADJUSTED, CREATED_BY);

            ArgumentCaptor<MlModel> captor = ArgumentCaptor.forClass(MlModel.class);
            verify(modelRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("swing_entry");
            assertThat(captor.getValue().getVersion()).isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("미지의 피처가 하나라도 있으면 거부한다")
        void shouldRejectWhenUnknownFeature() {
            List<String> features = List.of("rsi_14", "made_up_feature");
            String meta = metaJson(features, FeatureHasher.hash(features));

            assertThatThrownBy(() ->
                    service.register(ARTIFACT, meta, KRX_EXCHANGES, PriceType.ADJUSTED, CREATED_BY))
                    .isInstanceOf(InvalidModelMetaException.class)
                    .hasMessageContaining("UNKNOWN_FEATURE")
                    .hasMessageContaining("made_up_feature");

            verify(modelRepository, never()).save(any());
        }

        @Test
        @DisplayName("feature_hash가 피처 목록과 어긋나면 거부한다")
        void shouldRejectWhenHashMismatch() {
            String meta = metaJson(VALID_FEATURES, "deadbeefdeadbeef");

            assertThatThrownBy(() ->
                    service.register(ARTIFACT, meta, KRX_EXCHANGES, PriceType.ADJUSTED, CREATED_BY))
                    .isInstanceOf(InvalidModelMetaException.class)
                    .hasMessage("FEATURE_HASH_MISMATCH");

            verify(modelRepository, never()).save(any());
        }

        @Test
        @DisplayName("필수 스키마 필드가 빠지면 거부한다")
        void shouldRejectWhenSchemaIncomplete() {
            String meta = "{\"name\":\"swing_entry\",\"features\":[\"rsi_14\"]}";

            assertThatThrownBy(() ->
                    service.register(ARTIFACT, meta, KRX_EXCHANGES, PriceType.ADJUSTED, CREATED_BY))
                    .isInstanceOf(InvalidModelMetaException.class)
                    .hasMessage("INVALID_META_SCHEMA");

            verify(modelRepository, never()).save(any());
        }

        @Test
        @DisplayName("JSON 형식이 잘못되면 거부한다")
        void shouldRejectWhenMalformedJson() {
            assertThatThrownBy(() ->
                    service.register(ARTIFACT, "not-json", KRX_EXCHANGES, PriceType.ADJUSTED, CREATED_BY))
                    .isInstanceOf(InvalidModelMetaException.class)
                    .hasMessage("INVALID_META_JSON");

            verify(modelRepository, never()).save(any());
        }
    }

    private static String metaJson(List<String> features, String featureHash) {
        String featureArray = features.stream()
                .map(f -> "\"" + f + "\"")
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        return "{"
                + "\"name\":\"swing_entry\","
                + "\"version\":\"1.0.0\","
                + "\"output_type\":\"probability\","
                + "\"features\":[" + featureArray + "],"
                + "\"feature_hash\":\"" + featureHash + "\""
                + "}";
    }
}
