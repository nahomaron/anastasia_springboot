package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.BaseMember;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "children")
public class Child_MemberEntity extends BaseMember {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "child_seq")
    @SequenceGenerator(name = "child_seq", sequenceName = "child_id_seq", allocationSize = 1)
    private Long id;

    //todo - parent needs to be connected optional
}
