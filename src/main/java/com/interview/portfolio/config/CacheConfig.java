package com.interview.portfolio.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's own @Cacheable/@CacheEvict abstraction for SERVICE-layer method
 * caching, which is a SEPARATE cache from Hibernate's L2 entity/query cache configured
 * in application.yml + caffeine.properties. Both happen to use Caffeine as the backing
 * cache implementation here, but Hibernate L2 cache operates at the persistence-context
 * level (keyed by entity id) while Spring's @Cacheable operates at the method-invocation
 * level (keyed by method args) - a common point of confusion worth clarifying in interviews.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
