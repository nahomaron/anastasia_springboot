package com.anastasia.Anastasia_BackEnd.IntegrationTest.service.cache;


import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.Api.config.PostgresTestContainer;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.MemberService;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(MemberServiceCacheIT.TestCacheConfig.class)
class MemberServiceCacheIT extends PostgresTestContainer {

    @Autowired
    private MemberService memberService;

    @MockitoSpyBean
    private MemberRepository memberRepository;

    @Autowired
    private CacheManager cacheManager;

    private Adult_MemberEntity savedMember;

    @BeforeEach
    void setUp() {
        TenantEntity tenant = TestDataUtil.createTestTenantEntity();
        ChurchEntity church = TestDataUtil.createTestChurchEntity(tenant);
        Adult_MemberEntity entity = TestDataUtil.createTestMember(church);

        Adult_MemberEntity saved = memberRepository.save(entity);
        savedMember = saved;

        Cache membersCache = cacheManager.getCache("members");
        if (membersCache != null) {
            membersCache.clear(); // Ensure clean cache
        }
    }

    @Test
    void whenCalledTwice_shouldUseCacheOnSecondCall() {
        Long id = savedMember.getId();

        // 1️⃣ First call — should hit DB
        Optional<Adult_MemberEntity> firstCall = memberService.findMemberById(id);
        verify(memberRepository, times(1)).findById(id);

        // 2️⃣ Second call — should use cache (no new DB hit)
        Optional<Adult_MemberEntity> secondCall = memberService.findMemberById(id);
        verify(memberRepository, times(1)).findById(id); // still 1 call total

        assertThat(firstCall).isEqualTo(secondCall);
    }

    @Test
    void whenCacheEvicted_shouldCallRepositoryAgain() {
        Long id = savedMember.getId();

        // Cache the value
        memberService.findMemberById(id);
        verify(memberRepository, times(1)).findById(id);

        // Evict cache
        memberService.deleteMembership(id);
        Cache membersCache = cacheManager.getCache("members");
        if (membersCache != null) {
            membersCache.evict(id);
        }

        // Next call should hit DB again (since cache was evicted)
        memberService.findMemberById(id);
        verify(memberRepository, times(2)).findById(id);
    }


    @TestConfiguration
    @EnableCaching
    static class TestCacheConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("members", "members_all");
        }

        @Bean
        S3Client s3Client() {
            return Mockito.mock(S3Client.class);
        }
    }
}
