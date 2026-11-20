package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineReports.Level;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineReports.Report;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * <p>
 * Joined into a bigger screen, the top-left panel holds every link (taking over any the others had) and reads them all;
 * the others show nothing of their own.
 */
public class StatusPanelBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    /** Longest name a machine can be given. */
    public static final int MAX_NAME = 24;

    /** A linked machine: where it is, the name it's been given (empty: its block's), and whether its problems count. */
    public record Link(BlockPos pos, String name, boolean alert) {
        Link withName(String name) {
            return new Link(pos, name, alert);
        }

        Link withAlert(boolean alert) {
            return new Link(pos, name, alert);
        }
    }

    /**
     * One machine's line: which link it is (its place in the panel's own order), where it is, its block's name (a
     * translation key), the name it's been given (or empty), whether its problems count, and its report.
     */
    public record Row(int link, BlockPos pos, String name, String custom, boolean alert, Report report) {
        /** What the screen calls it: the name it's been given, or its block's. */
        public net.minecraft.network.chat.Component displayName() {
            return custom.isEmpty() ? net.minecraft.network.chat.Component.translatable(name) : net.minecraft.network.chat.Component.literal(custom);
        }
    }

    /** What linking a machine did. */
    public enum LinkResult { LINKED, UNLINKED, FULL, TOO_FAR, NOT_A_MACHINE }

    /** The order its rows go in. */
    public enum Sort {
        /** The order they were linked in, or set in its window. */
        OWN,
        /** Problems first, then waiting, working and idle. */
        PROBLEMS,
        /** By machine, then by its own order. */
        TYPE;

        public Sort next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** Its screen's colours (the status colours stay the same). */
    public enum Theme {
        CLASSIC(0xFFDFE4F5, 0xFFBFE8FF), GREEN(0xFF7CFF8A, 0xFFB8FFC0), AMBER(0xFFFFC14A, 0xFFFFE0A0);

        public final int name, number;

        Theme(int name, int number) {
            this.name = name;
            this.number = number;
        }

        public Theme next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** What its comparator reads: machines with a problem (with alerts on), or machines working. */
    public enum Signal { PROBLEMS, WORKING }

    private final List<Link> links = new ArrayList<>();
    private Sort sort = Sort.OWN;
    private Theme theme = Theme.CLASSIC;
    private Signal signal = Signal.PROBLEMS;
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

    /** Whether it's the top-left panel of its screen (or on its own): the one that holds the links and draws the rows. */
    public boolean isMaster() {
        return level == null || com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.master(level, worldPosition).equals(worldPosition);
    }

    /** Machines its screen shows: 4 for each panel in it. */
    public int capacity() {
        if (level == null) return maxLinks();
        BlockPos master = com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.master(level, worldPosition);
        return maxLinks() * com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.members(level, master).size();
    }

    private int indexOf(BlockPos pos) {
        for (int i = 0; i < links.size(); i++) if (links.get(i).pos().equals(pos)) return i;
        return -1;
    }

    /** Takes over links (positions only) from somewhere else, the ones it doesn't have already. */
    public void adopt(List<BlockPos> more) {
        adoptLinks(more.stream().map(p -> new Link(p.immutable(), "", true)).toList());
    }

    /** Takes over links from a panel that's gone or joined it (names and alerts too), the ones it doesn't have already. */
    public void adoptLinks(List<Link> more) {
        for (Link link : more) if (indexOf(link.pos()) < 0) links.add(link);
        cooldown = 0;
        setChanged();
    }

    /** Reads every linked machine's line, then updates its screen and its comparator. */
    public void refresh(ServerLevel level, BlockPos pos, BlockState state) {
        if (!isMaster()) {
            // Part of a bigger screen: its top-left panel shows everything (and takes its links).
            if (!rows.isEmpty()) {
                rows = List.of();
                syncedHash = 0;
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
            return;
        }
        for (BlockPos member : com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.members(level, pos)) {
            if (!member.equals(pos) && level.getBlockEntity(member) instanceof StatusPanelBlockEntity other && !other.links.isEmpty()) {
                adoptLinks(other.links);
                other.links.clear();
                other.setChanged();
            }
        }
        while (links.size() > capacity()) links.removeLast(); // a smaller screen than before: the last ones drop off
        List<Row> fresh = new ArrayList<>();
        for (int i = 0; i < links.size(); i++) {
            Link link = links.get(i);
            String name = level.getBlockState(link.pos()).getBlock().getDescriptionId();
            Report report;
            if (!level.isLoaded(link.pos())) {
                report = new Report("out_of_range", Level.IDLE, "");
            } else {
                report = MachineReports.report(level.getBlockEntity(link.pos()));
                if (report == null) report = new Report("missing", Level.PROBLEM, "");
            }
            fresh.add(new Row(i, link.pos(), name, link.name(), link.alert(), report));
        }
        if (sort == Sort.PROBLEMS) {
            fresh.sort(java.util.Comparator.comparingInt(r -> switch (r.report().level()) {
                case PROBLEM -> 0;
                case WAITING -> 1;
                case RUNNING -> 2;
                case IDLE -> 3;
            }));
        } else if (sort == Sort.TYPE) {
            fresh.sort(java.util.Comparator.comparing(Row::name));
        }
        rows = List.copyOf(fresh);
        int hash = java.util.Objects.hash(rows, sort, theme, signal);
        if (hash != syncedHash) {
            syncedHash = hash;
            setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
        int signal = comparatorSignal();
        if (signal != lastSignal) {
            lastSignal = signal;
            for (BlockPos member : com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.members(level, pos)) {
                level.updateNeighbourForOutputSignal(member, state.getBlock());
            }
        }
    }

    /** Broken: its links go to a panel next to it on the wall, so its screen keeps them. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || links.isEmpty() || !(state.getBlock() instanceof com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock)) return;
        Direction facing = state.getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.FACING);
        for (Direction d : new Direction[]{com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.right(facing), Direction.DOWN,
                com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.left(facing), Direction.UP}) {
            if (level.getBlockEntity(pos.relative(d)) instanceof StatusPanelBlockEntity other
                    && other.getBlockState().getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.FACING) == facing) {
                other.adoptLinks(links);
                links.clear();
                return;
            }
        }
    }

    /** Links {@code machine} to it, or unlinks it if it's linked already. */
    public LinkResult toggleLink(ServerLevel level, BlockPos clicked) {
        // Any block of a bigger machine links the machine: its dome, its core, its back.
        BlockPos machine = MachineReports.machinePos(level, clicked);
        int index = indexOf(machine);
        if (index >= 0) {
            links.remove(index);
            changed(level);
            return LinkResult.UNLINKED;
        }
        if (!MachineReports.supports(level.getBlockEntity(machine))) return LinkResult.NOT_A_MACHINE;
        if (!machine.closerThan(worldPosition, range())) return LinkResult.TOO_FAR;
        if (links.size() >= capacity()) return LinkResult.FULL;
        links.add(new Link(machine.immutable(), "", true));
        changed(level);
        return LinkResult.LINKED;
    }

    // ------------------------------------------------------------------ its window's settings

    /** Moves link {@code index} one place up ({@code -1}) or down ({@code 1}) its own order. */
    public void move(ServerLevel level, int index, int by) {
        int to = index + by;
        if (index < 0 || index >= links.size() || to < 0 || to >= links.size()) return;
        links.add(to, links.remove(index));
        changed(level);
    }

    public void unlink(ServerLevel level, int index) {
        if (index < 0 || index >= links.size()) return;
        links.remove(index);
        changed(level);
    }

    /** Whether link {@code index}'s problems count for its comparator. */
    public void toggleAlert(ServerLevel level, int index) {
        if (index < 0 || index >= links.size()) return;
        links.set(index, links.get(index).withAlert(!links.get(index).alert()));
        changed(level);
    }

    /** Gives link {@code index} a name (an empty one: its block's again). */
    public void rename(ServerLevel level, int index, String name) {
        if (index < 0 || index >= links.size()) return;
        String clean = net.minecraft.util.StringUtil.filterText(name).strip();
        links.set(index, links.get(index).withName(clean.length() > MAX_NAME ? clean.substring(0, MAX_NAME) : clean));
        changed(level);
    }

    public Sort sort() {
        return sort;
    }

    public void cycleSort(ServerLevel level) {
        sort = sort.next();
        changed(level);
    }

    public Theme theme() {
        return theme;
    }

    public void cycleTheme(ServerLevel level) {
        theme = theme.next();
        changed(level);
    }

    public Signal signal() {
        return signal;
    }

    public void toggleSignal(ServerLevel level) {
        signal = signal == Signal.PROBLEMS ? Signal.WORKING : Signal.PROBLEMS;
        changed(level);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("block.barelyfunctionalcontraptions.status_panel");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory,
                                                                          net.minecraft.world.entity.player.Player player) {
        return new com.jax3.barelyfunctionalcontraptions.sheep.menu.StatusPanelMenu(containerId, inventory, this);
    }

    private void changed(ServerLevel level) {
        cooldown = 0;
        setChanged();
        refresh(level, worldPosition, getBlockState());
    }

    public List<BlockPos> links() {
        return links.stream().map(Link::pos).toList();
    }

    /** Each linked machine's line (both sides). */
    public List<Row> rows() {
        return rows;
    }

    /** Linked machines with a problem (whose alerts are on), or, set to Working, machines working; up to 15. */
    public int comparatorSignal() {
        int count = 0;
        for (Row row : rows) {
            if (signal == Signal.WORKING ? row.report().level() == Level.RUNNING : row.report().level() == Level.PROBLEM && row.alert()) count++;
        }
        return Math.min(15, count);
    }

    // ------------------------------------------------------------------ persistence and sync

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        links.clear();
        for (CompoundTag l : in.read("machines", CompoundTag.CODEC.listOf()).orElse(List.of())) {
            links.add(new Link(BlockPos.of(l.getLongOr("pos", 0L)), l.getStringOr("name", ""), l.getBooleanOr("alert", true)));
        }
        sort = Sort.values()[Math.floorMod(in.getIntOr("sort", 0), Sort.values().length)];
        theme = Theme.values()[Math.floorMod(in.getIntOr("theme", 0), Theme.values().length)];
        signal = Signal.values()[Math.floorMod(in.getIntOr("signal", 0), Signal.values().length)];
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        List<CompoundTag> list = new ArrayList<>();
        for (Link link : links) {
            CompoundTag l = new CompoundTag();
            l.putLong("pos", link.pos().asLong());
            if (!link.name().isEmpty()) l.putString("name", link.name());
            l.putBoolean("alert", link.alert());
            list.add(l);
        }
        out.store("machines", CompoundTag.CODEC.listOf(), list);
        out.putInt("sort", sort.ordinal());
        out.putInt("theme", theme.ordinal());
        out.putInt("signal", signal.ordinal());
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
            r.putInt("link", row.link());
            r.putLong("pos", row.pos().asLong());
            r.putString("name", row.name());
            r.putString("custom", row.custom());
            r.putBoolean("alert", row.alert());
            r.putString("status", row.report().status());
            r.putInt("level", row.report().level().ordinal());
            r.putString("number", row.report().number());
            list.add(r);
        }
        tag.put("rows", list);
        tag.putInt("sort", sort.ordinal());
        tag.putInt("theme", theme.ordinal());
        tag.putInt("signal", signal.ordinal());
        return tag;
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        List<Row> list = new ArrayList<>();
        for (CompoundTag r : input.read("rows", CompoundTag.CODEC.listOf()).orElse(List.of())) {
            list.add(new Row(r.getIntOr("link", 0), BlockPos.of(r.getLongOr("pos", 0L)), r.getStringOr("name", ""), r.getStringOr("custom", ""),
                    r.getBooleanOr("alert", true),
                    new Report(r.getStringOr("status", "missing"), Level.byId(r.getIntOr("level", 3)), r.getStringOr("number", ""))));
        }
        rows = List.copyOf(list);
        sort = Sort.values()[Math.floorMod(input.getIntOr("sort", 0), Sort.values().length)];
        theme = Theme.values()[Math.floorMod(input.getIntOr("theme", 0), Theme.values().length)];
        signal = Signal.values()[Math.floorMod(input.getIntOr("signal", 0), Signal.values().length)];
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ValueInput input) {
        handleUpdateTag(input);
    }
}
