package com.autumnwind.botb;

import com.autumnwind.botb.clockhands.ClockHandsRenderer;
import com.autumnwind.botb.config.AssetPackTemplate;
import com.autumnwind.botb.config.PlayerConfig;
import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.gui.AssignRolesScreen;
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
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;
import com.autumnwind.botb.config.CustomRoleLibrary;
import com.autumnwind.botb.config.GrimoirePersistence;
import com.autumnwind.botb.config.RandomBanList;
import com.autumnwind.botb.hud.RoleIconRenderer;
import com.autumnwind.botb.hud.SetupHUD;
import com.autumnwind.botb.hud.WhisperEffectManager;
import com.autumnwind.botb.networking.RequestScriptC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.util.hit.HitResult;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.Collection;
import java.util.List;

public class BloodOnTheBlocktowerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PlayerConfig.load();

        // Written after languages load so the readme carries translated role names
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.of(BloodOnTheBlocktower.MOD_ID, "asset_pack_template");
            }

            @Override
            public Collection<Identifier> getFabricDependencies() {
                return List.of(ResourceReloadListenerKeys.LANGUAGES);
            }

            @Override
            public void reload(ResourceManager manager) {
                AssetPackTemplate.generate();
            }
        });

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

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) {
                return;
            }

            // Render Voice Chat Sidebar (always visible when players are seated)
            VoiceChatSidebar.render(drawContext, client);

            // Render Quick Role View if key is held down
            if (KeyInputHandler.quickRoleViewKey.isPressed()) {
                AssignRolesQuickHUD.render(drawContext, client);
                return; // Skip other HUD rendering when quick view is active
            }

            if (ClientState.isNightHudVisible && client.player.hasPermissionLevel(2)) {
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
            GameEndAnimationHUD.render(drawContext, client);

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
            if (!player.getMainHandStack().isOf(ModItems.SETUP_STICK)) return false;
            if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.MISS) return false;
            if (player.isSneaking() && clickCount != 0) { // a click, not a held button
                player.networkHandler.sendCommand("botb setup skip");
            }
            return true; // never treat a stick swing at the air as an attack
        });

        // Register item use callback for Script and Grimoire items
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient()) {
                return TypedActionResult.pass(ItemStack.EMPTY);
            }

            var stack = player.getStackInHand(hand);
            if (stack.isEmpty()) {
                return TypedActionResult.pass(ItemStack.EMPTY);
            }

            MinecraftClient client = MinecraftClient.getInstance();

            // Script item opens Script Reference screen (only if a script is assigned)
            if (stack.isOf(ModItems.SCRIPT)) {
                // Check if a script is assigned
                if (ClientState.currentScript == null) {
                    // Show overlay message that no script is assigned
                    client.inGameHud.setOverlayMessage(Text.translatable("message.blood-on-the-blocktower.client.no_script_assigned_overlay"), false);
                    return TypedActionResult.success(stack);
                }
                client.send(() -> client.setScreen(new ScriptReferenceScreen(Text.translatable("message.blood-on-the-blocktower.client.title_script_reference"))));
                return TypedActionResult.success(stack);
            }

            // Grimoire item opens Assign Roles screen
            if (stack.isOf(ModItems.GRIMOIRE)) {
                client.send(() -> client.setScreen(new AssignRolesScreen(Text.translatable("message.blood-on-the-blocktower.client.title_grimoire"))));
                return TypedActionResult.success(stack);
            }

            return TypedActionResult.pass(stack);
        });
    }
}
