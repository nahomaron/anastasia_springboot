package com.anastasia.Anastasia_BackEnd.controller.admin;

import com.anastasia.Anastasia_BackEnd.model.DTO.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.TenantEntity;
import com.anastasia.Anastasia_BackEnd.service.tenant.TenantService;
import com.anastasia.Anastasia_BackEnd.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenant")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping("/subscription")
    public ResponseEntity<TenantDTO> subscribeTenant(@Valid @RequestBody TenantDTO tenantDTO){
        TenantEntity tenantEntity = tenantService.convertTenantToEntity(tenantDTO);
        TenantEntity savedTenant = tenantService.subscribeTenant(tenantEntity);
        return new ResponseEntity<>(tenantService.convertTenantToDTO(savedTenant), HttpStatus.ACCEPTED);
    }

    @GetMapping
    public ResponseEntity<Page<TenantDTO>> listOfTenants(Pageable pageable){
        Page<TenantEntity> tenants = tenantService.findAll(pageable);
        return new ResponseEntity<>(tenants.map(tenantService::convertTenantToDTO), HttpStatus.OK);
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantDTO> getTenant(@PathVariable UUID tenantId){
        Optional<TenantEntity> foundTenant = tenantService.findTenantById(tenantId);
        return foundTenant.map(tenantEntity -> {
            TenantDTO tenantDTO = tenantService.convertTenantToDTO(tenantEntity);
            return new ResponseEntity<>(tenantDTO, HttpStatus.FOUND);
        }).orElse(
                new ResponseEntity<>(HttpStatus.NOT_FOUND)
        );
    }

    // todo instead of getting tenant id from url get it from the tenant context
    @PostMapping("/unsubscribe/{tenantId}")
    public ResponseEntity<?> unsubscribeTenant(@PathVariable UUID tenantId){
        tenantService.unsubscribeTenant(tenantId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
