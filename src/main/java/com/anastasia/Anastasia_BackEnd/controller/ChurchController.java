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

}
