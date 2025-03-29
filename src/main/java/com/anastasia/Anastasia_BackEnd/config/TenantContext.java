package com.anastasia.Anastasia_BackEnd.config;

import org.springframework.stereotype.Component;

@Component
public class TenantContext {

    private static final ThreadLocal<String> tenantId = new ThreadLocal<>();


    public static String getTenantId(){
        return tenantId.get();
    }

    public static boolean hasTenantId(){
        return tenantId.get() != null;
    }

    public static void setTenantId(String id){
         tenantId.set(id);
    }

    public static void clear(){
        tenantId.remove();
    }
}
