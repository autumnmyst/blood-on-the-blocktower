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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.Clipboard;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
import net.minecraft.client.gui.tooltip.Tooltip;

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

    private static final String IMPORT_CUSTOMS_LABEL = "Import Custom Roles";
    private static final String CLEAR_CUSTOMS_LABEL = "Clear Custom Roles";
    private static final String IMPORT_CUSTOMS_SHORT = "Customs";
    private static final String CLEAR_CUSTOMS_SHORT = "Wipe";

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

    private TextFieldWidget nameField;
    private TextFieldWidget authorField;
    private TextFieldWidget searchField;
    private ScriptListWidget scriptList;
    private PaletteWidget palette;
    private ButtonWidget saveButton;

    /** Import/Clear custom roles share one button, swapped by shift. */
    private ButtonWidget customsButton;
    private boolean customsShowingClear;
    private boolean customsRoomy = true;

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
    private List<Text> hoverTooltip;
    private int hoverTooltipX;
    private int hoverTooltipY;

    public ScriptBuilderScreen(Screen parent) {
        super(Text.literal("Script Builder"));
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

        if (nameField != null) nameField.setText(scriptName);
        if (authorField != null) authorField.setText(scriptAuthor);
    }

    /**
     * Who a new script is by, until an imported script says otherwise. Uses the player's name as
     * it renders in game rather than the Mojang profile name, so a custom name set with
     * {@code /botb setName} is what ends up on the script.
     */
    private static String defaultAuthor() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null ? client.player.getName().getString() : "";
    }

    /** Declare the current builder contents to be what's already saved as the active script. */
    private void captureBaseline() {
        seededIds = workingIds();
        seededName = nameField != null ? nameField.getText() : scriptName;
        seededAuthor = authorField != null ? authorField.getText() : scriptAuthor;
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
        // A character on the script is never barred from random draws. Every route onto the
        // script runs through here (opening the builder, Import Script, the palette, a random
        // draw), so the bar is lifted in one place.
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
        if (nameField != null && !nameField.getText().equals(seededName)) return true;
        return authorField != null && !authorField.getText().equals(seededAuthor);
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

        String currentName = nameField != null ? nameField.getText() : scriptName;
        String currentAuthor = authorField != null ? authorField.getText() : scriptAuthor;
        String currentSearch = searchField != null ? searchField.getText() : "";

        // init() runs again every time we're re-shown (coming back from a character's details
        // page, or on a resize) and builds new list widgets, so the old ones' scroll positions
        // have to be carried over by hand or you land back at the top of the palette.
        double scriptScroll = scriptList != null ? scriptList.getScrollAmount() : 0;
        double paletteScroll = palette != null ? palette.getScrollAmount() : 0;

        this.nameField = new TextFieldWidget(this.textRenderer, padding, rowY, nameWidth, fieldHeight, Text.empty());
        this.nameField.setMaxLength(64);
        this.nameField.setPlaceholder(Text.literal("Script name").formatted(Formatting.DARK_GRAY));
        this.nameField.setText(currentName);
        this.addDrawableChild(this.nameField);

        this.authorField = new TextFieldWidget(this.textRenderer, padding + nameWidth + 4, rowY, authorWidth, fieldHeight, Text.empty());
        this.authorField.setMaxLength(64);
        this.authorField.setPlaceholder(Text.literal("Author").formatted(Formatting.DARK_GRAY));
        this.authorField.setText(currentAuthor);
        this.addDrawableChild(this.authorField);

        this.searchField = new TextFieldWidget(this.textRenderer, paletteX, rowY, paletteWidth, fieldHeight, Text.empty());
        this.searchField.setPlaceholder(Text.literal("Search characters").formatted(Formatting.DARK_GRAY));
        this.searchField.setText(currentSearch);
        this.searchField.setChangedListener(text -> refreshPalette());
        this.addDrawableChild(this.searchField);

        // --- Lists ---
        this.scriptList = new ScriptListWidget(this.client, scriptWidth, listHeight, listTop);
        this.scriptList.setX(padding);
        this.addDrawableChild(this.scriptList);

        this.palette = new PaletteWidget(this.client, paletteWidth, listHeight, listTop, columns);
        this.palette.setX(paletteX);
        this.addDrawableChild(this.palette);

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
        boolean roomy = this.width - 2 * padding - rightGroup - 3 * spacing
                >= textWidth("Import Script") + customsWidth + textWidth("Random Script") + textWidth("Clear") + 36;
        String importLabel = roomy ? "Import Script" : "Import";
        String randomLabel = roomy ? "Random Script" : "Random";
        if (!roomy) {
            customsWidth = Math.max(textWidth(IMPORT_CUSTOMS_SHORT), textWidth(CLEAR_CUSTOMS_SHORT)) + 12;
        }
        this.customsRoomy = roomy;

        int buttonX = padding;
        buttonX += addFooterButton(Text.literal(importLabel), buttonX, buttonY, buttonHeight,
                "Load the script JSON in your clipboard into the builder.",
                button -> importScript()) + spacing;

        // Label, colour and tooltip are swapped by shift in render(), and the press reads shift too.
        this.customsButton = this.addDrawableChild(ButtonWidget.builder(
                Text.empty(),
                button -> {
                    if (hasShiftDown()) {
                        clearCustomRoleLibrary();
                    } else {
                        importCustomRoles();
                    }
                }
        ).dimensions(buttonX, buttonY, customsWidth, buttonHeight).build());
        this.customsShowingClear = !hasShiftDown(); // force the first update in render()
        buttonX += customsWidth + spacing;

        buttonX += addFooterButton(Text.literal(randomLabel).formatted(Formatting.AQUA), buttonX, buttonY, buttonHeight,
                "Roll a random script: " + FULL_COUNTS[0] + "/" + FULL_COUNTS[1] + "/"
                        + FULL_COUNTS[2] + "/" + FULL_COUNTS[3] + "\nShift for Teensyville: "
                        + TEENSYVILLE_COUNTS[0] + "/" + TEENSYVILLE_COUNTS[1] + "/"
                        + TEENSYVILLE_COUNTS[2] + "/" + TEENSYVILLE_COUNTS[3] + "\nCtrl+Click ban/unban all",
                button -> {
                    if (hasControlDown()) {
                        toggleBanAll();
                    } else {
                        randomScript(hasShiftDown());
                    }
                }) + spacing;

        addFooterButton(Text.literal("Clear").formatted(Formatting.RED), buttonX, buttonY, buttonHeight,
                "Empty the builder.",
                button -> clearBuilder());

        this.saveButton = this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Save").formatted(Formatting.GREEN),
                button -> save()
        ).dimensions(this.width - padding - backWidth - spacing - saveWidth, buttonY, saveWidth, buttonHeight)
        .tooltip(Tooltip.of(Text.literal(
                "Lock this script in as your active script. Use Send Roles to send it to players.")))
        .build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Back").formatted(Formatting.YELLOW),
                button -> this.close()
        ).dimensions(this.width - padding - backWidth, buttonY, backWidth, buttonHeight).build());
    }

    private int textWidth(String text) {
        return this.textRenderer.getWidth(text);
    }

    /** Adds a footer button sized to its label, and returns that width. */
    private int addFooterButton(Text label, int x, int y, int height, String tooltip, ButtonWidget.PressAction action) {
        int width = textWidth(label.getString()) + 12;
        this.addDrawableChild(ButtonWidget.builder(label, action)
                .dimensions(x, y, width, height)
                .tooltip(Tooltip.of(Text.literal(tooltip)))
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
        double scroll = scriptList.getScrollAmount();
        scriptList.populate(ordered());
        scriptList.setScrollAmount(scroll);
    }

    private void refreshPalette() {
        if (palette == null) return;
        String query = searchField == null ? "" : searchField.getText().toLowerCase();
        List<ScriptRole> filtered = paletteSource.stream()
                .filter(role -> role.getDisplayName().toLowerCase().contains(query)
                        || role.getTeam().name().toLowerCase().contains(query))
                .collect(Collectors.toList());

        // Adding or removing a character shouldn't yank the palette back to the top. Only a
        // changed search term justifies losing your place.
        boolean queryChanged = !query.equals(lastQuery);
        lastQuery = query;

        double scroll = palette.getScrollAmount();
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
        nameField.setText("");
        authorField.setText(scriptAuthor);
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

        scriptName = teensyville ? "Random Teensyville" : "Random Script";
        nameField.setText(scriptName);
        refreshBoth();
    }

    private void clearCustomRoleLibrary() {
        int removed = CustomRoleLibrary.clear();
        // Clearing an already-empty library changes nothing, so it says nothing.
        if (removed == 0) return;

        message(Text.literal("Cleared " + removed + " custom role" + (removed == 1 ? "" : "s")
                + " from your library").formatted(Formatting.YELLOW));
        refreshBoth();
    }

    private void importScript() {
        Optional<Script> parsed = readClipboardScript();
        if (parsed.isEmpty()) return;

        seedFrom(parsed.get());
        refreshBoth();
    }

    private void importCustomRoles() {
        Optional<Script> parsed = readClipboardScript();
        if (parsed.isEmpty()) return;

        int[] counts = CustomRoleLibrary.importFrom(parsed.get());
        int added = counts[0];
        int updated = counts[1];
        if (added == 0 && updated == 0) {
            message(Text.literal("No homebrew characters found in that script").formatted(Formatting.YELLOW));
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
        String summary = updated == 0
                ? "Added " + added + " custom role" + (added == 1 ? "" : "s")
                : "Added " + added + ", updated " + updated + " custom role" + (added + updated == 1 ? "" : "s");
        message(Text.literal(summary).formatted(Formatting.GREEN));
    }

    private Optional<Script> readClipboardScript() {
        String clipboardText = new Clipboard().getClipboard(0, (error, string) -> {});
        if (clipboardText == null || clipboardText.isEmpty()) {
            message(Text.literal("Clipboard is empty").formatted(Formatting.RED));
            return Optional.empty();
        }
        Optional<Script> parsed = Script.fromJson(clipboardText);
        if (parsed.isEmpty()) {
            message(Text.literal("Failed to read script - invalid format").formatted(Formatting.RED));
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
            message(Text.literal("Add at least one townsfolk, outsider, minion or demon before saving")
                    .formatted(Formatting.RED));
            return;
        }

        List<String> orphaned = findOrphanedAssignments(ordered);
        if (orphaned.isEmpty()) {
            apply(ordered);
            return;
        }

        Text warning = Text.literal("These are assigned in your grimoire but not on the new script:\n"
                + String.join(", ", orphaned)
                + "\n\nSaving will leave those assignments unresolved.");
        this.client.setScreen(new ConfirmScreen(
                confirmed -> {
                    this.client.setScreen(this);
                    if (confirmed) apply(ordered);
                },
                Text.literal("Save script?").formatted(Formatting.YELLOW),
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

        message(Text.literal("Removed the active script").formatted(Formatting.YELLOW));
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
                nameField.getText().isBlank() ? "Custom Script" : nameField.getText(),
                authorField.getText(),
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
            message(Text.literal("Could not build the script - no changes were made").formatted(Formatting.RED));
            return;
        }

        Script script = built.get();
        ClientState.currentScript = script;

        ClientReceive.onScriptLoaded(script);
        StorytellerState.syncGrimoire();
        captureBaseline();

        message(Text.literal("Saved \"" + script.name() + "\" (" + ordered.size() + " characters)")
                .formatted(Formatting.GREEN));
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

    private void message(Text text) {
        if (this.client != null && this.client.player != null) {
            this.client.player.sendMessage(text, false);
        }
    }

    // ================================================================
    // Screen plumbing
    // ================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (pendingAction != null) {
            Runnable action = pendingAction;
            pendingAction = null;
            action.run();
        }
        return handled;
    }

    @Override
    public void close() {
        if (discardConfirmed || !isDirty()) {
            this.client.setScreen(parent);
            return;
        }
        this.client.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        discardConfirmed = true;
                        this.client.setScreen(parent);
                    } else {
                        this.client.setScreen(this);
                    }
                },
                Text.literal("Discard changes?").formatted(Formatting.YELLOW),
                Text.literal("Your edits haven't been saved to the active script.")));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Cleared before the children draw, and a hovered entry re-queues it during super.render().
        this.hoverTooltip = null;

        // Greyed out until there's something to commit, which also covers "empty builder, no
        // active script" without needing a message to explain it.
        if (this.saveButton != null) {
            this.saveButton.active = isDirty();
        }

        // Only rebuilt when shift actually changes, so the tooltip object isn't reallocated
        // every frame.
        if (this.customsButton != null && hasShiftDown() != this.customsShowingClear) {
            this.customsShowingClear = hasShiftDown();
            if (this.customsShowingClear) {
                this.customsButton.setMessage(Text.literal(customsRoomy ? CLEAR_CUSTOMS_LABEL : CLEAR_CUSTOMS_SHORT)
                        .formatted(Formatting.RED));
                this.customsButton.setTooltip(Tooltip.of(Text.literal(
                        "Forget every character in your custom role library.")));
            } else {
                this.customsButton.setMessage(Text.literal(customsRoomy ? IMPORT_CUSTOMS_LABEL : IMPORT_CUSTOMS_SHORT)
                        .formatted(Formatting.LIGHT_PURPLE));
                this.customsButton.setTooltip(Tooltip.of(Text.literal(
                        "Add every homebrew character in the clipboard script to your custom role library. "
                                + "Hold Shift to clear the library.")));
            }
        }

        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 6, 0xFFFFFF);

        // Counts under the script list, so the composition is visible while editing. Each is
        // tinted with its team colour, and traveler gets two letters so it isn't confused with
        // townsfolk.
        int countX = 10;
        for (RoleType team : SECTION_ORDER) {
            long n = working.stream().filter(role -> role.getTeam() == team).count();
            if (n == 0) continue;
            Text label = Text.literal(abbreviate(team) + n).styled(s -> s.withColor(team.getColor()));
            context.drawTextWithShadow(this.textRenderer, label, countX, this.height - 44, 0xFFFFFF);
            countX += this.textRenderer.getWidth(label) + 6;
        }

        if (this.hoverTooltip != null) {
            context.drawTooltip(this.textRenderer, this.hoverTooltip, this.hoverTooltipX, this.hoverTooltipY);
        }
    }

    private static String abbreviate(RoleType team) {
        return team == RoleType.TRAVELER ? "Tr" : team.name().substring(0, 1);
    }

    private void queueTooltip(List<Text> lines, int mouseX, int mouseY) {
        this.hoverTooltip = lines;
        this.hoverTooltipX = mouseX;
        this.hoverTooltipY = mouseY;
    }

    /**
     * Wrap an ability into tooltip lines. The wrap width shrinks on narrow screens so a long
     * ability can't be pushed off the edge by the tooltip positioner.
     */
    private List<Text> tooltipLines(String body, List<Text> extra) {
        int wrapWidth = Math.max(120, Math.min(TOOLTIP_WIDTH, this.width - 40));
        List<Text> lines = this.textRenderer.getTextHandler()
                .wrapLines(body == null ? "" : body, wrapWidth, Style.EMPTY)
                .stream()
                .map((StringVisitable line) -> (Text) Text.literal(line.getString()).formatted(Formatting.YELLOW))
                .collect(Collectors.toList());
        lines.addAll(extra);
        return lines;
    }

    // ================================================================
    // Left pane: the script
    // ================================================================

    private class ScriptListWidget extends ElementListWidget<ScriptListWidget.Entry> {

        ScriptListWidget(MinecraftClient client, int width, int height, int y) {
            super(client, width, height, y, 24);
        }

        void populate(List<ScriptRole> roles) {
            this.clearEntries();
            if (roles.isEmpty()) {
                this.addEntry(new InfoEntry("Click to add characters"));
                return;
            }
            RoleType currentTeam = null;
            for (ScriptRole role : roles) {
                if (role.getTeam() != currentTeam) {
                    currentTeam = role.getTeam();
                    final RoleType team = currentTeam;
                    this.addEntry(new SectionEntry(
                            Text.literal(team.name()).styled(s -> s.withColor(team.getColor()).withBold(true))));
                }
                this.addEntry(new RoleEntry(role));
            }
        }

        @Override public int getRowWidth() { return this.width - 12; }
        @Override protected int getScrollbarX() { return this.getX() + this.width - 6; }

        /**
         * Entries at the top and bottom of the list are drawn partially outside the pane, so
         * their own hit test isn't enough, so the mouse has to be inside the visible viewport too.
         */
        boolean withinViewport(double mouseX, double mouseY) {
            return mouseX >= this.getX() && mouseX < this.getX() + this.width
                    && mouseY >= this.getY() && mouseY < this.getY() + this.height;
        }

        abstract class Entry extends ElementListWidget.Entry<Entry> {
            @Override public List<? extends Element> children() { return Collections.emptyList(); }
            @Override public List<? extends Selectable> selectableChildren() { return Collections.emptyList(); }
        }

        /** A wrapped, non-interactive message, used for the empty-script placeholder. */
        class InfoEntry extends Entry {
            private final String message;

            InfoEntry(String message) {
                this.message = message;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int rowLeft = ScriptListWidget.this.getRowLeft();
                int lineY = y + 2;
                for (StringVisitable line : textRenderer.getTextHandler()
                        .wrapLines(message, ScriptListWidget.this.getRowWidth() - 4, Style.EMPTY)) {
                    context.drawTextWithShadow(textRenderer,
                            Text.literal(line.getString()).formatted(Formatting.DARK_GRAY, Formatting.ITALIC),
                            rowLeft + 2, lineY, 0xFFFFFF);
                    lineY += textRenderer.fontHeight + 1;
                }
            }
        }

        class SectionEntry extends Entry {
            private final Text label;

            SectionEntry(Text label) {
                this.label = label;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawTextWithShadow(textRenderer, label,
                        ScriptListWidget.this.getRowLeft() + 2,
                        y + entryHeight - textRenderer.fontHeight - 2, 0xFFFFFF);
            }
        }

        class RoleEntry extends Entry {
            private final ScriptRole role;

            RoleEntry(ScriptRole role) {
                this.role = role;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
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

                context.drawTexture(role.getIcon(), iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
                context.drawBorder(iconX - 1, iconY - 1, iconSize + 2, iconSize + 2, role.getTeam().getColor());

                int textX = iconX + iconSize + 6;
                int textY = y + (entryHeight - textRenderer.fontHeight) / 2;
                Text name = Text.literal(role.getDisplayName()).styled(s -> s.withColor(role.getTeam().getColor()));
                context.drawTextWithShadow(textRenderer,
                        trimToWidth(name, rowWidth - (textX - rowLeft) - 4), textX, textY, 0xFFFFFF);

                if (isMouseOver) {
                    List<Text> hints = new ArrayList<>(List.of(
                            Text.literal("Click to remove").formatted(Formatting.GRAY),
                            Text.literal("Shift+Click for details").formatted(Formatting.DARK_GRAY)));
                    if (AbilityText.isBootlegger(role)) {
                        hints.add(Text.literal("Ctrl+Click to edit special rules").formatted(Formatting.AQUA));
                    }
                    queueTooltip(tooltipLines(AbilityText.of(role, baseBootlegger), hints), mouseX, mouseY);
                }
            }

            private Text trimToWidth(Text text, int maxWidth) {
                String raw = text.getString();
                if (textRenderer.getWidth(raw) <= maxWidth) return text;
                return Text.literal(textRenderer.trimToWidth(raw, Math.max(0, maxWidth - 6)) + "...")
                        .setStyle(text.getStyle());
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button != GLFW.GLFW_MOUSE_BUTTON_1) return false;
                if (Screen.hasControlDown() && AbilityText.isBootlegger(role)) {
                    pendingAction = () -> client.setScreen(new BootleggerRulesScreen(ScriptBuilderScreen.this, baseBootlegger));
                } else if (Screen.hasShiftDown()) {
                    pendingAction = () -> client.setScreen(new CharacterDetailsScreen(role, ScriptBuilderScreen.this));
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

    private class PaletteWidget extends ElementListWidget<PaletteWidget.GridEntry> {

        private final int columns;

        PaletteWidget(MinecraftClient client, int width, int height, int y, int columns) {
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
        @Override protected int getScrollbarX() { return this.getX() + this.width - 6; }

        /** See {@link ScriptListWidget#withinViewport}. */
        boolean withinViewport(double mouseX, double mouseY) {
            return mouseX >= this.getX() && mouseX < this.getX() + this.width
                    && mouseY >= this.getY() && mouseY < this.getY() + this.height;
        }

        class GridEntry extends ElementListWidget.Entry<GridEntry> {
            private final List<ScriptRole> rolesInRow;
            private int entryY;

            GridEntry(List<ScriptRole> roles) {
                this.rolesInRow = new ArrayList<>(roles);
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
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

                    context.drawBorder(borderX, iconY, ICON_SIZE, ICON_SIZE, role.getTeam().getColor());
                    context.drawTexture(role.getIcon(), borderX + 1, iconY + 1, 0, 0, 38, 38, 38, 38);

                    // Already-on-script entries stay visible but read as taken, and clicking one
                    // removes it, so the palette never disagrees with the list beside it.
                    if (onScript) {
                        context.fill(borderX + 1, iconY + 1, borderX + ICON_SIZE - 1, iconY + ICON_SIZE - 1, 0xA0000000);
                        // Escaped rather than a literal check glyph: the build sets no source
                        // encoding, so javac reads sources in the platform default charset.
                        // The glyph comes from the unicode font page, whose strokes are thinner
                        // than the ASCII X below, so it's drawn a second time a pixel across to
                        // carry the same weight.
                        Text check = Text.literal("\u2713").formatted(Formatting.GREEN, Formatting.BOLD);
                        int checkY = iconY + ICON_SIZE / 2 - 4;
                        context.drawCenteredTextWithShadow(textRenderer, check, borderX + ICON_SIZE / 2, checkY, 0xFFFFFF);
                        context.drawCenteredTextWithShadow(textRenderer, check, borderX + ICON_SIZE / 2 + 1, checkY, 0xFFFFFF);
                    }

                    // Excluded from the random draw. Plain ASCII rather than a cross glyph, for
                    // the same source-encoding reason as the check above.
                    if (banned) {
                        context.fill(borderX + 1, iconY + 1, borderX + ICON_SIZE - 1, iconY + ICON_SIZE - 1, 0xA0000000);
                        context.drawCenteredTextWithShadow(textRenderer,
                                Text.literal("X").formatted(Formatting.RED, Formatting.BOLD),
                                borderX + ICON_SIZE / 2, iconY + ICON_SIZE / 2 - 4, 0xFFFFFF);
                    }

                    int textCenterX = borderX + ICON_SIZE / 2;
                    String name = role.getDisplayName();
                    int wrapWidth = name.contains(" ") ? CELL_WIDTH - 4 : CELL_WIDTH + 1;
                    Formatting nameColor = onScript || banned ? Formatting.DARK_GRAY : Formatting.WHITE;
                    List<Text> lines = client.textRenderer.getTextHandler()
                            .wrapLines(name, wrapWidth, Style.EMPTY)
                            .stream()
                            .map(line -> (Text) Text.literal(line.getString()).formatted(nameColor))
                            .collect(Collectors.toList());
                    for (int j = 0; j < lines.size(); j++) {
                        context.drawCenteredTextWithShadow(client.textRenderer, lines.get(j),
                                textCenterX, y + 50 + j * client.textRenderer.fontHeight, 0xFFFFFF);
                    }

                    if (isMouseOver) {
                        List<Text> extra = new ArrayList<>();
                        extra.add(Text.literal("Shift+Click for details").formatted(Formatting.DARK_GRAY));
                        if (isBannable(role)) {
                            extra.add(Text.literal(banned
                                    ? "Ctrl+Click to allow in random draws"
                                    : "Ctrl+Click to bar from random draws").formatted(Formatting.DARK_GRAY));
                        }
                        CustomRoleLibrary.get(role.getId()).ifPresent(entry -> {
                            if (!entry.sourceScript().isBlank()) {
                                extra.add(Text.literal("From: " + entry.sourceScript()).formatted(Formatting.DARK_AQUA));
                            }
                            extra.add(Text.literal("Ctrl+Shift+Click to forget").formatted(Formatting.DARK_RED));
                        });
                        queueTooltip(tooltipLines(role.getAbility(), extra), mouseX, mouseY);
                    }
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button != GLFW.GLFW_MOUSE_BUTTON_1) return false;
                int rowX = PaletteWidget.this.getRowLeft();

                for (int i = 0; i < rolesInRow.size(); i++) {
                    int cellX = rowX + i * CELL_WIDTH;
                    if (mouseX < cellX || mouseX >= cellX + CELL_WIDTH) continue;
                    if (mouseY < entryY || mouseY >= entryY + PaletteWidget.this.itemHeight) continue;

                    ScriptRole role = rolesInRow.get(i);
                    // Ctrl+Shift is checked first because Ctrl alone now means ban, so forgetting a
                    // library character moved onto the compound gesture.
                    if (Screen.hasControlDown() && Screen.hasShiftDown()) {
                        if (CustomRoleLibrary.contains(role.getId())) {
                            pendingAction = () -> forgetLibraryRole(role);
                        }
                    } else if (Screen.hasShiftDown()) {
                        pendingAction = () -> client.setScreen(new CharacterDetailsScreen(role, ScriptBuilderScreen.this));
                    } else if (Screen.hasControlDown()) {
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

            @Override public List<? extends Element> children() { return Collections.emptyList(); }
            @Override public List<? extends Selectable> selectableChildren() { return Collections.emptyList(); }
        }
    }
}
