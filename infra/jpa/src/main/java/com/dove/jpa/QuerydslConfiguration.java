package com.dove.jpa;

import com.querydsl.jpa.JPQLTemplates;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Querydsl 설정. JPAQueryFactory 빈을 등록한다.
 */
@Configuration
public class QuerydslConfiguration {

    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(
                JPQLTemplates.DEFAULT,
                entityManager
        );
    }

}
