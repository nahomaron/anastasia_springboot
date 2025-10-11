package com.anastasia.Anastasia_BackEnd.model.avatar;

import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "avatars")
public class AvatarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Polymorphic design
    @Column(nullable = false)
    private UUID ownerId; // owner could be user, group, church, member ...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AvatarType avatarType;

//    @ManyToOne(optional = false) // user is mandatory
//    @JoinColumn(name = "user_id", nullable = false)
//    private UserEntity user;
//
//    @OneToOne
//    @JoinColumn(name = "member_id", unique = true)
//    private MemberEntity member;

    @Column(nullable = false)
    private String imageUrl;

    private String imageSize;

}
