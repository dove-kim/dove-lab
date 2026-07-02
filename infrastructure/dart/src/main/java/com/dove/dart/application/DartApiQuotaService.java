package com.dove.dart.application;

import com.dove.apiquota.DailyApiQuota;
import com.dove.dart.config.DartProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * DART 일일 호출 한도 관리 — 백필·폴링·corp동기화가 하나의 일일 카운터(Redis)를 공유한다.
 */
@Service
@RequiredArgsConstructor
public class DartApiQuotaService {

    private static final String NAMESPACE = "dart:api";

    private final StringRedisTemplate redis;
    private final DartProperties properties;

    private DailyApiQuota quota;

    @PostConstruct
    void init() {
        this.quota = new DailyApiQuota(redis, NAMESPACE, properties.getDailyQuota());
        quota.ensureLimit(properties.getDailyQuota(), "DART");
    }

    /**
     * 호출 1건을 카운트하고 한도 초과면 예외를 던진다.
     *
     * @throws DartRateLimitException 일일 한도 초과 시
     */
    public void requireQuota() {
        if (!quota.tryAcquire()) {
            throw new DartRateLimitException("DART_DAILY_QUOTA_EXCEEDED");
        }
    }

    /**
     * 서버가 한도 초과 응답을 보낼 때 로컬 카운트를 한도까지 채운다.
     */
    public void markRemoteLimited() {
        quota.markRemoteLimited();
    }

    /**
     * 오늘 누적 호출 횟수.
     */
    public int currentCount() {
        return quota.currentCount();
    }

    /**
     * 설정된 일일 한도.
     */
    public int currentLimit() {
        return quota.currentLimit();
    }
}
