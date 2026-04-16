package com.dove.distributedlock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * {@link IdempotentConsumer} 어노테이션이 적용된 메서드에 분산락과 멱등성 가드를 제공하는 AOP Aspect.
 * Redisson 분산락으로 동시 실행을 방지하고, Redis 키로 중복 처리를 차단한다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(DistributedLockProperties.class)
@ConditionalOnProperty(name = "distributed-lock.enabled", havingValue = "true", matchIfMissing = true)
public class IdempotentConsumerAspect {

    private final RedissonClient redissonClient;
    private final DistributedLockProperties properties;

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Around("@annotation(com.dove.distributedlock.IdempotentConsumer)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        IdempotentConsumer annotation = method.getAnnotation(IdempotentConsumer.class);

        String key = resolveKey(annotation, method, joinPoint.getArgs());
        long waitTime = annotation.waitTime() >= 0 ? annotation.waitTime() : properties.getWaitTime();
        long leaseTime = annotation.leaseTime() >= 0 ? annotation.leaseTime() : properties.getLeaseTime();
        long ttlSeconds = annotation.ttlSeconds() >= 0 ? annotation.ttlSeconds() : properties.getTtlSeconds();

        String lockKey = "lock:" + key;
        String processedKey = "processed:" + key;

        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        if (!acquired) {
            log.warn("분산락 획득 실패 - key: {}", lockKey);
            return null;
        }

        try {
            RBucket<String> bucket = redissonClient.getBucket(processedKey);
            if (bucket.isExists()) {
                log.info("이미 처리된 메시지 skip - key: {}", processedKey);
                return null;
            }

            Object result = joinPoint.proceed();
            bucket.set("done", Duration.ofSeconds(ttlSeconds));
            return result;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String resolveKey(IdempotentConsumer annotation, Method method, Object[] args) {
        EvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = NAME_DISCOVERER.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        String keyValue = PARSER.parseExpression(annotation.keyExpression()).getValue(context, String.class);
        String prefix = annotation.prefix();
        return prefix.isEmpty() ? keyValue : prefix + ":" + keyValue;
    }
}
