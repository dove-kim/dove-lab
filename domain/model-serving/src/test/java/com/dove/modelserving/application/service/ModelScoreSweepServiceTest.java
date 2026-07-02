package com.dove.modelserving.application.service;

import com.dove.indicator.domain.entity.StockFeatureDaily;
import com.dove.indicator.domain.entity.StockFeatureDailyId;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.indicator.domain.rank.entity.StockRankDaily;
import com.dove.indicator.domain.rank.entity.StockRankDailyId;
import com.dove.indicator.domain.rank.enums.RankType;
import com.dove.indicator.infrastructure.repository.RankSourceRepositorySupport;
import com.dove.modelserving.application.exception.ModelScoringException;
import com.dove.modelserving.application.exception.ScoreCursorRewoundException;
import com.dove.modelserving.application.port.ModelScorer;
import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.domain.entity.StockModelScore;
import com.dove.modelserving.domain.enums.ModelOutputType;
import com.dove.modelserving.domain.enums.ModelStatus;
import com.dove.modelserving.domain.feature.FeatureRowMapper;
import com.dove.modelserving.domain.meta.ModelMetaParser;
import com.dove.modelserving.domain.repository.MlModelRepository;
import com.dove.modelserving.domain.zone.EntryZoneParser;
import com.dove.modelserving.infrastructure.repository.ScoreSourceRepositorySupport;
import com.dove.modelserving.infrastructure.scorer.ArtifactMaterializer;
import com.dove.modelserving.infrastructure.scorer.PredictInput;
import com.dove.modelserving.infrastructure.scorer.ScoredRow;
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

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ModelScoreSweepService")
@ExtendWith(MockitoExtension.class)
class ModelScoreSweepServiceTest {

    private static final StockExchange KOSPI = StockExchange.KOSPI;
    private static final StockExchange KOSDAQ = StockExchange.KOSDAQ;
    private static final Set<StockExchange> MEMBERS = Set.of(KOSPI, KOSDAQ);
    private static final PriceType PRICE_TYPE = PriceType.ADJUSTED;
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 26);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);

    @Mock
    private MlModelRepository modelRepository;
    @Mock
    private ScoreSourceRepositorySupport sourceSupport;
    @Mock
    private RankSourceRepositorySupport rankSourceSupport;
    @Mock
    private ModelScorer modelScorer;
    @Mock
    private ArtifactMaterializer artifactMaterializer;
    @Mock
    private ScoreDateCommitService commitService;
    @Mock
    private ModelHealthService healthService;

    private ModelScoreSweepService service;

    @BeforeEach
    void setUp() {
        service = new ModelScoreSweepService(
                modelRepository,
                new ModelMetaParser(new ObjectMapper()),
                new EntryZoneParser(new com.dove.modelserving.domain.feature.FeatureResolver()),
                sourceSupport,
                rankSourceSupport,
                new FeatureRowMapper(),
                modelScorer,
                artifactMaterializer,
                commitService,
                healthService);
    }

    @Nested
    @DisplayName("scoreAllActiveModels")
    class ScoreAllActiveModels {

        @Test
        @DisplayName("진입존 행만 채점기로 보내고 그 점수를 한 거래일 단위로 커밋한다")
        void shouldScoreOnlyEntryZoneRowsAndCommit() {
            MlModel model = activeModel(1L, null);
            stubSingleDateUniverse(model);

            ArgumentCaptor<PredictInput> inputCaptor = ArgumentCaptor.forClass(PredictInput.class);
            when(modelScorer.score(inputCaptor.capture()))
                    .thenReturn(List.of(new ScoredRow("AAA", "2026-06-20", 0.62)));

            List<ModelScoringOutcome> outcomes = service.scoreAllActiveModels(TODAY);

            // 진입존을 만족하는 KOSPI AAA만 채점기로 전달(KOSDAQ는 진입존 미충족 → 채점기 미호출)
            assertThat(inputCaptor.getValue().rows()).hasSize(1);
            assertThat(inputCaptor.getValue().rows().get(0).ticker()).isEqualTo("AAA");

            ArgumentCaptor<List<StockModelScore>> scoreCaptor = listCaptor();
            verify(commitService).commit(eq(1L), scoreCaptor.capture(), eq(null), eq(LocalDate.of(2026, 6, 20)));
            assertThat(scoreCaptor.getValue()).singleElement()
                    .satisfies(s -> assertThat(s.getScore()).isEqualTo(0.62f));
            verify(artifactMaterializer).cleanup(any());

            // 성공 헬스 기록 + ok outcome
            verify(healthService).recordSuccess(1L);
            assertThat(outcomes).singleElement()
                    .satisfies(o -> {
                        assertThat(o.modelId()).isEqualTo(1L);
                        assertThat(o.errorCode()).isNull();
                    });
        }

        @Test
        @DisplayName("ModelScoringException이 나면 그 코드로 실패 헬스를 기록하고 실패 outcome을 반환한다")
        void shouldRecordFailureWhenScoringException() {
            // 단일 member(KOSPI)로 — 다중 member면 Set 반복 순서(JVM별 무작위)에 따라 예외가 한 member 스텁을 안 써 strict가 깨짐
            Set<StockExchange> members = Set.of(KOSPI);
            MlModel model = activeModel(1L, null, members);
            LocalDate date = LocalDate.of(2026, 6, 20);
            LocalDate prev = LocalDate.of(2026, 6, 19);
            when(modelRepository.findByStatus(ModelStatus.ACTIVE)).thenReturn(List.of(model));
            when(rankSourceSupport.findIndicatorFrontier(members, PRICE_TYPE)).thenReturn(YESTERDAY);
            when(sourceSupport.findScoreTradeDates(members, PRICE_TYPE, null, YESTERDAY))
                    .thenReturn(List.of(date));
            when(artifactMaterializer.materialize(eq(1L), any())).thenReturn(Path.of("model.pkl"));
            stubMember(KOSPI, "AAA", date, prev);
            when(modelScorer.score(any()))
                    .thenThrow(new ModelScoringException("FEATURE_MISMATCH", "피처 불일치"));

            List<ModelScoringOutcome> outcomes = service.scoreAllActiveModels(TODAY);

            verify(healthService).recordFailure(1L, "FEATURE_MISMATCH: 피처 불일치");
            assertThat(outcomes).singleElement()
                    .satisfies(o -> {
                        assertThat(o.modelId()).isEqualTo(1L);
                        assertThat(o.errorCode()).isEqualTo("FEATURE_MISMATCH");
                        assertThat(o.message()).isEqualTo("피처 불일치");
                    });
            verify(artifactMaterializer).cleanup(any());
        }

        @Test
        @DisplayName("scoreExchanges가 KOSPI·KOSDAQ면 두 거래소를 모두 채점해 합쳐 한 번에 커밋한다")
        void shouldScoreBothMembersWhenMultipleScoreExchanges() {
            MlModel model = activeModel(1L, null);
            LocalDate date = LocalDate.of(2026, 6, 20);
            LocalDate prev = LocalDate.of(2026, 6, 19);
            when(modelRepository.findByStatus(ModelStatus.ACTIVE)).thenReturn(List.of(model));
            when(rankSourceSupport.findIndicatorFrontier(MEMBERS, PRICE_TYPE)).thenReturn(YESTERDAY);
            when(sourceSupport.findScoreTradeDates(MEMBERS, PRICE_TYPE, null, YESTERDAY))
                    .thenReturn(List.of(date));
            when(artifactMaterializer.materialize(eq(model.getId()), any())).thenReturn(Path.of("model.pkl"));

            // 두 member 모두 AAA(KOSPI)·CCC(KOSDAQ)가 진입존을 만족하도록 스텁
            stubMember(KOSPI, "AAA", date, prev);
            stubMember(KOSDAQ, "CCC", date, prev);

            // 두 member 행을 모아 채점기는 1회만 호출(모델 1회 로드)
            when(modelScorer.score(any()))
                    .thenReturn(List.of(
                            new ScoredRow("AAA", "2026-06-20", 0.62),
                            new ScoredRow("CCC", "2026-06-20", 0.71)));

            service.scoreAllActiveModels(TODAY);

            verify(modelScorer, times(1)).score(any());

            // 두 member 점수를 합쳐 한 거래일에 한 번 커밋
            ArgumentCaptor<List<StockModelScore>> scoreCaptor = listCaptor();
            verify(commitService).commit(eq(1L), scoreCaptor.capture(), eq(null), eq(date));
            assertThat(scoreCaptor.getValue()).hasSize(2);
            assertThat(scoreCaptor.getValue())
                    .extracting(s -> s.getId().getTicker())
                    .containsExactlyInAnyOrder("AAA", "CCC");
            assertThat(scoreCaptor.getValue())
                    .extracting(s -> s.getId().getExchange())
                    .containsExactlyInAnyOrder(KOSPI, KOSDAQ);
        }

        @Test
        @DisplayName("커서가 있으면 30거래일 오버랩만큼 과거부터 다시 채점 범위를 잡는다")
        void shouldOverlapThirtyTradingDaysWhenCursorExists() {
            LocalDate cursor = LocalDate.of(2026, 6, 20);
            MlModel model = activeModel(1L, cursor);
            when(modelRepository.findByStatus(ModelStatus.ACTIVE)).thenReturn(List.of(model));
            when(rankSourceSupport.findIndicatorFrontier(MEMBERS, PRICE_TYPE)).thenReturn(YESTERDAY);

            // 오버랩 경계 계산: 커서에서 30거래일 뒤로 — 각 단계가 union findPreviousTradeDate 1회
            LocalDate overlapBoundary = LocalDate.of(2026, 5, 1);
            when(sourceSupport.findPreviousTradeDate(eq(MEMBERS), eq(PRICE_TYPE), any()))
                    .thenReturn(overlapBoundary);
            when(sourceSupport.findScoreTradeDates(MEMBERS, PRICE_TYPE, overlapBoundary, YESTERDAY))
                    .thenReturn(List.of());

            service.scoreAllActiveModels(TODAY);

            verify(sourceSupport).findScoreTradeDates(MEMBERS, PRICE_TYPE, overlapBoundary, YESTERDAY);
            verify(sourceSupport, times(ModelScoreSweepService.OVERLAP_TRADING_DAYS))
                    .findPreviousTradeDate(eq(MEMBERS), eq(PRICE_TYPE), any());
            verifyNoInteractions(modelScorer);
        }

        @Test
        @DisplayName("rank 프런티어가 없으면 채점하지 않는다")
        void shouldSkipWhenNoFrontier() {
            MlModel model = activeModel(1L, null);
            when(modelRepository.findByStatus(ModelStatus.ACTIVE)).thenReturn(List.of(model));
            when(rankSourceSupport.findIndicatorFrontier(MEMBERS, PRICE_TYPE)).thenReturn(null);

            service.scoreAllActiveModels(TODAY);

            verifyNoInteractions(modelScorer, artifactMaterializer, commitService);
        }

        @Test
        @DisplayName("커서가 변경되면 그 모델 채점을 중단한다")
        void shouldStopModelWhenCursorRewound() {
            MlModel model = activeModel(1L, null);
            stubSingleDateUniverse(model);
            when(modelScorer.score(any()))
                    .thenReturn(List.of(new ScoredRow("AAA", "2026-06-20", 0.62)));
            doThrow(new ScoreCursorRewoundException(1L))
                    .when(commitService).commit(eq(1L), any(), eq(null), any());

            service.scoreAllActiveModels(TODAY);

            verify(commitService).commit(eq(1L), any(), eq(null), any());
            verify(artifactMaterializer).cleanup(any());
        }

        @Test
        @DisplayName("한 모델이 실패해도 다른 모델 채점은 계속한다")
        void shouldIsolateModelFailures() {
            MlModel failing = activeModel(1L, null);
            MlModel healthy = activeModel(2L, null);
            when(modelRepository.findByStatus(ModelStatus.ACTIVE)).thenReturn(List.of(failing, healthy));

            // 모델1: 프런티어 조회에서 예외 — 격리되어야 함
            when(rankSourceSupport.findIndicatorFrontier(MEMBERS, PRICE_TYPE))
                    .thenThrow(new RuntimeException("boom"))
                    .thenReturn(null);

            service.scoreAllActiveModels(TODAY);

            verify(rankSourceSupport, times(2)).findIndicatorFrontier(MEMBERS, PRICE_TYPE);
        }
    }

    /**
     * 커서 없는 모델이 한 거래일(2026-06-20) {KOSPI,KOSDAQ} 중 KOSPI AAA만 진입존을 만족하도록 스텁한다.
     * KOSDAQ는 진입존 미충족(turnover 낮음)으로 채점 입력이 비어 점수가 없다.
     */
    private void stubSingleDateUniverse(MlModel model) {
        LocalDate date = LocalDate.of(2026, 6, 20);
        LocalDate prev = LocalDate.of(2026, 6, 19);
        when(modelRepository.findByStatus(ModelStatus.ACTIVE)).thenReturn(List.of(model));
        when(rankSourceSupport.findIndicatorFrontier(MEMBERS, PRICE_TYPE)).thenReturn(YESTERDAY);
        when(sourceSupport.findScoreTradeDates(MEMBERS, PRICE_TYPE, null, YESTERDAY))
                .thenReturn(List.of(date));
        when(artifactMaterializer.materialize(eq(model.getId()), any())).thenReturn(Path.of("model.pkl"));

        // KOSPI: AAA(진입존 충족)
        stubMember(KOSPI, "AAA", date, prev);

        // KOSDAQ: BBB(미충족: turnover 낮음)
        when(sourceSupport.findFeatures(KOSDAQ, PRICE_TYPE, date)).thenReturn(List.of(
                feature(KOSDAQ, "BBB", date, 55.0, 0.3)));
        when(sourceSupport.findRanks(KOSDAQ, PRICE_TYPE, date)).thenReturn(List.of(
                rank(KOSDAQ, "BBB", date, 0.4)));
        when(sourceSupport.findPreviousTradeDate(KOSDAQ, PRICE_TYPE, date)).thenReturn(prev);
        when(sourceSupport.findFeatures(KOSDAQ, PRICE_TYPE, prev)).thenReturn(List.of(
                feature(KOSDAQ, "BBB", prev, 45.0, 0.1)));
        when(sourceSupport.findRanks(KOSDAQ, PRICE_TYPE, prev)).thenReturn(List.of());
    }

    /**
     * 한 member 거래소에서 ticker가 진입존(rsi 상향돌파·macd>0·turnover>=0.5)을 만족하도록 스텁한다.
     */
    private void stubMember(StockExchange exchange, String ticker, LocalDate date, LocalDate prev) {
        when(sourceSupport.findFeatures(exchange, PRICE_TYPE, date)).thenReturn(List.of(
                feature(exchange, ticker, date, 55.0, 0.3)));
        when(sourceSupport.findRanks(exchange, PRICE_TYPE, date)).thenReturn(List.of(
                rank(exchange, ticker, date, 0.7)));
        when(sourceSupport.findPreviousTradeDate(exchange, PRICE_TYPE, date)).thenReturn(prev);
        when(sourceSupport.findFeatures(exchange, PRICE_TYPE, prev)).thenReturn(List.of(
                feature(exchange, ticker, prev, 45.0, 0.1)));
        when(sourceSupport.findRanks(exchange, PRICE_TYPE, prev)).thenReturn(List.of());
    }

    private static StockFeatureDaily feature(StockExchange exchange, String ticker, LocalDate date,
                                             double rsi14, double macdHist) {
        StockFeatureDailyId id = new StockFeatureDailyId(ticker, exchange, PRICE_TYPE, date);
        StockFeatureDaily row = new StockFeatureDaily(id, 1, 100L, 110L, 90L, 105L, 1000L, 5000L, LocalDateTime.now());
        row.set(IndicatorType.RSI_14, rsi14);
        row.set(IndicatorType.MACD_HISTOGRAM, macdHist);
        return row;
    }

    private static StockRankDaily rank(StockExchange exchange, String ticker, LocalDate date, double turnover) {
        StockRankDaily row = new StockRankDaily(
                new StockRankDailyId(ticker, exchange, PRICE_TYPE, date), LocalDateTime.now());
        row.set(RankType.RANK_TURNOVER, turnover);
        return row;
    }

    private static MlModel activeModel(Long id, LocalDate cursor) {
        return activeModel(id, cursor, MEMBERS);
    }

    private static MlModel activeModel(Long id, LocalDate cursor, Set<StockExchange> exchanges) {
        MlModel model = new MlModel("swing_entry", "1.0.0", new byte[]{1}, championMetaJson(),
                ModelOutputType.PROBABILITY, exchanges, PRICE_TYPE, "42");
        setField(model, "id", id);
        setField(model, "status", ModelStatus.ACTIVE);
        setField(model, "scoreCursor", cursor);
        return model;
    }

    private static String championMetaJson() {
        return "{"
                + "\"name\":\"swing_entry\",\"version\":\"1.0.0\",\"output_type\":\"probability\","
                + "\"features\":[\"rsi_14\"],\"feature_hash\":\"x\","
                + "\"entry_zone\":{\"desc\":\"d\",\"conditions\":["
                + "\"rsi_14>=50\",\"prev_rsi_14<50\",\"macd_histogram>0\",\"rank_turnover>=0.5\"]}"
                + "}";
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<StockModelScore>> listCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
