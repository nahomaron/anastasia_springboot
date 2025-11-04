package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.BaseMember;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;


@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@AttributeOverride(name = "phone", column = @Column(nullable = false))
@Table(name = "members", indexes = {
        @Index(name = "idx_member_church", columnList = "church_id"),
        @Index(name = "idx_member_tenant", columnList = "tenant_id")
})
//@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
//@Filters(@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId"))
public class Adult_MemberEntity extends BaseMember {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq")
    @SequenceGenerator(name = "member_seq", sequenceName = "member_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private boolean approvedByChurch;

    @Column(nullable = false)
    private boolean approvedByPriest;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "avatar_id", referencedColumnName = "id")
    private AvatarEntity avatar;

    private String eritreaContact;

    @Column(nullable = false)
    private String maritalStatus;

    private int numberOfChildren;

    private String profession;

    private String spouseIdNumber;

}
