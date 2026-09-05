package com.autumnwind.botb.util;

public enum FloatingRoleIconMode {
    OFF,
    ALWAYS,
    AFTER_END;

    public FloatingRoleIconMode cycle() {
        return switch (this) {
            case OFF -> ALWAYS;
            case ALWAYS -> AFTER_END;
            case AFTER_END -> OFF;
        };
    }

    public String displayName() {
        return switch (this) {
            case OFF -> "OFF";
            case ALWAYS -> "ALWAYS";
            case AFTER_END -> "GAME END";
        };
    }

    public static FloatingRoleIconMode fromName(String name) {
        if (name == null) return AFTER_END;
        try {
            return FloatingRoleIconMode.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return AFTER_END;
        }
    }
}
