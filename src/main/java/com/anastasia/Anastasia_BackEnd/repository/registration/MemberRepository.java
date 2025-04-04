package com.anastasia.Anastasia_BackEnd.repository.registration;

import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long>, JpaSpecificationExecutor<MemberEntity> {

    boolean existsByMembershipNumber(String membershipNumber);
}
