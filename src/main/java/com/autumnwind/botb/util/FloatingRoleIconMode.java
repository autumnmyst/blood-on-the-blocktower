package com.autumnwind.botb.util;

import net.minecraft.util.Language;

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
        return Language.getInstance().get(switch (this) {
            case OFF -> "gui.blood-on-the-blocktower.settings.off";
            case ALWAYS -> "gui.blood-on-the-blocktower.settings.role_icons.always";
            case AFTER_END -> "gui.blood-on-the-blocktower.settings.role_icons.game_end";
        });
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
