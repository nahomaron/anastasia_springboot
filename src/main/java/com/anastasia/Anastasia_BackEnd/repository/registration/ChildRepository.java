package com.anastasia.Anastasia_BackEnd.repository.registration;

import com.anastasia.Anastasia_BackEnd.model.child.ChildEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ChildRepository extends JpaRepository<ChildEntity, Long>, JpaSpecificationExecutor<ChildEntity> {
    boolean existsByMembershipNumber(String membershipNumber);
}
