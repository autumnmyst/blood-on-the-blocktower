package com.autumnwind.botb.util;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.Locale;
import net.minecraft.locale.Language;

public enum RoleType {
    TOWNSFOLK(true, 0xFF00AAFF),
    OUTSIDER(true, 0xFF00DDAA),
    MINION(false, 0xFFFF5555),
    DEMON(false, 0xFFAA0000),
    TRAVELER(true, 0xFF9932CC), // Purple color, default good
    FABLED(true, 0xFFD4AF37), // Gold color, neutral
    LORIC(true, 0xFF2E8B57), // Green color, neutral
    NONE(true, 0xFFFFFFFF); // Added for NO_ROLE

    private final boolean isDefaultGood;
    private final int color;
    private final String nameKey;

    RoleType(boolean isDefaultGood, int color) {
        this.isDefaultGood = isDefaultGood;
        this.color = color;
        this.nameKey = "roletype." + BloodOnTheBlocktower.MOD_ID + "." + name().toLowerCase(Locale.ROOT);
    }

    /** The type name from the lang file, in caps. */
    public String getDisplayName() {
        return Language.getInstance().getOrDefault(nameKey);
    }

    /** The one or two letter abbreviation from the lang file, as used for badges. */
    public String getShortName() {
        return Language.getInstance().getOrDefault(nameKey + ".short");
    }

    /** The plural type name from the lang file, as used for section headers. */
    public String getPluralName() {
        return Language.getInstance().getOrDefault(nameKey + ".plural");
    }

    public boolean isDefaultGood() {
        return isDefaultGood;
    }

    public int getColor() {
        return color;
    }
}