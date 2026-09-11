package com.autumnwind.botb.gui.assignroles;

import net.minecraft.resources.ResourceLocation;

public class AssignRolesConstants {
    public static final int ROLE_ICON_SIZE = 32;
    public static final int HEAD_ICON_SIZE = 24;
    public static final int ROLE_ICON_RADIUS_PADDING = 50;
    public static final int REMINDER_ICON_SIZE = 14;
    public static final int REMINDER_PADDING = 2;
    public static final int SEAT_NUMBER_RADIUS_OFFSET = 20;
    public static final ResourceLocation SHROUD_ICON = ResourceLocation.fromNamespaceAndPath("blood-on-the-blocktower", "textures/icons/barrier.png");
    public static final ResourceLocation DUSK_ICON = ResourceLocation.fromNamespaceAndPath("blood-on-the-blocktower", "textures/icons/dusk.png");
    public static final ResourceLocation DAWN_ICON = ResourceLocation.fromNamespaceAndPath("blood-on-the-blocktower", "textures/icons/dawn.png");

    // Animation constants
    public static final int FADE_DURATION_MS = 200;
    public static final int FADE_DELAY_MS = 50;

    private AssignRolesConstants() {} // Prevent instantiation
}
