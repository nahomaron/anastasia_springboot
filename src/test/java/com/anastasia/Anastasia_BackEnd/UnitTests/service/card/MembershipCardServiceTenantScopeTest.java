package com.anastasia.Anastasia_BackEnd.UnitTests.service.card;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.card.MembershipCardSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MembershipCardAuditRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MembershipCardRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MembershipCardTemplateRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.card.MembershipCardRenderService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.card.MembershipCardService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.card.MembershipCardStorageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.card.MembershipCardTokenService;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipCardServiceTenantScopeTest {

    @Mock private MembershipCardRepository membershipCardRepository;
    @Mock private MembershipCardAuditRepository membershipCardAuditRepository;
    @Mock private MembershipCardTemplateRepository membershipCardTemplateRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipCardTokenService tokenService;
    @Mock private MembershipCardStorageService storageService;
    @Mock private MembershipCardRenderService renderService;
    @Mock private LocalizedMessageService messageService;

    private MembershipCardService service;

    @BeforeEach
    void setUp() {
        service = new MembershipCardService(
                membershipCardRepository,
                membershipCardAuditRepository,
                membershipCardTemplateRepository,
                memberRepository,
                userRepository,
                tokenService,
                storageService,
                renderService,
                messageService
        );
        lenient().when(messageService.get(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserCardSummary_rejectsMembershipOutsideActiveTenant() {
        UUID activeTenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContext.setTenantId(activeTenantId);

        authenticate(userId);
        UserEntity user = userWithMembership(userId, 42L, otherTenantId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> service.getCurrentUserCardSummary()
        );

        assertEquals("Current membership is not in the active tenant", ex.getMessage());
        verify(membershipCardRepository, never()).findByTenantIdAndMemberId(activeTenantId, 42L);
    }

    @Test
    void getCurrentUserCardSummary_usesActiveTenantForCurrentMembershipCard() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        authenticate(userId);
        UserEntity user = userWithMembership(userId, 42L, tenantId);
        MembershipCardEntity card = MembershipCardEntity.builder()
                .id(7L)
                .tenantId(tenantId)
                .memberId(42L)
                .membershipNumber("MEM-42")
                .memberFullName("Test Member")
                .churchName("Test Church")
                .issueDate(LocalDate.now())
                .expirationDate(LocalDate.now().plusYears(1))
                .cardSerialNumber("CARD-42")
                .status(MembershipCardStatus.ACTIVE)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(membershipCardRepository.findByTenantIdAndMemberId(tenantId, 42L)).thenReturn(Optional.of(card));

        MembershipCardSummaryResponse response = service.getCurrentUserCardSummary();

        assertEquals(7L, response.cardId());
        assertEquals("MEM-42", response.membershipNumber());
        verify(membershipCardRepository).findByTenantIdAndMemberId(tenantId, 42L);
    }

    private void authenticate(UUID userId) {
        UserEntity principalUser = UserEntity.builder()
                .uuid(userId)
                .email("member@example.com")
                .fullName("Member User")
                .build();
        UserPrincipal principal = new UserPrincipal(principalUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities())
        );
    }

    private UserEntity userWithMembership(UUID userId, Long memberId, UUID tenantId) {
        Adult_MemberEntity membership = new Adult_MemberEntity();
        membership.setId(memberId);
        membership.setTenantId(tenantId);
        UserEntity user = UserEntity.builder()
                .uuid(userId)
                .email("member@example.com")
                .fullName("Member User")
                .build();
        user.assignMembership(membership);
        return user;
    }
}
