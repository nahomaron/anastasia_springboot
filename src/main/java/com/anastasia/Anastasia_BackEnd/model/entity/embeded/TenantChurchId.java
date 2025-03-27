package com.anastasia.Anastasia_BackEnd.model.entity.embeded;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class TenantChurchId implements Serializable {

    private UUID userId;

    private Long churchId;
}
