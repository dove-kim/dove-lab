package com.dove.systemevent.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.systemevent.domain.entity.SystemEvent;
import com.dove.systemevent.domain.enums.SystemEventType;
import com.dove.systemevent.domain.repository.SystemEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 시스템 이벤트를 기록·조회하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class SystemEventService {

    private final SystemEventRepository repository;

    /**
     * 시스템 이벤트를 기록한다. (호출 측 트랜잭션과 독립 커밋)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(SystemEventType eventType, MarketType marketType, Map<String, String> detail) {
        repository.save(SystemEvent.of(eventType, marketType, detail));
    }

    /**
     * KRX API 실패를 기록하는 편의 메서드.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordKrxApiFailure(MarketType marketType, String errorMessage) {
        record(SystemEventType.KRX_API_FAILURE, marketType,
                Map.of("error", errorMessage == null ? "" : errorMessage));
    }

    /**
     * KIS API 실패를 기록하는 편의 메서드.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordKisApiFailure(String priceSource, String errorMessage) {
        record(SystemEventType.KIS_API_FAILURE, null,
                Map.of("source", priceSource, "error", errorMessage == null ? "" : errorMessage));
    }

    /**
     * KRX 서버가 일일 한도 초과 응답을 보냈을 때의 편의 메서드.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordKrxRateLimit(MarketType marketType, LocalDate date, String responseBody) {
        Map<String, String> detail = new HashMap<>();
        if (date != null) detail.put("date", date.toString());
        String snippet = responseBody == null
                ? ""
                : responseBody.substring(0, Math.min(200, responseBody.length()));
        detail.put("responseBodySnippet", snippet);
        record(SystemEventType.KRX_RATE_LIMIT_EXCEEDED, marketType, detail);
    }

    /**
     * 일일 파이프라인 단계 실패를 기록하는 편의 메서드.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPipelineStageFailure(String stage, String errorMessage) {
        record(SystemEventType.PIPELINE_STAGE_FAILURE, null,
                Map.of("stage", stage, "error", errorMessage == null ? "" : errorMessage));
    }

    /**
     * 모델 채점 실패를 기록하는 편의 메서드.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordModelScoringFailure(Long modelId, String errorCode, String message) {
        Map<String, String> detail = new HashMap<>();
        detail.put("modelId", modelId == null ? "" : modelId.toString());
        detail.put("errorCode", errorCode == null ? "" : errorCode);
        detail.put("message", message == null ? "" : message);
        record(SystemEventType.MODEL_SCORING_FAILURE, null, detail);
    }

    /**
     * 전체 이벤트 페이징 조회 (발생 일시 내림차순).
     */
    @Transactional(readOnly = true)
    public Page<SystemEvent> findAll(Pageable pageable) {
        return repository.findAllByOrderByOccurredAtDesc(pageable);
    }
}
