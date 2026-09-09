package com.autumnwind.botb.gui;

import com.autumnwind.botb.event.KeyInputHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.List;
import com.autumnwind.botb.gui.widget.DocumentEntry;

/**
 * Advanced Guide screen for storytellers - explains special casing in the Night Order HUD.
 * Layout: Category buttons on the left, scrollable documentation on the right.
 */
public class AdvancedGuideScreen extends Screen {

    private final Screen parent;
    private GuideContentWidget contentWidget;
    private GuideCategory selectedCategory = GuideCategory.CONDITIONAL_ROLES;

    public AdvancedGuideScreen(Screen parent) {
        super(Text.literal("Advanced Guide"));
        this.parent = parent;
    }

    private enum GuideCategory {
        CONDITIONAL_ROLES("Conditional Roles"),
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
                        this.client.setScreen(this);
                    }
            ).dimensions(leftColumnX, currentY, buttonWidth, buttonHeight).build());
            currentY += buttonHeight + buttonSpacing;
        }

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
                case CONDITIONAL_ROLES -> populateConditionalRoles(textWidth);
                case TRIGGERS -> populateTriggers(textWidth);
                case ASSOCIATED_ROLES -> populateAssociatedRoles(textWidth);
                case ABILITY_BLOCKING -> populateAbilityBlocking(textWidth);
                case GLOBAL_EFFECTS -> populateGlobalEffects(textWidth);
                case VOTE_MODIFICATION -> populateVoteModification(textWidth);
                case MADNESS_HUD -> populateMadnessHud(textWidth);
                case CUSTOM_ROLES -> populateCustomRoles(textWidth);
            }
        }

        private void populateConditionalRoles(int textWidth) {
            addTitle("Conditional Night Order");
            addSpacer();
            addBody("Some roles only appear in the night order when specific conditions are met. These roles are NOT manually markable - they appear automatically when their condition triggers.", textWidth);
            addSpacer();
            addSpacer();

            // Godfather
            addSubtitle("Godfather");
            addBody("Only wakes if an Outsider died today. Place the \"Died Today\" reminder on the dead Outsider to trigger the Godfather's visit.", textWidth);
            addSpacer();

            // Summoner
            addSubtitle("Summoner");
            addBody("Only wakes on Night 3 specifically. All other nights are automatically skipped regardless of any other state.", textWidth);
            addSpacer();

            // Witch
            addSubtitle("Witch");
            addBody("Only wakes if 4 or more players are alive. Once 3 or fewer players remain alive, the Witch is automatically skipped.", textWidth);
            addSpacer();

            // King
            addSubtitle("King");
            addBody("Only wakes on Other Nights if dead players >= living players. First night the Demon learns who they are.", textWidth);
            addSpacer();

            // Zombuul
            addSubtitle("Zombuul");
            addBody("Only wakes if no one died during the day. Place a Zombuul \"Died Today\" reminder on any player to prevent the Zombuul from waking. This also affects Lunatic with Zombuul associated.", textWidth);
            addSpacer();

            // Leviathan
            addSubtitle("Leviathan");
            addBody("After Day 5, if the Leviathan is alive and has their ability, a \"game over\" visit is automatically injected after Nominations. Evil wins!", textWidth);
            addSpacer();

            // Xaan
            addSubtitle("Xaan");
            addBody("Acts on night X only, where X is the number of Outsiders when the game started. That count is recorded at the first Dusk as a Xaan \"Night N\" reminder on the Xaan player. To override, replace it with another Night reminder or type a custom \"Night N\" reminder on the Xaan. That night and the following day, all Townsfolk count as poisoned. This can be applied manually by giving the Xaan its \"X\" reminder.", textWidth);
            addSpacer();

            // Riot
            addSubtitle("Riot");
            addBody("On night 3, a Riot visit targets every Minion. Visit each and change their character to Riot. On day 3, the Nominations visit carries a Riot modifier with the day's rules. Immediate deaths and the countdown are left to the Storyteller.", textWidth);
            addSpacer();

            // Undertaker
            addSubtitle("Undertaker");
            addBody("Only appears if there was an execution today and the Undertaker has their ability.", textWidth);
        }

        private void populateTriggers(int textWidth) {
            addTitle("Triggered Visits");
            addSpacer();
            addBody("Triggered visits appear dynamically either after dusk or after the current position in the night order when their condition is met. They occur as soon as possible regardless of their formal night order position.", textWidth);
            addSpacer();
            addSpacer();

            // Death Triggers section
            addTitle("Death Triggers");
            addSpacer();
            addBody("Death-based triggers activate when a player dies. There are four types based on HOW they died:", textWidth);
            addSpacer();

            addSubtitle("ANY Death (execution or night)");
            addRoleList("Hatter, Barber, Sweetheart, Plague Doctor", textWidth);
            addBody("Triggers on any death, including execution.", textWidth);
            addSpacer();

            addSubtitle("NIGHT Death Only");
            addRoleList("Farmer, Ravenkeeper", textWidth);
            addBody("Only triggers if killed at night (when day counter != night counter).", textWidth);
            addSpacer();

            addSubtitle("DEMON Kill Only");
            addRoleList("Sage, Banshee, Choirboy", textWidth);
            addBody("Only triggers if killed by the demon at night. To mark a demon kill, place the demon's \"Dead\" reminder on the victim (e.g., Imp's \"Dead\" reminder). Al-Hadikhia's \"1\"/\"2\"/\"3\" reminders also count.", textWidth);
            addSpacer();

            addSubtitle("OTHER Player Death");
            addRoleList("Grandmother, Scarlet Woman", textWidth);
            addBody("Grandmother triggers when her GRANDCHILD dies to the demon, not when Grandmother herself dies. The grandchild must have the \"Grandchild\" reminder from Grandmother AND die to a demon kill.", textWidth);
            addSpacer();
            addBody("Scarlet Woman triggers automatically when a Demon dies with 5 or more living non-travelers. Use the Fang Gu's \"Once\" reminder to suppress a Fang Gu jump.", textWidth);
            addSpacer();
            addSpacer();

            // Revival & Role Switch section
            addTitle("Revival & Role Switch Triggers");
            addSpacer();

            addSubtitle("Revival Triggers");
            addBody("When a player is resurrected (e.g., by Professor or Shabaloth), if their role has a first-night-only ability, a triggered visit is created with their first night instructions. No auto-teleport.", textWidth);
            addSpacer();

            addSubtitle("Role Switch Triggers");
            addBody("When a player's role changes mid-night (not during their own visit), a triggered visit is created informing them of their new role. If the new role has first-night-only instructions, those are included. No auto-teleport.", textWidth);
            addSpacer();

            addSubtitle("Cannibal Execution Reminder");
            addBody("When a living player is executed, a triggered visit is automatically created after Dusk for players with the Cannibal ability reminding that a living player was executed.", textWidth);
        }

        private void populateAssociatedRoles(int textWidth) {
            addTitle("Associated Roles");
            addSpacer();
            addBody("Associated roles are secondary abilities granted via special reminders. The reminder text is the role name in all capital letters (e.g., \"IMP\") with the role's icon and color boarder. These create additional visits at that role's night order position.", textWidth);
            addSpacer();
            addSpacer();

            // Philosopher
            addSubtitle("Philosopher");
            addBody("Add a role reminder (e.g., \"Empath\" with Empath icon) to grant that ability. The Philosopher will get visits at their original position AND the associated role's position. If the associated role is in play, remember to mark the original as drunk.", textWidth);
            addSpacer();

            // Cannibal
            addSubtitle("Cannibal");
            addBody("Works like Philosopher. For first-night-only roles (e.g., Washerwoman), a triggered visit is created with FN instructions. The Cannibal execution reminder helps track when to add the ability.", textWidth);
            addSpacer();

            // Pixie
            addSubtitle("Pixie");
            addHighlight("Pixie requires the \"Has Ability\" reminder to activate associated role visits.", textWidth);
            addBody("Without \"Has Ability\", Pixie associated roles are completely blocked. Add \"Has Ability\" when Pixie becomes mad about their townsfolk to enable their visits.", textWidth);
            addSpacer();

            // Lunatic
            addSubtitle("Lunatic");
            addBody("Lunatic has a demon as their associated role. On first night, they get the demon's FN instructions. On other nights, the demon's instructions are APPENDED to Lunatic's own visit (not a separate visit). Lunatic with Zombuul associated also checks for \"Died Today\" reminder.", textWidth);
            addSpacer();

            // Plague Doctor
            addSubtitle("Plague Doctor (Storyteller Ability)");
            addHighlight("Plague Doctor minion abilities require the \"Storyteller Ability\" reminder.", textWidth);
            addBody("When Plague Doctor dies, add \"Storyteller Ability\" reminder. Then add a minion role reminder to grant that minion's ability to the storyteller. These visits have NO auto-teleport (storyteller acts, not player).", textWidth);
            addSpacer();
            addSpacer();

            // FN-Only Associated Roles
            addTitle("First-Night-Only Associated Roles");
            addSpacer();
            addBody("When adding an associated role that only has first night abilities (or has a first night ability and triggered other-nights abilities), a special triggered visit is created with the first night instructions. While potentially anyone can be given first night associated roles, it mainly applies to the Philosopher, Cannibal, and Pixie.", textWidth);
        }

        private void populateAbilityBlocking(int textWidth) {
            addTitle("Ability Blocking");
            addSpacer();
            addBody("The \"No Ability\" reminder blocks player abilities. There are three types with different scopes:", textWidth);
            addSpacer();
            addSpacer();

            // Preacher
            addSubtitle("Preacher \"No Ability\"");
            addBody("Blocks ALL minion abilities for the target player. Add this reminder to a minion chosen by the Preacher. Only affects minion-type roles.", textWidth);
            addSpacer();

            // Role-specific
            addSubtitle("Role-Specific \"No Ability\"");
            addBody("The \"No Ability\" for a specific role (e.g., \"No Ability\" with Assassin icon) blocks only that specific role's ability. Used for once-per-game abilities that have been spent.", textWidth);
            addSpacer();

            // Generic
            addSubtitle("Generic \"No Ability\"");
            addBody("Adding \"No Ability\" WITHOUT any role icon (i.e. as a custom reminder) will block ALL abilities for that player. This prevents all visits for their assigned role and any associated roles.", textWidth);
            addSpacer();
            addSpacer();

            // Vigormortis
            addTitle("Vigormortis Exception");
            addSpacer();
            addHighlight("Vigormortis \"Has Ability\" overrides death-based deactivation.", textWidth);
            addBody("Normally, death-based roles lose their ability when dead (e.g., Undertaker). If a dead minion has Vigormortis \"Has Ability\" reminder, they remain active despite being dead. This is how Vigormortis keeps minion abilities active after killing them.", textWidth);
            addSpacer();
            addSpacer();

            // Exorcist
            addTitle("Exorcist \"Chosen\"");
            addSpacer();
            addBody("Demons with the Exorcist \"Chosen\" reminder are completely skipped from their night visit. The demon does not wake and cannot kill that night.", textWidth);
            addSpacer();
            addSpacer();

            // Droisoned & fake roles
            addTitle("Droisoned & Fake Roles");
            addSpacer();
            addHighlight("Drunk/poisoned and fake roles keep their own visits but stop affecting everything else.", textWidth);
            addBody("A player with any \"Drunk\" or \"Poisoned\" reminder (or anyone drunk from the Minstrel or poisoned by the Xaan) still wakes for their own visits, but their role no longer affects other visits or systems: the Magician/Poppy Grower/Snitch/Damsel/King/Marionette info modifiers, the Vortox effect icon and \"Tell them lies\" line, Legion vote handling, Organ Grinder mode, the Boffin granting the demon an ability, Wraith auto-teleports, the Scarlet Woman and Grandmother triggers, and the Barber/Hatter/Poppy Grower death triggers.", textWidth);
            addSpacer();
            addBody("Visits that only remind you to do something (no player wakes) are skipped entirely for a droisoned holder, since there is nothing to fake: Gossip, Tinker, Moonchild, Mezepheles (other nights), Cult Leader, Princess, Legion, Vizier, Riot, Leviathan (night 1 and the day 5 game over), and the Sweetheart/Plague Doctor/Farmer/Banshee death triggers.", textWidth);
            addSpacer();
            addBody("Fake roles are treated the same way. They get their (false) visits as normal but leave no other footprint. Which associated roles count as fake, and what else each one does:", textWidth);
            addSpacer();
            addBody("Drunk: townsfolk and minion associated roles are fake. The Drunk's DRUNK reminder is an identity, not impairment, so they still wake at the associated role's position with its normal instructions.", textWidth);
            addSpacer();
            addBody("Marionette: townsfolk, minion, and outsider associated roles are fake. The Marionette never wakes for Minion Info (or the Lil' Monsta minion visit) and has no first-night visit of their own; Demon Info instead carries a Marionette icon and paragraph and names the Marionette.", textWidth);
            addSpacer();
            addBody("Lunatic: demon associated roles are fake. On the first night the demon's first-night instructions ride on the Lunatic's own visit; on other nights the demon's instructions are appended to the Lunatic's visit rather than a separate demon visit. A Lunatic with Zombuul associated is skipped when a Zombuul \"Died Today\" reminder is placed.", textWidth);
            addSpacer();
            addBody("Hermit: townsfolk, minion, and demon associated roles are fake; outsider ones are real. A Hermit with Lunatic associated gets the Lunatic handling for its demon on other nights.", textWidth);
        }

        private void populateGlobalEffects(int textWidth) {
            addTitle("Global Effects");
            addSpacer();
            addBody("These effects impact multiple players and display as icon reminders on affected visits.", textWidth);
            addSpacer();
            addSpacer();

            // Minstrel
            addSubtitle("Minstrel \"Everyone Is Drunk\"");
            addBody("When active, ALL players except the Minstrel get a \"Drunk\" icon reminder on their visits. The Minstrel themselves does NOT get their own drunk token.", textWidth);
            addSpacer();

            // Vortox
            addSubtitle("Vortox In Play");
            addBody("When a Vortox is assigned, all Townsfolk visits display the Vortox effect icon reminder. This serves as a visual reminder that townsfolk information is false.", textWidth);
            addSpacer();

            // Xaan
            addSubtitle("Xaan Poisoning");
            addBody("On the Xaan's night (see Conditional Roles) and the following day, all Townsfolk display an \"X\" icon reminder on their visits and count as poisoned. Adding a Xaan \"X\" reminder to any player forces the icon on manually.", textWidth);
            addSpacer();

            // Lil' Monsta
            addSubtitle("Lil' Monsta");
            addBody("When Lil' Monsta \"Is The Demon\" global reminder is present, the normal Minion Info and Demon Info static actions are SKIPPED. Instead, all minions wake together for the Lil' Monsta visit.", textWidth);
            addSpacer();

            // Princess
            addSubtitle("Princess \"Doesn't Kill\"");
            addBody("Add \"Doesn't Kill\" to a demon player (not to Princess). This adds an icon reminder to the demon's visit with instruction: \"PRINCESS: The demon doesn't kill tonight.\"", textWidth);
            addSpacer();

            // Nominations modifiers
            addSubtitle("Nominations Modifiers");
            addBody("Roles that change how nominations or votes work add an icon to the Nominations static action, each with its own instruction line.", textWidth);
            addBody("Organ Grinder: votes are secret (players are blinded and see \"???\" as the result).", textWidth);
            addSpacer();
            addBody("Bishop: only the storyteller nominates, via the Storyteller head in the center.", textWidth);
            addSpacer();
            addBody("Legion: a vote where only evil players voted counts as zero, and players aren't told.", textWidth);
            addSpacer();
            addBody("Riot (day 3): A reminder on Nominations, but no special casing. The Storyteller must kill nominees give them the countdown to nominate again.", textWidth);
            addSpacer();

            // Fabled & Loric
            addSubtitle("Fabled & Loric Visits");
            addBody("Fabled and loric are never assigned to a seat, so their night visits are based on script presence.", textWidth);
            addBody("Storm Catcher: a night 1 visit waking every evil player.", textWidth);
            addSpacer();
            addBody("Tor: a storyteller reminder visit just before Dawn.", textWidth);
            addSpacer();
            addBody("Buddhist: an icon on Dawn.", textWidth);
            addSpacer();
            addBody("Toymaker: the Minion and Demon Info steps run under 7 players, and its \"Final Night: No Attack\" reminder on the demon adds a no-attack line to the demon's visit.", textWidth);
            addSpacer();

            // Al-Hadikhia Homebrew
            addSubtitle("Al-Hadikhia Homebrew");
            addBody("When enabled via the \"AH\" toggle in Storyteller Tools, an additional visit is injected at the start of other nights (after Wraith) asking all players if they want to live or die.", textWidth);
        }

        private void populateVoteModification(int textWidth) {
            addTitle("Vote Modification");
            addSpacer();
            addBody("Several roles and reminders modify voting behavior. Understanding these mechanics helps storytellers run votes correctly.", textWidth);
            addSpacer();
            addSpacer();

            // Banshee
            addTitle("Banshee");
            addSpacer();
            addSubtitle("How It Works");
            addBody("When a Banshee dies to the Demon, add the \"Has Ability\" reminder (with Banshee icon) to activate their voting power. The Banshee's vote can be toggled between being worth 1 and 2.", textWidth);
            addSpacer();
            addSpacer();

            // Voudon
            addTitle("Voudon (Traveler)");
            addSpacer();
            addSubtitle("How It Works");
            addBody("When Voudon is alive with their ability, voting eligibility is reversed:", textWidth);
            addRoleList("- Dead players can vote (like they're alive)", textWidth);
            addRoleList("- Alive players CANNOT vote (except the Voudon)", textWidth);
            addRoleList("- Used ghost votes still count (dead players can always vote)", textWidth);
            addSpacer();
            addHighlight("The mod detects Voudon with \"Has Ability\" and automatically enables Voudon mode when running votes.", textWidth);
            addSpacer();
            addBody("Voudon mode affects both nominations and executions, but NOT exiles (exile voting always allows everyone to participate).", textWidth);
            addSpacer();
            addSpacer();

            // Bureaucrat
            addTitle("Bureaucrat (Traveler)");
            addSpacer();
            addSubtitle("How It Works");
            addBody("Each night, the Bureaucrat chooses a player. That player's vote counts as 3 votes the next day.", textWidth);
            addSpacer();
            addHighlight("Add the \"3 Votes\" reminder (with Bureaucrat icon) to the chosen player.", textWidth);
            addSpacer();
            addBody("The mod automatically detects \"3 Votes\" reminders and applies the 3x multiplier during vote tallies.", textWidth);
            addSpacer();
            addSpacer();

            // Thief
            addTitle("Thief (Traveler)");
            addSpacer();
            addSubtitle("How It Works");
            addBody("Each night, the Thief chooses a player. That player's vote counts negatively (-1) the next day.", textWidth);
            addSpacer();
            addHighlight("Add the \"Negative Vote\" reminder (with Thief icon) to the chosen player.", textWidth);
            addSpacer();
            addBody("The mod automatically detects \"Negative Vote\" reminders and applies the -1x multiplier during vote tallies. This means when that player votes, the total goes DOWN by 1 instead of up.", textWidth);
            addSpacer();
            addSpacer();

            // Multiplier Stacking
            addTitle("Multiplier Stacking");
            addSpacer();
            addBody("Vote multipliers stack multiplicatively:", textWidth);
            addRoleList("Banshee (2) × Bureaucrat (3) = 6 votes", textWidth);
            addRoleList("Banshee (2) × Thief (-1) = -2 votes", textWidth);
            addRoleList("All three: 2 × 3 × -1 = -6 votes", textWidth);
            addSpacer();
            addBody("Note: These multipliers only affect executions, NOT exiles. Exile votes are never modified by abilities.", textWidth);
            addSpacer();
            addSpacer();

            // Beggar Ghost Vote Toggle
            addTitle("Beggar Ghost Vote Toggle");
            addSpacer();
            addSubtitle("Manual Ghost Vote Control");
            addBody("The Beggar's ability lets them give their ghost vote to another player. To toggle a dead player's ghost vote availability:", textWidth);
            addSpacer();
            addHighlight("Ctrl + Alt + Click on a dead player's head in the grimoire", textWidth);
            addSpacer();
            addBody("This toggles whether the player has used their ghost vote. Use this to:", textWidth);
            addRoleList("- Grant an extra ghost vote (Beggar gave theirs)", textWidth);
            addRoleList("- Remove a ghost vote (player gave theirs to Beggar)", textWidth);
            addRoleList("- Fix mistakes in ghost vote tracking", textWidth);
            addSpacer();
            addBody("The used ghost vote indicator (obsidian block below vote indicator) will update to reflect the change.", textWidth);
        }

        private void populateMadnessHud(int textWidth) {
            addTitle("Madness HUD");
            addSpacer();
            addBody("The Madness HUD displays active madness conditions in the bottom-left corner. For storytellers, it shows all players' madnesses. For players, it shows only their own madness conditions (except Mutant, which is storyteller-only).", textWidth);
            addSpacer();
            addBody("Press the Role HUD toggle key to expand the madness display and see detailed text descriptions.", textWidth);
            addSpacer();
            addSpacer();

            addTitle("Applying Madness");
            addSpacer();

            // Cerenovus
            addSubtitle("Cerenovus Madness");
            addBody("1. Add the \"Mad\" reminder (with Cerenovus icon) to the target player.", textWidth);
            addBody("2. Open the reminder screen for that player - you'll see a list of all good roles plus Goblin.", textWidth);
            addBody("3. Select a \"Mad: [Role]\" reminder to specify which role they must claim to be.", textWidth);
            addSpacer();
            addHighlight("The player sees: \"You must be mad that you are the [Role], or you might be executed.\"", textWidth);
            addSpacer();

            // Harpy
            addSubtitle("Harpy Madness");
            addBody("1. Add the \"Mad\" reminder (with Harpy icon) to the source player.", textWidth);
            addBody("2. Open the reminder screen for that player - you'll see a list of all players.", textWidth);
            addBody("3. Select the target player's name to specify who they must claim is evil.", textWidth);
            addSpacer();
            addHighlight("The player sees: \"You must be mad that [Target] is evil, or one or both of you might die.\"", textWidth);
            addSpacer();

            // Pixie
            addSubtitle("Pixie Madness");
            addBody("Add a Townsfolk role reminder (e.g., \"EMPATH\" with Empath icon) to the Pixie player. This indicates which role they're mad about being.", textWidth);
            addSpacer();
            addHighlight("The player sees: \"If you are mad that you are the [Role], you gain their ability when they die.\"", textWidth);
            addSpacer();
            addBody("Note: Add the \"Has Ability\" reminder when the Pixie successfully becomes mad AND the townsfolk dies. This enables their associated role visits.", textWidth);
            addSpacer();

            // Mutant
            addSubtitle("Mutant Madness");
            addHighlight("Mutant madness is automatic and storyteller-only.", textWidth);
            addBody("Any player assigned the Mutant role (or with Mutant as an associated role) automatically displays Mutant madness in the storyteller's HUD. This is not sent to the player, as they already know they're the Mutant (or don't if they're a drunk/lunatic hermit).", textWidth);
            addSpacer();
            addBody("The storyteller sees: \"If [Player] (Mutant) is mad they are an outsider, they might be executed.\"", textWidth);
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

        private void addRoleList(String roles, int width) {
            MutableText text = Text.literal(roles).formatted(Formatting.AQUA);
            for (OrderedText line : textRenderer.wrapLines(text, width)) {
                this.addEntry(DocumentEntry.text(textRenderer, line, 0xCCCCCC));
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

        public abstract class Entry extends ElementListWidget.Entry<Entry> {}

        public class TitleEntry extends Entry {
            private final Text text;

            public TitleEntry(String title) {
                this.text = Text.literal(title).formatted(Formatting.GOLD, Formatting.BOLD);
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawTextWithShadow(textRenderer, text, x, y, 0xFFFFFF);
            }

            @Override
            public List<? extends Element> children() {
                return Collections.emptyList();
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return Collections.emptyList();
            }
        }

        public class SubtitleEntry extends Entry {
            private final Text text;

            public SubtitleEntry(String subtitle) {
                this.text = Text.literal(subtitle).formatted(Formatting.WHITE, Formatting.UNDERLINE);
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawTextWithShadow(textRenderer, text, x, y, 0xFFFFFF);
            }

            @Override
            public List<? extends Element> children() {
                return Collections.emptyList();
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return Collections.emptyList();
            }
        }

        public class BodyEntry extends Entry {
            private final OrderedText text;

            public BodyEntry(OrderedText text) {
                this.text = text;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawText(textRenderer, text, x, y, 0xCCCCCC, false);
            }

            @Override
            public List<? extends Element> children() {
                return Collections.emptyList();
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return Collections.emptyList();
            }
        }

        public class SpacerEntry extends Entry {
            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            }

            @Override
            public List<? extends Element> children() {
                return Collections.emptyList();
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return Collections.emptyList();
            }
        }
    }
}
