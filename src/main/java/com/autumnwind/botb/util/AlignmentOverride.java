package com.autumnwind.botb.util;

import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.locale.Language;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

public enum AlignmentOverride {
    DEFAULT("default"),
    FORCE_GOOD("good"),
    FORCE_BAD("bad");

    public static final IntFunction<AlignmentOverride> ID_TO_VALUE = ByIdMap.continuous(
            AlignmentOverride::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, AlignmentOverride> PACKET_CODEC = ByteBufCodecs.idMapper(ID_TO_VALUE, AlignmentOverride::ordinal);

    private final String nameKey;

    AlignmentOverride(String id) {
        this.nameKey = "gui.blood-on-the-blocktower.alignment_override." + id;
    }

    public String getDisplayName() {
        return Language.getInstance().getOrDefault(nameKey);
    }

    // Helper method to cycle through the options
    public AlignmentOverride next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}