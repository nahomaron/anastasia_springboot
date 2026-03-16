package com.anastasia.Anastasia_BackEnd.modules.staff.repository;

import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEmploymentStatus;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffRepository extends JpaRepository<StaffEntity, Long> {

    boolean existsByStaffNumber(String staffNumber);

    boolean existsByUser(UserEntity user);

    Optional<StaffEntity> findByUser_Uuid(UUID userId);

    @Query("""
        SELECT s
        FROM StaffEntity s
        JOIN s.user u
        WHERE s.tenant.id = :tenantId
          AND (:status IS NULL OR s.employmentStatus = :status)
          AND (
                :q IS NULL
                OR :qBlank = true
                OR LOWER(COALESCE(s.staffNumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(s.department, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        ORDER BY u.fullName
    """)
    Page<StaffEntity> searchTenantStaff(
            @Param("tenantId") UUID tenantId,
            @Param("q") String query,
            @Param("qBlank") boolean queryBlank,
            @Param("status") StaffEmploymentStatus status,
            Pageable pageable
    );
}
