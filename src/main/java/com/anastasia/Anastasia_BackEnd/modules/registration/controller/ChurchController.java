package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
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

    @PostMapping("/register")
    public ResponseEntity<String> createChurch(@Valid @RequestBody ChurchDTO churchDTO){
        ChurchEntity churchEntity = churchService.convertToEntity(churchDTO);
        String churchNumber =  churchService.createChurch(churchEntity);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("\"" + churchNumber + "\"");
    }

    @GetMapping
    public ResponseEntity<Page<ChurchResponse>> getChurches(
            Pageable pageable,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "usesOurServices", required = false) Boolean usesOurServices
    ){
        Page<ChurchResponse> churches = churchService.findAll(pageable, query, usesOurServices);
        return new ResponseEntity<>(churches, HttpStatus.OK);
    }

    @GetMapping("/by-number/{churchNumber}")
    public ResponseEntity<ChurchResponse> findByChurchNumber(
            @PathVariable String churchNumber,
            @RequestParam(value = "usesOurServicesOnly", required = false, defaultValue = "false") boolean usesOurServicesOnly
    ) {
        Optional<ChurchEntity> foundChurch = usesOurServicesOnly
                ? churchService.findOneByChurchNumberUsingOurServices(churchNumber)
                : churchService.findOneByChurchNumber(churchNumber);

        return foundChurch.map(church -> ResponseEntity.ok(churchService.convertToResponse(church)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{churchId}/profile")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OWNER', 'PRIMARY_ADMIN', 'ADMIN')")
    public ResponseEntity<ChurchResponse> findChurch(@PathVariable Long churchId){
        Optional<ChurchEntity> foundChurch = churchService.findOne(churchId);

        return foundChurch.map(churchEntity -> {
            ChurchResponse churchResponse = churchService.convertToResponse(churchEntity);
            return new ResponseEntity<>(churchResponse, HttpStatus.OK);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{churchId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OWNER', 'PRIMARY_ADMIN')")
    public ResponseEntity<String> updateChurch(@PathVariable Long churchId, @Valid @RequestBody ChurchDTO churchDTO){
        ChurchEntity churchEntity = churchService.convertToEntity(churchDTO);

        boolean churchExits = churchService.exists(churchId);

        if(!churchExits){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        churchService.updateChurch(churchId, churchEntity);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @DeleteMapping("/{churchId}")
    public ResponseEntity<?> deleteChurch(@PathVariable Long churchId){
        churchService.deleteChurch(churchId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
