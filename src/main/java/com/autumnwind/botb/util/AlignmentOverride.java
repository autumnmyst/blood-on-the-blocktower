package com.autumnwind.botb.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Language;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

public enum AlignmentOverride {
    DEFAULT("default"),
    FORCE_GOOD("good"),
    FORCE_BAD("bad");

    public static final IntFunction<AlignmentOverride> ID_TO_VALUE = ValueLists.createIdToValueFunction(
            AlignmentOverride::ordinal, values(), ValueLists.OutOfBoundsHandling.WRAP);
    public static final PacketCodec<ByteBuf, AlignmentOverride> PACKET_CODEC = PacketCodecs.indexed(ID_TO_VALUE, AlignmentOverride::ordinal);

    private final String nameKey;

    AlignmentOverride(String id) {
        this.nameKey = "gui.blood-on-the-blocktower.alignment_override." + id;
    }

    public String getDisplayName() {
        return Language.getInstance().get(nameKey);
    }

    // Helper method to cycle through the options
    public AlignmentOverride next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}