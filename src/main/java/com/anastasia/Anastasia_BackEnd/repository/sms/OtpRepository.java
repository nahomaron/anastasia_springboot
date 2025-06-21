package com.anastasia.Anastasia_BackEnd.repository.sms;

import com.anastasia.Anastasia_BackEnd.model.sms.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntity, Long> {

    @Query("select o from OtpEntity o where o.phone = :phone and o.expiresAt > :now")
    Optional<OtpEntity> findValid(@Param("phone") String phone,
                                  @Param("now") LocalDateTime now);
}
