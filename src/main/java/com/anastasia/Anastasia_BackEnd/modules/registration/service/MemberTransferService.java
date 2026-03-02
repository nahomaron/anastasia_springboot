package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MemberTransferRequestEntity;

import java.util.UUID;

public interface MemberTransferService {

    MemberTransferRequestEntity createTransferRequest(UUID actorTenantId,
                                                      UUID userId,
                                                      UUID targetTenantId,
                                                      UUID actorUserId,
                                                      String reason);

    MemberTransferRequestEntity approveTransferRequest(UUID actorTenantId,
                                                       UUID transferRequestId,
                                                       UUID actorUserId,
                                                       String decisionNote);

    MemberTransferRequestEntity rejectTransferRequest(UUID actorTenantId,
                                                      UUID transferRequestId,
                                                      UUID actorUserId,
                                                      String decisionNote);
}
