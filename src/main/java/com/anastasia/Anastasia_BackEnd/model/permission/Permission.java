package com.anastasia.Anastasia_BackEnd.model.permission;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
//@Enabled
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private PermissionType name;  // Enum instead of String

    private String description;

    public Permission(PermissionType name) {
        this.name = name;
        this.description = name.getDescription(); // Automatically set description
    }
}
