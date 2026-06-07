package com.dove.jobstatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 스케줄러/백필 작업의 진행 상태 스냅샷. Redis에 저장되어 ROOT 대시보드가 조회한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobStatus {

    private String name;
    private JobState state;
    private long total;
    private long processed;
    private long startedAtEpochMs;
    private long updatedAtEpochMs;
    private String message;
}
