package com.anastasia.Anastasia_BackEnd.controller.admin;

import com.anastasia.Anastasia_BackEnd.model.DTO.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.TenantEntity;
import com.anastasia.Anastasia_BackEnd.service.tenant.TenantService;
import com.anastasia.Anastasia_BackEnd.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final AuthService authService;

//    @PostMapping("/users/{userId}/subscribe-as-tenant")
//    public ResponseEntity<TenantDTO> subscribeAsTenant(@PathVariable UUID userId,
//                                                     @RequestBody TenantDTO tenantDTO) {
//
//        TenantEntity tenantEntity = tenantService.convertTenantToEntity(tenantDTO);
//
//        if(authService.exists(userId)){
//            TenantEntity createdTenant = tenantService.subscribeUserAsTenant(userId, tenantEntity);
//
//            return new ResponseEntity<>(tenantService.convertTenantToDTO(createdTenant), HttpStatus.OK);
//        }else{
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
//
//    }



}
