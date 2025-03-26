package com.anastasia.Anastasia_BackEnd.repository.registration;

import com.anastasia.Anastasia_BackEnd.model.entity.membership.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

}
