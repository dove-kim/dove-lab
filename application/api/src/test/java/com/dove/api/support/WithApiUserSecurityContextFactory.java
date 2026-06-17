package com.dove.api.support;

import com.dove.api.global.security.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;
import java.util.Set;

/**
 * {@link WithApiUser}로 선언한 사용자를 principal로 갖는 SecurityContext를 만든다.
 */
public class WithApiUserSecurityContextFactory implements WithSecurityContextFactory<WithApiUser> {

    @Override
    public SecurityContext createSecurityContext(WithApiUser annotation) {
        AuthenticatedUser principal = new AuthenticatedUser(
                annotation.memberId(), annotation.username(), annotation.role(),
                annotation.mustChangePassword(), Set.of(annotation.capabilities()));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(annotation.role())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}
