package com.dove.distributedlock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotentConsumerAspect 테스트")
class IdempotentConsumerAspectTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @Mock
    private RBucket<String> rBucket;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private IdempotentConsumerAspect aspect;
    private DistributedLockProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DistributedLockProperties();
        aspect = new IdempotentConsumerAspect(redissonClient, properties);
    }

    @SuppressWarnings("unchecked")
    private void setupMocks(String key, boolean lockAcquired, boolean alreadyProcessed) throws Exception {
        Method method = TestListener.class.getMethod("handle", String.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-value"});

        when(redissonClient.getLock(contains("lock:"))).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(lockAcquired);

        if (lockAcquired) {
            when(rLock.isHeldByCurrentThread()).thenReturn(true);
            when(redissonClient.<String>getBucket(contains("processed:"))).thenReturn(rBucket);
            when(rBucket.isExists()).thenReturn(alreadyProcessed);
        }
    }

    @Test
    @DisplayName("미처리 키일 때 비즈니스 로직을 실행하고 처리 완료를 마킹한다")
    void shouldExecuteWhenNotProcessedBefore() throws Throwable {
        // Given
        setupMocks("test", true, false);
        when(joinPoint.proceed()).thenReturn(null);

        // When
        aspect.around(joinPoint);

        // Then
        verify(joinPoint).proceed();
        verify(rBucket).set(eq("done"), any(Duration.class));
    }

    @Test
    @DisplayName("이미 처리된 키일 때 비즈니스 로직을 실행하지 않는다")
    void shouldSkipWhenAlreadyProcessed() throws Throwable {
        // Given
        setupMocks("test", true, true);

        // When
        aspect.around(joinPoint);

        // Then
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("분산락을 획득하고 실행 후 해제한다")
    void shouldAcquireAndReleaseLock() throws Throwable {
        // Given
        setupMocks("test", true, false);
        when(joinPoint.proceed()).thenReturn(null);

        // When
        aspect.around(joinPoint);

        // Then
        verify(rLock).tryLock(eq(properties.getWaitTime()), eq(properties.getLeaseTime()), eq(TimeUnit.SECONDS));
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("분산락 획득 실패 시 실행하지 않는다")
    void shouldSkipWhenLockNotAcquired() throws Throwable {
        // Given
        setupMocks("test", false, false);

        // When
        aspect.around(joinPoint);

        // Then
        verify(joinPoint, never()).proceed();
        verify(rLock, never()).unlock();
    }

    @Test
    @DisplayName("처리 완료 마킹에 TTL이 설정된다")
    void shouldMarkProcessedWithTtl() throws Throwable {
        // Given
        setupMocks("test", true, false);
        when(joinPoint.proceed()).thenReturn(null);

        // When
        aspect.around(joinPoint);

        // Then
        verify(rBucket).set("done", Duration.ofSeconds(properties.getTtlSeconds()));
    }

    @Test
    @DisplayName("예외 발생 시에도 분산락이 해제된다")
    void shouldReleaseLockEvenOnException() throws Throwable {
        // Given
        setupMocks("test", true, false);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("비즈니스 예외"));

        // When & Then
        assertThatThrownBy(() -> aspect.around(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("비즈니스 예외");
        verify(rLock).unlock();
    }

    /** 테스트용 리스너 */
    static class TestListener {
        @IdempotentConsumer(prefix = "test", keyExpression = "#key")
        public void handle(String key) {
        }
    }
}
