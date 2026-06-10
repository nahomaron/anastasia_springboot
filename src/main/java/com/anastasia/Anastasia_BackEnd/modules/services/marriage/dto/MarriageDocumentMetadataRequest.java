package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class MarriageDocumentMetadataRequest {

    private MarriagePartyRole partyRole;

    @NotBlank
    private String documentCategory;

    @NotBlank
    private String originalFileName;

    @NotBlank
    private String storageReference;

    private String contentType;

    private LocalDate expiryDate;

    private String notes;

    private final Map<String, Object> unsupportedMetadata = new LinkedHashMap<>();

    @JsonAnySetter
    void captureUnsupportedMetadata(String key, Object value) {
        unsupportedMetadata.put(key, value);
    }

    @AssertTrue(message = "Marriage uploads only accept the approved document fields.")
    public boolean isMetadataSupported() {
        return unsupportedMetadata.isEmpty();
    }
}
