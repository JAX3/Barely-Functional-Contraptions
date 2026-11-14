package com.jax3.barelyfunctionalcontraptions.sheep.furnace;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Loads the Blaze Sheep Furnace's heat sources ({@link FurnaceHeat}) from
 * {@code data/<namespace>/barelyfunctionalcontraptions/furnace_heat/**.json}, again on every {@code /reload}. Invalid
 * files are skipped with an error in the log; files may use NeoForge load conditions.
 */
public final class FurnaceHeatManager extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    public static final Identifier ID = ResourcefulSheep.id("furnace_heat");
    public static final FileToIdConverter LISTER = FileToIdConverter.json("barelyfunctionalcontraptions/furnace_heat");

    /** Loaded sources, most specific first. Only the server reads them. */
    private static volatile List<Map.Entry<Identifier, FurnaceHeat>> sources = List.of();

    public static List<Map.Entry<Identifier, FurnaceHeat>> sources() {
        return sources;
    }

    /** The heat source {@code entity} is right now, if any (the most specific match; a power of 0 means none). */
    public static Optional<FurnaceHeat> heatOf(Entity entity) {
        for (Map.Entry<Identifier, FurnaceHeat> entry : sources) {
            if (entry.getValue().matches(entity)) {
                return entry.getValue().power() > 0 ? Optional.of(entry.getValue()) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    /** Replaces the loaded sources (used by tests). */
    public static void set(Map<Identifier, FurnaceHeat> loaded) {
        List<Map.Entry<Identifier, FurnaceHeat>> list = new ArrayList<>(loaded.entrySet());
        list.sort(Comparator.<Map.Entry<Identifier, FurnaceHeat>>comparingInt(e -> e.getValue().specificity())
                .thenComparing(e -> e.getKey().toString()));
        sources = List.copyOf(list);
    }

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> raw = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : LISTER.listMatchingResources(manager).entrySet()) {
            Identifier id = LISTER.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                raw.put(id, StrictJsonParser.parse(reader));
            } catch (IOException | JsonParseException | IllegalArgumentException e) {
                ResourcefulSheep.LOGGER.error("Furnace heat source '{}' is not valid JSON ({}): {}", id, entry.getKey(), e.getMessage());
            }
        }
        return raw;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> raw, ResourceManager manager, ProfilerFiller profiler) {
        DynamicOps<JsonElement> ops = makeConditionalOps();
        var codec = ConditionalOps.createConditionalCodec(FurnaceHeat.CODEC);
        Map<Identifier, FurnaceHeat> loaded = new HashMap<>();
        for (Map.Entry<Identifier, JsonElement> entry : raw.entrySet()) {
            DataResult<Optional<FurnaceHeat>> result = codec.parse(ops, entry.getValue());
            if (result.error().isPresent()) {
                ResourcefulSheep.LOGGER.error("Skipping invalid furnace heat source '{}': {}", entry.getKey(), result.error().get().message());
                continue;
            }
            result.getOrThrow().ifPresent(heat -> loaded.put(entry.getKey(), heat));
        }
        set(loaded);
        ResourcefulSheep.LOGGER.info("Loaded {} Blaze Sheep Furnace heat sources", loaded.size());
    }

    /** Game-bus listeners. */
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void addReloadListener(AddServerReloadListenersEvent event) {
            event.addListener(ID, new FurnaceHeatManager());
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            set(Map.of());
        }
    }
}
