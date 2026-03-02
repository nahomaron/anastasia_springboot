package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.token.Token;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MemberTransferRequestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MemberTransferStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MembershipStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantRole;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantUserEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberTransferRequestRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantUserRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberTransferServiceImpl implements MemberTransferService {

    private final MemberTransferRequestRepository memberTransferRequestRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantUserRepository tenantUserRepository;
    private final MemberRepository memberRepository;
    private final ChildRepository childRepository;
    private final TokenRepository tokenRepository;

    @Override
    @Transactional
    public MemberTransferRequestEntity createTransferRequest(UUID actorTenantId,
                                                             UUID userId,
                                                             UUID targetTenantId,
                                                             UUID actorUserId,
                                                             String reason) {
        if (targetTenantId == null) {
            throw new IllegalArgumentException("Target tenant is required");
        }

        if (actorTenantId.equals(targetTenantId)) {
            throw new IllegalArgumentException("Target tenant must be different from current tenant");
        }

        UserEntity user = requireUser(userId);
        if (!actorTenantId.equals(user.getTenantId())) {
            throw new EntityNotFoundException("User not found in current tenant");
        }

        if (memberTransferRequestRepository.existsByUserIdAndStatus(userId, MemberTransferStatus.PENDING)) {
            throw new IllegalStateException("User already has a pending transfer request");
        }

        TenantEntity fromTenant = requireTenant(actorTenantId);
        TenantEntity toTenant = requireTenant(targetTenantId);

        MemberTransferRequestEntity request = MemberTransferRequestEntity.builder()
                .userId(userId)
                .fromTenant(fromTenant)
                .toTenant(toTenant)
                .status(MemberTransferStatus.PENDING)
                .requestedByUserId(actorUserId)
                .reason(trimToNull(reason))
                .build();

        return memberTransferRequestRepository.save(request);
    }

    @Override
    @Transactional
    public MemberTransferRequestEntity approveTransferRequest(UUID actorTenantId,
                                                              UUID transferRequestId,
                                                              UUID actorUserId,
                                                              String decisionNote) {
        MemberTransferRequestEntity request = requireTransferRequest(actorTenantId, transferRequestId);
        if (request.getStatus() != MemberTransferStatus.PENDING) {
            throw new IllegalStateException("Only pending transfer requests can be approved");
        }

        UUID userId = request.getUserId();
        UUID fromTenantId = request.getFromTenant().getId();
        UUID toTenantId = request.getToTenant().getId();

        UserEntity user = requireUser(userId);
        if (!fromTenantId.equals(user.getTenantId())) {
            throw new IllegalStateException("User is no longer assigned to the source tenant");
        }

        TenantUserEntity fromMembership = tenantUserRepository.findByTenant_IdAndUserId(fromTenantId, userId)
                .orElseThrow(() -> new IllegalStateException("Source tenant membership not found"));
        fromMembership.setStatus(MembershipStatus.REMOVED);
        fromMembership.setUpdatedByUserId(actorUserId);
        tenantUserRepository.save(fromMembership);

        TenantUserEntity toMembership = tenantUserRepository.findByTenant_IdAndUserId(toTenantId, userId)
                .orElseGet(() -> TenantUserEntity.builder()
                        .tenant(request.getToTenant())
                        .userId(userId)
                        .createdByUserId(actorUserId)
                        .build());

        if (toMembership.getRole() == null) {
            toMembership.setRole(TenantRole.COMMITTEE);
        }
        toMembership.setStatus(MembershipStatus.ACTIVE);
        toMembership.setUpdatedByUserId(actorUserId);
        tenantUserRepository.save(toMembership);

        user.assignTenant(request.getToTenant());
        synchronizeMemberChurchAndTenant(user, request.getToTenant());
        userRepository.save(user);
        revokeValidTokens(userId);

        request.setStatus(MemberTransferStatus.APPROVED);
        request.setDecidedByUserId(actorUserId);
        request.setDecisionNote(trimToNull(decisionNote));
        request.setDecidedAt(LocalDateTime.now());
        request.setExecutedAt(request.getDecidedAt());
        return memberTransferRequestRepository.save(request);
    }

    @Override
    @Transactional
    public MemberTransferRequestEntity rejectTransferRequest(UUID actorTenantId,
                                                             UUID transferRequestId,
                                                             UUID actorUserId,
                                                             String decisionNote) {
        MemberTransferRequestEntity request = requireTransferRequest(actorTenantId, transferRequestId);
        if (request.getStatus() != MemberTransferStatus.PENDING) {
            throw new IllegalStateException("Only pending transfer requests can be rejected");
        }

        request.setStatus(MemberTransferStatus.REJECTED);
        request.setDecidedByUserId(actorUserId);
        request.setDecisionNote(trimToNull(decisionNote));
        request.setDecidedAt(LocalDateTime.now());
        return memberTransferRequestRepository.save(request);
    }

    private MemberTransferRequestEntity requireTransferRequest(UUID actorTenantId, UUID transferRequestId) {
        return memberTransferRequestRepository.findByIdAndFromTenant_Id(transferRequestId, actorTenantId)
                .orElseThrow(() -> new EntityNotFoundException("Transfer request not found for current tenant"));
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private TenantEntity requireTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
    }

    private void synchronizeMemberChurchAndTenant(UserEntity user, TenantEntity targetTenant) {
        Adult_MemberEntity membership = user.getMembership();
        if (membership == null) {
            return;
        }

        ChurchEntity targetChurch = targetTenant.getChurch();
        if (targetChurch == null) {
            throw new IllegalStateException("Target tenant does not have a church assigned");
        }

        membership.setTenantId(targetTenant.getId());
        membership.setChurch(targetChurch);
        membership.setChurchNumber(targetChurch.getChurchNumber());
        memberRepository.save(membership);

        List<Child_MemberEntity> children = childRepository.findByFatherIdOrMotherId(membership.getId(), membership.getId());
        for (Child_MemberEntity child : children) {
            child.setTenantId(targetTenant.getId());
            child.setChurch(targetChurch);
            child.setChurchNumber(targetChurch.getChurchNumber());
        }
        childRepository.saveAll(children);
    }

    private void revokeValidTokens(UUID userId) {
        List<Token> tokens = tokenRepository.findAllValidUserTokens(userId);
        if (tokens.isEmpty()) {
            return;
        }
        for (Token token : tokens) {
            token.setExpired(true);
            token.setRevoked(true);
        }
        tokenRepository.saveAll(tokens);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
