package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.service.ChurchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/churches")
public class ChurchController {

    private final ChurchService churchService;

    @PostMapping("/register")
    public ResponseEntity<?> createChurch(@Valid @RequestBody ChurchDTO churchDTO){
        ChurchEntity churchEntity = churchService.convertToEntity(churchDTO);
        ChurchEntity savedChurch = churchService.createChurch(churchEntity);

        return new ResponseEntity<>(churchService.convertToDTO(savedChurch), HttpStatus.CREATED);
    }

}
