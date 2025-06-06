package com.anastasia.Anastasia_BackEnd.service.registration;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.mappers.MemberMapper;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.member.MemberDTO;
import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.model.member.MemberResponse;
import com.anastasia.Anastasia_BackEnd.model.member.MemberStatus;
import com.anastasia.Anastasia_BackEnd.model.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.repository.registration.MemberRepository;
import com.anastasia.Anastasia_BackEnd.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class MemberServiceUnitTest {

    @Mock private MemberRepository memberRepository;
    @Mock private ChurchRepository churchRepository;
    @Mock private UserRepository userRepository;
    @Mock private MemberMapper memberMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private SecurityContext securityContext;


    @InjectMocks
    private MemberServiceImpl memberService;

//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
    private UserEntity user;
    private MemberEntity member;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.setContext(securityContext);
        user = TestDataUtil.createTestUserEntityA();
        member = TestDataUtil.createTestMember(TestDataUtil.createTestChurchEntity(TestDataUtil.createTestTenantEntity()));
    }

    @Test
    void registerMember_shouldRegisterSuccessfully() {
        // Arrange
        UserEntity user = TestDataUtil.createTestUserEntityA();
        user.setUuid(UUID.randomUUID());
        ChurchEntity church = TestDataUtil.createTestChurchEntity(TestDataUtil.createTestTenantEntity());
        MemberEntity member = TestDataUtil.createTestMember(church);

        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(auth.isAuthenticated()).thenReturn(true);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
        when(churchRepository.findByChurchNumber(member.getChurchNumber())).thenReturn(Optional.of(church));
        when(securityUtils.generateUniqueIDNumber(anyInt(), anyString())).thenReturn("M123456");
        when(memberRepository.existsByMembershipNumber(anyString())).thenReturn(false);
        when(memberRepository.save(any(MemberEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        MemberResponse response = memberService.registerMember(member);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMembershipNumber()).isEqualTo("M123456");
        assertThat(response.getName()).contains(member.getFirstName());
    }

    @Test
    void testConvertToEntity() {
        MemberDTO dto = new MemberDTO();
        when(memberMapper.memberDTOToEntity(dto)).thenReturn(member);
        MemberEntity result = memberService.convertToEntity(dto);
        assertThat(result).isEqualTo(member);
    }

    @Test
    void testConvertToDTO() {
        MemberDTO dto = new MemberDTO();
        when(memberMapper.memberEntityToDTO(member)).thenReturn(dto);
        MemberDTO result = memberService.convertToDTO(member);
        assertThat(result).isEqualTo(dto);
    }

    @Test
    void testFindAll() {
        Page<MemberEntity> page = new PageImpl<>(List.of(member));
        when(memberRepository.findAll(any(PageRequest.class))).thenReturn(page);
        Page<MemberEntity> result = memberService.findAll(PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void testFindMemberById() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        Optional<MemberEntity> result = memberService.findMemberById(1L);
        assertThat(result).isPresent().contains(member);
    }

    @Test
    void testUpdateMembershipDetails() {
        MemberDTO request = new MemberDTO();
        request.setFirstName("Updated");
        when(memberRepository.findById(anyLong())).thenReturn(Optional.of(member));

        memberService.updateMembershipDetails(1L, request);

        verify(memberRepository).save(argThat(updated -> "Updated".equals(updated.getFirstName())));
    }

    @Test
    void testDeleteMembership() {
        memberService.deleteMembership(5L);
        verify(memberRepository).deleteById(5L);
    }

    @Test
    void testApproveByChurch() {
        member.setApprovedByChurch(true);
        member.setApprovedByPriest(true);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        memberService.approveByChurch(1L);
        verify(memberRepository).save(member);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.APPROVED.name());
    }

    @Test
    void testApproveByPriest() {
        member.setApprovedByChurch(true);
        member.setApprovedByPriest(true);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        memberService.approveByPriest(1L);
        verify(memberRepository).save(member);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.APPROVED.name());
    }

    @Test
    void testFindAllBySpecification() {
        Page<MemberEntity> page = new PageImpl<>(List.of(member));
        when(memberRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        Page<MemberEntity> result = memberService.findAllBySpecification(mock(Specification.class), PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
    }
}

