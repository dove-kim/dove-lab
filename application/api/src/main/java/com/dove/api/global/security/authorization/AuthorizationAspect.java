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
 * {@link RequireRole}, {@link RequireCapability} 어노테이션 기반 권한 검사 AOP advice.
 */
@Aspect
@Component
public class AuthorizationAspect {

    /**
     * 대상 메서드 실행 전 요구 권한 등급·capability 보유 여부를 검사한다.
     *
     * @throws org.springframework.security.access.AccessDeniedException 권한·capability가 부족할 때
     */
    @Around("@within(com.dove.api.global.security.authorization.RequireRole) || @annotation(com.dove.api.global.security.authorization.RequireRole) "
            + "|| @within(com.dove.api.global.security.authorization.RequireCapability) || @annotation(com.dove.api.global.security.authorization.RequireCapability)")
    public Object enforce(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = pjp.getTarget().getClass();

        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = targetClass.getAnnotation(RequireRole.class);
        }
        RequireCapability requireCapability = method.getAnnotation(RequireCapability.class);
        if (requireCapability == null) {
            requireCapability = targetClass.getAnnotation(RequireCapability.class);
        }

        if (requireRole != null || requireCapability != null) {
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
            if (requireCapability != null) {
                // ROOT 는 모든 capability 자동 통과 (시스템 최고 권한)
                if (!isRoot(user) && !user.capabilities().contains(requireCapability.value().name())) {
                    throw new AccessDeniedException("CAPABILITY_NOT_GRANTED");
                }
            }
        }

        return pjp.proceed();
    }

    private boolean isRoot(AuthenticatedUser user) {
        try {
            return Role.parse(user.role()) == Role.ROOT;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private AuthenticatedUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AuthenticationCredentialsNotFoundException("AUTH_REQUIRED");
        }
        return user;
    }
}
