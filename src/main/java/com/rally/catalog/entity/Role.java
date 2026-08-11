package com.rally.catalog.entity;

public enum Role {
    ADMIN,
    SELLER,
    BUYER;

    public static Role fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (Role role : values()) {
            if (role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        return null;
    }
}
