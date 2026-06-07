package com.dove.krx.quota;

import com.dove.apiquota.ApiQuotaStatus;
import com.dove.apiquota.PerSecondApiQuota;
import com.dove.apiquota.QuotaStatusProvider;
import com.dove.apiquota.QuotaType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * KRX API 일일 호출 한도 관리.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "krx.api.quota.enabled", havingValue = "true", matchIfMissing = true)
public class KrxApiQuotaService implements QuotaStatusProvider {

    private static final String NAMESPACE = "krx:api";

    private final StringRedisTemplate redis;

    @Value("${krx.api.daily-quota:6000}")
    private int configuredQuota;

    @Value("${krx.api.per-second:50}")
    private int perSecond;

    private DailyApiQuota quota;
    private PerSecondApiQuota rateLimiter;

    @PostConstruct
    void init() {
        this.quota = new DailyApiQuota(redis, NAMESPACE, configuredQuota);
        quota.ensureLimit(configuredQuota, "KRX");
        this.rateLimiter = new PerSecondApiQuota(perSecond);
    }

    /**
     * 호출 권한을 획득한다. 초당 제한 대기 후 일일 한도를 확인한다.
     *
     * @throws KrxDailyQuotaExceededException 일일 한도 초과 시
     */
    public void tryAcquire() {
        try {
            rateLimiter.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KrxRemoteRateLimitException("KRX 초당 제한 대기 중 인터럽트");
        }
        if (!quota.tryAcquire()) {
            throw new KrxDailyQuotaExceededException();
        }
    }

    /**
     * 서버 한도 초과 응답 시 로컬 카운트를 한도까지 채운다.
     */
    public void markRemoteRateLimited() {
        quota.markRemoteLimited();
    }

    /**
     * 오늘 누적 호출 횟수를 반환한다.
     */
    public int currentCount()  { return quota.currentCount(); }

    /**
     * 설정된 일일 한도를 반환한다.
     */
    public int currentQuota()  { return quota.currentLimit(); }

    /**
     * 마지막으로 한도 초과가 발생한 시각을 반환한다.
     */
    public Optional<ZonedDateTime> lastRateLimitAt() {
        return quota.lastLimitAt();
    }

    @Override
    public ApiQuotaStatus getStatus() {
        return new ApiQuotaStatus("KRX", QuotaType.DAILY,
                currentCount(), currentQuota(),
                lastRateLimitAt().map(ZonedDateTime::toString).orElse(null));
    }
}
