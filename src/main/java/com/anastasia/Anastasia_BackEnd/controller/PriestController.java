package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.service.registration.PriestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/priests")
public class PriestController {

    private final PriestService priestService;

    @PostMapping("/register")
    public ResponseEntity<?> registerPriest(@Valid @RequestBody PriestDTO priestDTO){

        if(!priestDTO.isPasswordMatch()){
            return ResponseEntity.badRequest().body("Password do not match");
        }
        priestService.registerPriest(priestDTO);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<PriestDTO>> listOfPriests(Pageable pageable){
        Page<PriestEntity> priests = priestService.findAllPriests(pageable);
        return new ResponseEntity<>(priests.map(priestService::convertToDTO), HttpStatus.OK);
    }

    @GetMapping("/{priestId}")
    public ResponseEntity<PriestDTO> getPriest(@PathVariable Long priestId){
        Optional<PriestEntity> foundPriest = priestService.findPriestById(priestId);

        return foundPriest.map(priestEntity -> {
            PriestDTO priestDTO = priestService.convertToDTO(priestEntity);
            return new ResponseEntity<>(priestDTO, HttpStatus.FOUND);
        }).orElse(
                new ResponseEntity<>(HttpStatus.NOT_FOUND)
        );
    }

    @PatchMapping("/{priestId}")
    public ResponseEntity<PriestDTO> updatePriestDetails(@PathVariable Long priestId,
                                                         @RequestBody PriestDTO priestDTO){
        PriestEntity priestEntity = priestService.convertToEntity(priestDTO);
        PriestEntity updatedPriest = priestService.updatePriestDetails(priestId, priestEntity);
        return new ResponseEntity<>(priestService.convertToDTO(updatedPriest), HttpStatus.ACCEPTED);
    }

    @PostMapping("/delete/{priestId}")
    public ResponseEntity<?> deletePriest(@PathVariable Long priestId){
        priestService.deletePriest(priestId);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
