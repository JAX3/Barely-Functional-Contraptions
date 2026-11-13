package com.jax3.barelyfunctionalcontraptions.sheep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.RedstoneMode;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.RelativeSide;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusion;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

/** Unit tests for logic that does not need a running game. In-world behaviour is covered by the game tests. */
class PureLogicTest {
    @Test
    void relativeSidesRoundTripForEveryFacing() {
        for (Direction facing : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            for (RelativeSide side : RelativeSide.values()) {
                assertEquals(side, RelativeSide.of(facing, side.toDirection(facing)), facing + "/" + side);
            }
        }
        assertEquals(RelativeSide.FRONT, RelativeSide.of(Direction.NORTH, Direction.NORTH));
        assertEquals(RelativeSide.BACK, RelativeSide.of(Direction.NORTH, Direction.SOUTH));
        assertEquals(RelativeSide.TOP, RelativeSide.of(Direction.EAST, Direction.UP));
    }

    @Test
    void guideContentIsWellFormed() throws Exception {
        try (var in = getClass().getResourceAsStream("/assets/barelyfunctionalcontraptions/guide/en_us.json")) {
            assertTrue(in != null, "the guide's en_us.json is in the jar");
            var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var chapters = json.getAsJsonArray("chapters");
            assertTrue(chapters.size() >= 10 && chapters.size() <= 12, "the chapter list fits the screen: " + chapters.size());
            for (var element : chapters) {
                var chapter = element.getAsJsonObject();
                assertFalse(chapter.get("title").getAsString().isBlank());
                assertTrue(Identifier.tryParse(chapter.get("icon").getAsString()) != null, "icon id " + chapter.get("icon"));
                assertFalse(chapter.getAsJsonArray("text").isEmpty(), chapter.get("title") + " has text");
            }
        }
    }

    @Test
    void pipeSideFilters() {
        var iron = Identifier.withDefaultNamespace("iron_ingot");
        var side = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.DEFAULT;
        assertTrue(side.allows(iron) && side.isDefault(), "no filter lets everything through");
        side = side.toggle(iron);
        assertTrue(side.allows(iron) && !side.allows(Identifier.withDefaultNamespace("gold_ingot")), "a filter only lets its entries through");
        assertTrue(side.toggle(iron).isDefault(), "toggling again removes the entry");
        var full = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.DEFAULT;
        for (int i = 0; i < com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.MAX_FILTER; i++) full = full.toggle(Identifier.withDefaultNamespace("item_" + i));
        assertEquals(full, full.toggle(Identifier.withDefaultNamespace("one_more")), "a full filter takes no more");
        assertEquals(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.NORMAL,
                com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.DISABLED.next(), "the modes cycle round");
    }

    @Test
    void energyMeterSignalScale() {
        int[][] expected = {{0, 0}, {1, 1}, {10, 3}, {100, 5}, {1_000, 8}, {10_000, 10}, {100_000, 13}, {1_000_000, 15}, {Integer.MAX_VALUE, 15}};
        for (int[] pair : expected) {
            assertEquals(pair[1], com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyMeterBlockEntity.signalFor(pair[0]), pair[0] + " FE/t");
        }
    }

    @Test
    void parsesSideNamesLeniently() {
        assertEquals(RelativeSide.LEFT, RelativeSide.parse(" left "));
        assertNull(RelativeSide.parse("sideways"));
    }

    @Test
    void redstoneModes() {
        assertTrue(RedstoneMode.IGNORED.allows(false));
        assertTrue(RedstoneMode.HIGH.allows(true));
        assertFalse(RedstoneMode.HIGH.allows(false));
        assertTrue(RedstoneMode.LOW.allows(false));
        assertFalse(RedstoneMode.DISABLED.allows(true));
        assertEquals(RedstoneMode.IGNORED, RedstoneMode.DISABLED.next());
        assertEquals(RedstoneMode.IGNORED, RedstoneMode.byId(99));
    }

    @Test
    void infusionCountsDownAndClears() {
        Identifier iron = Identifier.fromNamespaceAndPath("barelyfunctionalcontraptions", "iron");
        SheepInfusion infusion = SheepInfusion.NONE.withResource(iron, 2);
        infusion = infusion.afterShearing(100, 0);
        assertEquals(Optional.of(iron), infusion.resource());
        assertEquals(1, infusion.usesRemaining());
        infusion = infusion.afterShearing(200, 50);
        assertFalse(infusion.isInfused());
        assertTrue(infusion.onCooldown(220));
        assertFalse(infusion.onCooldown(250));
    }

    @Test
    void permanentInfusionNeverRunsOut() {
        Identifier gold = Identifier.fromNamespaceAndPath("barelyfunctionalcontraptions", "gold");
        SheepInfusion infusion = SheepInfusion.NONE.withResource(gold, -1);
        for (int i = 0; i < 100; i++) infusion = infusion.afterShearing(i, 0);
        assertTrue(infusion.isInfused());
    }
}
