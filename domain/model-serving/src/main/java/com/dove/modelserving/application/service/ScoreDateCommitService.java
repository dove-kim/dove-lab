package com.dove.modelserving.application.service;

import com.dove.modelserving.application.exception.ScoreCursorRewoundException;
import com.dove.modelserving.domain.entity.StockModelScore;
import com.dove.modelserving.domain.repository.StockModelScoreRepository;
import com.dove.modelserving.infrastructure.repository.MlModelRepositorySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 한 거래일의 채점 점수를 저장하고 모델의 채점 커서를 그 거래일로 CAS 전진하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class ScoreDateCommitService {

    private final StockModelScoreRepository scoreRepository;
    private final MlModelRepositorySupport modelRepositorySupport;

    /**
     * 한 거래일의 점수를 저장하고 그 거래일로 모델 커서를 CAS 전진한다.
     * 커서가 기대값과 다르면 저장과 함께 롤백하고 예외를 던진다(멱등 중단).
     *
     * @param modelId   채점 모델 ID
     * @param scores    저장할 점수 행(존 미충족이면 빈 목록일 수 있음)
     * @param expected  계산 시작 시점의 커서값(CAS 기준, null=미시작)
     * @param tradeDate 전진 대상 거래일
     * @throws ScoreCursorRewoundException 커서가 expected와 다를 때
     */
    @Transactional
    public void commit(Long modelId, List<StockModelScore> scores, LocalDate expected, LocalDate tradeDate) {
        if (!scores.isEmpty()) {
            scoreRepository.saveAll(scores);
        }
        long advanced = modelRepositorySupport.advanceScoreCursorIfMatches(modelId, expected, tradeDate);
        if (advanced == 0) {
            throw new ScoreCursorRewoundException(modelId);
        }
    }
}
