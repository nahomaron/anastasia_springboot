package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.model.sms.PhoneVerificationRequest;
import com.anastasia.Anastasia_BackEnd.model.sms.ResendOtpRequest;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.service.registration.TenantService;
import com.anastasia.Anastasia_BackEnd.service.sms.PhoneVerificationService;
import com.anastasia.Anastasia_BackEnd.util.RateLimiterService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenant")
public class TenantController {

    private final TenantService tenantService;
    private final RateLimiterService rateLimiterService;
    private final PhoneVerificationService phoneVerificationService;

    /**
     * Subscribes a new tenant to the system.
     * This endpoint is used for tenant registration.
     *
     * @param tenantDTO The data transfer object containing tenant details.
     * @return ResponseEntity indicating success or failure of the subscription.
     * @throws MessagingException If there's an issue sending the activation email.
     */
    @PostMapping("/subscription")
    public ResponseEntity<?> subscribeTenant(@Valid @RequestBody TenantDTO tenantDTO) throws MessagingException {
        if(tenantDTO.getPassword() != null && !tenantDTO.isPasswordMatch()){
            return ResponseEntity.badRequest().body("Password do not match");
        }
        tenantService.subscribeTenant(tenantDTO);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Verifies the phone number of a tenant using an OTP (One Time Password).
     * This endpoint is used to confirm the phone number during registration.
     *
     * @param request The request containing the phone number and OTP.
     * @return ResponseEntity indicating success or failure of the verification.
     */
    @PostMapping("/verify-phone")
    public ResponseEntity<?> verifyPhone(@RequestBody PhoneVerificationRequest request) {
        if (!rateLimiterService.isAllowed(request.getPhone())) {
            return ResponseEntity.status(429).body("Too many attempts. Try again later.");
        }
        boolean verified = tenantService.verifyTenantPhone(request.getPhone(), request.getOtp());

        return verified
                ? ResponseEntity.ok("Phone verified successfully.")
                : ResponseEntity.badRequest().body("Invalid or expired OTP or wrong phone number.");
    }

    /**
     * Resends the OTP to the tenant's phone number for verification.
     * This endpoint is used when the user requests a new OTP.
     *
     * @param request The request containing the phone number.
     * @return ResponseEntity indicating success or failure of the resend operation.
     */
    @PostMapping("/resend-phone-otp")
    public ResponseEntity<?> resendPhoneOtp(@RequestBody ResendOtpRequest request) {
        if (!rateLimiterService.isAllowed(request.getPhone())) {
            return ResponseEntity.status(429).body("Too many attempts. Try again later.");
        }
        if (request.getPhone() == null || request.getPhone().isEmpty()) {
            return ResponseEntity.badRequest().body("Phone number is required.");
        }
        phoneVerificationService.resendOtp(request.getPhone());
        return ResponseEntity.ok("OTP has been resent successfully.");
    }


    /**
     * Retrieves a paginated list of all tenants.
     * This endpoint is accessible only to users with the 'PLATFORM_ADMIN' role.
     *
     * @param pageable Pagination information.
     * @return ResponseEntity containing a page of TenantDTO objects.
     */
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<TenantDTO>> listOfTenants(Pageable pageable){
        Page<TenantEntity> tenants = tenantService.findAll(pageable);
        return new ResponseEntity<>(tenants.map(tenantService::convertTenantToDTO), HttpStatus.OK);
    }


    /**
     * Retrieves a specific tenant by its ID.
     * @param tenantId  The ID of the tenant to retrieve.
     * @return  tenantDTO that is converted from entity
     */
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
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


    /**
     * Unsubscribes a tenant from the system.
     * This endpoint is accessible only to users with the 'OWNER' or 'PLATFORM_ADMIN' role.
     *
     * @param tenantId The ID of the tenant to unsubscribe.
     * @return ResponseEntity indicating success or failure of the un-subscription.
     */
    @PreAuthorize("hasAnyRole('OWNER', 'PLATFORM_ADMIN')")
    @PostMapping("/unsubscribe/{tenantId}")
    public ResponseEntity<?> unsubscribeTenant(@PathVariable UUID tenantId){
        tenantService.unsubscribeTenant(tenantId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Updates the details of an existing tenant.
     * This endpoint is accessible only to users with the 'OWNER' or 'PLATFORM_ADMIN' role.
     *
     * @param tenantId  The ID of the tenant to update.
     * @param tenantDTO The data transfer object containing updated tenant details.
     * @return ResponseEntity indicating success or failure of the update operation.
     */
    @PreAuthorize("hasAnyRole('OWNER', 'PLATFORM_ADMIN')")
    @PatchMapping("/update/{tenantId}")
    public ResponseEntity<?> updateTenant(@PathVariable UUID tenantId, @Valid @RequestBody TenantDTO tenantDTO) {
        if(tenantDTO.getPassword() != null && !tenantDTO.isPasswordMatch()){
            return ResponseEntity.badRequest().body("Password do not match");
        }
        tenantService.updateTenant(tenantId, tenantDTO);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
