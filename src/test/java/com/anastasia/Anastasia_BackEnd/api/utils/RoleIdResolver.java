package com.anastasia.Anastasia_BackEnd.api.utils;

import io.restassured.response.Response;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.restassured.RestAssured.given;

public final class RoleIdResolver {
    private static final Map<String, Long> cache = new ConcurrentHashMap<>();

    private RoleIdResolver() {}

    public static Long getRoleId(String roleName) {
        return cache.computeIfAbsent(roleName.toUpperCase(), RoleIdResolver::fetchRoleId);
    }

    private static Long fetchRoleId(String roleName) {
        Response res = given()
                .header("Authorization", "Bearer " + RoleContextFactory.ownerToken())
                .get("/test/roles")
                .then()
                .statusCode(200)
                .extract()
                .response();

        var roles = res.jsonPath().getList("", Map.class);
        for (Object o : roles) {
            Map<String, Object> role = (Map<String, Object>) o;
            if (roleName.equalsIgnoreCase((String) role.get("roleName"))) {
                Object id = role.get("id");
                if (id instanceof Number number) {
                    return number.longValue();
                }
                if (id instanceof String stringValue) {
                    return Long.parseLong(stringValue);
                }
            }
        }
        throw new IllegalStateException("Role not found in /test/roles: " + roleName);
    }
}
