package com.dove.jobstatus;

import java.util.List;
import java.util.Optional;

/**
 * 스케줄러/백필 작업의 진행 상태를 기록·조회한다. 관측성 목적의 best-effort 동작으로,
 * 저장소(Redis) 오류가 작업 자체를 방해해서는 안 된다.
 */
public interface JobStatusRegistry {

    /**
     * 작업 시작을 기록한다. total을 모르면 0.
     */
    void start(String name, long total);

    /**
     * 진행 건수를 갱신한다.
     */
    void progress(String name, long processed);

    /**
     * 작업 정상 완료를 기록한다.
     */
    void complete(String name);

    /**
     * 작업 실패를 사유와 함께 기록한다.
     */
    void fail(String name, String message);

    /**
     * 기록된 모든 작업 상태를 반환한다 (대시보드용).
     */
    List<JobStatus> all();

    /**
     * 이름으로 단일 작업 상태를 조회한다.
     */
    Optional<JobStatus> find(String name);
}
