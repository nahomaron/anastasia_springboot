package com.anastasia.Anastasia_BackEnd.config;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantContext {

    private static final ThreadLocal<UUID> tenantId = new ThreadLocal<>();


    public static UUID getTenantId(){
        return tenantId.get();
    }

    public static boolean hasTenantId(){
        return tenantId.get() != null;
    }

    public static void setTenantId(UUID id){
         tenantId.set(id);
    }

    public static void clear(){
        tenantId.remove();
    }
}
