package com.anastasia.Anastasia_BackEnd.modules.registration.model.church;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChurchDTO {

    private String prefix;

    @JsonProperty("prefixLocal")
    @JsonAlias("PrefixLocal")
    private String prefixLocal;

    @NotBlank(message = "{validation.church.name.required}")
    private String churchName;

    @NotBlank(message = "{validation.church.localName.required}")
    @JsonProperty("churchNameLocal")
    @JsonAlias("ChurchNameLocal")
    private String churchNameLocal;

    @NotBlank(message = "{validation.church.neighborhood.required}")
    private String neighborhood;

    @NotBlank(message = "{validation.church.localNeighborhood.required}")
    @JsonProperty("neighborhoodLocal")
    @JsonAlias("NeighborhoodLocal")
    private String neighborhoodLocal;

    @NotBlank(message = "{validation.church.diocese.required}")
    private String diocese;

    @NotBlank(message = "{validation.church.localDiocese.required}")
    @JsonProperty("dioceseLocal")
    @JsonAlias("DioceseLocal")
    private String dioceseLocal;

    @Valid
    private Address address;

    @NotBlank(message = "{validation.church.email.required}")
    @Email(message = "{validation.church.email.invalid}")
    private String email;

    private String phone;

    private String timezone;

    private String locale;

    private String denomination;

    private String description;

    @JsonProperty("descriptionLocal")
    @JsonAlias("DescriptionLocal")
    private String descriptionLocal;

    private boolean usesOurServices;

    @Builder.Default
    private boolean publicDirectoryEnabled = false;

    private String gpsLocation;

    private Double latitude;

    private Double longitude;

    private String instagram;

    private String website;
    private String youtube;
    private String facebook;
    private boolean churchProfileComplete;
    private ChurchStatus status;

    private ImageAssetDTO profilePicture;
    

}
