package com.autumnwind.botb.gui;

import com.autumnwind.botb.config.CustomRoleLibrary;
import com.autumnwind.botb.config.RandomBanList;
import com.autumnwind.botb.networking.ClientReceive;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.AbilityText;
import com.autumnwind.botb.util.CustomRole;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.RoleType;
import com.autumnwind.botb.util.Script;
import com.autumnwind.botb.util.ScriptJson;
import com.autumnwind.botb.util.ScriptRole;
import com.google.gson.JsonObject;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import java.util.Locale;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * Mix-and-match script builder for storytellers.
 *
 * <p>Left pane: the script being built, a vertical list of icon + name rows grouped by team.
 * Right pane: a searchable grid of every character available: all official roles, travelers,
 * fabled and loric, plus everything in the {@link CustomRoleLibrary}.
 *
 * <p>Nothing here touches {@link ClientState#currentScript} until Save. Save regenerates real
 * script JSON and round-trips it back through {@link Script#fromJson}, because the packet codec
 * transmits {@code rawJson} rather than the parsed fields. A hand-assembled {@code Script}
 * record with a stale {@code rawJson} would look right locally and arrive wrong at the players.
 *
 * <p>Save is <em>not</em> Send Roles. It only commits the script to the storyteller's local
 * state (and to co-storytellers via grimoire sync), the same as Import Script does.
 */
public class ScriptBuilderScreen extends Screen implements ReturnOnClose {

    /** Team display order for both the script list's sections and the palette's grouping. */
    private static final RoleType[] SECTION_ORDER = {
            RoleType.TOWNSFOLK, RoleType.OUTSIDER, RoleType.MINION,
            RoleType.DEMON, RoleType.TRAVELER, RoleType.FABLED, RoleType.LORIC
    };

    private static final int CELL_WIDTH = 75;
    private static final int ICON_SIZE = 40;
    private static final int TOOLTIP_WIDTH = 170;

    /** Teams a random script draws from, and how many of each. */
    private static final RoleType[] RANDOM_TEAMS = {
            RoleType.TOWNSFOLK, RoleType.OUTSIDER, RoleType.MINION, RoleType.DEMON
    };
    private static final int[] FULL_COUNTS = {13, 4, 4, 4};
    private static final int[] TEENSYVILLE_COUNTS = {6, 2, 2, 2};

    private static final Component IMPORT_CUSTOMS_LABEL = Component.translatable("gui.blood-on-the-blocktower.script_builder.import_custom_roles");
    private static final Component CLEAR_CUSTOMS_LABEL = Component.translatable("gui.blood-on-the-blocktower.script_builder.clear_custom_roles");
    private static final Component IMPORT_CUSTOMS_SHORT = Component.translatable("gui.blood-on-the-blocktower.script_builder.import_custom_roles_short");
    private static final Component CLEAR_CUSTOMS_SHORT = Component.translatable("gui.blood-on-the-blocktower.script_builder.clear_custom_roles_short");

    private final Screen parent;

    /** The script being built. Order within a team is insertion order. */
    private final List<ScriptRole> working = new ArrayList<>();

    /** Normalized id → the raw JSON definition a custom character is re-emitted from. */
    private final Map<String, JsonObject> definitions = new HashMap<>();

    /** Normalized id → almanac URLs of the script a custom character came from. */
    private final Map<String, List<String>> almanacSources = new HashMap<>();

    /**
     * Every homebrew character this session has seen, whether or not it's currently on the
     * script. Without it, taking a script-local custom role off the list would also take it out
     * of the palette. It isn't in the library, so nothing else would remember it.
     */
    private final Map<String, ScriptRole> knownCustoms = new LinkedHashMap<>();

    /** {@code _meta} fields carried through from whichever script seeded the builder. */
    private String baseLogo;
    private String baseAlmanac;
    private List<String> baseExtraAlmanacs = new ArrayList<>();
    private List<String> baseBootlegger = new ArrayList<>();
    private List<String> baseFirstNightOrder;
    private List<String> baseOtherNightOrder;

    /** Name and author of the loaded script, holding them until init() creates the text fields. */
    private String scriptName = "";
    private String scriptAuthor = "";

    /**
     * Snapshot of the <em>active</em> script, which Save and the discard prompt compare against.
     * Only re-taken when the working set becomes the active script: on open, on save, and on
     * removal. A clipboard import is an edit like any other and deliberately leaves this alone.
     */
    private Set<String> seededIds = new HashSet<>();
    private String seededName = "";
    private String seededAuthor = "";
    private List<String> seededBootlegger = new ArrayList<>();

    private EditBox nameField;
    private EditBox authorField;
    private EditBox searchField;
    private ScriptListWidget scriptList;
    private PaletteWidget palette;
    private Button saveButton;

    /** Import/Clear custom roles share one button, swapped by shift. */
    private Button customsButton;
    private boolean customsShowingClear;
    private boolean customsRoomy = true;

    /** Import/Export script share one button, swapped by shift. */
    private Button importButton;
    private boolean importShowingExport;
    private boolean importRoomy = true;

    private List<ScriptRole> paletteSource = new ArrayList<>();
    private String lastQuery = "";

    /**
     * Mutations requested from inside a list entry's click handler. Running them immediately
     * would rebuild the entry list while the widget is still dispatching into it, so they're
     * deferred to just after {@code super.mouseClicked} returns.
     */
    private Runnable pendingAction;

    private boolean discardConfirmed;

    /**
     * Tooltip requested by whichever list entry the mouse is over, drawn once every child has
     * rendered. Drawing it from inside the entry puts it inside that list's scissor region: the
     * box gets clipped at the panel edge, and because the background and the text are flushed at
     * different points a tooltip straddling the two panes had its box clipped to one pane and its
     * text to the other. Deferring to the end of {@link #render} avoids both.
     */
    private List<Component> hoverTooltip;
    private int hoverTooltipX;
    private int hoverTooltipY;

    public ScriptBuilderScreen(Screen parent) {
        super(Component.translatable("gui.blood-on-the-blocktower.script_builder.title"));
        this.parent = parent;
        CustomRoleLibrary.preloadTextures();
        seedFrom(ClientState.currentScript);
        captureBaseline();
    }

    // ================================================================
    // Builder state
    // ================================================================

    /**
     * Load a script into the working set, replacing whatever was there. Used both for the initial
     * seed from the active script and for the Import Script button. The difference is that only
     * the former follows up with {@link #captureBaseline()}, so an import shows up as an edit.
     */
    private void seedFrom(Script script) {
        working.clear();
        // definitions, almanacSources and knownCustoms deliberately accumulate rather than
        // reset: they're what makes a homebrew character re-addable after you take it off the
        // script, and the library is the only durable store, so a role seen only in this
        // session would otherwise become unreachable the moment it left the list.
        baseLogo = null;
        baseAlmanac = null;
        baseExtraAlmanacs = new ArrayList<>();
        baseBootlegger = new ArrayList<>();
        baseFirstNightOrder = null;
        baseOtherNightOrder = null;
        scriptName = "";
        scriptAuthor = defaultAuthor();

        if (script != null) {
            definitions.putAll(ScriptJson.indexDefinitions(script.rawJson()));
            List<String> almanacs = CustomRoleLibrary.almanacUrlsOf(script);

            for (Role role : script.roles()) {
                addRole(new ScriptRole.Official(role), almanacs);
            }
            for (CustomRole customRole : script.customRoles()) {
                addRole(new ScriptRole.Custom(customRole), almanacs);
            }
            addAll(script.travelers(), almanacs);
            addAll(script.fabled(), almanacs);
            addAll(script.loric(), almanacs);

            baseLogo = script.logo();
            baseAlmanac = script.almanac();
            if (script.extraAlmanacs() != null) {
                baseExtraAlmanacs = new ArrayList<>(script.extraAlmanacs());
            }
            if (script.bootlegger() != null) {
                baseBootlegger = new ArrayList<>(script.bootlegger());
            }
            baseFirstNightOrder = script.firstNightOrder();
            baseOtherNightOrder = script.otherNightOrder();
            scriptName = script.name() == null ? "" : script.name();
            if (script.author() != null && !script.author().isBlank()) {
                scriptAuthor = script.author();
            }
        }

        if (nameField != null) nameField.setValue(scriptName);
        if (authorField != null) authorField.setValue(scriptAuthor);
    }

    /**
     * Who a new script is by, until an imported script says otherwise. Uses the player's name as
     * it renders in game rather than the Mojang profile name, so a custom name set with
     * {@code /botb setName} is what ends up on the script.
     */
    private static String defaultAuthor() {
        Minecraft client = Minecraft.getInstance();
        return client.player != null ? client.player.getName().getString() : "";
    }

    /** Declare the current builder contents to be what's already saved as the active script. */
    private void captureBaseline() {
        seededIds = workingIds();
        seededName = nameField != null ? nameField.getValue() : scriptName;
        seededAuthor = authorField != null ? authorField.getValue() : scriptAuthor;
        seededBootlegger = new ArrayList<>(baseBootlegger);
    }

    private void addAll(List<ScriptRole> roles, List<String> almanacs) {
        if (roles == null) return;
        for (ScriptRole role : roles) {
            addRole(role, almanacs);
        }
    }

    /** Add a character if it isn't already on the script. Returns true if it was added. */
    private boolean addRole(ScriptRole role, List<String> almanacs) {
        String key = ScriptJson.normalizeId(role.getId());
        if (key.isEmpty()) return false;
        if (role.isCustom()) {
            knownCustoms.putIfAbsent(key, role);
            if (almanacs != null && !almanacs.isEmpty()) {
                almanacSources.put(key, almanacs);
            }
        }
        if (contains(key)) return false;
        // A character on the script is never barred from random draws.
        RandomBanList.setBanned(key, false);
        working.add(role);
        return true;
    }

    private boolean contains(String normalizedId) {
        for (ScriptRole role : working) {
            if (ScriptJson.normalizeId(role.getId()).equals(normalizedId)) return true;
        }
        return false;
    }

    private Set<String> workingIds() {
        return working.stream()
                .map(role -> ScriptJson.normalizeId(role.getId()))
                .collect(Collectors.toCollection(HashSet::new));
    }

    /** The working set in display order: by team section, insertion order within a team. */
    private List<ScriptRole> ordered() {
        List<ScriptRole> out = new ArrayList<>();
        for (RoleType team : SECTION_ORDER) {
            for (ScriptRole role : working) {
                if (role.getTeam() == team) out.add(role);
            }
        }
        // Anything with an unexpected team (NONE) still ships, because an odd ordering beats a
        // character silently dropped from the saved script.
        for (ScriptRole role : working) {
            if (!out.contains(role)) out.add(role);
        }
        return out;
    }

    private boolean isDirty() {
        if (!workingIds().equals(seededIds)) return true;
        if (!baseBootlegger.equals(seededBootlegger)) return true;
        if (nameField != null && !nameField.getValue().equals(seededName)) return true;
        return authorField != null && !authorField.getValue().equals(seededAuthor);
    }

    // ================================================================
    // Layout
    // ================================================================

    @Override
    protected void init() {
        int padding = 10;
        int gap = 8;
        int fieldHeight = 20;
        int rowY = 20;
        int listTop = rowY + fieldHeight + 6;
        // Leaves a strip above the footer buttons for the team-count summary drawn in render().
        int listBottom = this.height - 46;
        int listHeight = Math.max(40, listBottom - listTop);

        // The palette wants whole 75px cells, so give the script list whatever's left, shedding a
        // column at a time until it has a workable width on smaller GUI scales.
        int available = this.width - 2 * padding - gap;
        int columns = 5;
        int paletteWidth = columns * CELL_WIDTH + 14;
        while (columns > 2 && available - paletteWidth < 150) {
            columns--;
            paletteWidth = columns * CELL_WIDTH + 14;
        }
        int scriptWidth = Math.max(110, available - paletteWidth);
        int paletteX = padding + scriptWidth + gap;
        paletteWidth = Math.max(CELL_WIDTH + 14, this.width - paletteX - padding);

        // --- Header fields ---
        int authorWidth = Math.max(50, scriptWidth * 2 / 5);
        int nameWidth = scriptWidth - authorWidth - 4;

        String currentName = nameField != null ? nameField.getValue() : scriptName;
        String currentAuthor = authorField != null ? authorField.getValue() : scriptAuthor;
        String currentSearch = searchField != null ? searchField.getValue() : "";

        // init() runs again every time we're re-shown (coming back from a character's details
        // page, or on a resize) and builds new list widgets, so the old ones' scroll positions
        // have to be carried over by hand or you land back at the top of the palette.
        double scriptScroll = scriptList != null ? scriptList.scrollAmount() : 0;
        double paletteScroll = palette != null ? palette.scrollAmount() : 0;

        this.nameField = new EditBox(this.font, padding, rowY, nameWidth, fieldHeight, Component.empty());
        this.nameField.setMaxLength(64);
        this.nameField.setHint(Component.translatable("gui.blood-on-the-blocktower.script_builder.script_name").withStyle(ChatFormatting.DARK_GRAY));
        this.nameField.setValue(currentName);
        this.addRenderableWidget(this.nameField);

        this.authorField = new EditBox(this.font, padding + nameWidth + 4, rowY, authorWidth, fieldHeight, Component.empty());
        this.authorField.setMaxLength(64);
        this.authorField.setHint(Component.translatable("gui.blood-on-the-blocktower.script_builder.author").withStyle(ChatFormatting.DARK_GRAY));
        this.authorField.setValue(currentAuthor);
        this.addRenderableWidget(this.authorField);

        this.searchField = new EditBox(this.font, paletteX, rowY, paletteWidth, fieldHeight, Component.empty());
        this.searchField.setHint(Component.translatable("gui.blood-on-the-blocktower.script_builder.search_characters").withStyle(ChatFormatting.DARK_GRAY));
        this.searchField.setValue(currentSearch);
        this.searchField.setResponder(text -> refreshPalette());
        this.addRenderableWidget(this.searchField);

        // --- Lists ---
        this.scriptList = new ScriptListWidget(this.minecraft, scriptWidth, listHeight, listTop);
        this.scriptList.updateSizeAndPosition(scriptWidth, listHeight, padding, listTop);
        this.addRenderableWidget(this.scriptList);

        this.palette = new PaletteWidget(this.minecraft, paletteWidth, listHeight, listTop, columns);
        this.palette.setX(paletteX);
        this.addRenderableWidget(this.palette);

        rebuildPaletteSource();
        refreshScriptList();
        refreshPalette();
        // Both are clamped to the new content height, so a shorter list or a smaller window
        // just lands at the bottom rather than scrolling into empty space.
        scriptList.setScrollAmount(scriptScroll);
        palette.setScrollAmount(paletteScroll);

        // --- Footer ---
        int buttonY = this.height - 28;
        int buttonHeight = 20;
        int spacing = 4;

        int backWidth = 60;
        int saveWidth = 70;
        int rightGroup = saveWidth + spacing + backWidth;

        // Buttons are sized from their labels, and the labels shorten when the left group would
        // otherwise run into Save/Back on a narrow screen. The tooltips carry the full meaning
        // either way. The customs button is sized for the longer of its two states so it doesn't
        // resize under the cursor when shift is pressed.
        int customsWidth = Math.max(textWidth(IMPORT_CUSTOMS_LABEL), textWidth(CLEAR_CUSTOMS_LABEL)) + 12;
        MutableComponent randomScriptLabel = Component.translatable("gui.blood-on-the-blocktower.script_builder.random_script");
        MutableComponent clearLabel = Component.translatable("gui.blood-on-the-blocktower.script_builder.clear");
        boolean roomy = this.width - 2 * padding - rightGroup - 3 * spacing
                >= textWidth(importLabel(true)) + customsWidth + textWidth(randomScriptLabel) + textWidth(clearLabel) + 36;
        MutableComponent importText = importLabel(roomy);
        MutableComponent randomLabel = roomy ? randomScriptLabel : Component.translatable("gui.blood-on-the-blocktower.script_builder.random_script_short");
        if (!roomy) {
            customsWidth = Math.max(textWidth(IMPORT_CUSTOMS_SHORT), textWidth(CLEAR_CUSTOMS_SHORT)) + 12;
        }
        this.customsRoomy = roomy;

        int buttonX = padding;
        // Label, colour and tooltip are swapped by shift in render(), and the press reads shift too.
        int importWidth = Math.max(textWidth(importText), textWidth(exportLabel(roomy))) + 12;
        this.importRoomy = roomy;
        this.importButton = this.addRenderableWidget(Button.builder(importText, button -> {
            if (Minecraft.getInstance().hasShiftDown()) {
                exportScript();
            } else {
                importScript();
            }
        }).bounds(buttonX, buttonY, importWidth, buttonHeight).build());
        this.importShowingExport = !Minecraft.getInstance().hasShiftDown(); // force the first update in render()
        buttonX += importWidth + spacing;

        // Label, colour and tooltip are swapped by shift in render(), and the press reads shift too.
        this.customsButton = this.addRenderableWidget(Button.builder(
                Component.empty(),
                button -> {
                    if (Minecraft.getInstance().hasShiftDown()) {
                        clearCustomRoleLibrary();
                    } else {
                        importCustomRoles();
                    }
                }
        ).bounds(buttonX, buttonY, customsWidth, buttonHeight).build());
        this.customsShowingClear = !Minecraft.getInstance().hasShiftDown(); // force the first update in render()
        buttonX += customsWidth + spacing;

        buttonX += addFooterButton(randomLabel.withStyle(ChatFormatting.AQUA), buttonX, buttonY, buttonHeight,
                Component.translatable("gui.blood-on-the-blocktower.script_builder.tooltip.random_script",
                        FULL_COUNTS[0], FULL_COUNTS[1], FULL_COUNTS[2], FULL_COUNTS[3],
                        TEENSYVILLE_COUNTS[0], TEENSYVILLE_COUNTS[1], TEENSYVILLE_COUNTS[2], TEENSYVILLE_COUNTS[3])
                        .append(Component.translatable("gui.blood-on-the-blocktower.script_builder.tooltip.random_script_ban_all")
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)),
                button -> {
                    if (Minecraft.getInstance().hasControlDown()) {
                        toggleBanAll();
                    } else {
                        randomScript(Minecraft.getInstance().hasShiftDown());
                    }
                }) + spacing;

        addFooterButton(clearLabel.withStyle(ChatFormatting.RED), buttonX, buttonY, buttonHeight,
                Component.translatable("gui.blood-on-the-blocktower.script_builder.tooltip.clear"),
                button -> clearBuilder());

        this.saveButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.blood-on-the-blocktower.script_builder.save").withStyle(ChatFormatting.GREEN),
                button -> save()
        ).bounds(this.width - padding - backWidth - spacing - saveWidth, buttonY, saveWidth, buttonHeight)
        .tooltip(Tooltip.create(Component.translatable("gui.blood-on-the-blocktower.script_builder.tooltip.save")))
        .build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.blood-on-the-blocktower.script_builder.back").withStyle(ChatFormatting.YELLOW),
                button -> this.onClose()
        ).bounds(this.width - padding - backWidth, buttonY, backWidth, buttonHeight).build());
    }

    private int textWidth(Component text) {
        return this.font.width(text);
    }

    private static MutableComponent importLabel(boolean roomy) {
        return Component.translatable(roomy
                ? "gui.blood-on-the-blocktower.script_builder.import_script"
                : "gui.blood-on-the-blocktower.script_builder.import_script_short");
    }

    private static MutableComponent exportLabel(boolean roomy) {
        return Component.translatable(roomy
                ? "gui.blood-on-the-blocktower.script_builder.export_script"
                : "gui.blood-on-the-blocktower.script_builder.export_script_short");
    }

    /** Adds a footer button sized to its label, and returns that width. */
    private int addFooterButton(Component label, int x, int y, int height, Component tooltip, Button.OnPress action) {
        int width = textWidth(label) + 12;
        this.addRenderableWidget(Button.builder(label, action)
                .bounds(x, y, width, height)
                .tooltip(Tooltip.create(tooltip))
                .build());
        return width;
    }

    // ================================================================
    // List population
    // ================================================================

    /**
     * Rebuild the palette's backing list: every official character in enum order (which is
     * already grouped townsfolk → outsider → minion → demon → traveler → fabled → loric, the
     * order the catalog relies on), with library and script-local homebrew appended to the end
     * of their own team's section.
     */
    private void rebuildPaletteSource() {
        Map<RoleType, List<ScriptRole>> customsByTeam = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();

        for (CustomRoleLibrary.Entry entry : CustomRoleLibrary.all()) {
            if (seen.add(entry.normalizedId())) {
                customsByTeam.computeIfAbsent(entry.role().getTeam(), t -> new ArrayList<>()).add(entry.role());
            }
        }
        // Homebrew seen this session but never saved to the library still needs a palette slot,
        // otherwise taking it off the script would make it unrecoverable.
        for (ScriptRole role : knownCustoms.values()) {
            if (seen.add(ScriptJson.normalizeId(role.getId()))) {
                customsByTeam.computeIfAbsent(role.getTeam(), t -> new ArrayList<>()).add(role);
            }
        }

        List<ScriptRole> source = new ArrayList<>();
        for (RoleType team : SECTION_ORDER) {
            for (Role role : Role.values()) {
                if (role != Role.NO_ROLE && role.getType() == team) {
                    source.add(new ScriptRole.Official(role));
                }
            }
            source.addAll(customsByTeam.getOrDefault(team, List.of()));
        }
        this.paletteSource = source;
    }

    private void refreshScriptList() {
        if (scriptList == null) return;
        double scroll = scriptList.scrollAmount();
        scriptList.populate(ordered());
        scriptList.setScrollAmount(scroll);
    }

    private void refreshPalette() {
        if (palette == null) return;
        String query = searchField == null ? "" : searchField.getValue().toLowerCase(Locale.ROOT);
        List<ScriptRole> filtered = paletteSource.stream()
                .filter(role -> role.getDisplayName().toLowerCase(Locale.ROOT).contains(query)
                        || role.getTeam().getDisplayName().toLowerCase(Locale.ROOT).contains(query))
                .collect(Collectors.toList());

        // Adding or removing a character shouldn't yank the palette back to the top. Only a
        // changed search term justifies losing your place.
        boolean queryChanged = !query.equals(lastQuery);
        lastQuery = query;

        double scroll = palette.scrollAmount();
        palette.populate(filtered);
        palette.setScrollAmount(queryChanged ? 0 : scroll);
    }

    private void refreshBoth() {
        rebuildPaletteSource();
        refreshScriptList();
        refreshPalette();
    }

    // ================================================================
    // Actions
    // ================================================================

    private void toggleRole(ScriptRole role) {
        String key = ScriptJson.normalizeId(role.getId());
        if (contains(key)) {
            working.removeIf(r -> ScriptJson.normalizeId(r.getId()).equals(key));
        } else {
            addFromPalette(role);
        }
        refreshBoth();
    }

    /** Only the four playable teams are drawn randomly, so only those can be banned. */
    private static boolean isBannable(ScriptRole role) {
        for (RoleType team : RANDOM_TEAMS) {
            if (role.getTeam() == team) return true;
        }
        return false;
    }

    /** The mirror of {@link #toggleRole}: banning takes a character off the script. */
    private void toggleBan(ScriptRole role) {
        if (!isBannable(role)) return;

        String key = ScriptJson.normalizeId(role.getId());
        if (RandomBanList.isBanned(key)) {
            RandomBanList.setBanned(key, false);
        } else {
            RandomBanList.setBanned(key, true);
            working.removeIf(r -> ScriptJson.normalizeId(r.getId()).equals(key));
        }
        refreshBoth();
    }

    /**
     * Ban every drawable character not on the script, or lift those bans if they're all banned
     * already. Characters on the script are ignored by both the test and the write, so clear the
     * script first to bar those too. Travelers, fabled and loric aren't drawn randomly, so they're
     * untouched either way.
     */
    private void toggleBanAll() {
        rebuildPaletteSource();
        List<String> bannable = paletteSource.stream()
                .filter(ScriptBuilderScreen::isBannable)
                .map(role -> ScriptJson.normalizeId(role.getId()))
                .filter(key -> !contains(key))
                .collect(Collectors.toList());

        boolean allBanned = bannable.stream().allMatch(RandomBanList::isBanned);
        RandomBanList.setBannedAll(bannable, !allBanned);
        refreshBoth();
    }

    /**
     * Put a palette character on the script, picking up its raw definition and almanac URLs from
     * the library on the way, because without them a homebrew pick would save as a bare id the script
     * can't resolve.
     */
    private void addFromPalette(ScriptRole role) {
        String key = ScriptJson.normalizeId(role.getId());
        CustomRoleLibrary.get(key).ifPresent(entry -> {
            definitions.put(key, entry.definition());
            if (!entry.almanacs().isEmpty()) {
                almanacSources.put(key, entry.almanacs());
            }
        });
        addRole(role, almanacSources.get(key));
    }

    /** Drop a specific character from the library. */
    private void forgetLibraryRole(ScriptRole role) {
        String key = ScriptJson.normalizeId(role.getId());
        if (!CustomRoleLibrary.remove(key)) return;
        refreshBoth();
    }

    /**
     * Empty the builder. Homebrew definitions, almanac sources and {@link #knownCustoms} survive
     * so everything stays re-addable from the palette, and only the script itself goes.
     *
     * <p>The active script is untouched until Save, and saving an empty builder is what removes
     * it, which is where the grimoire's old X button went.
     */
    private void clearBuilder() {
        working.clear();
        baseLogo = null;
        baseAlmanac = null;
        baseExtraAlmanacs = new ArrayList<>();
        baseBootlegger = new ArrayList<>();
        baseFirstNightOrder = null;
        baseOtherNightOrder = null;
        scriptName = "";
        scriptAuthor = defaultAuthor();
        nameField.setValue("");
        authorField.setValue(scriptAuthor);
        refreshBoth();
    }

    /**
     * Replace the script with a random draw from every official character plus the custom role
     * library. Nothing is confirmed, because the builder is scratch space and Save is the commit.
     *
     * @param teensyville roll the smaller 6/2/2/2 line-up instead of the standard 13/4/4/4
     */
    private void randomScript(boolean teensyville) {
        int[] counts = teensyville ? TEENSYVILLE_COUNTS : FULL_COUNTS;

        // Travelers, fabled and loric are left out: a script's line-up is the four playable teams.
        rebuildPaletteSource();
        working.clear();
        baseLogo = null;
        baseAlmanac = null;
        baseExtraAlmanacs = new ArrayList<>();
        baseBootlegger = new ArrayList<>();
        baseFirstNightOrder = null;
        baseOtherNightOrder = null;

        for (int i = 0; i < RANDOM_TEAMS.length; i++) {
            RoleType team = RANDOM_TEAMS[i];
            List<ScriptRole> pool = paletteSource.stream()
                    .filter(role -> role.getTeam() == team)
                    .filter(role -> !RandomBanList.isBanned(role.getId()))
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.shuffle(pool);

            // Bounded by the pool, which the official character set already dwarfs. A filtered
            // pool that couldn't fill the quota would just yield a shorter draw.
            int take = Math.min(counts[i], pool.size());
            for (int j = 0; j < take; j++) {
                addFromPalette(pool.get(j));
            }
        }

        scriptName = Component.translatable(teensyville
                ? "gui.blood-on-the-blocktower.script_builder.random_teensyville_name"
                : "gui.blood-on-the-blocktower.script_builder.random_script_name").getString();
        nameField.setValue(scriptName);
        refreshBoth();
    }

    private void clearCustomRoleLibrary() {
        int removed = CustomRoleLibrary.clear();
        // Clearing an already-empty library changes nothing, so it says nothing.
        if (removed == 0) return;

        message(Component.translatable(removed == 1
                ? "gui.blood-on-the-blocktower.script_builder.cleared_custom_role"
                : "gui.blood-on-the-blocktower.script_builder.cleared_custom_roles", removed)
                .withStyle(ChatFormatting.YELLOW));
        refreshBoth();
    }

    private void importScript() {
        Optional<Script> parsed = readClipboardScript();
        if (parsed.isEmpty()) return;

        seedFrom(parsed.get());
        refreshBoth();
    }

    private void exportScript() {
        Script script = ClientState.currentScript;
        if (script == null || script.rawJson() == null || script.rawJson().isEmpty()) {
            message(Component.translatable("gui.blood-on-the-blocktower.script_builder.no_script_to_export").withStyle(ChatFormatting.RED));
            return;
        }
        this.minecraft.keyboardHandler.setClipboard(script.rawJson());
        message(Component.translatable("gui.blood-on-the-blocktower.script_builder.script_copied").withStyle(ChatFormatting.GREEN));
    }

    private void importCustomRoles() {
        Optional<Script> parsed = readClipboardScript();
        if (parsed.isEmpty()) return;

        int[] counts = CustomRoleLibrary.importFrom(parsed.get());
        int added = counts[0];
        int updated = counts[1];
        if (added == 0 && updated == 0) {
            message(Component.translatable("gui.blood-on-the-blocktower.script_builder.no_homebrew_found").withStyle(ChatFormatting.YELLOW));
            return;
        }

        // Definitions and almanacs for these characters are now available to the builder too,
        // so a character added from the palette right after this import saves correctly.
        for (CustomRoleLibrary.Entry entry : CustomRoleLibrary.all()) {
            definitions.put(entry.normalizedId(), entry.definition());
            if (!entry.almanacs().isEmpty()) {
                almanacSources.putIfAbsent(entry.normalizedId(), entry.almanacs());
            }
        }

        refreshBoth();
        MutableComponent summary;
        if (updated == 0) {
            summary = Component.translatable(added == 1
                    ? "gui.blood-on-the-blocktower.script_builder.added_custom_role"
                    : "gui.blood-on-the-blocktower.script_builder.added_custom_roles", added);
        } else {
            summary = Component.translatable(added + updated == 1
                    ? "gui.blood-on-the-blocktower.script_builder.added_updated_custom_role"
                    : "gui.blood-on-the-blocktower.script_builder.added_updated_custom_roles", added, updated);
        }
        message(summary.withStyle(ChatFormatting.GREEN));
    }

    private Optional<Script> readClipboardScript() {
        String clipboardText = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clipboardText == null || clipboardText.isEmpty()) {
            message(Component.translatable("gui.blood-on-the-blocktower.script_builder.clipboard_empty").withStyle(ChatFormatting.RED));
            return Optional.empty();
        }
        Optional<Script> parsed = Script.fromJson(clipboardText);
        if (parsed.isEmpty()) {
            message(Component.translatable("gui.blood-on-the-blocktower.script_builder.invalid_script").withStyle(ChatFormatting.RED));
        }
        return parsed;
    }

    // ================================================================
    // Save
    // ================================================================

    private void save() {
        List<ScriptRole> ordered = ordered();
        boolean removing = ordered.isEmpty();

        // Script.fromJson rejects a document with no townsfolk/outsider/minion/demon, so a
        // travelers-and-fabled-only script would parse back as empty and silently do nothing.
        // A completely empty builder is a different intent, since that removes the active script.
        boolean hasPlayableRole = ordered.stream().anyMatch(role -> switch (role.getTeam()) {
            case TOWNSFOLK, OUTSIDER, MINION, DEMON -> true;
            default -> false;
        });
        if (!removing && !hasPlayableRole) {
            message(Component.translatable("gui.blood-on-the-blocktower.script_builder.no_playable_role")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        List<String> orphaned = findOrphanedAssignments(ordered);
        if (orphaned.isEmpty()) {
            apply(ordered);
            return;
        }

        Component warning = Component.translatable("gui.blood-on-the-blocktower.script_builder.orphaned_warning",
                String.join(", ", orphaned));
        this.minecraft.gui.setScreen(new ConfirmScreen(
                confirmed -> {
                    this.minecraft.gui.setScreen(this);
                    if (confirmed) apply(ordered);
                },
                Component.translatable("gui.blood-on-the-blocktower.script_builder.save_script_confirm").withStyle(ChatFormatting.YELLOW),
                warning));
    }

    private void apply(List<ScriptRole> ordered) {
        if (ordered.isEmpty()) {
            removeActiveScript();
        } else {
            commit(ordered);
        }
    }

    /** Saving an empty builder clears the storyteller's script, the way the grimoire's X used to. */
    private void removeActiveScript() {
        ClientState.currentScript = null;
        StorytellerState.syncGrimoire();
        captureBaseline();

        message(Component.translatable("gui.blood-on-the-blocktower.script_builder.removed_active_script").withStyle(ChatFormatting.YELLOW));
    }

    /**
     * Display names of homebrew characters assigned to players or used as demon bluffs that the
     * script being saved no longer contains. Those resolve their name, icon and ability against
     * {@code ClientState.currentScript}, so dropping one in use leaves a dangling placeholder.
     *
     * <p>Official roles are deliberately not reported: they carry everything they need in the
     * {@link Role} enum and keep rendering correctly whatever the script says, so warning about
     * them would just be noise on every edit.
     */
    private List<String> findOrphanedAssignments(List<ScriptRole> ordered) {
        Set<String> keptIds = ordered.stream()
                .map(role -> ScriptJson.normalizeId(role.getId()))
                .collect(Collectors.toSet());

        List<String> orphaned = new ArrayList<>();
        for (Map.Entry<UUID, PendingRoleAssignment> entry : StorytellerState.PENDING_ROLES.entrySet()) {
            PendingRoleAssignment assignment = entry.getValue();
            if (assignment == null || !assignment.isCustomRole()) continue;
            if (!keptIds.contains(ScriptJson.normalizeId(assignment.getRoleId()))
                    && !orphaned.contains(assignment.getDisplayName())) {
                orphaned.add(assignment.getDisplayName());
            }
        }
        for (ScriptRole bluff : StorytellerState.DEMON_BLUFFS) {
            if (bluff == null || !bluff.isCustom()) continue;
            if (!keptIds.contains(ScriptJson.normalizeId(bluff.getId()))
                    && !orphaned.contains(bluff.getDisplayName())) {
                orphaned.add(bluff.getDisplayName());
            }
        }
        return orphaned;
    }

    private void commit(List<ScriptRole> ordered) {
        // The main slot only ever holds an almanac the script genuinely has, the one the seeding
        // script declared. It's what carries script-level synopsis, overview and changelog, and
        // what gates the script reference's details screen, so a script assembled out of borrowed
        // characters must not claim one.
        String mainAlmanac = baseAlmanac != null && !baseAlmanac.isBlank() ? baseAlmanac : null;

        // Every almanac the included homebrew came from goes into extras, which carry role data
        // only. Players receiving this script have no custom role library of their own, so these
        // URLs are the only way their character details resolve.
        List<String> extraAlmanacs = new ArrayList<>();
        for (String url : baseExtraAlmanacs) {
            if (isNewAlmanac(url, mainAlmanac, extraAlmanacs)) extraAlmanacs.add(url);
        }
        for (ScriptRole role : ordered) {
            if (!role.isCustom()) continue;
            for (String url : almanacSources.getOrDefault(ScriptJson.normalizeId(role.getId()), List.of())) {
                if (isNewAlmanac(url, mainAlmanac, extraAlmanacs)) extraAlmanacs.add(url);
            }
        }

        ScriptJson.Meta meta = new ScriptJson.Meta(
                nameField.getValue().isBlank()
                        ? Component.translatable("gui.blood-on-the-blocktower.script_builder.default_script_name").getString()
                        : nameField.getValue(),
                authorField.getValue(),
                baseLogo,
                mainAlmanac,
                extraAlmanacs,
                baseBootlegger,
                prunedNightOrder(baseFirstNightOrder, ordered),
                prunedNightOrder(baseOtherNightOrder, ordered)
        );

        String json = ScriptJson.build(meta, ordered, definitions);
        Optional<Script> built = Script.fromJson(json);
        if (built.isEmpty()) {
            message(Component.translatable("gui.blood-on-the-blocktower.script_builder.build_failed").withStyle(ChatFormatting.RED));
            return;
        }

        Script script = built.get();
        ClientState.currentScript = script;

        ClientReceive.onScriptLoaded(script);
        StorytellerState.syncGrimoire();
        captureBaseline();

        message(Component.translatable("gui.blood-on-the-blocktower.script_builder.saved", script.name(), ordered.size())
                .withStyle(ChatFormatting.GREEN));
    }

    private static boolean isNewAlmanac(String url, String main, List<String> collected) {
        return url != null && !url.isBlank() && !url.equals(main) && !collected.contains(url);
    }

    /**
     * Drop characters that are no longer on the script from a carried-over {@code _meta} night
     * order, keeping everything else. Those arrays also hold pseudo-entries ("dusk", "dawn",
     * "minioninfo"), so anything not recognisable as a character is left alone rather than
     * filtered out for failing a membership test it was never going to pass.
     *
     * <p>Nothing reads these arrays today (the night order is built from {@code NightOrder} and
     * each custom role's own priorities), but shipping a stale list in a script we authored
     * would be wrong data waiting to be believed.
     */
    private List<String> prunedNightOrder(List<String> order, List<ScriptRole> ordered) {
        if (order == null || order.isEmpty()) return order;

        Set<String> keptIds = ordered.stream()
                .map(role -> ScriptJson.normalizeId(role.getId()))
                .collect(Collectors.toSet());

        List<String> pruned = new ArrayList<>();
        for (String id : order) {
            String key = ScriptJson.normalizeId(id);
            boolean isCharacter = ScriptJson.findOfficialRole(id) != null || definitions.containsKey(key);
            if (!isCharacter || keptIds.contains(key)) {
                pruned.add(id);
            }
        }
        return pruned;
    }

    private void message(Component text) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.sendSystemMessage(text);
        }
    }

    // ================================================================
    // Screen plumbing
    // ================================================================

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        boolean handled = super.mouseClicked(event, doubleClick);
        if (pendingAction != null) {
            Runnable action = pendingAction;
            pendingAction = null;
            action.run();
        }
        return handled;
    }

    @Override
    public void onClose() {
        if (discardConfirmed || !isDirty()) {
            this.minecraft.gui.setScreen(parent);
            return;
        }
        this.minecraft.gui.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        discardConfirmed = true;
                        this.minecraft.gui.setScreen(parent);
                    } else {
                        this.minecraft.gui.setScreen(this);
                    }
                },
                Component.translatable("gui.blood-on-the-blocktower.script_builder.discard_changes").withStyle(ChatFormatting.YELLOW),
                Component.translatable("gui.blood-on-the-blocktower.script_builder.discard_changes_message")));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Cleared before the children draw, and a hovered entry re-queues it during super.render().
        this.hoverTooltip = null;

        // Greyed out until there's something to commit, which also covers "empty builder, no
        // active script" without needing a message to explain it.
        if (this.saveButton != null) {
            this.saveButton.active = isDirty();
        }

        if (this.importButton != null && Minecraft.getInstance().hasShiftDown() != this.importShowingExport) {
            this.importShowingExport = Minecraft.getInstance().hasShiftDown();
            if (this.importShowingExport) {
                this.importButton.setMessage(exportLabel(importRoomy).withStyle(ChatFormatting.AQUA));
                this.importButton.setTooltip(Tooltip.create(Component.translatable("gui.blood-on-the-blocktower.script_builder.tooltip.export_script")));
            } else {
                this.importButton.setMessage(importLabel(importRoomy));
                this.importButton.setTooltip(Tooltip.create(Component.translatable("gui.blood-on-the-blocktower.script_builder.tooltip.import_script")
                        .append(Component.translatable("gui.blood-on-the-blocktower.script_builder.tooltip.import_script_shift")
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC))));
            }
        }
        if (this.importButton != null) {
            this.importButton.active = !this.importShowingExport || ClientState.currentScript != null;
        }

        // Only rebuilt when shift actually changes, so the tooltip object isn't reallocated
        // every frame.
        if (this.customsButton != null && Minecraft.getInstance().hasShiftDown() != this.customsShowingClear) {
            this.customsShowingClear = Minecraft.getInstance().hasShiftDown();
            if (this.customsShowingClear) {
                this.customsButton.setMessage((customsRoomy ? CLEAR_CUSTOMS_LABEL : CLEAR_CUSTOMS_SHORT).copy()
                        .withStyle(ChatFormatting.RED));
                this.customsButton.setTooltip(Tooltip.create(Component.translatable(
                        "gui.blood-on-the-blocktower.script_builder.tooltip.clear_custom_roles")));
            } else {
                this.customsButton.setMessage((customsRoomy ? IMPORT_CUSTOMS_LABEL : IMPORT_CUSTOMS_SHORT).copy()
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                this.customsButton.setTooltip(Tooltip.create(Component.translatable(
                        "gui.blood-on-the-blocktower.script_builder.tooltip.import_custom_roles")
                        .append(Component.translatable("gui.blood-on-the-blocktower.script_builder.tooltip.import_custom_roles_shift")
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC))));
            }
        }

        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(this.font, this.title, this.width / 2, 6, 0xFFFFFFFF);

        // Counts under the script list, so the composition is visible while editing. Each is
        // tinted with its team colour, and traveler gets two letters so it isn't confused with
        // townsfolk.
        int countX = 10;
        for (RoleType team : SECTION_ORDER) {
            long n = working.stream().filter(role -> role.getTeam() == team).count();
            if (n == 0) continue;
            Component label = Component.literal(abbreviate(team) + n).withStyle(s -> s.withColor(team.getColor()));
            context.text(this.font, label, countX, this.height - 44, 0xFFFFFFFF);
            countX += this.font.width(label) + 6;
        }

        if (this.hoverTooltip != null) {
            context.setComponentTooltipForNextFrame(this.font, this.hoverTooltip, this.hoverTooltipX, this.hoverTooltipY);
        }
    }

    private static String abbreviate(RoleType team) {
        return team.getShortName();
    }

    private void queueTooltip(List<Component> lines, int mouseX, int mouseY) {
        this.hoverTooltip = lines;
        this.hoverTooltipX = mouseX;
        this.hoverTooltipY = mouseY;
    }

    /**
     * Wrap an ability into tooltip lines. The wrap width shrinks on narrow screens so a long
     * ability can't be pushed off the edge by the tooltip positioner.
     */
    private List<Component> tooltipLines(String body, List<Component> extra) {
        int wrapWidth = Math.max(120, Math.min(TOOLTIP_WIDTH, this.width - 40));
        List<Component> lines = this.font.getSplitter()
                .splitLines(body == null ? "" : body, wrapWidth, Style.EMPTY)
                .stream()
                .map((FormattedText line) -> (Component) Component.literal(line.getString()).withStyle(ChatFormatting.YELLOW))
                .collect(Collectors.toList());
        lines.addAll(extra);
        return lines;
    }

    // ================================================================
    // Left pane: the script
    // ================================================================

    private class ScriptListWidget extends ContainerObjectSelectionList<ScriptListWidget.Entry> {

        ScriptListWidget(Minecraft client, int width, int height, int y) {
            super(client, width, height, y, 24);
        }

        void populate(List<ScriptRole> roles) {
            this.clearEntries();
            if (roles.isEmpty()) {
                this.addEntry(new InfoEntry(Component.translatable("gui.blood-on-the-blocktower.script_builder.empty_hint")));
                return;
            }
            RoleType currentTeam = null;
            for (ScriptRole role : roles) {
                if (role.getTeam() != currentTeam) {
                    currentTeam = role.getTeam();
                    final RoleType team = currentTeam;
                    this.addEntry(new SectionEntry(
                            Component.literal(team.getDisplayName()).withStyle(s -> s.withColor(team.getColor()).withBold(true))));
                }
                this.addEntry(new RoleEntry(role));
            }
        }

        @Override public int getRowWidth() { return this.width - 12; }
        @Override protected int scrollBarX() { return this.getX() + this.width - 6; }

        /**
         * Entries at the top and bottom of the list are drawn partially outside the pane, so
         * their own hit test isn't enough, so the mouse has to be inside the visible viewport too.
         */
        boolean withinViewport(double mouseX, double mouseY) {
            return mouseX >= this.getX() && mouseX < this.getX() + this.width
                    && mouseY >= this.getY() && mouseY < this.getY() + this.height;
        }

        abstract class Entry extends ContainerObjectSelectionList.Entry<com.autumnwind.botb.gui.ScriptBuilderScreen.ScriptListWidget.Entry> {
            @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
            @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
        }

        /** A wrapped, non-interactive message, used for the empty-script placeholder. */
        class InfoEntry extends com.autumnwind.botb.gui.ScriptBuilderScreen.ScriptListWidget.Entry {
            private final Component message;

            InfoEntry(Component message) {
                this.message = message;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int y = getContentY();

                int rowLeft = ScriptListWidget.this.getRowLeft();
                int lineY = y + 2;
                for (FormattedText line : font.getSplitter()
                        .splitLines(message, ScriptListWidget.this.getRowWidth() - 4, Style.EMPTY)) {
                    context.text(font,
                            Component.literal(line.getString()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC),
                            rowLeft + 2, lineY, 0xFFFFFFFF);
                    lineY += font.lineHeight + 1;
                }
            }
        }

        class SectionEntry extends com.autumnwind.botb.gui.ScriptBuilderScreen.ScriptListWidget.Entry {
            private final Component label;

            SectionEntry(Component label) {
                this.label = label;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int y = getContentY();
                int entryHeight = getContentHeight();

                context.text(font, label,
                        ScriptListWidget.this.getRowLeft() + 2,
                        y + entryHeight - font.lineHeight - 2, 0xFFFFFFFF);
            }
        }

        class RoleEntry extends com.autumnwind.botb.gui.ScriptBuilderScreen.ScriptListWidget.Entry {
            private final ScriptRole role;

            RoleEntry(ScriptRole role) {
                this.role = role;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int y = getContentY();
                int entryHeight = getContentHeight();

                int rowLeft = ScriptListWidget.this.getRowLeft();
                int rowWidth = ScriptListWidget.this.getRowWidth();
                int iconSize = 20;
                int iconX = rowLeft + 5;
                int iconY = y + (entryHeight - iconSize) / 2;

                boolean isMouseOver = mouseX >= rowLeft && mouseX < rowLeft + rowWidth
                        && mouseY >= y && mouseY < y + entryHeight
                        && ScriptListWidget.this.withinViewport(mouseX, mouseY);

                if (isMouseOver) {
                    context.fill(rowLeft, y, rowLeft + rowWidth, y + entryHeight, 0x30FFFFFF);
                }

                context.blit(RenderPipelines.GUI_TEXTURED, role.getIcon(), iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
                context.outline(iconX - 1, iconY - 1, iconSize + 2, iconSize + 2, role.getTeam().getColor());

                int textX = iconX + iconSize + 6;
                int textY = y + (entryHeight - font.lineHeight) / 2;
                Component name = Component.literal(role.getDisplayName()).withStyle(s -> s.withColor(role.getTeam().getColor()));
                context.text(font,
                        trimToWidth(name, rowWidth - (textX - rowLeft) - 4), textX, textY, 0xFFFFFFFF);

                if (isMouseOver) {
                    List<Component> hints = new ArrayList<>();
                    if (AbilityText.isBootlegger(role)) {
                        hints.add(Component.translatable("gui.blood-on-the-blocktower.script_builder.tooltip.edit_special_rules").withStyle(ChatFormatting.AQUA));
                    }
                    queueTooltip(tooltipLines(AbilityText.of(role, baseBootlegger), hints), mouseX, mouseY);
                }
            }

            private Component trimToWidth(Component text, int maxWidth) {
                String raw = text.getString();
                if (font.width(raw) <= maxWidth) return text;
                return Component.literal(font.plainSubstrByWidth(raw, Math.max(0, maxWidth - 6)) + "...")
                        .setStyle(text.getStyle());
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                int button = event.button();

                if (button != GLFW.GLFW_MOUSE_BUTTON_1) return false;
                if (Minecraft.getInstance().hasControlDown() && AbilityText.isBootlegger(role)) {
                    pendingAction = () -> minecraft.gui.setScreen(new BootleggerRulesScreen(ScriptBuilderScreen.this, baseBootlegger));
                } else if (Minecraft.getInstance().hasShiftDown()) {
                    pendingAction = () -> minecraft.gui.setScreen(new CharacterDetailsScreen(role, ScriptBuilderScreen.this));
                } else {
                    pendingAction = () -> toggleRole(role);
                }
                return true;
            }
        }
    }

    // ================================================================
    // Right pane: the palette
    // ================================================================

    private class PaletteWidget extends ContainerObjectSelectionList<PaletteWidget.GridEntry> {

        private final int columns;

        PaletteWidget(Minecraft client, int width, int height, int y, int columns) {
            super(client, width, height, y, 70);
            this.columns = columns;
        }

        void populate(List<ScriptRole> roles) {
            this.clearEntries();
            for (int i = 0; i < roles.size(); i += columns) {
                this.addEntry(new GridEntry(roles.subList(i, Math.min(i + columns, roles.size()))));
            }
        }

        @Override public int getRowWidth() { return columns * CELL_WIDTH; }
        @Override protected int scrollBarX() { return this.getX() + this.width - 6; }

        /** See {@link ScriptListWidget#withinViewport}. */
        boolean withinViewport(double mouseX, double mouseY) {
            return mouseX >= this.getX() && mouseX < this.getX() + this.width
                    && mouseY >= this.getY() && mouseY < this.getY() + this.height;
        }

        class GridEntry extends ContainerObjectSelectionList.Entry<GridEntry> {
            private final List<ScriptRole> rolesInRow;
            private int entryY;

            GridEntry(List<ScriptRole> roles) {
                this.rolesInRow = new ArrayList<>(roles);
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX();
                int y = getContentY();

                this.entryY = y;

                for (int i = 0; i < rolesInRow.size(); i++) {
                    ScriptRole role = rolesInRow.get(i);
                    int cellX = x + i * CELL_WIDTH;
                    int borderX = cellX + (CELL_WIDTH - ICON_SIZE) / 2;
                    int iconY = y + 5;
                    boolean onScript = contains(ScriptJson.normalizeId(role.getId()));
                    // Mutually exclusive: anything reaching the script is unbarred by addRole,
                    // and barring takes a character off the script.
                    boolean banned = RandomBanList.isBanned(role.getId());

                    boolean isMouseOver = mouseX >= borderX && mouseX < borderX + ICON_SIZE
                            && mouseY >= iconY && mouseY < iconY + ICON_SIZE
                            && PaletteWidget.this.withinViewport(mouseX, mouseY);

                    context.outline(borderX, iconY, ICON_SIZE, ICON_SIZE, role.getTeam().getColor());
                    context.blit(RenderPipelines.GUI_TEXTURED, role.getIcon(), borderX + 1, iconY + 1, 0, 0, 38, 38, 38, 38);

                    // Already-on-script entries stay visible but read as taken, and clicking one
                    // removes it, so the palette never disagrees with the list beside it.
                    if (onScript) {
                        context.fill(borderX + 1, iconY + 1, borderX + ICON_SIZE - 1, iconY + ICON_SIZE - 1, 0xA0000000);
                        // Escaped rather than a literal check glyph: the build sets no source
                        // encoding, so javac reads sources in the platform default charset.
                        // The glyph comes from the unicode font page, whose strokes are thinner
                        // than the ASCII X below, so it's drawn a second time a pixel across to
                        // carry the same weight.
                        Component check = Component.literal("\u2713").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
                        int checkY = iconY + ICON_SIZE / 2 - 4;
                        context.centeredText(font, check, borderX + ICON_SIZE / 2, checkY, 0xFFFFFFFF);
                        context.centeredText(font, check, borderX + ICON_SIZE / 2 + 1, checkY, 0xFFFFFFFF);
                    }

                    // Excluded from the random draw. Plain ASCII rather than a cross glyph, for
                    // the same source-encoding reason as the check above.
                    if (banned) {
                        context.fill(borderX + 1, iconY + 1, borderX + ICON_SIZE - 1, iconY + ICON_SIZE - 1, 0xA0000000);
                        context.centeredText(font,
                                Component.literal("X").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                                borderX + ICON_SIZE / 2, iconY + ICON_SIZE / 2 - 4, 0xFFFFFFFF);
                    }

                    int textCenterX = borderX + ICON_SIZE / 2;
                    String name = role.getDisplayName();
                    int wrapWidth = name.contains(" ") ? CELL_WIDTH - 4 : CELL_WIDTH + 1;
                    ChatFormatting nameColor = onScript || banned ? ChatFormatting.DARK_GRAY : ChatFormatting.WHITE;
                    List<Component> lines = minecraft.font.getSplitter()
                            .splitLines(name, wrapWidth, Style.EMPTY)
                            .stream()
                            .map(line -> (Component) Component.literal(line.getString()).withStyle(nameColor))
                            .collect(Collectors.toList());
                    for (int j = 0; j < lines.size(); j++) {
                        context.centeredText(minecraft.font, lines.get(j),
                                textCenterX, y + 50 + j * minecraft.font.lineHeight, 0xFFFFFFFF);
                    }

                    if (isMouseOver) {
                        List<Component> extra = new ArrayList<>();
                        if (isBannable(role)) {
                            extra.add(Component.translatable(banned
                                    ? "gui.blood-on-the-blocktower.script_builder.tooltip.allow_in_random"
                                    : "gui.blood-on-the-blocktower.script_builder.tooltip.bar_from_random").withStyle(ChatFormatting.DARK_GRAY));
                        }
                        CustomRoleLibrary.get(role.getId()).ifPresent(entry -> {
                            if (!entry.sourceScript().isBlank()) {
                                extra.add(Component.translatable("gui.blood-on-the-blocktower.script_builder.tooltip.from_script", entry.sourceScript()).withStyle(ChatFormatting.DARK_AQUA));
                            }
                            extra.add(Component.translatable("gui.blood-on-the-blocktower.script_builder.tooltip.forget").withStyle(ChatFormatting.DARK_RED));
                        });
                        queueTooltip(tooltipLines(role.getAbility(), extra), mouseX, mouseY);
                    }
                }
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                double mouseX = event.x();
                double mouseY = event.y();
                int button = event.button();

                if (button != GLFW.GLFW_MOUSE_BUTTON_1) return false;
                int rowX = PaletteWidget.this.getRowLeft();

                for (int i = 0; i < rolesInRow.size(); i++) {
                    int cellX = rowX + i * CELL_WIDTH;
                    if (mouseX < cellX || mouseX >= cellX + CELL_WIDTH) continue;
                    if (mouseY < entryY || mouseY >= entryY + PaletteWidget.this.defaultEntryHeight) continue;

                    ScriptRole role = rolesInRow.get(i);
                    // Ctrl+Shift is checked first because Ctrl alone now means ban, so forgetting a
                    // library character moved onto the compound gesture.
                    if (Minecraft.getInstance().hasControlDown() && Minecraft.getInstance().hasShiftDown()) {
                        if (CustomRoleLibrary.contains(role.getId())) {
                            pendingAction = () -> forgetLibraryRole(role);
                        }
                    } else if (Minecraft.getInstance().hasShiftDown()) {
                        pendingAction = () -> minecraft.gui.setScreen(new CharacterDetailsScreen(role, ScriptBuilderScreen.this));
                    } else if (Minecraft.getInstance().hasControlDown()) {
                        if (isBannable(role)) {
                            pendingAction = () -> toggleBan(role);
                        }
                    } else {
                        pendingAction = () -> toggleRole(role);
                    }
                    return true;
                }
                return false;
            }

            @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
            @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
        }
    }
}
