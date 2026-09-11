package com.autumnwind.botb.networking;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlaySoundS2CPayload(String soundType) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlaySoundS2CPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "play_sound"));

    public static final StreamCodec<ByteBuf, PlaySoundS2CPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PlaySoundS2CPayload::soundType,
            PlaySoundS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    // Sound type constants
    public static final String DOORBELL = "doorbell";
    public static final String DOORKNOCK = "doorknock";
    public static final String DUSK = "dusk";
    public static final String DAWN = "dawn";
    public static final String CALL_BACK = "call_back";
    public static final String NOMINATION = "nomination";
    public static final String VOTE_START = "vote_start";
    public static final String VOTE_MUSIC = "vote_music";
    public static final String VOTE_MUSIC_ORGAN_GRINDER = "vote_music_organ_grinder";
    public static final String VOTE_MUSIC_STOP = "vote_music_stop";
    public static final String CLOCK_TICKING = "clock_ticking";
    public static final String CLOCK_TICKING_STOP = "clock_ticking_stop";
    public static final String NOT_ENOUGH_VOTES = "not_enough_votes";
    public static final String TIE = "tie";
    public static final String MARKED = "marked";
    public static final String EXECUTION = "execution";
    public static final String EXECUTION_SURVIVED = "execution_survived";
    public static final String GAME_END = "game_end";
}
