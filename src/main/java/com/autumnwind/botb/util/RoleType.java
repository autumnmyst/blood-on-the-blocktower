package com.autumnwind.botb.util;

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

    RoleType(boolean isDefaultGood, int color) {
        this.isDefaultGood = isDefaultGood;
        this.color = color;
    }

    public boolean isDefaultGood() {
        return isDefaultGood;
    }

    public int getColor() {
        return color;
    }
}