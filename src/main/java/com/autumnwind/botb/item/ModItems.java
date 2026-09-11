package com.autumnwind.botb.item;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

/**
 * Registers mod items for Blood on the Blocktower.
 */
public class ModItems {

    // Script item - opens Script Reference screen on right-click
    public static final Item SCRIPT = registerItem("script", new Item.Properties());

    // Grimoire item - opens Grimoire (Assign Roles) screen on right-click
    public static final Item GRIMOIRE = registerItem("grimoire", new Item.Properties());

    // Setup stick - MB1 blocks to walk through map setup (see SetupStick). Not in the creative tab.
    public static final Item SETUP_STICK = registerItem("setup_stick", new Item.Properties().stacksTo(1));

    /**
     * Registers an item with the given name and settings.
     */
    private static Item registerItem(String name, Item.Properties settings) {
        Identifier id = Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, name);
        return Registry.register(BuiltInRegistries.ITEM, id, new Item(settings.setId(ResourceKey.create(Registries.ITEM, id))));
    }

    /**
     * Initializes and registers all mod items.
     * Call this from the mod initializer.
     */
    public static void registerItems() {
        BloodOnTheBlocktower.LOGGER.info("Registering Blood on the Blocktower items");

        // Add items to the Tools creative tab
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(SCRIPT);
            entries.accept(GRIMOIRE);
            // The setup stick is only handed out by /botb setup, so it stays out of the creative tab
        });
    }
}
