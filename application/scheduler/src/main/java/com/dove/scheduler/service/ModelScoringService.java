package com.dove.scheduler.service;

import com.dove.jobstatus.JobStatusRegistry;
import com.dove.jobstatus.SchedulerJobName;
import com.dove.modelserving.application.service.ModelScoreSweepService;
import com.dove.modelserving.application.service.ModelScoringOutcome;
import com.dove.systemevent.application.service.SystemEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * ACTIVE 모델을 채점기로 채점하는 스케줄러 단계 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelScoringService {

    private final ModelScoreSweepService sweepService;
    private final JobStatusRegistry jobStatusRegistry;
    private final SystemEventService systemEventService;

    /**
     * 전 ACTIVE 모델을 채점하고 실패한 모델은 시스템 이벤트로 기록한다. 진행률은 best-effort로 시작·완료만 기록한다.
     */
    public void scoreAll(LocalDate today) {
        jobStatusRegistry.start(SchedulerJobName.MODEL_SCORING.name(), 0);
        log.info("모델 채점 시작 (트리거일 {})", today);
        try {
            List<ModelScoringOutcome> outcomes = sweepService.scoreAllActiveModels(today);
            for (ModelScoringOutcome outcome : outcomes) {
                if (outcome.errorCode() != null) {
                    systemEventService.recordModelScoringFailure(
                            outcome.modelId(), outcome.errorCode(), outcome.message());
                }
            }
        } finally {
            jobStatusRegistry.complete(SchedulerJobName.MODEL_SCORING.name());
        }
        log.info("모델 채점 완료");
    }
}
