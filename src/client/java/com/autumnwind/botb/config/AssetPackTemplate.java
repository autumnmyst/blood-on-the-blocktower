package com.autumnwind.botb.config;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.sound.CustomSounds;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.RoleType;
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

    /** Left out of the readme: the mod icon and title art aren't meant to be replaced. */
    private static final List<String> UNLISTED = List.of("icon.png", "textures/botb_title.png");

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
                if (UNLISTED.contains(relative)) continue;
                if (relative.endsWith(".png")) {
                    textures.add(new Asset(relative, imageSize(file)));
                } else if (relative.endsWith(".ogg")) {
                    sounds.add(new Asset(relative, oggDuration(file)));
                }
            }

            Path assets = packDir.resolve(ASSET_ROOT);
            for (Asset asset : textures) Files.createDirectories(assets.resolve(asset.path()).getParent());
            for (Asset asset : sounds) Files.createDirectories(assets.resolve(asset.path()).getParent());
            for (String dir : List.of(CustomSounds.ROLE_RECEIVE_DIR, CustomSounds.VOTE_MUSIC_DIR, CustomSounds.GAME_END_DIR)) {
                Files.createDirectories(assets.resolve("sounds/" + dir));
            }
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
        sb.append("\n## Extra sounds\n\n");
        sb.append("These have no built-in file, but a custom ogg at the path overrides the default sound. ");
        sb.append("Role receive plays the first of these that exists: the role, then its type, then its alignment, then the default.\n\n");
        sb.append("| Path | Plays when |\n|---|---|\n");
        sb.append("| `sounds/").append(CustomSounds.MADNESS_RECEIVE).append(".ogg` | Gaining a madness. Without it, the role receive chain below is used |\n");
        sb.append("| `sounds/").append(CustomSounds.VOTE_MUSIC_DIR).append("organ_grinder.ogg` | Vote music during an Organ Grinder vote |\n");
        sb.append("| `sounds/").append(CustomSounds.GAME_END_DIR).append("victory.ogg` | Game end, for players on the winning team and the storyteller |\n");
        sb.append("| `sounds/").append(CustomSounds.GAME_END_DIR).append("defeat.ogg` | Game end, for players on the losing team |\n");
        sb.append("| `sounds/").append(CustomSounds.ROLE_RECEIVE_DIR).append("good.ogg` | Role receive, any good role |\n");
        sb.append("| `sounds/").append(CustomSounds.ROLE_RECEIVE_DIR).append("evil.ogg` | Role receive, any evil role |\n");
        for (RoleType type : RoleType.values()) {
            if (type == RoleType.NONE || type == RoleType.FABLED || type == RoleType.LORIC) continue;
            String name = type.name().toLowerCase(Locale.ROOT);
            sb.append("| `sounds/").append(CustomSounds.ROLE_RECEIVE_DIR).append(name).append(".ogg` | Role receive, any ").append(name).append(" |\n");
        }
        for (Role role : Role.values()) {
            if (role == Role.NO_ROLE || role.getType() == RoleType.FABLED || role.getType() == RoleType.LORIC) continue;
            sb.append("| `sounds/").append(CustomSounds.ROLE_RECEIVE_DIR).append(role.getId()).append(".ogg` | Role receive, ").append(role.getDisplayName()).append(" |\n");
        }
        sb.append("\nCustom roles from a script use their id the same way.\n");

        sb.append("\n## sounds.json reference\n\n");
        sb.append("Optionally, if you want sounds.json features such as random variants, volume, or pitch adjustment, declare an event in sounds.json whose name is the path ");
        sb.append("without `sounds/` and `.ogg`, for example `custom/role_receive/imp`. The event is checked before the bare file.\n\n");
        sb.append("Every setting sounds.json supports, in `").append(ASSET_ROOT).append("/sounds.json`:\n\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"custom/role_receive/imp\": {\n");
        sb.append("    \"replace\": true,\n");
        sb.append("    \"subtitle\": \"subtitles.botb.imp_reveal\",\n");
        sb.append("    \"sounds\": [\n");
        sb.append("      {\n");
        sb.append("        \"name\": \"blood-on-the-blocktower:custom/role_receive/imp\",\n");
        sb.append("        \"weight\": 3,\n");
        sb.append("        \"volume\": 1.0,\n");
        sb.append("        \"pitch\": 1.0,\n");
        sb.append("        \"stream\": false,\n");
        sb.append("        \"preload\": false,\n");
        sb.append("        \"attenuation_distance\": 16\n");
        sb.append("      },\n");
        sb.append("      {\n");
        sb.append("        \"name\": \"blood-on-the-blocktower:custom/role_receive/imp_alt\",\n");
        sb.append("        \"weight\": 1,\n");
        sb.append("        \"volume\": 0.8,\n");
        sb.append("        \"pitch\": 0.9\n");
        sb.append("      },\n");
        sb.append("      {\n");
        sb.append("        \"name\": \"blood-on-the-blocktower:role_receive\",\n");
        sb.append("        \"type\": \"event\"\n");
        sb.append("      }\n");
        sb.append("    ]\n");
        sb.append("  }\n");
        sb.append("}\n");
        sb.append("```\n\n");
        sb.append("Each play picks one entry at random by `weight` (default 1). `name` is a file under `sounds/` without the extension, ");
        sb.append("or another event when `type` is `event`. `volume` and `pitch` multiply the mod's own values. ");
        sb.append("`stream` plays from disk instead of memory, use it for music. `preload` loads at resource load instead of first play. ");
        sb.append("`attenuation_distance` has no effect here since these sounds are not positional. ");
        sb.append("`replace` makes this list replace any lower pack's instead of merging with it. ");
        sb.append("`subtitle` is a translation key shown when subtitles are on. Only `name` is required.\n");
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
