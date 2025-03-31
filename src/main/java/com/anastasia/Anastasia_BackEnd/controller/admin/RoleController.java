package com.anastasia.Anastasia_BackEnd.controller.admin;

import com.anastasia.Anastasia_BackEnd.model.DTO.RoleRequest;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.Role;
import com.anastasia.Anastasia_BackEnd.service.auth.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('MANAGE_ROLES')")
public class RoleController {

    private final RoleService roleService;

    @PostMapping("/roles")
    public ResponseEntity<Role> createRole(@RequestBody RoleRequest request){

        Role role = Role.builder()
                .roleName(request.getRoleName())
                .description(request.getDescription())
                .build();

        return new ResponseEntity<>(roleService.createRole(role), HttpStatus.CREATED);
    }
}
