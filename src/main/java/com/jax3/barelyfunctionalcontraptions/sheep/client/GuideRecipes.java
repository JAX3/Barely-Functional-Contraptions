package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.BarelyFunctionalContraptions;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
import com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem;
import com.jax3.barelyfunctionalcontraptions.sheep.recipe.SeparationLookup;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.Separation;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The guide's Recipes tab, built from the game when the book opens rather than from the guide's JSON, so it always
 * matches the recipes, resources and config in play (data packs and modpack tweaks included):
 * <ul>
 *   <li><b>Machines</b> and <b>Parts and Tools</b>: every crafting recipe of this mod, as a 3x3 grid;</li>
 *   <li><b>Infusion</b>: the item each sheep is fed, and the wool it then grows;</li>
 *   <li><b>Infusion Altar</b>: the altar's catalysts round the feeding item, for the sheep that need it;</li>
 *   <li><b>Mechanical Separator</b>: what every wool separates into, with its power and time;</li>
 *   <li><b>Wool Bales</b>: packing, unpacking and separating bales.</li>
 * </ul>
 */
public final class GuideRecipes {
    /** One slot of a recipe, relative to the recipe's top left; its options cycle once a second. */
    public record Slot(int x, int y, List<ItemStack> options, @Nullable String label) {
        Slot(int x, int y, List<ItemStack> options) {
            this(x, y, options, null);
        }

        /** The stack to show now: all slots of a recipe cycle together, so they stay in step. */
        public ItemStack shown(long tick) {
            return options.isEmpty() ? ItemStack.EMPTY : options.get((int) (tick % options.size()));
        }
    }

    /** One recipe on a page: a title, its slots, an arrow, and notes under it. */
    public record Recipe(Component title, List<Slot> slots, int arrowX, int arrowY, List<Component> notes, int bodyHeight) {
        public int height() {
            return 11 + bodyHeight + notes.size() * 9 + 6;
        }
    }

    public static final int SLOT = 18;
    /** Where a separator's first output goes, and the step between outputs. */
    private static final int OUT = 64, OUT_GAP = 32;
    /** Items that aren't machines: they go in Parts and Tools. */
    private static final Set<String> PARTS = Set.of("functional_machine_block", "contraptions_manual", "mycelium_wrench",
            "woollen_boots",
            "energy_cable", "fluid_pipe", "item_pipe");

    private GuideRecipes() {}

    private static Component t(String key, Object... args) {
        return Component.translatable("guide.barelyfunctionalcontraptions.recipes." + key, args);
    }

    /** The Recipes tab's chapters, in order; chapters with nothing to show are left out. */
    public static List<GuideBook.Chapter> chapters() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        List<GuideBook.Chapter> result = new ArrayList<>();
        if (level == null) return result;
        ContextMap context = SlotDisplayContext.fromLevel(level);
        List<SheepResource> resources = SheepResourceRegistry.client().all().stream()
                .sorted(Comparator.comparing(r -> r.id().toString())).toList();

        // Crafting recipes, machines first.
        List<Recipe> machines = new ArrayList<>(), parts = new ArrayList<>();
        List<RecipeHolder<?>> crafting = new ArrayList<>(SeparationLookup.clientRecipeMap().byType(RecipeType.CRAFTING));
        crafting.removeIf(h -> !h.id().identifier().getNamespace().equals(BarelyFunctionalContraptions.MOD_ID));
        // Dyeing Woollen Boots works as it does for leather; one recipe is enough to show.
        crafting.removeIf(h -> h.id().identifier().getPath().endsWith("_dyed"));
        crafting.sort(Comparator.comparingInt(h -> sortKey(h, context)));
        for (RecipeHolder<?> holder : crafting) {
            for (RecipeDisplay display : holder.value().display()) {
                Recipe recipe = crafting(display, context);
                if (recipe == null) continue;
                ItemStack out = display.result().resolveForFirstStack(context);
                String path = BuiltInRegistries.ITEM.getKey(out.getItem()).getPath();
                (PARTS.contains(path) ? parts : machines).add(recipe);
            }
        }
        add(result, "machines", new ItemStack(ModItems.FUNCTIONAL_MACHINE_BLOCK.get()), machines);
        add(result, "items", new ItemStack(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get()), parts);

        // Infusion by hand and at the altar.
        List<Recipe> infusion = new ArrayList<>(), altar = new ArrayList<>();
        for (SheepResource r : resources) {
            List<ItemStack> feed = r.feedingItems().stream().map(h -> new ItemStack(h, r.effectiveFeedingAmount())).toList();
            ItemStack wool = ResourceWoolItem.create(r.id(), 1);
            boolean altarOnly = r.altar().altarOnly();
            if (!altarOnly) {
                infusion.add(new Recipe(r.name(), List.of(new Slot(0, 0, feed), new Slot(56, 0, List.of(wool))), 24, 1,
                        List.of(t("by_hand", r.effectiveInfusionUses())), SLOT));
            }
            if (altarOnly || !r.altar().catalysts().isEmpty()) {
                List<Slot> slots = new ArrayList<>();
                slots.add(new Slot(SLOT, SLOT, feed));
                int[][] around = {{SLOT, 0}, {SLOT * 2, SLOT}, {SLOT, SLOT * 2}, {0, SLOT}};
                // All four catalyst slots, laid out like the altar's window; the unused ones stay empty.
                for (int c = 0; c < around.length; c++) {
                    if (c < r.altar().catalysts().size()) {
                        var need = r.altar().catalysts().get(c);
                        slots.add(new Slot(around[c][0], around[c][1], need.ingredient().items().map(h -> new ItemStack(h, need.count())).toList()));
                    } else {
                        slots.add(new Slot(around[c][0], around[c][1], List.of()));
                    }
                }
                slots.add(new Slot(98, SLOT, List.of(wool)));
                altar.add(new Recipe(r.name(), slots, 66, SLOT + 1, List.of(t("altar_note", r.altar().tier(),
                        String.format(Locale.ROOT, "%,d", r.altar().effectiveEnergy()))), SLOT * 3));
            }
        }
        add(result, "infusion", new ItemStack(Items.IRON_INGOT), infusion);
        add(result, "altar", new ItemStack(ModItems.INFUSION_ALTAR.get()), altar);

        // What each wool separates into.
        List<Recipe> separator = new ArrayList<>();
        for (SheepResource r : resources) {
            ItemStack wool = ResourceWoolItem.create(r.id(), 1);
            SeparationLookup.find(level, wool).ifPresent(s -> separator.add(separation(r.name(), wool, s)));
        }
        add(result, "separator", new ItemStack(ModItems.MECHANICAL_SEPARATOR.get()), separator);

        // Wool Bales: pack, unpack, and separate.
        List<Recipe> bales = new ArrayList<>();
        if (!resources.isEmpty()) {
            List<ItemStack> nine = resources.stream().map(r -> ResourceWoolItem.create(r.id(), WoolBaleItem.WOOL_PER_BALE)).toList();
            List<ItemStack> one = resources.stream().map(r -> WoolBaleItem.create(r.id(), 1)).toList();
            bales.add(new Recipe(new ItemStack(ModItems.WOOL_PACKER.get()).getHoverName(),
                    List.of(new Slot(0, 0, nine), new Slot(56, 0, one)), 24, 1, List.of(t("packer")), SLOT));
            bales.add(new Recipe(t("unpack"), List.of(new Slot(0, 0, one), new Slot(56, 0, nine)), 24, 1, List.of(t("unpack_note")), SLOT));
            for (SheepResource r : resources) {
                ItemStack bale = WoolBaleItem.create(r.id(), 1);
                SeparationLookup.find(level, bale).ifPresent(s -> bales.add(separation(bale.getHoverName(), bale, s)));
            }
        }
        add(result, "bales", new ItemStack(ModItems.WOOL_PACKER.get()), bales);
        return result;
    }

    private static void add(List<GuideBook.Chapter> chapters, String key, ItemStack icon, List<Recipe> recipes) {
        if (recipes.isEmpty()) return;
        chapters.add(new GuideBook.Chapter(t(key).getString(), icon, GuideBook.Tab.RECIPES, List.of(), List.copyOf(recipes)));
    }

    /** Machines in the order they're registered, which is roughly the order they're unlocked. */
    private static int sortKey(RecipeHolder<?> holder, ContextMap context) {
        return holder.value().display().stream().findFirst()
                .map(d -> BuiltInRegistries.ITEM.getId(d.result().resolveForFirstStack(context).getItem()))
                .orElse(Integer.MAX_VALUE);
    }

    /** A shaped or shapeless crafting recipe as a 3x3 grid, or null for any other display. */
    static @Nullable Recipe crafting(RecipeDisplay display, ContextMap context) {
        List<Slot> slots = new ArrayList<>();
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            for (int i = 0; i < shaped.ingredients().size(); i++) {
                List<ItemStack> options = stacks(shaped.ingredients().get(i), context);
                if (!options.isEmpty()) slots.add(new Slot((i % shaped.width()) * SLOT, (i / shaped.width()) * SLOT, options));
            }
        } else if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            for (int i = 0; i < shapeless.ingredients().size() && i < 9; i++) {
                slots.add(new Slot((i % 3) * SLOT, (i / 3) * SLOT, stacks(shapeless.ingredients().get(i), context)));
            }
        } else {
            return null;
        }
        // Empty grid cells are still drawn, so the shape reads like a crafting table.
        List<Slot> grid = new ArrayList<>();
        for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) {
            int sx = x * SLOT, sy = y * SLOT;
            if (slots.stream().noneMatch(s -> s.x() == sx && s.y() == sy)) grid.add(new Slot(sx, sy, List.of()));
        }
        grid.addAll(slots);
        ItemStack result = display.result().resolveForFirstStack(context);
        if (result.isEmpty()) return null;
        grid.add(new Slot(98, SLOT, List.of(result)));
        return new Recipe(result.getHoverName(), grid, 66, SLOT + 1, List.of(), SLOT * 3);
    }

    private static List<ItemStack> stacks(SlotDisplay display, ContextMap context) {
        return display.resolveForStacks(context);
    }

    /** {@code input} in the separator: its main output (as a range), its second output and its chance of a bonus. */
    static Recipe separation(Component title, ItemStack input, Separation s) {
        List<Slot> slots = new ArrayList<>();
        slots.add(new Slot(0, 0, List.of(input)));
        // Outputs spaced out so their counts ("24-48") don't run into the slot or arrow before them.
        slots.add(new Slot(OUT, 0, List.of(s.primary().create()), s.primaryLabel()));
        s.secondary().ifPresent(sec -> slots.add(new Slot(OUT + OUT_GAP, 0, List.of(sec.create()))));
        s.bonus().ifPresent(b -> slots.add(new Slot(OUT + OUT_GAP * 2, 0, List.of(b.item().create()),
                Math.round(b.chance() * 100) + "%")));
        return new Recipe(title, slots, 24, 1, List.of(t("energy", String.format(Locale.ROOT, "%,d", s.effectiveEnergy()),
                String.format(Locale.ROOT, "%.1f", s.effectiveTicks() / 20.0))), SLOT);
    }
}
