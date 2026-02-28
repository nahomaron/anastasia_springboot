package com.anastasia.Anastasia_BackEnd.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.features")
public class AppFeatureFlagsProperties {

    private Messaging messaging = new Messaging();

    @Getter
    @Setter
    public static class Messaging {
        /**
         * Feature flag for internal app messaging module.
         */
        private boolean enabled = false;
    }
}
