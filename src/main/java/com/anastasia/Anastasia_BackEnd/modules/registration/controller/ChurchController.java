package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.PublicChurchResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChurchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/churches")
public class ChurchController {

    private final ChurchService churchService;

    @PostMapping({"", "/register"})
    @PreAuthorize("hasAnyAuthority('MANAGE_TENANTS', 'MANAGE_TENANT_BILLING', 'OWN_SUBSCRIPTION')")
    public ResponseEntity<ChurchResponse> createChurch(@Valid @RequestBody ChurchDTO churchDTO){
        ChurchEntity churchEntity = churchService.convertToEntity(churchDTO);
        ChurchResponse churchResponse = churchService.createChurch(churchEntity);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(churchResponse);
    }

    @GetMapping
    public ResponseEntity<Page<PublicChurchResponse>> getChurches(
            Pageable pageable,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "usesOurServices", required = false) Boolean usesOurServices
    ){
        Page<PublicChurchResponse> churches = churchService.findAllPublic(pageable, query, usesOurServices);
        return new ResponseEntity<>(churches, HttpStatus.OK);
    }

    @GetMapping("/by-number/{churchNumber}")
    public ResponseEntity<PublicChurchResponse> findByChurchNumber(
            @PathVariable String churchNumber,
            @RequestParam(value = "usesOurServicesOnly", required = false, defaultValue = "false") boolean usesOurServicesOnly
    ) {
        Optional<ChurchEntity> foundChurch = usesOurServicesOnly
                ? churchService.findOnePublicByChurchNumberUsingOurServices(churchNumber)
                : churchService.findOnePublicByChurchNumber(churchNumber);

        return foundChurch.map(church -> ResponseEntity.ok(churchService.convertToPublicResponse(church)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping({"/{churchId}", "/{churchId}/profile"})
    @PreAuthorize("hasAnyAuthority('MANAGE_TENANTS', 'VIEW_ALL_DATA', 'MANAGE_TENANT_BILLING', 'OWN_SUBSCRIPTION')")
    public ResponseEntity<ChurchResponse> findChurch(@PathVariable Long churchId){
        Optional<ChurchEntity> foundChurch = churchService.findOne(churchId);

        return foundChurch.map(churchEntity -> {
            ChurchResponse churchResponse = churchService.convertToResponse(churchEntity);
            return new ResponseEntity<>(churchResponse, HttpStatus.OK);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{churchId}")
    @PreAuthorize("hasAnyAuthority('MANAGE_TENANTS', 'MANAGE_TENANT_BILLING', 'OWN_SUBSCRIPTION')")
    public ResponseEntity<ChurchResponse> updateChurch(@PathVariable Long churchId, @Valid @RequestBody ChurchDTO churchDTO){
        ChurchEntity churchEntity = churchService.convertToEntity(churchDTO);

        boolean churchExits = churchService.exists(churchId);

        if(!churchExits){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        ChurchResponse churchResponse = churchService.updateChurch(churchId, churchEntity);

        return ResponseEntity.ok(churchResponse);
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @DeleteMapping("/{churchId}")
    public ResponseEntity<?> deleteChurch(@PathVariable Long churchId){
        churchService.deleteChurch(churchId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
