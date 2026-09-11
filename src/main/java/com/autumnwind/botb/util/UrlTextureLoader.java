package com.autumnwind.botb.util;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

/**
 * Provides texture loading for custom roles from URLs.
 * The actual implementation is registered by the client mod initializer.
 * On server, this just returns placeholders.
 */
public class UrlTextureLoader {

    // Use the existing custom reminder icon as placeholder until custom role images load
    public static final ResourceLocation PLACEHOLDER = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/reminder_custom.png");

    // Client-side implementation, set during client initialization
    private static Function<String, ResourceLocation> urlLoader = null;
    private static Function<CustomRole, ResourceLocation> customRoleLoader = null;

    /**
     * Register the client-side texture loader.
     * Called from client mod initializer.
     */
    public static void registerLoader(Function<String, ResourceLocation> loader, Function<CustomRole, ResourceLocation> roleLoader) {
        urlLoader = loader;
        customRoleLoader = roleLoader;
    }

    /**
     * Get texture identifier for a URL.
     * Returns placeholder if loader not registered or texture not yet loaded.
     */
    public static ResourceLocation getTexture(String url) {
        if (urlLoader == null || url == null || url.isEmpty()) {
            return PLACEHOLDER;
        }
        return urlLoader.apply(url);
    }

    /**
     * Get texture identifier for a custom role.
     * Returns placeholder if loader not registered or texture not yet loaded.
     */
    public static ResourceLocation getTexture(CustomRole customRole) {
        if (customRoleLoader == null || customRole == null) {
            return PLACEHOLDER;
        }
        return customRoleLoader.apply(customRole);
    }

    /**
     * Get texture identifier for a custom role with specific alignment.
     */
    public static ResourceLocation getTexture(CustomRole customRole, boolean isGood) {
        if (customRole == null) {
            return PLACEHOLDER;
        }
        String url = customRole.getImageUrl(isGood);
        return getTexture(url);
    }

    /**
     * Get texture identifier for a custom role by ID.
     * Looks up the custom role from the current script and returns its icon.
     */
    public static ResourceLocation getTextureByCustomRoleId(String customRoleId) {
        // This is called for custom role reminders - need to look up the role from the script
        // Delegate to the URL loader which handles the lookup on client side
        if (customRoleId == null || customRoleId.isEmpty()) {
            return PLACEHOLDER;
        }

        // The URL loader will detect this isn't a URL and look up the custom role
        return getTexture(customRoleId);
    }
}
