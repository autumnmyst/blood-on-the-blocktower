package com.autumnwind.botb.gui;

import com.autumnwind.botb.event.KeyInputHandler;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import com.autumnwind.botb.gui.widget.DocumentEntry;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.RoleGuides;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * Advanced Guide screen for storytellers - explains special casing in the Night Order HUD.
 * Layout: Category buttons on the left, scrollable documentation on the right.
 */
public class AdvancedGuideScreen extends Screen {

    private static final String KEY_PREFIX = "gui.blood-on-the-blocktower.advanced_guide.";

    private final Screen parent;
    private GuideContentWidget contentWidget;
    private GuideCategory selectedCategory = GuideCategory.TRIGGERS;
    private double savedScrollAmount = 0.0;

    /** Script id to role. */
    private static final Map<String, Role> ROLES_BY_ID = new HashMap<>();
    static {
        for (Role role : Role.values()) {
            if (role != Role.NO_ROLE) ROLES_BY_ID.put(role.getId(), role);
        }
    }

    public AdvancedGuideScreen(Screen parent) {
        super(Component.translatable(KEY_PREFIX + "title"));
        this.parent = parent;
    }

    private enum GuideCategory {
        TRIGGERS,
        ASSOCIATED_ROLES,
        ABILITY_BLOCKING,
        GLOBAL_EFFECTS,
        VOTE_MODIFICATION,
        MADNESS_HUD,
        CUSTOM_ROLES;

        private final String key = KEY_PREFIX + name().toLowerCase(Locale.ROOT);

        MutableComponent getDisplayName() {
            return Component.translatable(key);
        }
    }

    @Override
    protected void init() {
        int buttonWidth = 120;
        int buttonHeight = 20;
        int buttonSpacing = 5;
        int leftColumnX = 15;
        int startY = 35;

        // Category buttons
        int currentY = startY;
        for (GuideCategory category : GuideCategory.values()) {
            final GuideCategory cat = category;
            ChatFormatting color = (category == selectedCategory) ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
            this.addRenderableWidget(Button.builder(
                    category.getDisplayName().withStyle(color),
                    button -> {
                        selectedCategory = cat;
                        savedScrollAmount = 0.0;
                        this.minecraft.gui.setScreen(this);
                    }
            ).bounds(leftColumnX, currentY, buttonWidth, buttonHeight).build());
            currentY += buttonHeight + buttonSpacing;
        }

        // Role Guides opens its own screen rather than a category
        currentY += buttonSpacing;
        this.addRenderableWidget(Button.builder(
                Component.translatable(KEY_PREFIX + "role_guides").withStyle(ChatFormatting.AQUA),
                button -> this.minecraft.gui.setScreen(new RoleGuidesScreen(this))
        ).bounds(leftColumnX, currentY, buttonWidth, buttonHeight).build());

        // Back button
        int backButtonWidth = 60;
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.blood-on-the-blocktower.back").withStyle(ChatFormatting.YELLOW),
                button -> this.minecraft.gui.setScreen(this.parent)
        ).bounds(this.width - backButtonWidth - 10, this.height - 30, backButtonWidth, 20).build());

        // Content widget (right side)
        int contentX = leftColumnX + buttonWidth + 20;
        int contentY = 30;
        int contentWidth = this.width - contentX - 15;
        int contentHeight = this.height - contentY - 40;

        this.contentWidget = new GuideContentWidget(this.minecraft, contentWidth, contentHeight, contentY, selectedCategory);
        this.contentWidget.updateSizeAndPosition(contentWidth, contentHeight, contentX, contentY);
        this.contentWidget.setScrollAmount(savedScrollAmount);
        this.addRenderableWidget(this.contentWidget);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();

        boolean exitKeyPressed = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E;
        boolean openAssignGuiPressed = KeyInputHandler.openAssignGui != null && KeyInputHandler.openAssignGui.matches(event);
        if (exitKeyPressed || openAssignGuiPressed) {
            this.minecraft.gui.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(event);
    }

    /**
     * Scrollable content widget that displays guide text based on selected category.
     */
    private class GuideContentWidget extends ContainerObjectSelectionList<DocumentEntry> {

        public GuideContentWidget(Minecraft client, int width, int height, int y, GuideCategory category) {
            super(client, width, height, y, client.font.lineHeight + 2);
            populateContent(category);
        }

        private void populateContent(GuideCategory category) {
            int textWidth = this.getRowWidth() - 10;

            switch (category) {
                case TRIGGERS -> populateTriggers(textWidth);
                case ASSOCIATED_ROLES -> populateAssociatedRoles(textWidth);
                case ABILITY_BLOCKING -> populateAbilityBlocking(textWidth);
                case GLOBAL_EFFECTS -> populateGlobalEffects(textWidth);
                case VOTE_MODIFICATION -> populateVoteModification(textWidth);
                case MADNESS_HUD -> populateMadnessHud(textWidth);
                case CUSTOM_ROLES -> populateCustomRoles(textWidth);
            }
        }

        private void populateTriggers(int textWidth) {
            addTitle("triggers.triggered_visits.title");
            addSpacer();
            addBody("triggers.triggered_visits.body", textWidth);
            addSpacer();
            addSpacer();

            addTitle("triggers.conditional_visits.title");
            addSpacer();
            addBody("triggers.conditional_visits.body", textWidth);
            addRoleList("godfather, summoner, witch, king, zombuul, leviathan, xaan, riot, undertaker", textWidth);
            addSpacer();
            addSpacer();

            addTitle("triggers.death_triggers.title");
            addSpacer();
            addBody("triggers.death_triggers.body", textWidth);
            addSpacer();

            addSubtitle("triggers.death_triggers.any.subtitle");
            addRoleList("hatter, barber, sweetheart, plaguedoctor, poppygrower", textWidth);
            addSpacer();

            addSubtitle("triggers.death_triggers.night.subtitle");
            addRoleList("farmer, ravenkeeper", textWidth);
            addBody("triggers.death_triggers.night.body", textWidth);
            addSpacer();

            addSubtitle("triggers.death_triggers.demon.subtitle");
            addRoleList("sage, banshee", textWidth);
            addBody("triggers.death_triggers.demon.body", textWidth);
            addSpacer();

            addSubtitle("triggers.death_triggers.other.subtitle");
            addRoleList("grandmother, choirboy, scarletwoman", textWidth);
            addBody("triggers.death_triggers.other.body", textWidth);
            addSpacer();
            addSpacer();

            addTitle("triggers.revival_and_role_switch.title");
            addSpacer();

            addSubtitle("triggers.revival.subtitle");
            addBody("triggers.revival.body", textWidth);
            addSpacer();

            addSubtitle("triggers.role_switch.subtitle");
            addBody("triggers.role_switch.body", textWidth);
        }

        private void populateAssociatedRoles(int textWidth) {
            addTitle("associated_roles");
            addSpacer();
            addBody("associated_roles.body_1", textWidth);
            addSpacer();
            addBody("associated_roles.body_2", textWidth);
            addRoleList("philosopher, cannibal, pixie, alchemist, boffin, drunk, marionette, lunatic, hermit, plaguedoctor", textWidth);
            addSpacer();
            addSpacer();

            addTitle("associated_roles.first_night_only.title");
            addSpacer();
            addBody("associated_roles.first_night_only.body", textWidth);
        }

        private void populateAbilityBlocking(int textWidth) {
            addTitle("ability_blocking");
            addSpacer();
            addBody("ability_blocking.body", textWidth);
            addSpacer();
            addSpacer();

            addSubtitle("ability_blocking.preacher.subtitle");
            addBody("ability_blocking.preacher.body", textWidth);
            addSpacer();

            addSubtitle("ability_blocking.role_specific.subtitle");
            addBody("ability_blocking.role_specific.body", textWidth);
            addSpacer();

            addSubtitle("ability_blocking.generic.subtitle");
            addBody("ability_blocking.generic.body", textWidth);
            addSpacer();
            addSpacer();

            addTitle("ability_blocking.has_ability.title");
            addSpacer();
            addHighlight("ability_blocking.has_ability.highlight", textWidth);
            addBody("ability_blocking.has_ability.body", textWidth);
            addSpacer();
            addSpacer();

            addTitle("ability_blocking.droisoned.title");
            addSpacer();
            addHighlight("ability_blocking.droisoned.highlight", textWidth);
            addBody("ability_blocking.droisoned.body_1", textWidth);
            addRoleList("magician, poppygrower, snitch, damsel, king, marionette", textWidth);
            addBody("ability_blocking.droisoned.body_2", textWidth);
            addRoleList("vortox, legion, organgrinder, boffin, wraith", textWidth);
            addBody("ability_blocking.droisoned.body_3", textWidth);
            addRoleList("scarletwoman, grandmother, barber, hatter, poppygrower", textWidth);
            addSpacer();
            addBody("ability_blocking.droisoned.body_4", textWidth);
            addRoleList("gossip, tinker, moonchild, cultleader, princess, legion, vizier, riot", textWidth);
            addBody("ability_blocking.droisoned.body_5", textWidth);
            addRoleList("mezepheles, leviathan, sweetheart, plaguedoctor, farmer, banshee", textWidth);
            addSpacer();
            addBody("ability_blocking.droisoned.body_6", textWidth);
            addRoleList("drunk, marionette, lunatic, hermit", textWidth);
        }

        private void populateGlobalEffects(int textWidth) {
            addTitle("global_effects");
            addSpacer();
            addBody("global_effects.body", textWidth);
            addRoleList("minstrel, vortox, xaan, lilmonsta, princess, alhadikhia", textWidth);
            addSpacer();
            addSpacer();

            addSubtitle("global_effects.nominations_modifiers.subtitle");
            addBody("global_effects.nominations_modifiers.body", textWidth);
            addRoleList("organgrinder, bishop, legion, riot", textWidth);
            addSpacer();

            addSubtitle("global_effects.fabled_and_loric.subtitle");
            addBody("global_effects.fabled_and_loric.body", textWidth);
            addRoleList("stormcatcher, tor, buddhist, toymaker", textWidth);
        }

        private void populateVoteModification(int textWidth) {
            addTitle("vote_modification");
            addSpacer();
            addBody("vote_modification.body", textWidth);
            addRoleList("banshee, voudon, bureaucrat, thief, godofug, beggar, organgrinder, legion, butcher", textWidth);
            addSpacer();
            addSpacer();

            addTitle("vote_modification.multiplier_stacking.title");
            addSpacer();
            addBody("vote_modification.multiplier_stacking.body", textWidth);
            addAqua("vote_modification.multiplier_stacking.example_1", textWidth);
            addAqua("vote_modification.multiplier_stacking.example_2", textWidth);
            addAqua("vote_modification.multiplier_stacking.example_3", textWidth);
            addSpacer();
            addBody("vote_modification.multiplier_stacking.note", textWidth);
            addSpacer();
            addSpacer();

            addTitle("vote_modification.ghost_vote_toggle.title");
            addSpacer();
            addHighlight("vote_modification.ghost_vote_toggle.highlight", textWidth);
            addBody("vote_modification.ghost_vote_toggle.body", textWidth);
        }

        private void populateMadnessHud(int textWidth) {
            addTitle("madness_hud");
            addSpacer();
            addBody("madness_hud.body_1", textWidth);
            addSpacer();
            addBody("madness_hud.body_2", textWidth);
            addSpacer();
            addBody("madness_hud.body_3", textWidth);
            addRoleList("cerenovus, harpy, pixie, mutant", textWidth);
        }

        private void populateCustomRoles(int textWidth) {
            addTitle("custom_roles");
            addSpacer();
            addBody("custom_roles.body", textWidth);
            addSpacer();
            addSpacer();

            addTitle("custom_roles.script_json_format.title");
            addSpacer();
            addBody("custom_roles.script_json_format.body", textWidth);
            addSpacer();

            addSubtitle("custom_roles.object_fields.subtitle");
            addBody("custom_roles.object_fields.required", textWidth);
            addAqua("custom_roles.object_fields.required_list", textWidth);
            addSpacer();
            addBody("custom_roles.object_fields.optional", textWidth);
            addAqua("custom_roles.object_fields.optional_list", textWidth);
            addSpacer();

            addSubtitle("custom_roles.team_values.subtitle");
            addBody("custom_roles.team_values.body", textWidth);
            addSpacer();

            addSubtitle("custom_roles.image_field.subtitle");
            addBody("custom_roles.image_field.body", textWidth);
            addBody("custom_roles.image_field.one_url", textWidth);
            addBody("custom_roles.image_field.two_urls", textWidth);
            addBody("custom_roles.image_field.three_urls", textWidth);
            addSpacer();

            addSubtitle("custom_roles.night_order.subtitle");
            addBody("custom_roles.night_order.body", textWidth);
            addSpacer();
            addSpacer();

            addTitle("custom_roles.almanac.title");
            addSpacer();
            addBody("custom_roles.almanac.body", textWidth);
            addSpacer();

            addSubtitle("custom_roles.almanac.main.subtitle");
            addBody("custom_roles.almanac.main.body_1", textWidth);
            addHighlight("custom_roles.almanac.main.highlight", textWidth);
            addSpacer();
            addBody("custom_roles.almanac.main.body_2", textWidth);
            addSpacer();

            addSubtitle("custom_roles.almanac.extra.subtitle");
            addBody("custom_roles.almanac.extra.body_1", textWidth);
            addHighlight("custom_roles.almanac.extra.highlight", textWidth);
            addSpacer();
            addBody("custom_roles.almanac.extra.body_2", textWidth);
            addSpacer();

            addSubtitle("custom_roles.almanac.role_id_matching.subtitle");
            addBody("custom_roles.almanac.role_id_matching.body", textWidth);
            addSpacer();
            addSpacer();

            addTitle("custom_roles.example_meta.title");
            addSpacer();
            addBody("custom_roles.example_meta.line_1", textWidth);
            addBody("custom_roles.example_meta.line_2", textWidth);
            addBody("custom_roles.example_meta.line_3", textWidth);
            addBody("custom_roles.example_meta.line_4", textWidth);
            addBody("custom_roles.example_meta.line_5", textWidth);
            addBody("custom_roles.example_meta.line_6", textWidth);
            addBody("custom_roles.example_meta.line_7", textWidth);
            addBody("custom_roles.example_meta.line_8", textWidth);
        }

        // Helper methods for adding entries
        private void addTitle(String key) {
            this.addEntry(DocumentEntry.title(font, Component.translatable(KEY_PREFIX + key).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        }

        private void addSubtitle(String key) {
            this.addEntry(DocumentEntry.title(font, Component.translatable(KEY_PREFIX + key).withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE)));
        }

        private void addBody(String key, int width) {
            for (FormattedCharSequence line : font.split(Component.translatable(KEY_PREFIX + key), width)) {
                this.addEntry(DocumentEntry.text(font, line, 0xFFCCCCCC));
            }
        }

        private void addHighlight(String key, int width) {
            MutableComponent highlighted = Component.translatable(KEY_PREFIX + key).withStyle(ChatFormatting.GOLD);
            for (FormattedCharSequence line : font.split(highlighted, width)) {
                this.addEntry(DocumentEntry.text(font, line, 0xFFCCCCCC));
            }
        }

        /** Plain aqua text in the same style as a role list, without links. */
        private void addAqua(String key, int width) {
            MutableComponent aqua = Component.translatable(KEY_PREFIX + key).withStyle(ChatFormatting.AQUA);
            for (FormattedCharSequence line : font.split(aqua, width)) {
                this.addEntry(DocumentEntry.text(font, line, 0xFFCCCCCC));
            }
        }

        /**
         * A comma-separated list of role script ids, drawn as role names in aqua. Each item links
         * to the role's guide, or its details page if it doesn't have one.
         */
        private void addRoleList(String roleIds, int width) {
            List<RoleLink> line = new ArrayList<>();
            int lineWidth = 0;
            String[] ids = roleIds.split(", ");
            for (int i = 0; i < ids.length; i++) {
                Role role = ROLES_BY_ID.get(ids[i]);
                String label = (role != null ? role.getDisplayName() : ids[i]) + (i < ids.length - 1 ? "," : "");
                int labelWidth = font.width(label);
                int gap = line.isEmpty() ? 0 : font.width(" ");
                if (!line.isEmpty() && lineWidth + gap + labelWidth > width) {
                    this.addEntry(new RoleListEntry(line));
                    line = new ArrayList<>();
                    lineWidth = 0;
                    gap = 0;
                }
                line.add(new RoleLink(label, role, lineWidth + gap, labelWidth));
                lineWidth += gap + labelWidth;
            }
            if (!line.isEmpty()) {
                this.addEntry(new RoleListEntry(line));
            }
        }

        /** One item of a role list: its drawn label, the guide it links to (or null), and its x span. */
        private record RoleLink(String label, Role role, int x, int width) {}

        private class RoleListEntry extends DocumentEntry {
            private final List<RoleLink> links;
            private int lastX;
            private int lastY;

            RoleListEntry(List<RoleLink> links) {
                this.links = links;
            }

            private RoleLink linkAt(double mouseX, double mouseY) {
                if (mouseY < lastY || mouseY >= lastY + font.lineHeight) return null;
                for (RoleLink link : links) {
                    if (link.role() != null && mouseX >= lastX + link.x() && mouseX < lastX + link.x() + link.width()) {
                        return link;
                    }
                }
                return null;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX();
                int y = getContentY();

                lastX = x;
                lastY = y;
                RoleLink hoveredLink = linkAt(mouseX, mouseY);
                for (RoleLink link : links) {
                    MutableComponent text = Component.literal(link.label()).withStyle(ChatFormatting.AQUA);
                    if (link == hoveredLink) text.withStyle(ChatFormatting.UNDERLINE);
                    context.text(font, text, x + link.x(), y, 0xFFCCCCCC, false);
                }
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                double mouseX = event.x();
                double mouseY = event.y();
                int button = event.button();

                RoleLink link = linkAt(mouseX, mouseY);
                if (button != GLFW.GLFW_MOUSE_BUTTON_1 || link == null) return false;
                savedScrollAmount = GuideContentWidget.this.scrollAmount();
                if (RoleGuides.has(link.role())) {
                    minecraft.gui.setScreen(new RoleGuideDetailsScreen(link.role(), AdvancedGuideScreen.this, RoleGuides.roles()));
                } else {
                    minecraft.gui.setScreen(new CharacterDetailsScreen(link.role(), AdvancedGuideScreen.this));
                }
                return true;
            }
        }

        private void addSpacer() {
            this.addEntry(DocumentEntry.spacer());
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        @Override
        protected int scrollBarX() {
            return this.getX() + this.width - 6;
        }
    }
}
