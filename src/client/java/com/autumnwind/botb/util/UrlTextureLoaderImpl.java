package com.autumnwind.botb.util;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.states.ClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.io.ByteArrayInputStream;
import java.util.Iterator;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import com.autumnwind.botb.util.FetchLimits;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.net.URI;

/**
 * Client-side implementation of URL texture loading.
 * Downloads images from URLs and registers them as Minecraft textures.
 */
public class UrlTextureLoaderImpl {

    private static final Map<String, Identifier> loadedTextures = new ConcurrentHashMap<>();
    private static final Map<String, int[]> textureDimensions = new ConcurrentHashMap<>(); // [width, height]
    private static final Set<String> pendingLoads = ConcurrentHashMap.newKeySet();
    private static final Set<String> failedLoads = ConcurrentHashMap.newKeySet();
    private static final Map<String, Integer> retryCount = new ConcurrentHashMap<>();

    // Retry configuration
    private static final int MAX_RETRIES = 4;
    private static final long INITIAL_RETRY_DELAY_MS = 1000; // 1 second
    private static final ScheduledExecutorService retryScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "TextureLoader-Retry");
        t.setDaemon(true);
        return t;
    });

    /**
     * Initialize the URL texture loader and register with the main UrlTextureLoader.
     */
    public static void init() {
        UrlTextureLoader.registerLoader(
            url -> {
                // Check if this is a custom role ID lookup (doesn't start with http)
                if (url != null && !url.startsWith("http")) {
                    return getTextureForCustomRoleId(url);
                }
                return getTextureForUrl(url);
            },
            UrlTextureLoaderImpl::getTextureForCustomRole
        );
    }

    /**
     * Get texture for a URL. Starts async load if not already loaded.
     */
    public static Identifier getTextureForUrl(String url) {
        if (url == null || url.isEmpty()) {
            return UrlTextureLoader.PLACEHOLDER;
        }

        // Already loaded
        if (loadedTextures.containsKey(url)) {
            return loadedTextures.get(url);
        }

        // Failed previously, return placeholder
        if (failedLoads.contains(url)) {
            return UrlTextureLoader.PLACEHOLDER;
        }

        // Start loading if not already pending
        if (!pendingLoads.contains(url)) {
            pendingLoads.add(url);
            loadTextureAsync(url);
        }

        return UrlTextureLoader.PLACEHOLDER;
    }

    /**
     * Get texture for a custom role (uses neutral/default image).
     */
    public static Identifier getTextureForCustomRole(CustomRole customRole) {
        if (customRole == null || customRole.imageUrls().isEmpty()) {
            return UrlTextureLoader.PLACEHOLDER;
        }
        return getTextureForUrl(customRole.getNeutralImageUrl());
    }

    /**
     * Get texture for a custom role by ID.
     * Looks up the custom role from the current script.
     * Also handles fabled characters with "fabled:" prefix.
     */
    public static Identifier getTextureForCustomRoleId(String customRoleId) {
        if (customRoleId == null || customRoleId.isEmpty()) {
            return UrlTextureLoader.PLACEHOLDER;
        }

        // Look up from current script
        Script script = ClientState.currentScript;
        if (script == null) {
            return UrlTextureLoader.PLACEHOLDER;
        }

        // Handle fabled character lookup (customRoleId starts with "fabled:")
        if (customRoleId.startsWith("fabled:")) {
            String fabledId = customRoleId.substring(7); // Remove "fabled:" prefix
            Optional<ScriptRole> fabled = script.getFabledOrLoric(fabledId);
            if (fabled.isPresent()) {
                // ScriptRole.getIcon() already handles the icon resolution
                return fabled.get().getIcon();
            }
            return UrlTextureLoader.PLACEHOLDER;
        }

        Optional<CustomRole> customRole = script.getCustomRole(customRoleId);
        if (customRole.isEmpty()) {
            return UrlTextureLoader.PLACEHOLDER;
        }

        return getTextureForCustomRole(customRole.get());
    }

    /**
     * Preload all textures for a script's custom content: custom roles, custom travelers,
     * and custom fabled/loric. Done unconditionally (we pass {@code true} for travelers and
     * preload all fabled/loric regardless of which are active) so any later toggle reveals
     * icons that are already cached. Lazy-load on first display would otherwise cause a
     * visible delay.
     */
    public static void preloadScript(Script script) {
        if (script == null) return;

        // Custom roles + custom travelers
        for (CustomRole cr : script.allCustomRoles()) {
            for (String url : cr.imageUrls()) {
                getTextureForUrl(url);
            }
        }

        // Custom fabled and loric (ScriptRole.Fabled instances, official ones use built-in textures)
        for (ScriptRole sr : script.fabled()) {
            if (sr instanceof ScriptRole.Fabled fabled) {
                String url = fabled.fabledCharacter().imageUrl();
                if (url != null && !url.isEmpty()) {
                    getTextureForUrl(url);
                }
            }
        }
        for (ScriptRole sr : script.loric()) {
            if (sr instanceof ScriptRole.Fabled loric) {
                String url = loric.fabledCharacter().imageUrl();
                if (url != null && !url.isEmpty()) {
                    getTextureForUrl(url);
                }
            }
        }
    }

    /**
     * Load a texture asynchronously from a URL.
     */
    private static void loadTextureAsync(String url) {
        CompletableFuture.runAsync(() -> {
            try {
                // Scripts name these URLs and every client fetches them, so only web URLs are
                // allowed and the download is bounded by the player's configured limits
                if (!FetchLimits.isAllowedScheme(url)) {
                    BloodOnTheBlocktower.LOGGER.warn("Refusing non-http(s) image URL: {}", url);
                    failPermanently(url);
                    return;
                }
                URLConnection conn = URI.create(url).toURL().openConnection();
                conn.setConnectTimeout(FetchLimits.timeoutMs);
                conn.setReadTimeout(FetchLimits.timeoutMs);
                byte[] bytes;
                try (InputStream stream = conn.getInputStream()) {
                    bytes = FetchLimits.readBounded(stream, FetchLimits.maxBytes);
                }

                // Check the declared size from the header before decoding: a tiny PNG can
                // claim 30000x30000 and would allocate gigabytes on decode
                int[] declared = readDeclaredDimensions(bytes);
                if (declared != null && (declared[0] > FetchLimits.maxImageDimension || declared[1] > FetchLimits.maxImageDimension)) {
                    BloodOnTheBlocktower.LOGGER.warn("Image {}x{} exceeds the {} px limit: {}", declared[0], declared[1], FetchLimits.maxImageDimension, url);
                    failPermanently(url);
                    return;
                }

                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

                if (image == null) {
                    BloodOnTheBlocktower.LOGGER.warn("Failed to decode image from URL: {}", url);
                    handleLoadFailure(url, "Failed to decode image");
                    return;
                }

                // Store original dimensions before any processing
                textureDimensions.put(url, new int[]{image.getWidth(), image.getHeight()});

                // Convert to NativeImage
                NativeImage nativeImage = convertToNativeImage(image);

                // Generate unique identifier
                String hash = Integer.toHexString(url.hashCode() & 0x7FFFFFFF);
                Identifier id = Identifier.of(BloodOnTheBlocktower.MOD_ID, "dynamic/custom_" + hash);

                // Register on main thread
                MinecraftClient.getInstance().execute(() -> {
                    try {
                        NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
                        MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
                        loadedTextures.put(url, id);
                        pendingLoads.remove(url);
                        retryCount.remove(url); // Clear retry count on success
                        BloodOnTheBlocktower.LOGGER.debug("Loaded custom role texture: {}", url);
                    } catch (Exception e) {
                        BloodOnTheBlocktower.LOGGER.error("Failed to register texture for URL: {}", url, e);
                        handleLoadFailure(url, e.getMessage());
                    }
                });

            } catch (Exception e) {
                BloodOnTheBlocktower.LOGGER.error("Failed to load texture from URL: {}", url, e);
                handleLoadFailure(url, e.getMessage());
            }
        });
    }

    /** Width and height from the image header without decoding pixels, or null if unreadable. */
    private static int[] readDeclaredDimensions(byte[] bytes) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (iis == null) return null;
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            return null;
        }
    }

    /** A failure that retrying can't fix (bad scheme, oversized image): give up on the URL now. */
    private static void failPermanently(String url) {
        failedLoads.add(url);
        pendingLoads.remove(url);
        retryCount.remove(url);
    }

    /**
     * Handle a load failure with exponential backoff retry.
     */
    private static void handleLoadFailure(String url, String reason) {
        int currentRetries = retryCount.getOrDefault(url, 0);

        if (currentRetries >= MAX_RETRIES) {
            // Max retries exceeded, mark as permanently failed
            BloodOnTheBlocktower.LOGGER.warn("Giving up on texture after {} retries: {}", MAX_RETRIES, url);
            failedLoads.add(url);
            pendingLoads.remove(url);
            retryCount.remove(url);
            return;
        }

        // Schedule retry with exponential backoff (1s, 2s, 4s, 8s)
        long delayMs = INITIAL_RETRY_DELAY_MS * (1L << currentRetries);
        retryCount.put(url, currentRetries + 1);

        BloodOnTheBlocktower.LOGGER.debug("Scheduling retry {} for texture in {}ms: {}", currentRetries + 1, delayMs, url);

        retryScheduler.schedule(() -> {
            // Only retry if still pending (not cleared)
            if (pendingLoads.contains(url) && !loadedTextures.containsKey(url)) {
                loadTextureAsync(url);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Convert a BufferedImage to Minecraft's NativeImage format.
     * Smart-scales and centers the content to remove empty padding.
     * Uses 1/18 buffer on each side to match official icon proportions (18x18 icon with 16x16 content).
     */
    private static NativeImage convertToNativeImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Buffer is 1/18 of icon size (matches official 18x18 icons with 16x16 content)
        int buffer = Math.min(width, height) / 18;

        // Find the bounding box of non-transparent content
        int minX = width, minY = height, maxX = 0, maxY = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha > 10) { // Non-transparent pixel
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        NativeImage nativeImage = new NativeImage(width, height, false);

        // Check if we found any content
        if (maxX <= minX || maxY <= minY) {
            // No content found, just copy as-is
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    nativeImage.setColor(x, y, convertArgbToAbgr(image.getRGB(x, y)));
                }
            }
            return nativeImage;
        }

        int contentWidth = maxX - minX + 1;
        int contentHeight = maxY - minY + 1;
        float contentCenterX = minX + contentWidth / 2.0f;
        float contentCenterY = minY + contentHeight / 2.0f;

        // Calculate scale to fit content in frame with buffer (maintaining aspect ratio)
        int availableWidth = width - 2 * buffer;
        int availableHeight = height - 2 * buffer;
        float scaleX = (float) availableWidth / contentWidth;
        float scaleY = (float) availableHeight / contentHeight;
        float scale = Math.min(scaleX, scaleY);

        // Output center
        float outCenterX = width / 2.0f;
        float outCenterY = height / 2.0f;

        // For each output pixel, find corresponding source pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Map output coord to source coord (centered on content)
                float srcX = contentCenterX + (x - outCenterX) / scale;
                float srcY = contentCenterY + (y - outCenterY) / scale;

                int sampleX = Math.round(srcX);
                int sampleY = Math.round(srcY);

                // If outside source bounds, use transparent
                if (sampleX < 0 || sampleX >= width || sampleY < 0 || sampleY >= height) {
                    nativeImage.setColor(x, y, 0); // Transparent
                } else {
                    nativeImage.setColor(x, y, convertArgbToAbgr(image.getRGB(sampleX, sampleY)));
                }
            }
        }

        return nativeImage;
    }

    private static int convertArgbToAbgr(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    /**
     * Get the original dimensions of a loaded texture.
     * @return [width, height] or null if not loaded
     */
    public static int[] getDimensions(String url) {
        return textureDimensions.get(url);
    }
}
