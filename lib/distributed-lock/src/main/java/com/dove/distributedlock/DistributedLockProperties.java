package com.dove.distributedlock;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 분산락 기본 설정. 환경변수로 오버라이드 가능하다.
 *
 * <ul>
 *   <li>DISTRIBUTED_LOCK_WAIT_TIME — 락 획득 대기 시간 (초, 기본 5)</li>
 *   <li>DISTRIBUTED_LOCK_LEASE_TIME — 락 임대 시간 (초, 기본 60)</li>
 *   <li>DISTRIBUTED_LOCK_TTL_SECONDS — 처리 완료 마킹 TTL (초, 기본 86400)</li>
 * </ul>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "distributed-lock")
public class DistributedLockProperties {
    private long waitTime = 5;
    private long leaseTime = 60;
    private long ttlSeconds = 86400;
}
