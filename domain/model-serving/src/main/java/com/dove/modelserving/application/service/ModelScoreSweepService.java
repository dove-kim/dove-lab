package com.dove.modelserving.application.service;

import com.dove.indicator.infrastructure.repository.RankSourceRepositorySupport;
import com.dove.modelserving.application.exception.ModelScoringException;
import com.dove.modelserving.application.exception.ScoreCursorRewoundException;
import com.dove.modelserving.application.port.ModelScorer;
import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.domain.entity.StockModelScore;
import com.dove.modelserving.domain.entity.StockModelScoreId;
import com.dove.modelserving.domain.enums.ModelStatus;
import com.dove.modelserving.domain.meta.ModelMeta;
import com.dove.modelserving.domain.meta.ModelMetaParser;
import com.dove.modelserving.domain.repository.MlModelRepository;
import com.dove.modelserving.domain.zone.EntryZone;
import com.dove.modelserving.domain.zone.EntryZoneParser;
import com.dove.modelserving.infrastructure.repository.ScoreSourceRepositorySupport;
import com.dove.modelserving.infrastructure.scorer.ArtifactMaterializer;
import com.dove.modelserving.infrastructure.scorer.PredictInput;
import com.dove.modelserving.infrastructure.scorer.PredictRow;
import com.dove.modelserving.infrastructure.scorer.ScoredRow;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ACTIVE 모델별로 진입존 행을 채점기로 채점해 점수를 저장하고 채점 커서를 전진시키는 sweep 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelScoreSweepService {

    /** 가격 변경·재조정으로 뒤늦게 바뀐 행을 다시 채점하기 위한 재계산 오버랩(거래일 수). */
    static final int OVERLAP_TRADING_DAYS = 30;

    /** 한 번에 수집·채점하는 거래일 수 — peak 메모리를 한 청크치로 고정한다(전이력 일괄 로드로 인한 OOM 방지). */
    static final int SCORE_CHUNK_TRADE_DATES = 100;

    private final MlModelRepository modelRepository;
    private final ModelMetaParser metaParser;
    private final EntryZoneParser entryZoneParser;
    private final ScoreSourceRepositorySupport sourceSupport;
    private final RankSourceRepositorySupport rankSourceSupport;
    private final EntryZoneRowAssembler rowAssembler;
    private final ModelScorer modelScorer;
    private final ArtifactMaterializer artifactMaterializer;
    private final ScoreDateCommitService commitService;
    private final ModelHealthService healthService;

    /** 채점기·결과 해석 외 일반 실패에 부여하는 기본 에러 코드. */
    private static final String GENERIC_ERROR = "SCORING_ERROR";

    /**
     * 모든 ACTIVE 모델을 채점하고 모델별 결과를 반환한다. 한 모델 실패는 다른 모델을 막지 않으며, 실패 모델은 커서를 전진하지 않는다.
     * 채점 상한은 호출일 전일(어제)로 캡한다(당일은 일일 잡 전담).
     */
    public List<ModelScoringOutcome> scoreAllActiveModels(LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        List<ModelScoringOutcome> outcomes = new ArrayList<>();
        for (MlModel model : modelRepository.findByStatus(ModelStatus.ACTIVE)) {
            try {
                scoreModel(model, yesterday);
                healthService.recordSuccess(model.getId());
                outcomes.add(ModelScoringOutcome.ok(model.getId(), model.getName()));
            } catch (Exception e) {
                String errorCode = e instanceof ModelScoringException me ? me.errorCode() : GENERIC_ERROR;
                String message = e.getMessage();
                log.warn("[model {}] 채점 실패: {}", model.getId(), message, e);
                healthService.recordFailure(model.getId(), errorCode + ": " + message);
                outcomes.add(ModelScoringOutcome.failure(model.getId(), model.getName(), errorCode, message));
            }
        }
        return outcomes;
    }

    /**
     * 한 모델의 범위 [커서-오버랩, min(프런티어, ceiling)]의 진입존 행을 거래일 청크 단위로 채점해 거래일별로 저장한다.
     * 아티팩트는 한 번만 로드해 청크마다 재사용하고, 커밋·커서 전진은 거래일 단위로 유지한다(peak 메모리 = 한 청크치).
     */
    private void scoreModel(MlModel model, LocalDate ceiling) {
        EntryZone zone = entryZoneParser.parse(parseMeta(model).entryZone());
        Set<StockExchange> members = model.getScoreExchanges();
        if (members.isEmpty()) {
            log.warn("[model {}] 채점 대상 거래소가 비어 있어 건너뜀", model.getId());
            return;
        }
        PriceType priceType = model.getScorePriceType();

        LocalDate frontier = rankSourceSupport.findIndicatorFrontier(members, priceType);
        if (frontier == null) return; // rank 미완비 → 다음 배치로
        LocalDate upperBound = frontier.isBefore(ceiling) ? frontier : ceiling;

        LocalDate expected = model.getScoreCursor();
        LocalDate from = expected == null ? null : overlapStart(members, priceType, expected);
        List<LocalDate> dates = sourceSupport.findScoreTradeDates(members, priceType, from, upperBound);
        if (dates.isEmpty()) return;

        Path artifactPath = artifactMaterializer.materialize(model.getId(), model.getArtifact());
        try {
            // 거래일을 청크로 나눠 (수집→채점→커밋) 스트리밍 — 아티팩트는 위에서 1회 로드해 재사용하고,
            // 각 청크 종료 시 그 청크의 행·점수만 메모리에 두어 peak를 한 청크치로 고정한다(전이력 백필 OOM 방지).
            for (int start = 0; start < dates.size(); start += SCORE_CHUNK_TRADE_DATES) {
                List<LocalDate> chunk = dates.subList(start, Math.min(start + SCORE_CHUNK_TRADE_DATES, dates.size()));

                // 1) 이 청크 거래일×member의 진입존 행 수집 + (종목,거래일)→native 거래소 매핑
                List<PredictRow> rows = new ArrayList<>();
                Map<String, StockExchange> exchangeByKey = new HashMap<>();
                for (LocalDate date : chunk) {
                    for (StockExchange member : members) {
                        for (PredictRow row : rowAssembler.assemble(zone, member, priceType, date)) {
                            rows.add(row);
                            exchangeByKey.put(rowKey(row.ticker(), row.tradeDate()), member);
                        }
                    }
                }

                // 2) 이 청크 행 채점 → 거래일별로 묶기
                Map<LocalDate, List<StockModelScore>> scoresByDate =
                        scoreAll(model, artifactPath, priceType, rows, exchangeByKey);

                // 3) 청크 거래일별 커밋(커서 CAS 유지). rewind 감지 시 이 모델 채점 중단(finally에서 정리).
                for (LocalDate date : chunk) {
                    try {
                        commitService.commit(model.getId(), scoresByDate.getOrDefault(date, List.of()), expected, date);
                    } catch (ScoreCursorRewoundException e) {
                        log.info("[model {}] {} 채점 중단 — 커서 변경 감지", model.getId(), date);
                        return;
                    }
                    expected = date;
                }
            }
        } finally {
            artifactMaterializer.cleanup(artifactPath);
        }
    }

    /**
     * 전달받은 진입존 행을 채점기로 채점하고, 결과를 거래일별 점수 행으로 묶는다.
     */
    private Map<LocalDate, List<StockModelScore>> scoreAll(MlModel model, Path artifactPath, PriceType priceType,
                                                          List<PredictRow> rows,
                                                          Map<String, StockExchange> exchangeByKey) {
        Map<LocalDate, List<StockModelScore>> byDate = new HashMap<>();
        if (rows.isEmpty()) return byDate;

        List<ScoredRow> scored = modelScorer.score(new PredictInput(model.getId(), artifactPath.toString(), rows));
        LocalDateTime now = LocalDateTime.now();
        for (ScoredRow row : scored) {
            Double score = row.score();
            // null·비유한값(NaN·±Infinity)은 결측 취급 → 저장 안 함(배치 insert 리터럴 인라인 SQL 오류 방지)
            if (score == null || !Double.isFinite(score)) continue;
            StockExchange exchange = exchangeByKey.get(rowKey(row.ticker(), row.tradeDate()));
            if (exchange == null) continue;
            LocalDate date = LocalDate.parse(row.tradeDate());
            StockModelScoreId id = new StockModelScoreId(row.ticker(), exchange, priceType, date, model.getId());
            byDate.computeIfAbsent(date, k -> new ArrayList<>())
                    .add(new StockModelScore(id, score.floatValue(), now));
        }
        return byDate;
    }

    private static String rowKey(String ticker, String tradeDate) {
        return ticker + "|" + tradeDate;
    }

    /**
     * 커서일에서 오버랩 거래일 수만큼 앞선 거래일을 재계산 시작 경계(exclusive)로 반환한다.
     * 거래일 수가 부족하면 null(처음부터 다시).
     */
    private LocalDate overlapStart(Set<StockExchange> members, PriceType priceType, LocalDate cursor) {
        LocalDate boundary = cursor;
        for (int i = 0; i < OVERLAP_TRADING_DAYS; i++) {
            LocalDate prev = sourceSupport.findPreviousTradeDate(members, priceType, boundary);
            if (prev == null) return null;
            boundary = prev;
        }
        return boundary;
    }

    private ModelMeta parseMeta(MlModel model) {
        return metaParser.parse(model.getMetaJson());
    }
}
