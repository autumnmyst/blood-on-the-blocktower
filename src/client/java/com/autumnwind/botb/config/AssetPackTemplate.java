package com.autumnwind.botb.config;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.fabricmc.loader.api.FabricLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Writes an empty resource pack to {@code config/botb/asset_pack_template} listing every
 * texture and sound the mod ships, with sizes and durations, so anyone can replace them
 * with an ordinary resource pack. Regenerated at startup, but user added files are left alone.
 */
public final class AssetPackTemplate {

    private AssetPackTemplate() {}

    private static final String ASSET_ROOT = "assets/" + BloodOnTheBlocktower.MOD_ID;
    private static final int PACK_FORMAT = 34; // Minecraft 1.21.1

    private record Asset(String path, String detail) {}

    public static void generate() {
        Path packDir = BotbConfigDir.resolve("asset_pack_template");
        Path bundled = FabricLoader.getInstance().getModContainer(BloodOnTheBlocktower.MOD_ID)
                .flatMap(container -> container.findPath(ASSET_ROOT))
                .orElse(null);
        if (bundled == null) {
            BloodOnTheBlocktower.LOGGER.warn("Bundled assets not found; skipping asset pack template");
            return;
        }

        List<Asset> textures = new ArrayList<>();
        List<Asset> sounds = new ArrayList<>();
        try (Stream<Path> files = Files.walk(bundled)) {
            for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile).sorted()::iterator) {
                String relative = bundled.relativize(file).toString().replace('\\', '/');
                if (relative.endsWith(".png")) {
                    textures.add(new Asset(relative, imageSize(file)));
                } else if (relative.endsWith(".ogg")) {
                    sounds.add(new Asset(relative, oggDuration(file)));
                }
            }

            Path assets = packDir.resolve(ASSET_ROOT);
            for (Asset asset : textures) Files.createDirectories(assets.resolve(asset.path()).getParent());
            for (Asset asset : sounds) Files.createDirectories(assets.resolve(asset.path()).getParent());
            Files.writeString(packDir.resolve("pack.mcmeta"),
                    "{\n  \"pack\": {\n    \"pack_format\": " + PACK_FORMAT
                            + ",\n    \"description\": \"Blood on the Blocktower asset overrides\"\n  }\n}\n");
            Files.writeString(packDir.resolve("README.md"), readme(textures, sounds));
        } catch (IOException e) {
            BloodOnTheBlocktower.LOGGER.error("Could not write asset pack template", e);
        }
    }

    private static String readme(List<Asset> textures, List<Asset> sounds) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Blood on the Blocktower asset overrides\n\n");
        sb.append("This folder is an empty resource pack. Drop a file at any path listed below and it replaces the mod's own copy.\n\n");
        sb.append("1. Copy this folder into your `resourcepacks` folder (or zip it).\n");
        sb.append("2. Enable it in Options > Resource Packs, above the mod's built-in assets.\n");
        sb.append("3. Paths below are relative to `").append(ASSET_ROOT).append("/` inside the pack.\n\n");
        sb.append("Textures are PNGs at the listed size. Other sizes work but will be scaled and/or stretched to fit. ");
        sb.append("Sounds are OGG Vorbis and can be any length.\n\n");
        sb.append("## Textures\n\n| Path | Size |\n|---|---|\n");
        for (Asset asset : textures) sb.append("| `").append(asset.path()).append("` | ").append(asset.detail()).append(" |\n");
        sb.append("\n## Sounds\n\n| Path | Duration |\n|---|---|\n");
        for (Asset asset : sounds) sb.append("| `").append(asset.path()).append("` | ").append(asset.detail()).append(" |\n");
        return sb.toString();
    }

    private static String imageSize(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            BufferedImage image = ImageIO.read(in);
            return image == null ? "?" : image.getWidth() + "x" + image.getHeight();
        } catch (IOException e) {
            return "?";
        }
    }

    /**
     * Ogg container duration: the Vorbis identification header carries the sample
     * rate and the last page's granule position is the total sample count.
     */
    private static String oggDuration(Path file) {
        try {
            byte[] data = Files.readAllBytes(file);
            int ident = indexOf(data, "vorbis".getBytes(StandardCharsets.ISO_8859_1), 0);
            int lastPage = lastIndexOf(data, "OggS".getBytes(StandardCharsets.ISO_8859_1));
            if (ident < 0 || lastPage < 0) return "?";
            ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            int sampleRate = buf.getInt(ident + 12); // marker byte, "vorbis", 4-byte version, 1-byte channel count
            long samples = buf.getLong(lastPage + 6);
            if (sampleRate <= 0 || samples < 0) return "?";
            return String.format(Locale.ROOT, "%.1fs", samples / (double) sampleRate);
        } catch (IOException | IndexOutOfBoundsException e) {
            return "?";
        }
    }

    private static int indexOf(byte[] data, byte[] needle, int from) {
        outer:
        for (int i = from; i <= data.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static int lastIndexOf(byte[] data, byte[] needle) {
        outer:
        for (int i = data.length - needle.length; i >= 0; i--) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}
