package com.anastasia.Anastasia_BackEnd.modules.registration.model.church;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicChurchResponse {

    private String churchNumber;
    private String prefix;
    private String prefixLocal;
    private ImageAssetDTO profilePicture;
    private String churchName;
    private String churchNameLocal;
    private String neighborhood;
    private String neighborhoodLocal;
    private String diocese;
    private String dioceseLocal;
    private String denomination;
    private boolean usesOurServices;
    private String city;
    private String stateProvince;
    private String country;
}
