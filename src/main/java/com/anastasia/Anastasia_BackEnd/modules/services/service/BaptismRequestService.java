package com.anastasia.Anastasia_BackEnd.modules.services.service;

import com.anastasia.Anastasia_BackEnd.modules.services.dto.BaptismServiceRequestCreateRequest;
import com.anastasia.Anastasia_BackEnd.modules.services.dto.MemberServiceRequestListItemResponse;
import com.anastasia.Anastasia_BackEnd.modules.services.dto.BaptismServiceRequestResponse;

import java.util.List;

public interface BaptismRequestService {
    BaptismServiceRequestResponse create(BaptismServiceRequestCreateRequest request);
    List<MemberServiceRequestListItemResponse> listMine();
}
