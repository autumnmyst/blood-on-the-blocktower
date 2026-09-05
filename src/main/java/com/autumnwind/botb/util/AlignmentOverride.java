package com.autumnwind.botb.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

public enum AlignmentOverride {
    DEFAULT("Default"),
    FORCE_GOOD("Good"),
    FORCE_BAD("Bad");

    public static final IntFunction<AlignmentOverride> ID_TO_VALUE = ValueLists.createIdToValueFunction(
            AlignmentOverride::ordinal, values(), ValueLists.OutOfBoundsHandling.WRAP);
    public static final PacketCodec<ByteBuf, AlignmentOverride> PACKET_CODEC = PacketCodecs.indexed(ID_TO_VALUE, AlignmentOverride::ordinal);

    private final String displayName;

    AlignmentOverride(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    // Helper method to cycle through the options
    public AlignmentOverride next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}