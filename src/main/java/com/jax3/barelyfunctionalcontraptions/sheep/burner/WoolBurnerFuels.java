package com.jax3.barelyfunctionalcontraptions.sheep.burner;

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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The Wool Burner's fuels ({@link WoolBurnerFuel}), loaded from
 * {@code data/<namespace>/barelyfunctionalcontraptions/wool_burner_fuel/**.json} on every {@code /reload} and sent to
 * clients (so the burner's slot and tooltips know what burns). Invalid files are skipped with an error in the log;
 * files may use NeoForge load conditions.
 */
public final class WoolBurnerFuels extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    public static final Identifier ID = ResourcefulSheep.id("wool_burner_fuel");
    public static final FileToIdConverter LISTER = FileToIdConverter.json("barelyfunctionalcontraptions/wool_burner_fuel");

    private static volatile List<WoolBurnerFuel> server = List.of();
    private static volatile List<WoolBurnerFuel> client = List.of();

    /** The fuels on this side: the server's when a server is running here, else the copy sent to this client. */
    public static List<WoolBurnerFuel> all() {
        return server.isEmpty() ? client : server;
    }

    /** The best fuel for {@code stack} (the most energy in all), if it burns. */
    public static Optional<WoolBurnerFuel> of(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        return all().stream().filter(f -> f.matches(stack)).max(Comparator.comparingLong(WoolBurnerFuel::totalEnergy));
    }

    public static boolean isFuel(ItemStack stack) {
        return of(stack).isPresent();
    }

    /** Replaces the server's fuels (also used by tests). */
    public static void setServer(List<WoolBurnerFuel> fuels) {
        server = List.copyOf(fuels);
    }

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> raw = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : LISTER.listMatchingResources(manager).entrySet()) {
            Identifier id = LISTER.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                raw.put(id, StrictJsonParser.parse(reader));
            } catch (IOException | JsonParseException | IllegalArgumentException e) {
                ResourcefulSheep.LOGGER.error("Wool Burner fuel '{}' is not valid JSON ({}): {}", id, entry.getKey(), e.getMessage());
            }
        }
        return raw;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> raw, ResourceManager manager, ProfilerFiller profiler) {
        DynamicOps<JsonElement> ops = makeConditionalOps();
        var codec = ConditionalOps.createConditionalCodec(WoolBurnerFuel.CODEC);
        List<WoolBurnerFuel> loaded = new ArrayList<>();
        raw.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            DataResult<Optional<WoolBurnerFuel>> result = codec.parse(ops, entry.getValue());
            if (result.error().isPresent()) {
                ResourcefulSheep.LOGGER.error("Skipping invalid Wool Burner fuel '{}': {}", entry.getKey(), result.error().get().message());
                return;
            }
            result.getOrThrow().ifPresent(loaded::add);
        });
        setServer(loaded);
        ResourcefulSheep.LOGGER.info("Loaded {} Wool Burner fuels", loaded.size());
    }

    /** Server -> client: every loaded fuel (on join and after {@code /reload}). */
    public record SyncPayload(List<WoolBurnerFuel> fuels) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(ResourcefulSheep.id("sync_wool_burner_fuels"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> STREAM_CODEC = StreamCodec.composite(
                WoolBurnerFuel.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncPayload::fuels,
                SyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public void handle(IPayloadContext context) {
            client = List.copyOf(fuels);
        }
    }

    /** Game-bus listeners. */
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void addReloadListener(AddServerReloadListenersEvent event) {
            event.addListener(ID, new WoolBurnerFuels());
        }

        @SubscribeEvent
        public static void onDatapackSync(OnDatapackSyncEvent event) {
            var payload = new SyncPayload(server);
            event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            setServer(List.of());
        }
    }
}
