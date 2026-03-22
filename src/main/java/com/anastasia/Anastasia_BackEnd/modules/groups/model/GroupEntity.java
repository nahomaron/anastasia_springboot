package com.anastasia.Anastasia_BackEnd.modules.groups.model;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "groups")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filters(@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId"))
@EntityListeners(AuditingEntityListener.class)
public class GroupEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;

    @Column(nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "church_id", nullable = false)
    private ChurchEntity church;


    @Column(nullable = false)
    private String groupName;

    private String description;

    private String avatar;

    @Column(nullable = false)
    private String visibility;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "group_managers",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "manager_id")
    )
    @Builder.Default
    private Set<UserEntity> managers = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_users",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<UserEntity> users = new HashSet<>();

    @Version
    @Column(nullable = false)
    private long version;


    public void addUser(UserEntity user) {
        if (user != null && !this.users.contains(user)) {
            this.users.add(user);
            user.addGroup(this); // avoid recursion
        }
    }


}
