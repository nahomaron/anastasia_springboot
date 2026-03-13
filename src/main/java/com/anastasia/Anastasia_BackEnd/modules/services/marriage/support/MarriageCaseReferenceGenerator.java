package com.anastasia.Anastasia_BackEnd.modules.services.marriage.support;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository.MarriageCaseRepository;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
public class MarriageCaseReferenceGenerator {

    private final MarriageCaseRepository marriageCaseRepository;

    public MarriageCaseReferenceGenerator(MarriageCaseRepository marriageCaseRepository) {
        this.marriageCaseRepository = marriageCaseRepository;
    }

    public String nextReference() {
        String candidate;
        do {
            candidate = "MAR-" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 10)
                    .toUpperCase(Locale.ROOT);
        } while (marriageCaseRepository.existsByCaseReference(candidate));

        return candidate;
    }
}
