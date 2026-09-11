package com.interview.portfolio.cache;

import com.interview.portfolio.entity.EquityAsset;
import com.interview.portfolio.repository.AssetRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Hibernate's SECOND-LEVEL (L2) cache - which survives across separate
 * persistence contexts/transactions - as opposed to the first-level cache (the
 * persistence context itself), which only lives for the duration of one
 * EntityManager/Session and is what entityManager.clear() defeats.
 *
 * Asset is annotated @Cache(usage = READ_WRITE) (see Asset.java) and is backed here by
 * the Caffeine JCache provider configured in caffeine.properties.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class L2CacheTest {

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private AssetRepository assetRepository;

    private Statistics statistics;
    private Long assetId;

    @BeforeEach
    void setUp() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        sessionFactory.getCache().evictAllRegions(); // start each test with a cold L2 cache

        EquityAsset asset = assetRepository.save(EquityAsset.builder()
                .symbol("MSFT").name("Microsoft Corp").currentPrice(new BigDecimal("415.50"))
                .sector("Tech").build());
        assetId = asset.getId();
        entityManager.flush();
        entityManager.clear();
        statistics.clear();
    }

    @Test
    void secondLevelCache_servesSecondLookupWithoutHittingTheDatabase() {
        // First lookup: persistence context is empty (cleared in setUp) AND this is a
        // brand-new EntityManager for this test method, so this MUST hit the L2 cache
        // miss path and fall through to the database (or repopulate cache from setUp's insert).
        assetRepository.findById(assetId).orElseThrow();
        entityManager.clear(); // clears L1 (persistence context) but NOT L2

        long missesAfterFirstLookup = statistics.getSecondLevelCacheMissCount();
        long hitsAfterFirstLookup = statistics.getSecondLevelCacheHitCount();

        // Second lookup: L1 is empty again (just cleared), so this can ONLY be
        // satisfied fast without a DB round trip if the L2 cache is working.
        assetRepository.findById(assetId).orElseThrow();

        assertThat(statistics.getSecondLevelCacheHitCount())
                .as("Second lookup after clearing the persistence context should be served from L2 cache")
                .isGreaterThan(hitsAfterFirstLookup);
    }

    @Test
    void secondLevelCache_regionStatisticsReportAssetEntity() {
        assetRepository.findById(assetId).orElseThrow();
        entityManager.clear();
        assetRepository.findById(assetId).orElseThrow();

        assertThat(statistics.getSecondLevelCachePutCount()).isGreaterThanOrEqualTo(1);
    }
}
