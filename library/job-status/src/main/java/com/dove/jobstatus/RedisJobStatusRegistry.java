package com.dove.jobstatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Redis 해시(scheduler:job:status)에 작업 상태를 JSON으로 보관하는 레지스트리.
 *
 * <p>모든 동작은 best-effort다 — Redis 오류는 로그만 남기고 삼켜, 진행률 기록이 작업을 막지 않게 한다.
 */
@Slf4j
@Component
public class RedisJobStatusRegistry implements JobStatusRegistry {

    private static final String KEY = "scheduler:job:status";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisJobStatusRegistry(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void start(String name, long total) {
        long now = System.currentTimeMillis();
        save(new JobStatus(name, JobState.RUNNING, total, 0, now, now, null));
    }

    @Override
    public void progress(String name, long processed) {
        find(name).ifPresent(status -> {
            status.setProcessed(processed);
            status.setUpdatedAtEpochMs(System.currentTimeMillis());
            save(status);
        });
    }

    @Override
    public void complete(String name) {
        find(name).ifPresent(status -> {
            status.setState(JobState.COMPLETED);
            status.setProcessed(status.getTotal()); // 완료 = 전부 처리 (스로틀 자투리까지 반영)
            status.setUpdatedAtEpochMs(System.currentTimeMillis());
            save(status);
        });
    }

    @Override
    public void fail(String name, String message) {
        find(name).ifPresentOrElse(status -> {
            status.setState(JobState.FAILED);
            status.setMessage(message);
            status.setUpdatedAtEpochMs(System.currentTimeMillis());
            save(status);
        }, () -> {
            long now = System.currentTimeMillis();
            save(new JobStatus(name, JobState.FAILED, 0, 0, now, now, message));
        });
    }

    @Override
    public List<JobStatus> all() {
        try {
            Map<Object, Object> entries = redis.opsForHash().entries(KEY);
            List<JobStatus> result = new ArrayList<>(entries.size());
            for (Object value : entries.values()) {
                result.add(objectMapper.readValue(value.toString(), JobStatus.class));
            }
            return result;
        } catch (Exception e) {
            log.warn("작업 상태 조회 실패: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<JobStatus> find(String name) {
        try {
            Object value = redis.opsForHash().get(KEY, name);
            if (value == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(value.toString(), JobStatus.class));
        } catch (Exception e) {
            log.warn("작업 상태 조회 실패({}): {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    private void save(JobStatus status) {
        try {
            redis.opsForHash().put(KEY, status.getName(), objectMapper.writeValueAsString(status));
        } catch (Exception e) {
            log.warn("작업 상태 기록 실패({}): {}", status.getName(), e.getMessage());
        }
    }
}
