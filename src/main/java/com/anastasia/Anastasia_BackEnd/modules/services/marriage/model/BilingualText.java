package com.anastasia.Anastasia_BackEnd.modules.services.marriage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class BilingualText {

    @Column(name = "english_value", length = 255)
    private String english;

    @Column(name = "local_value", length = 255)
    private String local;
}
