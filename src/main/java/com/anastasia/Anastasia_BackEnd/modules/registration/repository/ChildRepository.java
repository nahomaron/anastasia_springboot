package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChildRepository extends JpaRepository<Child_MemberEntity, Long>, JpaSpecificationExecutor<Child_MemberEntity> {
    boolean existsByMembershipNumber(String membershipNumber);

    List<Child_MemberEntity> findByFatherIdOrMotherId(Long fatherId, Long motherId);
}
