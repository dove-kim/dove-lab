package com.dove.commonjpa;

import com.querydsl.jpa.JPQLTemplates;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Querydsl 설정
 *
 * @see JpaConfig
 */
@Configuration
public class QuerydslConfiguration {

    /**
     * for Querydsl
     *
     * @param entityManager EntityManager
     * @return JPAQueryFactory
     * @see EntityManager
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(
                JPQLTemplates.DEFAULT,
                entityManager
        );
    }

}
