package com.autumnwind.botb.networking;

import com.autumnwind.botb.util.AlmanacParser;
import com.autumnwind.botb.util.ReminderCatalog;
import com.autumnwind.botb.util.Script;
import com.autumnwind.botb.util.UrlTextureLoaderImpl;
import net.minecraft.client.sound.SoundInstance;

public class ClientReceive {

    // Store looping sound instances so we can stop them later
    private static SoundInstance voteMusicInstance = null;
    private static SoundInstance clockTickingInstance = null;

    /** Registers every client-bound receiver, grouped by domain. */
    public static void registerClientReceivers() {
        WhisperReceivers.register();
        RoleReceivers.register();
        ScriptReceivers.register();
        NightOrderReceivers.register();
        GrimoireReceivers.register();
        DaytimeReceivers.register();
        GameEndReceivers.register();
        MiscReceivers.register();
        SoundReceivers.register();
    }

    /**
     * Called when a script is loaded to set up custom role support.
     * Registers custom role reminders, preloads custom role textures, and pre-fetches almanac data.
     */
    public static void onScriptLoaded(Script script) {
        if (script == null) return;

        // Register custom role reminders
        ReminderCatalog.registerScriptReminders(script);

        // Preload custom role textures
        if (script.hasCustomRoles()) {
            UrlTextureLoaderImpl.preloadScript(script);
        }

        // Pre-fetch almanac data so it's ready when viewing character details or script reference.
        // Extras count: a script built from borrowed homebrew has no almanac of its own, but its
        // characters' pages still need warming.
        if (script.hasAnyAlmanac()) {
            AlmanacParser.fetchAlmanacs(script.almanac(), script.extraAlmanacs());
        }
    }
}