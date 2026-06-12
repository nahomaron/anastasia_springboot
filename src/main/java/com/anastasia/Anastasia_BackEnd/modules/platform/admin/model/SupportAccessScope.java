package com.anastasia.Anastasia_BackEnd.modules.platform.admin.model;

public enum SupportAccessScope {
    READ_ONLY,
    READ_WRITE;

    public boolean allowsWrites() {
        return this == READ_WRITE;
    }
}
