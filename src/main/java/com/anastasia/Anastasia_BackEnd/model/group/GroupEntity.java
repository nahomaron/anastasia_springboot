package com.anastasia.Anastasia_BackEnd.model.group;

import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;

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
public class GroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
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
    private Set<UserEntity> managers;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_users",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<UserEntity> users = new HashSet<>();


    public void addUser(UserEntity user) {
        if (user != null && !this.users.contains(user)) {
            this.users.add(user);
            user.addGroup(this); // avoid recursion
        }
    }


}
