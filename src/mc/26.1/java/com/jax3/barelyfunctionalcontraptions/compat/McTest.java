package com.jax3.barelyfunctionalcontraptions.compat;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.advancements.Criterion;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * Calls the GameTests and the screenshot harness make that differ between Minecraft versions (Minecraft 26.1). Dev only:
 * left out of the released jar with them.
 */
public final class McTest {
    private McTest() {}

    /** A server player in {@code gameType} that isn't connected to anything. */
    public static ServerPlayer mockServerPlayer(GameTestHelper h, GameType gameType) {
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new GameProfile(UUID.randomUUID(), "test-mock-player"),
                ClientInformation.createDefault()) {
            @Override
            public GameType gameMode() {
                return gameType;
            }

            @Override
            public boolean isClientAuthoritative() {
                return false;
            }
        };
        gameType.updatePlayerAbilities(player.getAbilities());
        return player;
    }

    /** Whether an inventory_changed criterion is met by picking up {@code changed}. */
    public static boolean inventoryChangeMatches(Criterion<?> criterion, Inventory inventory, ItemStack changed) {
        return ((net.minecraft.advancements.criterion.InventoryChangeTrigger.TriggerInstance) criterion.triggerInstance()).matches(inventory, changed, 0, 0, 1);
    }

    /** A superflat world for the screenshot harness. */
    public static WorldDimensions testWorldDimensions(HolderLookup.Provider registries) {
        return WorldPresets.createFlatWorldDimensions(registries);
    }

    /** NeoForge's mod list (screenshot harness). Client only. */
    public static Screen modListScreen(Screen parent) {
        return new net.neoforged.neoforge.client.gui.ModListScreen(parent);
    }
}
