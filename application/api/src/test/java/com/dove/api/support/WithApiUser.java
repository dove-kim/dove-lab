package com.dove.api.support;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 테스트에서 인증된 사용자를 SecurityContext에 주입한다. DB 계정·토큰 없이 인증/인가를 충족한다.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@WithSecurityContext(factory = WithApiUserSecurityContextFactory.class)
public @interface WithApiUser {

    /**
     * 회원 식별자.
     */
    long memberId() default 1L;

    /**
     * 로그인 아이디.
     */
    String username() default "tester";

    /**
     * 권한 등급 (USER/ADMIN/ROOT).
     */
    String role() default "USER";

    /**
     * 부여된 기능 코드 집합 (FeatureCode.name()).
     */
    String[] features() default {};

    /**
     * 비밀번호 변경 강제 여부.
     */
    boolean mustChangePassword() default false;
}
