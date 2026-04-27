package com.dove.distributedlock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Kafka 컨슈머 메서드에 멱등성을 보장하는 어노테이션.
 * AOP가 메서드 실행 전 분산락을 획득하고, 이미 처리된 키이면 실행을 건너뛴다.
 *
 * <p>분산락 기본 설정은 환경변수로 주입되며, 어노테이션 속성으로 개별 오버라이드 가능하다.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotentConsumer {

    /** Redis 키 생성에 사용할 SpEL 표현식. ConsumerRecord의 key(), value() 등을 참조 가능. */
    String keyExpression();

    /** Redis 키 prefix. 리스너별로 네임스페이스를 구분한다. */
    String prefix() default "";

    /** 처리 완료 마킹의 TTL (초). -1이면 환경변수 기본값 사용. */
    long ttlSeconds() default -1;

    /** 분산락 대기 시간 (초). -1이면 환경변수 기본값 사용. */
    long waitTime() default -1;

    /** 분산락 임대 시간 (초). -1이면 환경변수 기본값 사용. */
    long leaseTime() default -1;
}
