package com.dove.api.global.security.authorization;

import com.dove.userfeature.domain.capability.Capability;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * capability 권한을 요구하는 어노테이션.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireCapability {
    Capability value();
}
