package com.jax3.barelyfunctionalcontraptions.mycelial.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Server-computed snapshot of everything the generator GUI displays. Sent to the viewing player only;
 * the client never computes gameplay values itself.
 */
public record GeneratorStatus(
        int energy, int energyCapacity, double generation, boolean running, double extraction,
        String fluidId, int fluidAmount, int tankCapacity, double waterPerInterval, int waterInterval,
        String fuelItem, int burnRemaining, int burnTotal,
        int networkSize, int networkActive, double networkScale, double networkTotal, int networkStatus, int maxNetworkSize,
        boolean myceliumEnabled, double chance, double chanceIncrease, int attemptProgress, int attemptInterval,
        int converted, boolean trackConversions,
        int state, int manualLinks, int activeManualLinks, int maxManualLinks, int connectionType,
        int hugeMushrooms,
        List<PowerLink> powerLinks, int maxPowerLinks, double wirelessRate) {

    /**
     * One machine this generator powers wirelessly: its block id ("" when its chunk is not loaded), position, and
     * whether the link carries power right now (loaded, still a machine, within range).
     */
    public record PowerLink(String blockId, BlockPos pos, boolean active) {}

    public static final GeneratorStatus EMPTY = new GeneratorStatus(0, 1, 0, false, 0, "", 0, 1, 0, 20, "", 0, 0,
            1, 0, 1, 0, 1, 64, false, 0, 0, 0, 100, 0, false, 1, 0, 0, 8, 0, 0, List.of(), 8, 0);

    public int activePowerLinks() {
        int count = 0;
        for (PowerLink link : powerLinks) if (link.active()) count++;
        return count;
    }

    public static final StreamCodec<FriendlyByteBuf, GeneratorStatus> STREAM_CODEC = StreamCodec.of(
            (buf, s) -> s.write(buf), GeneratorStatus::read);

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(energy);
        buf.writeVarInt(energyCapacity);
        buf.writeDouble(generation);
        buf.writeBoolean(running);
        buf.writeDouble(extraction);
        buf.writeUtf(fluidId, 256);
        buf.writeVarInt(fluidAmount);
        buf.writeVarInt(tankCapacity);
        buf.writeDouble(waterPerInterval);
        buf.writeVarInt(waterInterval);
        buf.writeUtf(fuelItem, 256);
        buf.writeVarInt(burnRemaining);
        buf.writeVarInt(burnTotal);
        buf.writeVarInt(networkSize);
        buf.writeVarInt(networkActive);
        buf.writeDouble(networkScale);
        buf.writeDouble(networkTotal);
        buf.writeVarInt(networkStatus);
        buf.writeVarInt(maxNetworkSize);
        buf.writeBoolean(myceliumEnabled);
        buf.writeDouble(chance);
        buf.writeDouble(chanceIncrease);
        buf.writeVarInt(attemptProgress);
        buf.writeVarInt(attemptInterval);
        buf.writeVarInt(converted);
        buf.writeBoolean(trackConversions);
        buf.writeVarInt(state);
        buf.writeVarInt(manualLinks);
        buf.writeVarInt(activeManualLinks);
        buf.writeVarInt(maxManualLinks);
        buf.writeVarInt(connectionType);
        buf.writeVarInt(hugeMushrooms);
        buf.writeVarInt(powerLinks.size());
        for (PowerLink link : powerLinks) {
            buf.writeUtf(link.blockId(), 256);
            buf.writeBlockPos(link.pos());
            buf.writeBoolean(link.active());
        }
        buf.writeVarInt(maxPowerLinks);
        buf.writeDouble(wirelessRate);
    }

    private static GeneratorStatus read(FriendlyByteBuf buf) {
        // Arguments are evaluated left to right, so the fields are read in the order they were written.
        return new GeneratorStatus(
                buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readBoolean(), buf.readDouble(),
                buf.readUtf(256), buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readVarInt(),
                buf.readUtf(256), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readVarInt(), buf.readVarInt(),
                buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readBoolean(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(),
                readPowerLinks(buf), buf.readVarInt(), buf.readDouble());
    }

    private static List<PowerLink> readPowerLinks(FriendlyByteBuf buf) {
        int count = Math.min(buf.readVarInt(), 256);
        List<PowerLink> links = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            links.add(new PowerLink(buf.readUtf(256), buf.readBlockPos(), buf.readBoolean()));
        }
        return List.copyOf(links);
    }
}
