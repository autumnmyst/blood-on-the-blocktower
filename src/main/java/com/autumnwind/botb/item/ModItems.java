package com.autumnwind.botb.item;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registers mod items for Blood on the Blocktower.
 */
public class ModItems {

    // Script item - opens Script Reference screen on right-click
    public static final Item SCRIPT = registerItem("script", new Item.Settings());

    // Grimoire item - opens Grimoire (Assign Roles) screen on right-click
    public static final Item GRIMOIRE = registerItem("grimoire", new Item.Settings());

    // Setup stick - MB1 blocks to walk through map setup (see SetupStick). Not in the creative tab.
    public static final Item SETUP_STICK = registerItem("setup_stick", new Item.Settings().maxCount(1));

    /**
     * Registers an item with the given name and settings.
     */
    private static Item registerItem(String name, Item.Settings settings) {
        Identifier id = Identifier.of(BloodOnTheBlocktower.MOD_ID, name);
        return Registry.register(Registries.ITEM, id, new Item(settings));
    }

    /**
     * Initializes and registers all mod items.
     * Call this from the mod initializer.
     */
    public static void registerItems() {
        BloodOnTheBlocktower.LOGGER.info("Registering Blood on the Blocktower items");

        // Add items to the Tools creative tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(SCRIPT);
            entries.add(GRIMOIRE);
            // The setup stick is only handed out by /botb setup, so it stays out of the creative tab
        });
    }
}
