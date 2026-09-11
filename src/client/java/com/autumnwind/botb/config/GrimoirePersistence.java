package com.autumnwind.botb.config;

import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.AlignmentOverride;
import com.autumnwind.botb.util.CustomRole;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Reminder;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.Script;
import com.autumnwind.botb.util.ScriptRole;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.autumnwind.botb.util.RoleType;

/**
 * Saves and restores the local grimoire view (the StorytellerState fields a player
 * curates client-side: their role-assignment guesses, reminders, demon bluffs, marks,
 * fabled toggles, etc.) across disconnect/reconnect within the same JVM session and
 * across Minecraft restarts.
 *
 * <p>One global save file, not keyed per server. Storyteller-only fields like
 * markedPlayers persist too. For non-operators the data simply goes unread by their UI.
 *
 * <p>Custom roles in PENDING_ROLES and DEMON_BLUFFS are stored by ID and resolved
 * against {@link ClientState#currentScript} on load. If the script hasn't arrived yet
 * (it usually arrives shortly after JOIN via the request-on-join hook), placeholders
 * remain until {@link #resolveCustomRoles()} is called from the SendScriptS2CPayload
 * receiver.
 *
 * <p>Custom-role <em>reminders</em> store only {@code customRoleId} and look up the
 * full {@link CustomRole} on demand from the script, so no resolution step is needed.
 */
public class GrimoirePersistence {
    private static final Path FILE = BotbConfigDir.resolve("grimoire.json");

    private static final int VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("version", VERSION);

            // Pending roles: { uuid: { isCustom, override, role|customRoleId } }
            JsonObject pendingRoles = new JsonObject();
            for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
                pendingRoles.add(entry.getKey().toString(), serializeAssignment(entry.getValue()));
            }
            root.add("pendingRoles", pendingRoles);

            // Seat numbers: { uuid: int }
            JsonObject seatNumbers = new JsonObject();
            for (Map.Entry<UUID, Integer> entry : StorytellerState.PENDING_SEAT_NUMBERS.entrySet()) {
                seatNumbers.addProperty(entry.getKey().toString(), entry.getValue());
            }
            root.add("seatNumbers", seatNumbers);

            // Reminders: { uuid: [ { text, role?, customRoleId?, playerUuid? } ] }
            JsonObject reminders = new JsonObject();
            for (Map.Entry<UUID, List<Reminder>> entry : StorytellerState.REMINDERS.entrySet()) {
                JsonArray arr = new JsonArray();
                for (Reminder r : entry.getValue()) {
                    arr.add(serializeReminder(r));
                }
                reminders.add(entry.getKey().toString(), arr);
            }
            root.add("reminders", reminders);

            // Demon bluffs reuse the existing string format ("" / "ROLE_NAME" / "custom:id")
            JsonArray bluffs = new JsonArray();
            for (String s : StorytellerState.bluffsToStrings(StorytellerState.DEMON_BLUFFS)) {
                bluffs.add(s);
            }
            root.add("demonBluffs", bluffs);

            JsonArray markedPlayers = new JsonArray();
            for (UUID u : StorytellerState.markedPlayers) {
                markedPlayers.add(u.toString());
            }
            root.add("markedPlayers", markedPlayers);

            root.addProperty("showBluffs", StorytellerState.showBluffs);
            root.addProperty("setupOutsiderCount", StorytellerState.setupOutsiderCount);
            // nextSeatNumber isn't serialized: it's a cached cursor over the seat-numbers
            // map, always max(seats) + 1 under the contiguous-seat invariant, so load()
            // rebuilds it. ClientReceive does the same after a grimoire sync.

            Files.writeString(FILE, GSON.toJson(root));
        } catch (IOException e) {
            // Persistence is best-effort, so don't crash the disconnect path on a write failure.
        }
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            String content = Files.readString(FILE);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();

            // Future schema migrations would branch on this. Today there's only v1, so
            // we just bail on anything else rather than risk applying garbage.
            int version = root.has("version") ? root.get("version").getAsInt() : 0;
            if (version != VERSION) return;

            // Apply to StorytellerState. Any in-memory state is overwritten. Server
            // broadcasts that arrive after this point (e.g. SyncGrimoireS2CPayload from
            // another op) will overwrite us in turn, which is the right precedence.
            StorytellerState.PENDING_ROLES.clear();
            JsonObject pendingRoles = getObject(root, "pendingRoles");
            if (pendingRoles != null) {
                for (Map.Entry<String, JsonElement> e : pendingRoles.entrySet()) {
                    UUID uuid = parseUuid(e.getKey());
                    if (uuid == null) continue;
                    PendingRoleAssignment a = deserializeAssignment(e.getValue().getAsJsonObject());
                    if (a != null) StorytellerState.PENDING_ROLES.put(uuid, a);
                }
            }

            StorytellerState.PENDING_SEAT_NUMBERS.clear();
            JsonObject seatNumbers = getObject(root, "seatNumbers");
            if (seatNumbers != null) {
                for (Map.Entry<String, JsonElement> e : seatNumbers.entrySet()) {
                    UUID uuid = parseUuid(e.getKey());
                    if (uuid == null) continue;
                    StorytellerState.PENDING_SEAT_NUMBERS.put(uuid, e.getValue().getAsInt());
                }
            }

            StorytellerState.REMINDERS.clear();
            JsonObject reminders = getObject(root, "reminders");
            if (reminders != null) {
                for (Map.Entry<String, JsonElement> e : reminders.entrySet()) {
                    UUID uuid = parseUuid(e.getKey());
                    if (uuid == null) continue;
                    List<Reminder> list = new ArrayList<>();
                    for (JsonElement el : e.getValue().getAsJsonArray()) {
                        Reminder r = deserializeReminder(el.getAsJsonObject());
                        if (r != null) list.add(r);
                    }
                    StorytellerState.REMINDERS.put(uuid, list);
                }
            }

            // Demon bluffs use the existing string-based deserializer (it resolves customs
            // against ClientState.currentScript when present, leaves nulls otherwise).
            JsonArray bluffsArr = getArray(root, "demonBluffs");
            if (bluffsArr != null) {
                List<String> strs = new ArrayList<>();
                for (JsonElement el : bluffsArr) strs.add(el.getAsString());
                List<ScriptRole> resolved = StorytellerState.stringsToBluffs(strs);
                StorytellerState.DEMON_BLUFFS.clear();
                StorytellerState.DEMON_BLUFFS.addAll(resolved);
                while (StorytellerState.DEMON_BLUFFS.size() < 3) StorytellerState.DEMON_BLUFFS.add(null);
            }

            StorytellerState.markedPlayers.clear();
            JsonArray markedArr = getArray(root, "markedPlayers");
            if (markedArr != null) {
                for (JsonElement el : markedArr) {
                    UUID uuid = parseUuid(el.getAsString());
                    if (uuid != null) StorytellerState.markedPlayers.add(uuid);
                }
            }

            if (root.has("showBluffs")) {
                StorytellerState.showBluffs = root.get("showBluffs").getAsBoolean();
            }
            if (root.has("setupOutsiderCount")) {
                StorytellerState.setupOutsiderCount = root.get("setupOutsiderCount").getAsInt();
            }

            // Rebuild nextSeatNumber from the loaded seat map. It's max(seats) + 1 by
            // the contiguous-seat invariant maintained by RoleSelectionScreen.
            int maxSeat = 0;
            for (int seat : StorytellerState.PENDING_SEAT_NUMBERS.values()) {
                if (seat > maxSeat) maxSeat = seat;
            }
            StorytellerState.nextSeatNumber = maxSeat + 1;
        } catch (Exception e) {
            // Corrupted file or schema mismatch, so start fresh rather than crash.
        }
    }

    /** Delete the persisted file. Called on game-end / hard-reset so a finished game's grimoire isn't restored. */
    public static void clear() {
        try {
            Files.deleteIfExists(FILE);
        } catch (IOException ignored) {
        }
    }

    /**
     * Re-resolve custom-role placeholders in PENDING_ROLES and DEMON_BLUFFS against the
     * current script. Called from the SendScriptS2CPayload receiver. At load time the
     * script is often not yet present, so placeholders sit until the server pushes one.
     */
    public static void resolveCustomRoles() {
        Script script = ClientState.currentScript;
        if (script == null) return;

        // PENDING_ROLES: replace any placeholder custom-role assignments with the
        // resolved version. The PendingRoleAssignment record already exposes
        // resolveCustomRole(Script) for this exact case.
        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            PendingRoleAssignment a = entry.getValue();
            if (a.isCustomRole()) {
                PendingRoleAssignment resolved = a.resolveCustomRole(script);
                if (resolved != a) {
                    StorytellerState.PENDING_ROLES.put(entry.getKey(), resolved);
                }
            }
        }

        // DEMON_BLUFFS: re-run string conversion. bluffsToStrings emits the canonical
        // form, then stringsToBluffs resolves customs against the now-present script.
        List<String> bluffStrings = StorytellerState.bluffsToStrings(StorytellerState.DEMON_BLUFFS);
        List<ScriptRole> resolved = StorytellerState.stringsToBluffs(bluffStrings);
        StorytellerState.DEMON_BLUFFS.clear();
        StorytellerState.DEMON_BLUFFS.addAll(resolved);
        while (StorytellerState.DEMON_BLUFFS.size() < 3) StorytellerState.DEMON_BLUFFS.add(null);
    }

    // -------- helpers --------

    private static JsonObject serializeAssignment(PendingRoleAssignment a) {
        JsonObject obj = new JsonObject();
        obj.addProperty("isCustom", a.isCustomRole());
        obj.addProperty("override", a.override().name());
        if (a.isCustomRole()) {
            obj.addProperty("customRoleId", a.customRole().get().id());
        } else {
            obj.addProperty("role", a.role().name());
        }
        return obj;
    }

    private static PendingRoleAssignment deserializeAssignment(JsonObject obj) {
        try {
            AlignmentOverride override = AlignmentOverride.valueOf(obj.get("override").getAsString());
            boolean isCustom = obj.get("isCustom").getAsBoolean();
            if (isCustom) {
                String customId = obj.get("customRoleId").getAsString();
                CustomRole resolved = ClientState.currentScript != null
                        ? ClientState.currentScript.getCustomRole(customId).orElse(null)
                        : null;
                if (resolved != null) {
                    return new PendingRoleAssignment(resolved, override);
                }
                // A placeholder that resolveCustomRoles() will swap for the real one
                // once the script arrives.
                return new PendingRoleAssignment(
                        Role.NO_ROLE,
                        Optional.of(new CustomRole(
                                customId, customId, RoleType.TOWNSFOLK,
                                "", "", List.of(), 0, 0, "", "",
                                List.of(), List.of(), false, List.of()
                        )),
                        override
                );
            } else {
                Role role = Role.valueOf(obj.get("role").getAsString());
                return new PendingRoleAssignment(role, override);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static JsonObject serializeReminder(Reminder r) {
        JsonObject obj = new JsonObject();
        obj.addProperty("text", r.text());
        r.role().ifPresent(role -> obj.addProperty("role", role.name()));
        r.customRoleId().ifPresent(id -> obj.addProperty("customRoleId", id));
        r.playerUuid().ifPresent(uuid -> obj.addProperty("playerUuid", uuid.toString()));
        return obj;
    }

    private static Reminder deserializeReminder(JsonObject obj) {
        try {
            String text = obj.get("text").getAsString();
            Optional<Role> role = obj.has("role")
                    ? Optional.of(Role.valueOf(obj.get("role").getAsString()))
                    : Optional.empty();
            Optional<String> customRoleId = obj.has("customRoleId")
                    ? Optional.of(obj.get("customRoleId").getAsString())
                    : Optional.empty();
            Optional<UUID> playerUuid = obj.has("playerUuid")
                    ? Optional.ofNullable(parseUuid(obj.get("playerUuid").getAsString()))
                    : Optional.empty();
            return new Reminder(text, role, customRoleId, playerUuid);
        } catch (Exception e) {
            return null;
        }
    }

    private static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static JsonObject getObject(JsonObject root, String key) {
        return root.has(key) && root.get(key).isJsonObject() ? root.getAsJsonObject(key) : null;
    }

    private static JsonArray getArray(JsonObject root, String key) {
        return root.has(key) && root.get(key).isJsonArray() ? root.getAsJsonArray(key) : null;
    }
}
