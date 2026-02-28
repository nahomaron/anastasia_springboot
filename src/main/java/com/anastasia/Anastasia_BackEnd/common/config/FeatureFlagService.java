package com.anastasia.Anastasia_BackEnd.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final AppFeatureFlagsProperties featureFlags;

    public boolean isMessagingEnabled() {
        return featureFlags.getMessaging().isEnabled();
    }
}
