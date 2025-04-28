package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.service.registration.ChurchService;
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
@RequestMapping("/api/v1/churches")
public class ChurchController {

    private final ChurchService churchService;

    @PostMapping("/register")
    public ResponseEntity<String> createChurch(@Valid @RequestBody ChurchDTO churchDTO){
        ChurchEntity churchEntity = churchService.convertToEntity(churchDTO);
        String churchNumber =  churchService.createChurch(churchEntity);
        return new ResponseEntity<>(churchNumber, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<ChurchDTO>> getChurches(Pageable pageable){
        Page<ChurchEntity> churches = churchService.findAll(pageable);
        return new ResponseEntity<>(churches.map(churchService::convertToDTO), HttpStatus.OK);
    }

    @GetMapping("/{churchId}")
    public ResponseEntity<ChurchDTO> findChurch(@PathVariable Long churchId){
        Optional<ChurchEntity> foundChurch = churchService.findOne(churchId);

        return foundChurch.map(churchEntity -> {
            ChurchDTO churchDTO = churchService.convertToDTO(churchEntity);
            return new ResponseEntity<>(churchDTO, HttpStatus.OK);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{churchId}")
    public ResponseEntity<String> updateChurch(@PathVariable Long churchId, @Valid @RequestBody ChurchDTO churchDTO){
        ChurchEntity churchEntity = churchService.convertToEntity(churchDTO);

        boolean churchExits = churchService.exists(churchId);

        if(!churchExits){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        churchService.updateChurch(churchId, churchEntity);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/{churchId}")
    public ResponseEntity<?> deleteChurch(@PathVariable Long churchId){
        churchService.deleteChurch(churchId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
