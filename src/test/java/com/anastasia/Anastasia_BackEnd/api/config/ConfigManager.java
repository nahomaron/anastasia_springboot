package com.anastasia.Anastasia_BackEnd.api.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for managing application configuration properties.
 * It loads settings from a 'config.properties' file located in the classpath
 * and provides a static method to retrieve property values.
 */
public class ConfigManager {
    private static final Properties props = new Properties();

    static {
        try(InputStream input = ConfigManager.class.getClassLoader()
                .getResourceAsStream("application-api.properties")){
            if(input == null){
                throw new RuntimeException("application-api.properties not found in resources folder");
            }
            props.load(input);
        }catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    public static String get(String key){
        return props.getProperty(key);
    }
}
