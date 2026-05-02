package com.dove.distributedlock;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "distributed-lock")
public class DistributedLockProperties {
    private long waitTime = 5;
    private long leaseTime = 60;
    private long ttlSeconds = 86400;
}
