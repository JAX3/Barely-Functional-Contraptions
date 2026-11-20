package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineReports.Level;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineReports.Report;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Machine Status Panel: shows how the machines linked to it (with the Barely Functional Wrench) are doing. Every
 * {@code refreshTicks} (a second) it reads each one's line from {@link MachineReports}: its name, its status (running,
 * waiting, a problem or idle) and its key number. A machine that's been broken reads <i>Missing</i>; one in an
 * unloaded chunk <i>Out of range</i>. Its comparator reads how many have a problem, up to 15. It needs no power.
 */
public class StatusPanelBlockEntity extends BlockEntity {
    /** One machine's line on the screen: where it is, its block's name (a translation key), and its report. */
    public record Row(BlockPos pos, String name, Report report) {}

    /** What linking a machine did. */
    public enum LinkResult { LINKED, UNLINKED, FULL, TOO_FAR, NOT_A_MACHINE }

    private final List<BlockPos> links = new ArrayList<>();
    private List<Row> rows = List.of();
    private int cooldown;
    private int lastSignal;
    private int syncedHash;

    public StatusPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STATUS_PANEL.get(), pos, state);
    }

    public static int maxLinks() {
        return ModConfigs.getInt(ModConfigs.SERVER.statusPanelRows);
    }

    public static int range() {
        return ModConfigs.getInt(ModConfigs.SERVER.statusPanelRange);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, StatusPanelBlockEntity be) {
        if (--be.cooldown > 0) return;
        be.cooldown = ModConfigs.getInt(ModConfigs.SERVER.statusPanelRefresh);
        be.refresh(level, pos, state);
    }

    /** Reads every linked machine's line, then updates its screen and its comparator. */
    public void refresh(ServerLevel level, BlockPos pos, BlockState state) {
        List<Row> fresh = new ArrayList<>();
        for (BlockPos link : links) {
            String name = level.getBlockState(link).getBlock().getDescriptionId();
            Report report;
            if (!level.isLoaded(link)) {
                report = new Report("out_of_range", Level.IDLE, "");
            } else {
                report = MachineReports.report(level.getBlockEntity(link));
                if (report == null) report = new Report("missing", Level.PROBLEM, "");
            }
            fresh.add(new Row(link, name, report));
        }
        rows = List.copyOf(fresh);
        int hash = rows.hashCode();
        if (hash != syncedHash) {
            syncedHash = hash;
            setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
        int signal = comparatorSignal();
        if (signal != lastSignal) {
            lastSignal = signal;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    /** Links {@code machine} to it, or unlinks it if it's linked already. */
    public LinkResult toggleLink(ServerLevel level, BlockPos machine) {
        if (links.remove(machine)) {
            changed(level);
            return LinkResult.UNLINKED;
        }
        if (!MachineReports.supports(level.getBlockEntity(machine))) return LinkResult.NOT_A_MACHINE;
        if (!machine.closerThan(worldPosition, range())) return LinkResult.TOO_FAR;
        if (links.size() >= maxLinks()) return LinkResult.FULL;
        links.add(machine.immutable());
        changed(level);
        return LinkResult.LINKED;
    }

    private void changed(ServerLevel level) {
        cooldown = 0;
        setChanged();
        refresh(level, worldPosition, getBlockState());
    }

    public List<BlockPos> links() {
        return List.copyOf(links);
    }

    /** Each linked machine's line (both sides). */
    public List<Row> rows() {
        return rows;
    }

    /** Linked machines with a problem, up to 15. */
    public int comparatorSignal() {
        int problems = 0;
        for (Row row : rows) if (row.report().level() == Level.PROBLEM) problems++;
        return Math.min(15, problems);
    }

    // ------------------------------------------------------------------ persistence and sync

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        links.clear();
        for (long p : in.read("links", com.mojang.serialization.Codec.LONG.listOf()).orElse(List.of())) links.add(BlockPos.of(p));
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("links", com.mojang.serialization.Codec.LONG.listOf(), links.stream().map(BlockPos::asLong).toList());
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Clients get the rows to draw. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Row row : rows) {
            CompoundTag r = new CompoundTag();
            r.putLong("pos", row.pos().asLong());
            r.putString("name", row.name());
            r.putString("status", row.report().status());
            r.putInt("level", row.report().level().ordinal());
            r.putString("number", row.report().number());
            list.add(r);
        }
        tag.put("rows", list);
        return tag;
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        List<Row> list = new ArrayList<>();
        for (CompoundTag r : input.read("rows", CompoundTag.CODEC.listOf()).orElse(List.of())) {
            list.add(new Row(BlockPos.of(r.getLongOr("pos", 0L)), r.getStringOr("name", ""),
                    new Report(r.getStringOr("status", "missing"), Level.byId(r.getIntOr("level", 3)), r.getStringOr("number", ""))));
        }
        rows = List.copyOf(list);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ValueInput input) {
        handleUpdateTag(input);
    }
}
