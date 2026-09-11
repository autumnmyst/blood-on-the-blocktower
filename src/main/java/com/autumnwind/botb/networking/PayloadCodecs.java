package com.autumnwind.botb.networking;

import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Reminder;
import com.autumnwind.botb.util.Role;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Shared packet codecs for networking payloads.
 * Centralizes common map codecs to avoid duplication across payload classes.
 */
public final class PayloadCodecs {

    private PayloadCodecs() {
        // Utility class - prevent instantiation
    }

    /**
     * Codec for Map<UUID, PendingRoleAssignment> - used for role assignments
     */
    /**
     * Largest collection accepted from the wire. Every map here is keyed by seated player, so
     * a real one has at most a few dozen entries; the cap only stops a crafted packet.
     */
    private static final int MAX_ENTRIES = 1024;

    public static final StreamCodec<RegistryFriendlyByteBuf, Map<UUID, PendingRoleAssignment>> ROLE_MAP_CODEC =
            ByteBufCodecs.map(
                    HashMap::new,
                    UUIDUtil.STREAM_CODEC,
                    PendingRoleAssignment.PACKET_CODEC,
                    MAX_ENTRIES
            );

    /**
     * Codec for Map<UUID, Integer> - used for seat numbers
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, Map<UUID, Integer>> SEAT_MAP_CODEC =
            ByteBufCodecs.map(
                    HashMap::new,
                    UUIDUtil.STREAM_CODEC,
                    ByteBufCodecs.VAR_INT,
                    MAX_ENTRIES
            );

    /**
     * Codec for Map<UUID, Boolean> - used for death status
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, Map<UUID, Boolean>> DEATH_MAP_CODEC =
            ByteBufCodecs.map(
                    HashMap::new,
                    UUIDUtil.STREAM_CODEC,
                    ByteBufCodecs.BOOL,
                    MAX_ENTRIES
            );

    /**
     * Codec for List<Reminder> - used as part of reminder maps
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, List<Reminder>> REMINDER_LIST_CODEC =
            Reminder.PACKET_CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES));

    /**
     * Codec for Map<UUID, List<Reminder>> - used for player reminders
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, Map<UUID, List<Reminder>>> REMINDER_MAP_CODEC =
            ByteBufCodecs.map(
                    HashMap::new,
                    UUIDUtil.STREAM_CODEC,
                    REMINDER_LIST_CODEC,
                    MAX_ENTRIES
            );

    // Bluff type markers
    private static final byte BLUFF_EMPTY = 0;
    private static final byte BLUFF_OFFICIAL = 1;
    private static final byte BLUFF_CUSTOM = 2;

    /**
     * Encodes a list of bluffs to the buffer.
     * Each bluff is encoded as:
     * - byte type (0=empty, 1=official, 2=custom)
     * - if official: role ordinal (varint)
     * - if custom: custom role ID (string)
     *
     * @param buf the buffer to write to
     * @param bluffs list of bluff strings where:
     *               - null or empty = empty slot
     *               - "custom:id" = custom role with given ID
     *               - anything else = official role name
     */
    public static void encodeBluffs(RegistryFriendlyByteBuf buf, List<String> bluffs) {
        buf.writeVarInt(bluffs.size());
        for (String bluff : bluffs) {
            if (bluff == null || bluff.isEmpty()) {
                buf.writeByte(BLUFF_EMPTY);
            } else if (bluff.startsWith("custom:")) {
                buf.writeByte(BLUFF_CUSTOM);
                ByteBufCodecs.STRING_UTF8.encode(buf, bluff.substring(7)); // Strip "custom:" prefix
            } else {
                buf.writeByte(BLUFF_OFFICIAL);
                // Find the role by name and write its ordinal
                try {
                    Role role = Role.valueOf(bluff);
                    buf.writeVarInt(role.ordinal());
                } catch (IllegalArgumentException e) {
                    // Fallback to NO_ROLE if name not found
                    buf.writeVarInt(Role.NO_ROLE.ordinal());
                }
            }
        }
    }

    /**
     * Decodes a list of bluffs from the buffer.
     *
     * @param buf the buffer to read from
     * @return list of bluff strings where:
     *         - empty string = empty slot
     *         - "custom:id" = custom role
     *         - role name = official role
     */
    /** A demon gets three bluffs; the cap only stops a crafted count from pre-sizing a huge list. */
    private static final int MAX_BLUFFS = 16;

    public static List<String> decodeBluffs(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_BLUFFS) {
            throw new io.netty.handler.codec.DecoderException("Bluff count out of range: " + count);
        }
        List<String> bluffs = new ArrayList<>(count);
        Role[] roles = Role.values();
        for (int i = 0; i < count; i++) {
            byte type = buf.readByte();
            switch (type) {
                case BLUFF_EMPTY -> bluffs.add("");
                case BLUFF_OFFICIAL -> {
                    int ordinal = buf.readVarInt();
                    Role role = ordinal >= 0 && ordinal < roles.length ? roles[ordinal] : Role.NO_ROLE;
                    bluffs.add(role == Role.NO_ROLE ? "" : role.name());
                }
                case BLUFF_CUSTOM -> {
                    String customId = ByteBufCodecs.STRING_UTF8.decode(buf);
                    bluffs.add("custom:" + customId);
                }
                default -> bluffs.add(""); // Unknown type, treat as empty
            }
        }
        return bluffs;
    }
}
