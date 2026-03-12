package com.anastasia.Anastasia_BackEnd.modules.staff.service;

import com.anastasia.Anastasia_BackEnd.modules.staff.dto.CreateStaffRequest;
import com.anastasia.Anastasia_BackEnd.modules.staff.dto.StaffResponse;
import com.anastasia.Anastasia_BackEnd.modules.staff.dto.UpdateStaffRequest;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEmploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StaffService {

    StaffResponse create(CreateStaffRequest request);

    Page<StaffResponse> list(String query, StaffEmploymentStatus status, Pageable pageable);

    StaffResponse getById(Long staffId);

    StaffResponse update(Long staffId, UpdateStaffRequest request);

    StaffResponse deactivate(Long staffId);

    void resetCredentials(Long staffId);
}
