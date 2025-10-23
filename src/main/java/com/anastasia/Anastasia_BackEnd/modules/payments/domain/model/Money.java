package com.anastasia.Anastasia_BackEnd.modules.payments.domain.model;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Money {
    private long amount;    // minor units (e.g., cents)
    private String currency; // e.g., "USD"
}
