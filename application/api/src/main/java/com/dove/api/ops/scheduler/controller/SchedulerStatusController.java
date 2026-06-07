package com.dove.api.ops.scheduler.controller;

import com.dove.api.global.security.authorization.RequireRole;
import com.dove.api.global.security.authorization.Role;
import com.dove.jobstatus.JobStatus;
import com.dove.jobstatus.JobStatusRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ROOT 전용 — 스케줄러/백필 진행 상황 대시보드 조회.
 */
@RestController
@RequestMapping("/admin/ops/scheduler")
@RequiredArgsConstructor
@RequireRole(Role.ROOT)
public class SchedulerStatusController {

    private final JobStatusRegistry jobStatusRegistry;

    /**
     * 모든 스케줄러 작업의 최신 진행 상태.
     */
    @GetMapping("/status")
    public List<JobStatus> status() {
        return jobStatusRegistry.all();
    }
}
