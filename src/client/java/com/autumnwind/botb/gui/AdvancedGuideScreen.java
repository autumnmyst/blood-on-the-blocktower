package com.autumnwind.botb.gui;

import com.autumnwind.botb.event.KeyInputHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.autumnwind.botb.gui.widget.DocumentEntry;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.RoleGuides;

/**
 * Advanced Guide screen for storytellers - explains special casing in the Night Order HUD.
 * Layout: Category buttons on the left, scrollable documentation on the right.
 */
public class AdvancedGuideScreen extends Screen {

    private final Screen parent;
    private GuideContentWidget contentWidget;
    private GuideCategory selectedCategory = GuideCategory.TRIGGERS;
    private double savedScrollAmount = 0.0;

    /** Lowercased display name to role. */
    private static final Map<String, Role> ROLES_BY_NAME = new HashMap<>();
    static {
        for (Role role : Role.values()) {
            if (role != Role.NO_ROLE) ROLES_BY_NAME.put(role.getDisplayName().toLowerCase(Locale.ROOT), role);
        }
    }

    public AdvancedGuideScreen(Screen parent) {
        super(Text.literal("Advanced Guide"));
        this.parent = parent;
    }

    private enum GuideCategory {
        TRIGGERS("Triggers"),
        ASSOCIATED_ROLES("Associated Roles"),
        ABILITY_BLOCKING("Ability Blocking"),
        GLOBAL_EFFECTS("Global Effects"),
        VOTE_MODIFICATION("Vote Modification"),
        MADNESS_HUD("Madness HUD"),
        CUSTOM_ROLES("Custom Roles");

        private final String displayName;

        GuideCategory(String displayName) {
            this.displayName = displayName;
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
            Formatting color = (category == selectedCategory) ? Formatting.YELLOW : Formatting.WHITE;
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(category.displayName).formatted(color),
                    button -> {
                        selectedCategory = cat;
                        savedScrollAmount = 0.0;
                        this.client.setScreen(this);
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight).build());
            currentY += buttonHeight + buttonSpacing;
        }

        // Role Guides opens its own screen rather than a category
        currentY += buttonSpacing;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Role Guides").formatted(Formatting.AQUA),
                button -> this.client.setScreen(new RoleGuidesScreen(this))
        ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight).build());

        // Back button
        int backButtonWidth = 60;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Back").formatted(Formatting.YELLOW),
                button -> this.client.setScreen(this.parent)
        ).dimensions(this.width - backButtonWidth - 10, this.height - 30, backButtonWidth, 20).build());

        // Content widget (right side)
        int contentX = leftColumnX + buttonWidth + 20;
        int contentY = 30;
        int contentWidth = this.width - contentX - 15;
        int contentHeight = this.height - contentY - 40;

        this.contentWidget = new GuideContentWidget(this.client, contentWidth, contentHeight, contentY, selectedCategory);
        this.contentWidget.setX(contentX);
        this.contentWidget.setScrollAmount(savedScrollAmount);
        this.addDrawableChild(this.contentWidget);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean exitKeyPressed = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E;
        boolean openAssignGuiPressed = KeyInputHandler.openAssignGui != null && KeyInputHandler.openAssignGui.matchesKey(keyCode, scanCode);
        if (exitKeyPressed || openAssignGuiPressed) {
            this.client.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Scrollable content widget that displays guide text based on selected category.
     */
    private class GuideContentWidget extends ElementListWidget<DocumentEntry> {

        public GuideContentWidget(MinecraftClient client, int width, int height, int y, GuideCategory category) {
            super(client, width, height, y, client.textRenderer.fontHeight + 2);
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
            addTitle("Triggered Visits");
            addSpacer();
            addBody("Triggered visits appear dynamically either after dusk or after the current position in the night order when their condition is met. They occur as soon as possible regardless of their formal night order position.", textWidth);
            addSpacer();
            addSpacer();

            addTitle("Conditional Visits");
            addSpacer();
            addBody("Some roles only appear in the night order when a condition is met. The visit shows up on its own when the condition holds.", textWidth);
            addRoleList("Godfather, Summoner, Witch, King, Zombuul, Leviathan, Xaan, Riot, Undertaker", textWidth);
            addSpacer();
            addSpacer();

            addTitle("Death Triggers");
            addSpacer();
            addBody("Death-based triggers activate when a player dies. There are four types based on HOW they died:", textWidth);
            addSpacer();

            addSubtitle("ANY Death (execution or night)");
            addRoleList("Hatter, Barber, Sweetheart, Plague Doctor, Poppy Grower", textWidth);
            addSpacer();

            addSubtitle("NIGHT Death Only");
            addRoleList("Farmer, Ravenkeeper", textWidth);
            addBody("Only triggers if killed at night (when day counter != night counter).", textWidth);
            addSpacer();

            addSubtitle("DEMON Kill Only");
            addRoleList("Sage, Banshee", textWidth);
            addBody("Only triggers if killed by the demon at night. To mark a demon kill, place the demon's \"Dead\" reminder on the victim (e.g., Imp's \"Dead\" reminder). Al-Hadikhia's \"1\"/\"2\"/\"3\" reminders also count.", textWidth);
            addSpacer();

            addSubtitle("OTHER Player Death");
            addRoleList("Grandmother, Choirboy, Scarlet Woman", textWidth);
            addBody("These fire on someone else's death, not the role holder's: the grandchild, the King, and the demon respectively.", textWidth);
            addSpacer();
            addSpacer();

            addTitle("Revival & Role Switch Triggers");
            addSpacer();

            addSubtitle("Revival Triggers");
            addBody("When a player is resurrected (e.g., by Professor or Shabaloth), if their role has a first-night-only ability, a triggered visit is created with their first night instructions. No auto-teleport.", textWidth);
            addSpacer();

            addSubtitle("Role Switch Triggers");
            addBody("When a player's role changes mid-night (not during their own visit), a triggered visit is created informing them of their new role. If the new role has first-night-only instructions, those are included. No auto-teleport.", textWidth);
        }

        private void populateAssociatedRoles(int textWidth) {
            addTitle("Associated Roles");
            addSpacer();
            addBody("Associated roles are secondary abilities granted via special reminders. The reminder text is the role name in all capital letters (e.g., \"IMP\") with the role's icon and color boarder. These create additional visits at that role's night order position.", textWidth);
            addSpacer();
            addBody("Roles that are built around them, each covered in Role Guides:", textWidth);
            addRoleList("Philosopher, Cannibal, Pixie, Alchemist, Boffin, Drunk, Marionette, Lunatic, Hermit, Plague Doctor", textWidth);
            addSpacer();
            addSpacer();

            addTitle("First-Night-Only Associated Roles");
            addSpacer();
            addBody("When adding an associated role that only has first night abilities (or has a first night ability and triggered other-nights abilities), a special triggered visit is created with the first night instructions. While potentially anyone can be given first night associated roles mid-game, it mainly applies to the Philosopher, Cannibal, and Pixie.", textWidth);
        }

        private void populateAbilityBlocking(int textWidth) {
            addTitle("Ability Blocking");
            addSpacer();
            addBody("The \"No Ability\" reminder blocks player abilities. There are three types with different scopes:", textWidth);
            addSpacer();
            addSpacer();

            addSubtitle("Preacher \"No Ability\"");
            addBody("Blocks ALL minion abilities for the target player. Add this reminder to a minion chosen by the Preacher. Only affects minion-type roles.", textWidth);
            addSpacer();

            addSubtitle("Role-Specific \"No Ability\"");
            addBody("The \"No Ability\" for a specific role (e.g., \"No Ability\" with Assassin icon) blocks only that specific role's ability. Used for once-per-game abilities that have been spent.", textWidth);
            addSpacer();

            addSubtitle("Generic \"No Ability\"");
            addBody("Adding \"No Ability\" WITHOUT any role icon (i.e. as a custom reminder) will block ALL abilities for that player. This prevents all visits for their assigned role and any associated roles.", textWidth);
            addSpacer();
            addSpacer();

            addTitle("\"Has Ability\" Overrides");
            addSpacer();
            addHighlight("A role's \"Has Ability\" reminder overrides death-based deactivation.", textWidth);
            addBody("Normally, death-based roles lose their ability when dead (e.g., Undertaker). A dead player carrying a \"Has Ability\" reminder stays active for visits and ability checks despite being dead.", textWidth);
            addSpacer();
            addSpacer();

            addTitle("Droisoned & Fake Roles");
            addSpacer();
            addHighlight("Drunk/poisoned and fake roles keep their own visits but stop affecting everything else.", textWidth);
            addBody("A player with any \"Drunk\" or \"Poisoned\" reminder (or anyone drunk from the Minstrel or poisoned by the Xaan) still wakes for their own visits, but their role no longer affects other visits or systems. That means no Minion Info or Demon Info modifiers from:", textWidth);
            addRoleList("Magician, Poppy Grower, Snitch, Damsel, King, Marionette", textWidth);
            addBody("No Vortox icon and \"Tell them lies\" line, Legion vote handling, Organ Grinder mode, Boffin ability for the demon, or Wraith auto-teleports from:", textWidth);
            addRoleList("Vortox, Legion, Organ Grinder, Boffin, Wraith", textWidth);
            addBody("And no triggers from:", textWidth);
            addRoleList("Scarlet Woman, Grandmother, Barber, Hatter, Poppy Grower", textWidth);
            addSpacer();
            addBody("Visits that only remind the Storyteller to do something (no player wakes) are skipped entirely for a droisoned holder, since there is nothing to fake:", textWidth);
            addRoleList("Gossip, Tinker, Moonchild, Cult Leader, Princess, Legion, Vizier, Riot", textWidth);
            addBody("Likewise the Mezepheles on other nights, the Leviathan on night 1 and its day 5 game over, and the death triggers of:", textWidth);
            addRoleList("Mezepheles, Leviathan, Sweetheart, Plague Doctor, Farmer, Banshee", textWidth);
            addSpacer();
            addBody("Fake roles are treated the same way. They get their (false) visits as normal but leave no other footprint. Certain role types count as fake for:", textWidth);
            addRoleList("Drunk, Marionette, Lunatic, Hermit", textWidth);
        }

        private void populateGlobalEffects(int textWidth) {
            addTitle("Global Effects");
            addSpacer();
            addBody("These effects impact multiple players and display as icon reminders on affected visits. Each is covered in Role Guides:", textWidth);
            addRoleList("Minstrel, Vortox, Xaan, Lil' Monsta, Princess, Al-Hadikhia", textWidth);
            addSpacer();
            addSpacer();

            addSubtitle("Nominations Modifiers");
            addBody("Roles that change how nominations or votes work add an icon to the Nominations static action, each with its own instruction line.", textWidth);
            addRoleList("Organ Grinder, Bishop, Legion, Riot", textWidth);
            addSpacer();

            addSubtitle("Fabled & Loric Visits");
            addBody("Fabled and loric are never assigned to a seat, so their night visits are based on script presence.", textWidth);
            addRoleList("Storm Catcher, Tor, Buddhist, Toymaker", textWidth);
        }

        private void populateVoteModification(int textWidth) {
            addTitle("Vote Modification");
            addSpacer();
            addBody("Several roles and reminders modify voting behavior. Roles that modify voting:", textWidth);
            addRoleList("Banshee, Voudon, Bureaucrat, Thief, God of Ug, Beggar, Organ Grinder, Legion, Butcher", textWidth);
            addSpacer();
            addSpacer();

            addTitle("Multiplier Stacking");
            addSpacer();
            addBody("Vote multipliers stack multiplicatively:", textWidth);
            addRoleList("Banshee (2) × Bureaucrat (3) = 6 votes", textWidth);
            addRoleList("God of Ug (2) × Thief (-1) = -2 votes", textWidth);
            addRoleList("All four: 2 × 2 × 3 × -1 = -12 votes", textWidth);
            addSpacer();
            addBody("Note: These multipliers only affect executions, NOT exiles. Exile votes are never modified by abilities.", textWidth);
            addSpacer();
            addSpacer();

            addTitle("Ghost Vote Toggle");
            addSpacer();
            addHighlight("Ctrl + Alt + Click on a dead player's head in the grimoire", textWidth);
            addBody("Toggles whether that player has used their ghost vote, for the Beggar or to fix a mistake. The used ghost vote indicator (obsidian block below vote indicator) updates to reflect this change.", textWidth);
        }

        private void populateMadnessHud(int textWidth) {
            addTitle("Madness HUD");
            addSpacer();
            addBody("The Madness HUD displays active madness conditions in the bottom-left corner. For storytellers, it shows all players' madnesses. For players, it shows only their own madness conditions (except Mutant, which is storyteller-only).", textWidth);
            addSpacer();
            addBody("Press the Role HUD toggle key to expand the madness display.", textWidth);
            addSpacer();
            addBody("Madness comes from reminders placed on players, and the player's HUD only updates when you send roles again. Roles that cause madness:", textWidth);
            addRoleList("Cerenovus, Harpy, Pixie, Mutant", textWidth);
        }

        private void populateCustomRoles(int textWidth) {
            addTitle("Custom Roles");
            addSpacer();
            addBody("Custom (homebrew) roles are supported using the official Blood on the Clocktower script JSON schema. Custom roles appear alongside official roles in the grimoire screen, script reference, and night order.", textWidth);
            addSpacer();
            addSpacer();

            addTitle("Script JSON Format");
            addSpacer();
            addBody("Scripts are imported via clipboard (import button in grimoire screen), as JSON arrays. Each role can be a string (official role ID) or an object (custom role definition).", textWidth);
            addSpacer();

            addSubtitle("Custom Role Object Fields");
            addBody("Required fields:", textWidth);
            addRoleList("id, name, team, ability", textWidth);
            addSpacer();
            addBody("Optional fields:", textWidth);
            addRoleList("image, flavor, firstNight, otherNight, firstNightReminder, otherNightReminder, reminders, remindersGlobal, setup, jinxes", textWidth);
            addSpacer();

            addSubtitle("Team Values");
            addBody("Valid team values: \"townsfolk\", \"outsider\", \"minion\", \"demon\", \"traveler\" (or \"traveller\"), \"fabled\", \"loric\".", textWidth);
            addSpacer();

            addSubtitle("Image Field");
            addBody("The image field can be a single URL string or an array of 1-3 URLs:", textWidth);
            addBody("- 1 URL: Used for all alignments", textWidth);
            addBody("- 2 URLs: [good, evil]", textWidth);
            addBody("- 3 URLs: [neutral, good, evil]", textWidth);
            addSpacer();

            addSubtitle("Night Order");
            addBody("Set firstNight and otherNight to decimal values (e.g., 15.5) to position the role in the night order. Use 0 or omit for roles that don't wake.", textWidth);
            addSpacer();
            addSpacer();

            addTitle("Almanac Support");
            addSpacer();
            addBody("Custom scripts can link to bloodstar.clocktica.com almanac pages for additional role documentation (Overview, Examples, How To Run, Tips).", textWidth);
            addSpacer();

            addSubtitle("Main Almanac");
            addBody("In the _meta object, add an \"almanac\" field with the URL to the almanac HTML page:", textWidth);
            addHighlight("\"almanac\": \"https://bloodstar.clocktica.com/p/YourScript/almanac.html\"", textWidth);
            addSpacer();
            addBody("This provides script-level data (Synopsis, Overview, Changelog) plus role documentation for all roles in that almanac.", textWidth);
            addSpacer();

            addSubtitle("Extra Almanacs");
            addBody("If your script includes custom roles documented in other almanacs, add their almanac URLs to the \"extraAlmanacs\" array:", textWidth);
            addHighlight("\"extraAlmanacs\": [\"https://bloodstar.clocktica.com/p/Other/almanac.html\", \"https://bloodstar.clocktica.com/p/Another/almanac.html\"]", textWidth);
            addSpacer();
            addBody("Extra almanacs only provide role data (not script-level data). Role data from extra almanacs is loaded first, then the main almanac overlays it (main takes precedence).", textWidth);
            addSpacer();

            addSubtitle("Role ID Matching");
            addBody("Almanac role IDs are matched by normalizing: lowercase, removing underscores/spaces/dashes. For example, \"My_Custom-Role\" matches \"mycustomrole\" in the almanac HTML.", textWidth);
            addSpacer();
            addSpacer();

            addTitle("Example _meta Object");
            addSpacer();
            addBody("{", textWidth);
            addBody("  \"id\": \"_meta\",", textWidth);
            addBody("  \"name\": \"My Custom Script\",", textWidth);
            addBody("  \"author\": \"Your Name\",", textWidth);
            addBody("  \"logo\": \"https://example.com/logo.png\",", textWidth);
            addBody("  \"almanac\": \"https://bloodstar.clocktica.com/p/.../almanac.html\",", textWidth);
            addBody("  \"extraAlmanacs\": [\"https://bloodstar.clocktica.com/p/.../almanac.html\"]", textWidth);
            addBody("}", textWidth);
        }

        // Helper methods for adding entries
        private void addTitle(String text) {
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal(text).formatted(Formatting.GOLD, Formatting.BOLD)));
        }

        private void addSubtitle(String text) {
            this.addEntry(DocumentEntry.title(textRenderer, Text.literal(text).formatted(Formatting.WHITE, Formatting.UNDERLINE)));
        }

        private void addBody(String text, int width) {
            for (OrderedText line : textRenderer.wrapLines(Text.literal(text), width)) {
                this.addEntry(DocumentEntry.text(textRenderer, line, 0xCCCCCC));
            }
        }

        private void addHighlight(String text, int width) {
            MutableText highlighted = Text.literal(text).formatted(Formatting.GOLD);
            for (OrderedText line : textRenderer.wrapLines(highlighted, width)) {
                this.addEntry(DocumentEntry.text(textRenderer, line, 0xCCCCCC));
            }
        }

        /**
         * A comma-separated list in aqua. Items that name a role become links to its guide, or
         * its details page if it doesn't have one.
         */
        private void addRoleList(String roles, int width) {
            List<RoleLink> line = new ArrayList<>();
            int lineWidth = 0;
            String[] items = roles.split(", ");
            for (int i = 0; i < items.length; i++) {
                String label = items[i] + (i < items.length - 1 ? "," : "");
                int labelWidth = textRenderer.getWidth(label);
                int gap = line.isEmpty() ? 0 : textRenderer.getWidth(" ");
                if (!line.isEmpty() && lineWidth + gap + labelWidth > width) {
                    this.addEntry(new RoleListEntry(line));
                    line = new ArrayList<>();
                    lineWidth = 0;
                    gap = 0;
                }
                line.add(new RoleLink(label, ROLES_BY_NAME.get(items[i].toLowerCase(Locale.ROOT)), lineWidth + gap, labelWidth));
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
                if (mouseY < lastY || mouseY >= lastY + textRenderer.fontHeight) return null;
                for (RoleLink link : links) {
                    if (link.role() != null && mouseX >= lastX + link.x() && mouseX < lastX + link.x() + link.width()) {
                        return link;
                    }
                }
                return null;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                lastX = x;
                lastY = y;
                RoleLink hoveredLink = linkAt(mouseX, mouseY);
                for (RoleLink link : links) {
                    MutableText text = Text.literal(link.label()).formatted(Formatting.AQUA);
                    if (link == hoveredLink) text.formatted(Formatting.UNDERLINE);
                    context.drawText(textRenderer, text, x + link.x(), y, 0xCCCCCC, false);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                RoleLink link = linkAt(mouseX, mouseY);
                if (button != GLFW.GLFW_MOUSE_BUTTON_1 || link == null) return false;
                savedScrollAmount = GuideContentWidget.this.getScrollAmount();
                if (RoleGuides.has(link.role())) {
                    client.setScreen(new RoleGuideDetailsScreen(link.role(), AdvancedGuideScreen.this, RoleGuides.roles()));
                } else {
                    client.setScreen(new CharacterDetailsScreen(link.role(), AdvancedGuideScreen.this));
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
        protected int getScrollbarX() {
            return this.getX() + this.width - 6;
        }
    }
}
