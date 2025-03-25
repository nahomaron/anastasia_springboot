package com.anastasia.Anastasia_BackEnd.model.entity.auth;

import com.anastasia.Anastasia_BackEnd.model.entity.base.BaseEntity;
import jakarta.persistence.*;
import jdk.jfr.Enabled;
import lombok.*;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Enabled
@Table(name = "permissions")
public class Permission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "permission_seq")
    @SequenceGenerator(name = "permission_seq", sequenceName = "permission_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    private String description;



}
