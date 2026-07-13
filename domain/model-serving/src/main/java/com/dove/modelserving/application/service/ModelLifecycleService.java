package com.dove.modelserving.application.service;

import com.dove.modelserving.application.exception.ModelActivationException;
import com.dove.modelserving.application.exception.ModelNotFoundException;
import com.dove.modelserving.application.exception.ModelScoringException;
import com.dove.modelserving.application.port.DryRunSampleSource;
import com.dove.modelserving.application.port.ModelScorer;
import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.domain.enums.ModelOutputType;
import com.dove.modelserving.domain.meta.ModelMetaParser;
import com.dove.modelserving.domain.repository.MlModelRepository;
import com.dove.modelserving.infrastructure.repository.StockModelScoreRepositorySupport;
import com.dove.modelserving.infrastructure.scorer.ArtifactMaterializer;
import com.dove.modelserving.infrastructure.scorer.PredictInput;
import com.dove.modelserving.infrastructure.scorer.PredictRow;
import com.dove.modelserving.infrastructure.scorer.ScoredRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 모델의 활성화·비활성화·커서 리셋·삭제 생명주기를 다루는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ModelLifecycleService {

    /** 드라이런에 사용할 표본 행 수 상한. */
    static final int DRY_RUN_SAMPLE_SIZE = 200;

    private final MlModelRepository modelRepository;
    private final StockModelScoreRepositorySupport scoreSupport;
    private final ModelMetaParser metaParser;
    private final DryRunSampleSource sampleSource;
    private final ModelScorer modelScorer;
    private final ArtifactMaterializer artifactMaterializer;

    /**
     * 드라이런 검증을 통과하면 모델을 ACTIVE로 전환한다.
     *
     * @throws ModelNotFoundException   모델이 없을 때
     * @throws ModelActivationException 드라이런 검증(표본 없음·피처 불일치·출력 위반·채점기 실행 실패)에 실패할 때
     */
    public MlModel activate(Long modelId) {
        MlModel model = find(modelId);
        dryRun(model);
        model.activate();
        return model;
    }

    /**
     * 활성화 전 드라이런: DB 표본 진입존 행을 채점기로 채점해 피처 이름 일치·출력 유효성을 확인한다. 위반 시 활성화를 차단한다.
     *
     * @throws ModelActivationException 표본 없음·피처 불일치·출력 위반·채점기 실행 실패 시
     */
    private void dryRun(MlModel model) {
        List<PredictRow> rows = sampleSource.sample(model, DRY_RUN_SAMPLE_SIZE);
        if (rows.isEmpty()) {
            throw new ModelActivationException(ModelActivationException.DRY_RUN_NO_SAMPLE);
        }
        validateFeatureMatch(metaParser.parse(model.getMetaJson()).features(), rows);
        validateOutput(model.getOutputType(), dryRunScore(model, rows));
    }

    /**
     * 표본 행을 채점기로 1회 채점한다. 채점기 실행·해석 실패도 활성화 차단 사유로 매핑하되 원인 메시지를 보존한다.
     *
     * @throws ModelActivationException 채점기 실행·해석이 실패할 때
     */
    private List<ScoredRow> dryRunScore(MlModel model, List<PredictRow> rows) {
        Path artifactPath = artifactMaterializer.materialize(model.getId(), model.getArtifact());
        try {
            return modelScorer.score(new PredictInput(model.getId(), artifactPath.toString(), rows));
        } catch (ModelScoringException e) {
            throw new ModelActivationException(
                    ModelActivationException.DRY_RUN_SCORING_FAILED + ": " + e.getMessage());
        } finally {
            artifactMaterializer.cleanup(artifactPath);
        }
    }

    /**
     * meta 피처 이름이 표본 입력 피처 키 집합에 모두(대소문자 구분) 존재하는지 검증한다.
     * 서버가 입력 키를 소문자화하므로 meta 피처명이 대문자면 여기서 불일치로 걸린다.
     *
     * @throws ModelActivationException 하나라도 대소문자까지 일치하는 키가 없을 때
     */
    static void validateFeatureMatch(List<String> metaFeatures, List<PredictRow> rows) {
        Set<String> inputKeys = new HashSet<>();
        for (PredictRow row : rows) {
            if (row.features() != null) inputKeys.addAll(row.features().keySet());
        }
        if (metaFeatures == null) return;
        for (String feature : metaFeatures) {
            if (!inputKeys.contains(feature)) {
                throw new ModelActivationException(ModelActivationException.DRY_RUN_FEATURE_MISMATCH);
            }
        }
    }

    /**
     * 표본 점수가 유효한지 검증한다: NaN·무한 없음, 확률형은 [0,1] 범위, 유효 점수가 최소 한 건 이상 존재.
     * 대소문자 버그로 입력이 전부 결측이면 점수가 전부 null/NaN이 되어 여기서 걸린다.
     *
     * @throws ModelActivationException 점수가 NaN·무한이거나 확률 범위를 벗어나거나 유효 점수가 하나도 없을 때
     */
    static void validateOutput(ModelOutputType outputType, List<ScoredRow> scored) {
        boolean anyValid = false;
        for (ScoredRow row : scored) {
            Double score = row.score();
            if (score == null) continue;
            if (Double.isNaN(score) || Double.isInfinite(score)) {
                throw new ModelActivationException(ModelActivationException.DRY_RUN_INVALID_OUTPUT);
            }
            if (outputType == ModelOutputType.PROBABILITY && (score < 0.0 || score > 1.0)) {
                throw new ModelActivationException(ModelActivationException.DRY_RUN_INVALID_OUTPUT);
            }
            anyValid = true;
        }
        if (!anyValid) {
            throw new ModelActivationException(ModelActivationException.DRY_RUN_INVALID_OUTPUT);
        }
    }

    /**
     * 모델을 INACTIVE로 전환한다.
     *
     * @throws ModelNotFoundException 모델이 없을 때
     */
    public MlModel deactivate(Long modelId) {
        MlModel model = find(modelId);
        model.deactivate();
        return model;
    }

    /**
     * 채점 커서를 지정 거래일로 되돌리고, 그 거래일 이후의 점수를 삭제한다. null이면 미시작으로 되돌리고 전 점수를 삭제한다.
     *
     * @throws ModelNotFoundException 모델이 없을 때
     */
    public MlModel resetScoreCursor(Long modelId, LocalDate toDate) {
        MlModel model = find(modelId);
        scoreSupport.deleteByModelAndDateRange(modelId, toDate == null ? null : toDate.plusDays(1), null);
        model.resetScoreCursor(toDate);
        return model;
    }

    /**
     * 모델과 그 모델의 모든 점수를 함께 삭제한다.
     *
     * @throws ModelNotFoundException 모델이 없을 때
     */
    public void delete(Long modelId) {
        MlModel model = find(modelId);
        scoreSupport.deleteByModel(modelId);
        modelRepository.delete(model);
    }

    private MlModel find(Long modelId) {
        return modelRepository.findById(modelId)
                .orElseThrow(() -> new ModelNotFoundException(modelId));
    }
}
