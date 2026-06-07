package com.dove.api.global.security.authorization;

import com.dove.api.global.security.AuthenticatedUser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * {@link RequireRole}, {@link RequireFeature} 어노테이션 기반 권한 검사 AOP advice.
 */
@Aspect
@Component
public class AuthorizationAspect {

    /**
     * 대상 메서드 실행 전 요구 권한 등급·기능 보유 여부를 검사한다.
     *
     * @throws org.springframework.security.access.AccessDeniedException 권한 또는 기능이 부족할 때
     */
    @Around("@within(com.dove.api.global.security.authorization.RequireRole) || @annotation(com.dove.api.global.security.authorization.RequireRole) "
            + "|| @within(com.dove.api.global.security.authorization.RequireFeature) || @annotation(com.dove.api.global.security.authorization.RequireFeature)")
    public Object enforce(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = pjp.getTarget().getClass();

        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = targetClass.getAnnotation(RequireRole.class);
        }
        RequireFeature requireFeature = method.getAnnotation(RequireFeature.class);
        if (requireFeature == null) {
            requireFeature = targetClass.getAnnotation(RequireFeature.class);
        }

        if (requireRole != null || requireFeature != null) {
            AuthenticatedUser user = currentUser();
            if (requireRole != null) {
                Role current;
                try {
                    current = Role.parse(user.role());
                } catch (IllegalArgumentException e) {
                    throw new AccessDeniedException("ROLE_INVALID");
                }
                if (!current.isAtLeast(requireRole.value())) {
                    throw new AccessDeniedException("ROLE_INSUFFICIENT");
                }
            }
            if (requireFeature != null) {
                // ROOT 는 모든 feature 자동 통과 (시스템 최고 권한)
                Role current;
                try {
                    current = Role.parse(user.role());
                } catch (IllegalArgumentException e) {
                    current = null;
                }
                if (current != Role.ROOT && !user.grantedFeatures().contains(requireFeature.value())) {
                    throw new AccessDeniedException("FEATURE_NOT_GRANTED");
                }
            }
        }

        return pjp.proceed();
    }

    private AuthenticatedUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AuthenticationCredentialsNotFoundException("AUTH_REQUIRED");
        }
        return user;
    }
}
