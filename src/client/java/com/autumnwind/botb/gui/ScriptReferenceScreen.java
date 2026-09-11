package com.autumnwind.botb.gui;

import com.autumnwind.botb.util.RoleType;
import com.autumnwind.botb.util.Script;
import com.autumnwind.botb.util.ScriptRole;
import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.util.*;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class ScriptReferenceScreen extends Screen {

    private enum Page { ROLES, NIGHT_ORDER, JINXES }
    private Page currentPage = Page.ROLES;
    private ScriptListWidget listWidget;
    private final Script script;
    private final Map<Page, Double> savedScrollAmounts = new EnumMap<>(Page.class);

    private static final Identifier DAWN_ICON = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/dawn.png");
    private static final Identifier DUSK_ICON = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/dusk.png");
    private static final Identifier MINION_ICON = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/minion_info.png");
    private static final Identifier DEMON_ICON = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/demon_info.png");

    // Map to hold tooltips for static night order actions.
    private static final Map<NightOrder.StaticAction, Component> STATIC_ACTION_TOOLTIPS = Map.of(
            NightOrder.StaticAction.MINION_INFO, Component.translatable("gui.blood-on-the-blocktower.script_reference.tooltip.minion_info"),
            NightOrder.StaticAction.DEMON_INFO, Component.translatable("gui.blood-on-the-blocktower.script_reference.tooltip.demon_info")
    );

    public ScriptReferenceScreen(Component title) {
        super(title);
        this.script = ClientState.currentScript;
    }

    private boolean hasAuthor() {
        return script.author() != null && !script.author().isEmpty();
    }

    /**
     * Check if this script has any jinxes (official or custom).
     */
    private boolean hasJinxes() {
        // Check official jinxes
        Set<Role> scriptRolesSet = new HashSet<>(script.roles());
        for (Jinxes.JinxInfo jinx : Jinxes.JINX_LIST) {
            if (scriptRolesSet.contains(jinx.role1()) && scriptRolesSet.contains(jinx.role2())) {
                return true;
            }
        }

        // Check custom role jinxes
        for (CustomRole customRole : script.allCustomRoles()) {
            for (CustomRole.Jinx jinx : customRole.jinxes()) {
                if (script.getScriptRole(jinx.roleId()).isPresent()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected void init() {
        int buttonWidth = 90;
        int spacing = 10;
        // Move buttons down slightly if author is present, but keep list at same position
        int topY = hasAuthor() ? 27 : 25;
        int nightOrderX = this.width / 2 - buttonWidth / 2;
        int rolesX = nightOrderX - spacing - buttonWidth;
        int jinxesX = nightOrderX + buttonWidth + spacing;

        // Almanac sits left of Roles as a "?", mirroring the Djinn "?" right of Jinxes.
        if (script.hasAlmanac()) {
            AlmanacData cached = AlmanacParser.getCached(script.almanac());
            if (cached != null && cached.hasAnyData()) {
                this.addRenderableWidget(Button.builder(Component.literal("?"), b -> openAlmanac())
                        .bounds(rolesX - 18, topY, 16, 20)
                        .tooltip(Tooltip.create(Component.translatable("gui.blood-on-the-blocktower.script_reference.tooltip.view_almanac")))
                        .build());
            }
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.blood-on-the-blocktower.script_reference.roles"), b -> switchPage(Page.ROLES)).bounds(rolesX, topY, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.blood-on-the-blocktower.script_reference.night_order"), b -> switchPage(Page.NIGHT_ORDER)).bounds(nightOrderX, topY, buttonWidth, 20).build());

        // Only add Jinxes button if there are jinxes for this script
        boolean hasJinxes = hasJinxes();
        Button jinxesButton = Button.builder(Component.translatable("gui.blood-on-the-blocktower.script_reference.jinxes"), b -> switchPage(Page.JINXES)).bounds(jinxesX, topY, buttonWidth, 20).build();
        jinxesButton.active = hasJinxes;
        this.addRenderableWidget(jinxesButton);

        // Add "?" button next to Jinxes that opens Djinn character details (only if jinxes exist)
        if (hasJinxes) {
            int questionButtonX = jinxesX + buttonWidth + 2;
            this.addRenderableWidget(Button.builder(Component.literal("?"), b -> openDjinnDetails())
                    .bounds(questionButtonX, topY, 16, 20)
                    .tooltip(Tooltip.create(Component.translatable("gui.blood-on-the-blocktower.script_reference.tooltip.view_djinn")))
                    .build());
        }

        this.switchPage(this.currentPage, true);
    }

    private void openAlmanac() {
        this.minecraft.gui.setScreen(new CustomScriptDetailsScreen(this));
    }

    private void openDjinnDetails() {
        // Open the Djinn character details screen
        ScriptRole djinnRole = new ScriptRole.Official(Role.DJINN);
        this.minecraft.gui.setScreen(new CharacterDetailsScreen(djinnRole, this));
    }

    private void switchPage(Page newPage, boolean isInitial) {
        if (!isInitial && this.currentPage == newPage) return;
        if (this.listWidget != null) {
            this.savedScrollAmounts.put(this.currentPage, this.listWidget.scrollAmount());
            this.removeWidget(this.listWidget);
        }
        this.currentPage = newPage;

        int itemHeight = switch (newPage) {
            case ROLES -> 70;
            case NIGHT_ORDER -> 30;
            case JINXES -> 60;
        };
        // Keep list at same position regardless of author - slightly reduce gap when author shown
        int buttonTopY = hasAuthor() ? 27 : 25;
        int gapBelowButtons = hasAuthor() ? 8 : 10;
        int listTopY = buttonTopY + 20 + gapBelowButtons;
        int footerHeight = 10;

        this.listWidget = new ScriptListWidget(this.minecraft, this.width, this.height - listTopY - footerHeight, listTopY, itemHeight);
        this.listWidget.switchPage(newPage);
        this.listWidget.setScrollAmount(this.savedScrollAmounts.getOrDefault(newPage, 0.0));
        this.addRenderableWidget(this.listWidget);
    }

    private void switchPage(Page newPage) {
        this.switchPage(newPage, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        if (hasAuthor()) {
            // Title moved up, author in between title and buttons
            context.centeredText(this.font, script.name(), this.width / 2, 4, 0xFFFFFFFF);
            Component authorText = Component.translatable("gui.blood-on-the-blocktower.script_reference.by_author", script.author()).withStyle(ChatFormatting.GRAY);
            context.centeredText(this.font, authorText, this.width / 2, 15, 0xFFAAAAAA);
        } else {
            // Normal title position
            context.centeredText(this.font, script.name(), this.width / 2, 8, 0xFFFFFFFF);
        }
    }

    // Night action item that can hold official roles, custom roles, or static actions
    // Uses priority for sorting when interleaving official and custom roles
    private record NightActionItem(
            NightOrder.NightOrderInfo info,      // For official roles and static actions
            CustomRole customRole,                // For custom roles (null if official/static)
            double priority                       // Sorting priority
    ) {
        // Factory for official role or static action
        public static NightActionItem fromInfo(NightOrder.NightOrderInfo info, double priority) {
            return new NightActionItem(info, null, priority);
        }

        // Factory for custom role
        public static NightActionItem fromCustomRole(CustomRole customRole, double priority) {
            return new NightActionItem(null, customRole, priority);
        }

        public Component getName() {
            if (customRole != null) return Component.literal(customRole.getDisplayName());
            if (info.isRole()) return Component.literal(info.getRole().getDisplayName());
            return switch (info.getStaticAction()) {
                case DAWN -> Component.translatable("gui.blood-on-the-blocktower.script_reference.dawn");
                case NOMINATIONS -> Component.translatable("gui.blood-on-the-blocktower.script_reference.nominations"); // Should be filtered out
                case DUSK -> Component.translatable("gui.blood-on-the-blocktower.script_reference.dusk");
                case MINION_INFO -> Component.translatable("gui.blood-on-the-blocktower.script_reference.minion_info");
                case DEMON_INFO -> Component.translatable("gui.blood-on-the-blocktower.script_reference.demon_info");
            };
        }

        public Identifier getIcon() {
            if (customRole != null) return UrlTextureLoader.getTexture(customRole);
            if (info.isRole()) return info.getRole().getIcon();
            return switch (info.getStaticAction()) {
                case DAWN -> DAWN_ICON;
                case NOMINATIONS -> DAWN_ICON; // Should be filtered out, placeholder icon
                case DUSK -> DUSK_ICON;
                case MINION_INFO -> MINION_ICON;
                case DEMON_INFO -> DEMON_ICON;
            };
        }

        public boolean isRole() { return customRole != null || (info != null && info.isRole()); }
        public boolean isCustomRole() { return customRole != null; }
        public boolean isOfficialRole() { return customRole == null && info != null && info.isRole(); }
        public Optional<Role> getRole() { return info != null ? info.role() : Optional.empty(); }
        public Optional<NightOrder.StaticAction> getStaticAction() {
            return info != null ? info.staticAction() : Optional.empty();
        }

        public ScriptRole getScriptRole() {
            if (customRole != null) return new ScriptRole.Custom(customRole);
            if (info != null && info.isRole()) return new ScriptRole.Official(info.getRole());
            return null;
        }

        public RoleType getRoleType() {
            if (customRole != null) return customRole.team();
            if (info != null && info.isRole()) return info.getRole().getType();
            return RoleType.NONE;
        }

        public String getDescription() {
            if (customRole != null) return customRole.ability();
            if (info != null && info.isRole()) return info.getRole().getDescription();
            return "";
        }

        public String getRoleId() {
            if (customRole != null) return customRole.id();
            if (info != null && info.isRole()) return info.getRole().getId();
            return "";
        }
    }

    private class ScriptListWidget extends ContainerObjectSelectionList<ScriptListWidget.ScriptEntry> {
        public ScriptListWidget(Minecraft client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }

        public void switchPage(Page page) {
            this.clearEntries();

            switch (page) {
                case ROLES -> buildRolesPage();
                case NIGHT_ORDER -> buildNightOrderPage();
                case JINXES -> buildJinxesPage();
            }
        }

        private void buildRolesPage() {
            // Every character on the script, grouped by team and gridded in team order.
            List<ScriptRole> allRoles = new ArrayList<>(script.allRoles());
            allRoles.addAll(script.allFabledAndLoric());
            Map<RoleType, List<ScriptRole>> rolesByType = allRoles.stream()
                    .collect(Collectors.groupingBy(ScriptRole::getTeam));
            final RoleType[] order = {
                    RoleType.TOWNSFOLK, RoleType.OUTSIDER, RoleType.MINION, RoleType.DEMON,
                    RoleType.TRAVELER, RoleType.FABLED, RoleType.LORIC
            };
            final int columns = 5;

            // Build the full list of roles in the visual order they will appear
            List<ScriptRole> visuallyOrderedRoles = new ArrayList<>();
            for (RoleType type : order) {
                List<ScriptRole> roles = rolesByType.get(type);
                if (roles != null && !roles.isEmpty()) {
                    visuallyOrderedRoles.addAll(roles);
                }
            }

            for (RoleType type : order) {
                List<ScriptRole> roles = rolesByType.get(type);
                if (roles != null && !roles.isEmpty()) {
                    for (int i = 0; i < roles.size(); i += columns) {
                        this.addEntry(new RoleGridEntry(
                                roles.subList(i, Math.min(i + columns, roles.size())),
                                visuallyOrderedRoles // Pass the full visual list
                        ));
                    }
                }
            }
        }

        private void buildNightOrderPage() {
            this.addEntry(new NightOrderHeaderEntry());

            List<NightActionItem> firstNightItems = new ArrayList<>();
            List<NightActionItem> otherNightsItems = new ArrayList<>();
            Set<Role> scriptRoles = new HashSet<>(script.roles());

            // Build set of official travelers in the script (if travelers enabled)
            Set<Role> officialTravelersInScript = new HashSet<>();
            if (script.hasTravelers()) {
                for (ScriptRole sr : script.travelers()) {
                    // Official travelers are ScriptRole.Official
                    if (sr instanceof ScriptRole.Official official) {
                        officialTravelersInScript.add(official.role());
                    }
                }
            }

            // Build set of official fabled/loric in the script
            Set<Role> officialFabledInScript = new HashSet<>();
            if (script.hasFabledOrLoric()) {
                for (ScriptRole sr : script.allFabledAndLoric()) {
                    // Official fabled/loric are ScriptRole.Official
                    if (sr instanceof ScriptRole.Official official) {
                        officialFabledInScript.add(official.role());
                    }
                }
            }

            // Add official roles and static actions, numbered by their night order position
            int firstNightIndex = 0;
            for (NightOrder.NightOrderInfo info : NightOrder.getFirstNightOrder()) {
                boolean isNominations = info.isStatic() && info.getStaticAction() == NightOrder.StaticAction.NOMINATIONS;
                boolean shouldInclude = info.isStatic() ||
                        (info.isRole() && scriptRoles.contains(info.getRole())) ||
                        (info.isRole() && officialTravelersInScript.contains(info.getRole())) ||
                        (info.isRole() && officialFabledInScript.contains(info.getRole()));
                if (!isNominations && !info.isAnnouncementOnly() && shouldInclude) {
                    firstNightItems.add(NightActionItem.fromInfo(info, firstNightIndex));
                }
                firstNightIndex++;
            }

            int otherNightIndex = 0;
            for (NightOrder.NightOrderInfo info : NightOrder.getOtherNightOrder()) {
                boolean isNominations = info.isStatic() && info.getStaticAction() == NightOrder.StaticAction.NOMINATIONS;
                boolean shouldInclude = info.isStatic() ||
                        (info.isRole() && scriptRoles.contains(info.getRole())) ||
                        (info.isRole() && officialTravelersInScript.contains(info.getRole())) ||
                        (info.isRole() && officialFabledInScript.contains(info.getRole()));
                if (!isNominations && !info.isAnnouncementOnly() && shouldInclude) {
                    otherNightsItems.add(NightActionItem.fromInfo(info, otherNightIndex));
                }
                otherNightIndex++;
            }

            // Add custom roles with their priority values (includes custom travelers if enabled).
            // Skip official travelers/fabled/loric since they're already added above from NightOrder.
            for (CustomRole customRole : script.allCustomRoles()) {
                // Skip if this is an official traveler (already handled above)
                boolean isOfficialTraveler = officialTravelersInScript.stream()
                        .anyMatch(r -> r.getId().equals(customRole.id()));
                if (isOfficialTraveler) continue;

                if (customRole.wakesFirstNight()) {
                    firstNightItems.add(NightActionItem.fromCustomRole(customRole, customRole.firstNight()));
                }
                if (customRole.wakesOtherNights()) {
                    otherNightsItems.add(NightActionItem.fromCustomRole(customRole, customRole.otherNight()));
                }
            }

            // Sort both lists by priority
            firstNightItems.sort(Comparator.comparingDouble(NightActionItem::priority));
            otherNightsItems.sort(Comparator.comparingDouble(NightActionItem::priority));

            // Create the full lists of ScriptRoles for each night (for CharacterDetailsScreen navigation)
            List<ScriptRole> firstNightRoles = firstNightItems.stream()
                    .filter(NightActionItem::isRole)
                    .map(NightActionItem::getScriptRole)
                    .collect(Collectors.toList());
            List<ScriptRole> otherNightsRoles = otherNightsItems.stream()
                    .filter(NightActionItem::isRole)
                    .map(NightActionItem::getScriptRole)
                    .collect(Collectors.toList());

            int maxRows = Math.max(firstNightItems.size(), otherNightsItems.size());
            for (int i = 0; i < maxRows; i++) {
                this.addEntry(new NightOrderRowEntry(
                        (i < firstNightItems.size()) ? firstNightItems.get(i) : null,
                        (i < otherNightsItems.size()) ? otherNightsItems.get(i) : null,
                        firstNightRoles,
                        otherNightsRoles
                ));
            }
        }

        private void buildJinxesPage() {
            // Collect all jinxes (official + custom)
            List<UnifiedJinx> allJinxes = new ArrayList<>();

            // Official jinxes (only if both roles are in script)
            Set<Role> scriptRolesSet = new HashSet<>(script.roles());
            for (Jinxes.JinxInfo jinx : Jinxes.JINX_LIST) {
                if (scriptRolesSet.contains(jinx.role1()) && scriptRolesSet.contains(jinx.role2())) {
                    allJinxes.add(new UnifiedJinx(
                            new ScriptRole.Official(jinx.role1()),
                            new ScriptRole.Official(jinx.role2()),
                            jinx.description()
                    ));
                }
            }

            // Custom role jinxes (including custom travelers when travelers are enabled).
            for (CustomRole customRole : script.allCustomRoles()) {
                for (CustomRole.Jinx jinx : customRole.jinxes()) {
                    // Find the other role (could be official or custom)
                    Optional<ScriptRole> otherRole = script.getScriptRole(jinx.roleId());
                    if (otherRole.isPresent()) {
                        allJinxes.add(new UnifiedJinx(
                                new ScriptRole.Custom(customRole),
                                otherRole.get(),
                                jinx.reason()
                        ));
                    }
                }
            }

            if (allJinxes.isEmpty()) {
                this.addEntry(new HeaderEntry(RoleType.NONE, 75));
            } else {
                for (UnifiedJinx jinx : allJinxes) {
                    this.addEntry(new JinxEntry(jinx));
                }
            }
        }

        // Unified jinx representation for both official and custom roles
        private record UnifiedJinx(ScriptRole role1, ScriptRole role2, String description) {}

        @Override protected int scrollBarX() { return super.scrollBarX() + 30; }
        @Override public int getRowWidth() { return 400; }

        public abstract class ScriptEntry extends ContainerObjectSelectionList.Entry<ScriptEntry> {}

        private class HeaderEntry extends ScriptEntry {
            private final Component text;
            private final int color;
            private final int entryHeight;

            public HeaderEntry(RoleType type, int height) {
                this.entryHeight = height;
                this.color = type.getColor();

                Component header;
                if (type == RoleType.NONE) {
                    header = Component.translatable("gui.blood-on-the-blocktower.script_reference.no_jinxes");
                } else {
                    header = Component.literal(type.getPluralName());
                }
                this.text = header.copy().withStyle(ChatFormatting.UNDERLINE);
            }
            @Override
            public void extractContent(GuiGraphicsExtractor c, int mX, int mY, boolean hv, float t) {
                int y = getContentY();

                int textY = y + this.entryHeight - font.lineHeight - 10;
                c.centeredText(font, text, ScriptListWidget.this.width / 2, textY, color);
            }
            @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
            @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
        }

        private class RoleGridEntry extends ScriptEntry {
            private final List<ScriptRole> roles;
            private final List<ScriptRole> fullVisualList; // Full list of all roles on the page
            private int entryY;

            public RoleGridEntry(List<ScriptRole> rolesInRow, List<ScriptRole> fullVisualList) {
                this.roles = rolesInRow;
                this.fullVisualList = fullVisualList;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor c, int mX, int mY, boolean hv, float t) {
                int y = getContentY();

                this.entryY = y;
                int itemWidth = 75;
                int totalRowWidth = roles.size() * itemWidth;
                int startX = (ScriptListWidget.this.width - totalRowWidth) / 2;
                for (int j = 0; j < roles.size(); j++) {
                    ScriptRole scriptRole = roles.get(j);
                    int roleX = startX + j * itemWidth;
                    int borderWidth = 40, borderHeight = 40;
                    int borderX = roleX + (itemWidth - borderWidth) / 2;
                    int borderY = y + 5;
                    boolean isMouseOverIcon = mX >= borderX && mX < borderX + borderWidth && mY >= borderY && mY < borderY + borderHeight;
                    int borderColor = scriptRole.getTeam().getColor();
                    c.outline(borderX, borderY, borderWidth, borderHeight, borderColor);
                    c.blit(RenderPipelines.GUI_TEXTURED, scriptRole.getIcon(), borderX + 1, borderY + 1, 0, 0, 38, 38, 38, 38);

                    // Draw barrier overlay if role is crossed out
                    if (ClientState.crossedOutRoles.contains(scriptRole.getId())) {
                        Identifier barrierTexture = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/barrier.png");
                        c.blit(RenderPipelines.GUI_TEXTURED, barrierTexture, borderX + 1, borderY + 1, 0, 0, 38, 38, 38, 38);
                    }

                    String roleNameString = scriptRole.getDisplayName();

                    // Use wider margin for single words, narrower for multi-word names
                    int wrapWidth = roleNameString.contains(" ") ? itemWidth - 4 : itemWidth + 1;

                    List<Component> textLines = minecraft.font.getSplitter()
                            .splitLines(roleNameString, wrapWidth, Style.EMPTY)
                            .stream()
                            .map(line -> Component.literal(line.getString()))
                            .collect(Collectors.toList());
                    int textCenterX = borderX + (borderWidth / 2);
                    int startTextY = y + 50;
                    for (int k = 0; k < textLines.size(); k++) {
                        Component line = textLines.get(k);
                        int currentLineY = startTextY + (k * minecraft.font.lineHeight);
                        c.centeredText(minecraft.font, line, textCenterX, currentLineY, 0xFFFFFFFF);
                    }
                    if (isMouseOverIcon) {
                        List<FormattedText> wrappedLines = minecraft.font.getSplitter()
                                .splitLines(AbilityText.of(scriptRole), 170, Style.EMPTY);
                        List<Component> tooltipTextLines = wrappedLines.stream()
                                .map(line -> Component.literal(line.getString()).withStyle(ChatFormatting.YELLOW))
                                .collect(Collectors.toList());
                        c.setComponentTooltipForNextFrame(minecraft.font, tooltipTextLines, mX, mY);
                    }
                }
            }

            @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                double mouseX = event.x();
                double mouseY = event.y();
                int button = event.button();

                if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
                    int itemWidth = 75;
                    int totalRowWidth = roles.size() * itemWidth;
                    int startX = ScriptListWidget.this.getRowLeft() + (ScriptListWidget.this.getRowWidth() - totalRowWidth) / 2;
                    for (int i = 0; i < this.roles.size(); i++) {
                        int cellX = startX + i * itemWidth;
                        if (mouseX >= cellX && mouseX < cellX + itemWidth && mouseY >= this.entryY && mouseY < this.entryY + ScriptListWidget.this.defaultEntryHeight) {
                            ScriptRole selectedScriptRole = this.roles.get(i);

                            // Ctrl+Click toggles crossed out status
                            if (Minecraft.getInstance().hasControlDown()) {
                                String roleId = selectedScriptRole.getId();
                                if (ClientState.crossedOutRoles.contains(roleId)) {
                                    ClientState.crossedOutRoles.remove(roleId);
                                } else {
                                    ClientState.crossedOutRoles.add(roleId);
                                }
                                return true;
                            }

                            // Normal click opens character details
                            ScriptReferenceScreen.this.savedScrollAmounts.put(
                                    ScriptReferenceScreen.this.currentPage,
                                    ScriptReferenceScreen.this.listWidget.scrollAmount()
                            );
                            minecraft.gui.setScreen(new CharacterDetailsScreen(selectedScriptRole, ScriptReferenceScreen.this, this.fullVisualList));
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
            @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
        }

        private class NightOrderHeaderEntry extends ScriptEntry {
            @Override public void extractContent(GuiGraphicsExtractor c, int mX, int mY, boolean hv, float t) {
                int y = getContentY();
                int h = getContentHeight();

                int rowLeft = ScriptListWidget.this.getRowLeft();
                int rowWidth = ScriptListWidget.this.getRowWidth();
                c.centeredText(font, Component.translatable("gui.blood-on-the-blocktower.script_reference.first_night"), rowLeft + rowWidth / 4, y + h/2 - 4, 0xFFFFFFFF);
                c.centeredText(font, Component.translatable("gui.blood-on-the-blocktower.script_reference.other_nights"), rowLeft + rowWidth * 3 / 4, y + h/2 - 4, 0xFFFFFFFF);
            }
            @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
            @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
        }

        private class NightOrderRowEntry extends ScriptEntry {
            private final NightActionItem firstNightItem, otherNightsItem;
            private final List<ScriptRole> firstNightRoles, otherNightsRoles;
            private int entryY;

            public NightOrderRowEntry(NightActionItem fni, NightActionItem oni, List<ScriptRole> fnr, List<ScriptRole> onr) {
                this.firstNightItem = fni;
                this.otherNightsItem = oni;
                this.firstNightRoles = fnr;
                this.otherNightsRoles = onr;
            }

            @Override public void extractContent(GuiGraphicsExtractor c, int mX, int mY, boolean hv, float t) {
                int y = getContentY();
                int h = getContentHeight();

                this.entryY = y;
                int rowLeft = ScriptListWidget.this.getRowLeft();
                int rowWidth = ScriptListWidget.this.getRowWidth();
                if (firstNightItem != null) drawItem(c, firstNightItem, rowLeft + rowWidth / 4, y, h, mX, mY);
                if (otherNightsItem != null) drawItem(c, otherNightsItem, rowLeft + rowWidth * 3 / 4, y, h, mX, mY);
            }

            private void drawItem(GuiGraphicsExtractor c, NightActionItem item, int centerX, int y, int h, int mX, int mY) {
                int iconSize = 24;
                int iconX = centerX - 60;
                int iconY = y + (h - iconSize) / 2;
                int textX = centerX - 25;
                int textY = y + (h - font.lineHeight) / 2;
                int textWidth = font.width(item.getName());

                boolean isMouseOver = mX >= iconX && mX < textX + textWidth && mY >= y && mY < y + h;

                c.blit(RenderPipelines.GUI_TEXTURED, item.getIcon(), iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);

                if (item.isRole()) {
                    // Draw barrier overlay if role is crossed out
                    if (ClientState.crossedOutRoles.contains(item.getRoleId())) {
                        Identifier barrierTexture = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/barrier.png");
                        c.blit(RenderPipelines.GUI_TEXTURED, barrierTexture, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
                    }

                    int borderColor = item.getRoleType().getColor();
                    c.outline(iconX - 1, iconY - 1, iconSize + 2, iconSize + 2, borderColor);
                }

                c.text(font, item.getName(), textX, textY, 0xFFFFFFFF);

                if (item.isRole()) {
                    if (isMouseOver) {
                        List<FormattedText> wrappedLines = minecraft.font.getSplitter()
                                .splitLines(item.getDescription(), 170, Style.EMPTY);
                        List<Component> tooltipTextLines = wrappedLines.stream()
                                .map(line -> Component.literal(line.getString()).withStyle(ChatFormatting.YELLOW))
                                .collect(Collectors.toList());
                        c.setComponentTooltipForNextFrame(minecraft.font, tooltipTextLines, mX, mY);
                    }
                } else if (item.getStaticAction().isPresent()) {
                    // Add hover text for specific static actions
                    NightOrder.StaticAction action = item.getStaticAction().get();
                    if (isMouseOver && STATIC_ACTION_TOOLTIPS.containsKey(action)) {
                        List<FormattedText> wrappedLines = minecraft.font.getSplitter()
                                .splitLines(STATIC_ACTION_TOOLTIPS.get(action), 170, Style.EMPTY);
                        List<Component> tooltipTextLines = wrappedLines.stream()
                                .map(line -> Component.literal(line.getString()).withStyle(ChatFormatting.YELLOW))
                                .collect(Collectors.toList());
                        c.setComponentTooltipForNextFrame(minecraft.font, tooltipTextLines, mX, mY);
                    }
                }
            }

            @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                double mouseX = event.x();
                double mouseY = event.y();
                int button = event.button();

                if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
                    int rowLeft = ScriptListWidget.this.getRowLeft();
                    int rowWidth = ScriptListWidget.this.getRowWidth();

                    if (firstNightItem != null && firstNightItem.isRole()) {
                        int centerX = rowLeft + rowWidth / 4;
                        int iconX = centerX - 60;
                        int textX = centerX - 25;
                        int textWidth = font.width(firstNightItem.getName());
                        if (mouseX >= iconX && mouseX < textX + textWidth && mouseY >= this.entryY && mouseY < this.entryY + ScriptListWidget.this.defaultEntryHeight) {
                            ScriptReferenceScreen.this.savedScrollAmounts.put(
                                    ScriptReferenceScreen.this.currentPage,
                                    ScriptReferenceScreen.this.listWidget.scrollAmount()
                            );
                            minecraft.gui.setScreen(new CharacterDetailsScreen(firstNightItem.getScriptRole(), ScriptReferenceScreen.this, this.firstNightRoles));
                            return true;
                        }
                    }
                    if (otherNightsItem != null && otherNightsItem.isRole()) {
                        int centerX = rowLeft + rowWidth * 3 / 4;
                        int iconX = centerX - 60;
                        int textX = centerX - 25;
                        int textWidth = font.width(otherNightsItem.getName());
                        if (mouseX >= iconX && mouseX < textX + textWidth && mouseY >= this.entryY && mouseY < this.entryY + ScriptListWidget.this.defaultEntryHeight) {
                            ScriptReferenceScreen.this.savedScrollAmounts.put(
                                    ScriptReferenceScreen.this.currentPage,
                                    ScriptReferenceScreen.this.listWidget.scrollAmount()
                            );
                            minecraft.gui.setScreen(new CharacterDetailsScreen(otherNightsItem.getScriptRole(), ScriptReferenceScreen.this, this.otherNightsRoles));
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
            @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
        }

        private class JinxEntry extends ScriptEntry {
            private final UnifiedJinx jinx;
            private int entryY;
            public JinxEntry(UnifiedJinx jinx) { this.jinx = jinx; }

            @Override public void extractContent(GuiGraphicsExtractor c, int mX, int mY, boolean hv, float t) {
                int y = getContentY();
                int h = getContentHeight();

                this.entryY = y;
                int iconSize = 48;
                int contentWidth = iconSize * 2 + 10 + 200;
                int startX = ScriptListWidget.this.getRowLeft() + (ScriptListWidget.this.getRowWidth() - contentWidth) / 2;

                int role1X = startX;
                int role1Y = y + (h-iconSize)/2;
                int role2X = startX + iconSize + 10;
                int role2Y = role1Y;

                c.blit(RenderPipelines.GUI_TEXTURED, jinx.role1().getIcon(), role1X, role1Y, 0, 0, iconSize, iconSize, iconSize, iconSize);
                c.blit(RenderPipelines.GUI_TEXTURED, jinx.role2().getIcon(), role2X, role2Y, 0, 0, iconSize, iconSize, iconSize, iconSize);

                // Draw barrier overlays if roles are crossed out
                String role1Id = jinx.role1().getId();
                String role2Id = jinx.role2().getId();
                if (ClientState.crossedOutRoles.contains(role1Id)) {
                    Identifier barrierTexture = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/barrier.png");
                    c.blit(RenderPipelines.GUI_TEXTURED, barrierTexture, role1X, role1Y, 0, 0, iconSize, iconSize, iconSize, iconSize);
                }
                if (ClientState.crossedOutRoles.contains(role2Id)) {
                    Identifier barrierTexture = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/barrier.png");
                    c.blit(RenderPipelines.GUI_TEXTURED, barrierTexture, role2X, role2Y, 0, 0, iconSize, iconSize, iconSize, iconSize);
                }

                c.outline(role1X - 1, role1Y - 1, iconSize + 2, iconSize + 2, jinx.role1().getTeam().getColor());
                c.outline(role2X - 1, role2Y - 1, iconSize + 2, iconSize + 2, jinx.role2().getTeam().getColor());

                int textX = startX + iconSize*2 + 20;
                int textY = y + (h - font.wordWrapHeight(Component.literal(jinx.description()), 200)) / 2;
                c.textWithWordWrap(font, Component.literal(jinx.description()), textX, textY, 200, 0xFFFFFFFF);

                boolean mouseOnRole1 = mX >= role1X && mX < role1X + iconSize && mY >= role1Y && mY < role1Y + iconSize;
                boolean mouseOnRole2 = mX >= role2X && mX < role2X + iconSize && mY >= role2Y && mY < role2Y + iconSize;

                ScriptRole hoveredRole = null;
                if (mouseOnRole1) hoveredRole = jinx.role1();
                else if (mouseOnRole2) hoveredRole = jinx.role2();

                if (hoveredRole != null) {
                    List<FormattedText> wrappedLines = minecraft.font.getSplitter()
                            .splitLines(AbilityText.of(hoveredRole), 170, Style.EMPTY);
                    List<Component> tooltipTextLines = wrappedLines.stream()
                            .map(line -> Component.literal(line.getString()).withStyle(ChatFormatting.YELLOW))
                            .collect(Collectors.toList());
                    c.setComponentTooltipForNextFrame(minecraft.font, tooltipTextLines, mX, mY);
                }
            }

            @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                double mouseX = event.x();
                double mouseY = event.y();
                int button = event.button();

                if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
                    int iconSize = 48;
                    int h = ScriptListWidget.this.defaultEntryHeight;
                    int contentWidth = iconSize * 2 + 10 + 200;
                    int startX = ScriptListWidget.this.getRowLeft() + (ScriptListWidget.this.getRowWidth() - contentWidth) / 2;

                    int role1X = startX;
                    int role1Y = this.entryY + (h - iconSize) / 2;
                    int role2X = startX + iconSize + 10;
                    int role2Y = role1Y;

                    int padding = 4;
                    int paddedSize = iconSize + (padding * 2);
                    int role1ClickX = role1X - padding;
                    int role1ClickY = role1Y - padding;
                    int role2ClickX = role2X - padding;
                    int role2ClickY = role2Y - padding;

                    List<ScriptRole> jinxPair = List.of(jinx.role1(), jinx.role2());

                    if (mouseX >= role1ClickX && mouseX < role1ClickX + paddedSize && mouseY >= role1ClickY && mouseY < role1ClickY + paddedSize) {
                        ScriptReferenceScreen.this.savedScrollAmounts.put(
                                ScriptReferenceScreen.this.currentPage,
                                ScriptReferenceScreen.this.listWidget.scrollAmount()
                        );
                        minecraft.gui.setScreen(new CharacterDetailsScreen(jinx.role1(), ScriptReferenceScreen.this, jinxPair));
                        return true;
                    }

                    if (mouseX >= role2ClickX && mouseX < role2ClickX + paddedSize && mouseY >= role2ClickY && mouseY < role2ClickY + paddedSize) {
                        ScriptReferenceScreen.this.savedScrollAmounts.put(
                                ScriptReferenceScreen.this.currentPage,
                                ScriptReferenceScreen.this.listWidget.scrollAmount()
                        );
                        minecraft.gui.setScreen(new CharacterDetailsScreen(jinx.role2(), ScriptReferenceScreen.this, jinxPair));
                        return true;
                    }
                }
                return false;
            }

            @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
            @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();

        if (KeyInputHandler.openScriptKey.matches(event) || keyCode == GLFW.GLFW_KEY_E) {
            this.onClose(); // Close the screen
            return true;
        }

        // If it was some other key, let the superclass handle it
        return super.keyPressed(event);
    }
}