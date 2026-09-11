package com.autumnwind.botb;

import com.autumnwind.botb.clockhands.ClockHandsRenderer;
import com.autumnwind.botb.config.AssetPackTemplate;
import com.autumnwind.botb.config.PlayerConfig;
import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.gui.AssignRolesScreen;
import com.autumnwind.botb.gui.GameEndHoldingScreen;
import com.autumnwind.botb.gui.ScriptReferenceScreen;
import com.autumnwind.botb.hud.AssignRolesQuickHUD;
import com.autumnwind.botb.hud.GameEndAnimationHUD;
import com.autumnwind.botb.hud.MadnessHUD;
import com.autumnwind.botb.hud.NightOrderHudManager;
import com.autumnwind.botb.hud.RoleAssignmentAnimation;
import com.autumnwind.botb.hud.RoleHUD;
import com.autumnwind.botb.hud.ElectionHUD;
import com.autumnwind.botb.item.ModItems;
import com.autumnwind.botb.networking.ClientReceive;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.util.UrlTextureLoaderImpl;
import com.autumnwind.botb.voicechat.VoiceChatSidebar;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import com.autumnwind.botb.config.CustomRoleLibrary;
import com.autumnwind.botb.config.GrimoirePersistence;
import com.autumnwind.botb.config.RandomBanList;
import com.autumnwind.botb.hud.RoleIconRenderer;
import com.autumnwind.botb.hud.SetupHUD;
import com.autumnwind.botb.hud.WhisperEffectManager;
import com.autumnwind.botb.networking.RequestScriptC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionResult;
import net.minecraft.resources.Identifier;
import com.autumnwind.botb.BloodOnTheBlocktower;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public class BloodOnTheBlocktowerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PlayerConfig.load();

        // Written after languages load so the readme carries translated role names
        Identifier templateReloader = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "asset_pack_template");
        ResourceLoader resources = ResourceLoader.get(PackType.CLIENT_RESOURCES);
        resources.registerReloadListener(templateReloader, new SimpleReloadListener<Void>() {
            @Override
            protected Void prepare(PreparableReloadListener.SharedState state) {
                return null;
            }

            @Override
            protected void apply(Void prepared, PreparableReloadListener.SharedState state) {
                AssetPackTemplate.generate();
            }
        });
        resources.addListenerOrdering(ResourceReloaderKeys.Client.LANGUAGES, templateReloader);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Restore the saved grimoire BEFORE asking the server for the script. Order
            // matters: the script arrives after the request below and rightly overwrites
            // our saved copy. Locally-curated state (PENDING_ROLES, REMINDERS,
            // markedPlayers, DEMON_BLUFFS) isn't broadcast, so it survives.
            GrimoirePersistence.load();

            // Ask the server for the current script (if any). Lets a (re)joining player
            // recover it without waiting for the storyteller to re-send roles. Server
            // stays silent if nothing is cached yet.
            ClientPlayNetworking.send(
                    new RequestScriptC2SPayload());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // Persist on disconnect so the next session restores the same view.
            GrimoirePersistence.save();
        });

        // Initialize URL texture loading for custom roles
        UrlTextureLoaderImpl.init();

        // Saved homebrew characters for the Script Builder's palette. Loaded after the texture
        // loader is registered so their icons can resolve as soon as they're displayed.
        CustomRoleLibrary.load();

        // Characters the Script Builder's random draw skips.
        RandomBanList.load();

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "hud"), (drawContext, tickDelta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) {
                return;
            }

            // Render Voice Chat Sidebar (always visible when players are seated)
            VoiceChatSidebar.render(drawContext, client);

            // Render Quick Role View if key is held down
            if (KeyInputHandler.quickRoleViewKey.isDown()) {
                AssignRolesQuickHUD.render(drawContext, client);
                return; // Skip other HUD rendering when quick view is active
            }

            if (ClientState.isNightHudVisible && client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                NightOrderHudManager.render(drawContext, client);
            }

            // Render madness HUD (for both storyteller and players)
            if (ClientState.isHudEnabled) {
                MadnessHUD.render(drawContext, client);
            }

            // Render unified election HUD (handles both votes and exile support)
            if (ClientState.isHudEnabled) {
                ElectionHUD.render(drawContext, client);
            }

            // Map setup wizard box (only while the storyteller is walking the setup stick)
            SetupHUD.render(drawContext, client);

            // Render role HUD (player counts and role info)
            // Hide during role assignment animation - the animation shows the role instead
            if (ClientState.isHudEnabled && !RoleAssignmentAnimation.shouldHideRoleHUD()) {
                RoleHUD.render(drawContext, client);
            }

            // Render game end animation HUD (on top of everything, even when HUD is disabled)
            if (!(client.gui.screen() instanceof GameEndHoldingScreen)) {
                GameEndAnimationHUD.render(drawContext, client);
            }

            // Render role assignment animation (on top of everything)
            RoleAssignmentAnimation.render(drawContext, client);
        });

        // Register keybinds and networking
        KeyInputHandler.register();
        ClientReceive.registerClientReceivers();

        // Register clock hands world renderer
        ClockHandsRenderer.register();

        // Register floating role icon renderer (above players' heads)
        RoleIconRenderer.register();

        // Register whisper visual/audio effect renderer (arcs from sender → target)
        WhisperEffectManager.register();

        // Setup stick: sneak + left-click on nothing skips the current step. Attacks on air never
        // reach the server, so the client turns it into the skip command. Block clicks go through
        // the server-side attack callback instead.
        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
            if (!player.getMainHandItem().is(ModItems.SETUP_STICK)) return false;
            if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.MISS) return false;
            if (player.isShiftKeyDown() && clickCount != 0) { // a click, not a held button
                player.connection.sendCommand("botb setup skip");
            }
            return true; // never treat a stick swing at the air as an attack
        });

        // Register item use callback for Script and Grimoire items
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClientSide()) {
                return InteractionResult.PASS;
            }

            var stack = player.getItemInHand(hand);
            if (stack.isEmpty()) {
                return InteractionResult.PASS;
            }

            Minecraft client = Minecraft.getInstance();

            // Script item opens Script Reference screen (only if a script is assigned)
            if (stack.is(ModItems.SCRIPT)) {
                // Check if a script is assigned
                if (ClientState.currentScript == null) {
                    // Show overlay message that no script is assigned
                    client.gui.hud.setOverlayMessage(Component.translatable("message.blood-on-the-blocktower.client.no_script_assigned_overlay"), false);
                    return InteractionResult.SUCCESS;
                }
                client.schedule(() -> client.gui.setScreen(new ScriptReferenceScreen(Component.translatable("message.blood-on-the-blocktower.client.title_script_reference"))));
                return InteractionResult.SUCCESS;
            }

            // Grimoire item opens Assign Roles screen
            if (stack.is(ModItems.GRIMOIRE)) {
                client.schedule(() -> client.gui.setScreen(new AssignRolesScreen(Component.translatable("message.blood-on-the-blocktower.client.title_grimoire"))));
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });
    }
}
