package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.ZeroPointEnergyModuleBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * A Zero Point Energy Module. Every few ticks it looks along its facing (up or down) for a partner: the nearest module
 * in the same column that faces back at it, with only see-through blocks in between and at least {@code minGap}
 * blocks of open space. A linked pair makes {@code energyPerBlock} FE/t for every block of gap, half going into each
 * module's buffer. The two buffers act as one pool: the energy capability of either module reaches both, and each
 * module pushes from the pool into its neighbours, so the pair's full output can be taken from either end.
 *
 * <p>The link distance and rate are synced to the client, which draws the beam and shows them in Jade. Like a beacon
 * beam, the beam is coloured by stained glass (blocks or panes) in its path: the first one sets its colour, and every
 * different one after that mixes with the colour so far. The floor module, which draws the beam, works out these
 * colour sections and syncs them too.</p>
 *
 * <p>The beam is dangerous: the floor module hurts every living thing touching it ({@code beamDamage}, with its own
 * "vaporised by a Zero Point beam" death message) and sets it on fire for {@code beamBurnSeconds}. With
 * {@code beamCountsAsPlayerKill} each hit also marks the mob as hurt by a fake player, so a beam kill drops experience
 * and player-only loot as if a player had killed it.</p>
 */
public class ZeroPointEnergyModuleBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    private static final int SCAN_INTERVAL = 5;

    private final Storage energy = new Storage();
    /** What neighbours see on every face: this module's buffer plus its partner's. */
    private final PairEnergy pairEnergy = new PairEnergy();
    /** Distance in blocks to the partner (2 = one block of gap), or 0 when not linked. */
    private int linkDistance;
    /** FE this module adds to its own buffer every tick (its half of the pair's output). */
    private int generating;
    private long lastTickPushed;
    private ZeroPointLink link = ZeroPointLink.NO_MODULE;
    /** The beam's colour sections, as pairs of (start, in blocks above this module; ARGB colour). Floor module only. */
    private int[] beamSections = new int[0];
    /** How far along the column the last scan stopped (the partner, the blocking block or the range limit). */
    private int stoppedAt;
    /** FE that left this module's buffer this second (through either module), and the last second's average. */
    private int outThisSecond;
    private int outRate;
    private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> pairEnergy.getAmountAsInt() & 0xFFFF;
                case 1 -> pairEnergy.getAmountAsInt() >>> 16;
                case 2 -> pairEnergy.getCapacityAsInt() & 0xFFFF;
                case 3 -> pairEnergy.getCapacityAsInt() >>> 16;
                case 4 -> generating & 0xFFFF;
                case 5 -> generating >>> 16;
                case 6 -> getPairOutput() & 0xFFFF;
                case 7 -> getPairOutput() >>> 16;
                case 8 -> linkDistance;
                case 9 -> getPairOutRate() & 0xFFFF;
                case 10 -> getPairOutRate() >>> 16;
                case 11 -> getBlockState().getValue(ZeroPointEnergyModuleBlock.FACING) == Direction.UP ? 1 : 0;
                case 12 -> link.ordinal();
                case 13 -> stoppedAt;
                case 14 -> ModConfigs.getInt(ModConfigs.SERVER.zpeMaxRange);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };
    public static final int DATA_COUNT = 15;

    public ZeroPointEnergyModuleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZERO_POINT_ENERGY_MODULE.get(), pos, state);
    }

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        energy.refreshLimits();
        if (Math.floorMod(level.getGameTime() + pos.asLong(), SCAN_INTERVAL) == 0) {
            updateLink(level, pos, state);
        }
        if (generating > 0) energy.generate(generating);
        pushEnergy(level, pos);
        if (linkDistance > 0 && state.getValue(ZeroPointEnergyModuleBlock.FACING) == Direction.UP) burnEntitiesInBeam(level, pos);
        if (level.getGameTime() % 20 == 0) {
            outRate = outThisSecond / 20;
            outThisSecond = 0;
        }
    }

    /** Rescans the column and stores the result, syncing it to clients when it changes. */
    public void updateLink(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(ZeroPointEnergyModuleBlock.FACING);
        ZeroPointLink.Scan scan = scan(level, pos, facing);
        link = scan.link();
        stoppedAt = scan.stoppedAt();
        int distance = scan.distance();
        int pairRate = pairOutput(distance);
        // The floor module (facing up) keeps the odd FE so the pair adds up to exactly pairRate.
        int share = facing == Direction.UP ? (pairRate + 1) / 2 : pairRate / 2;
        int[] sections = facing == Direction.UP ? beamSections(level, pos, distance) : new int[0];
        if (distance == linkDistance && share == generating && java.util.Arrays.equals(sections, beamSections)) return;
        linkDistance = distance;
        generating = share;
        beamSections = sections;
        boolean linked = distance > 0;
        if (state.getValue(ZeroPointEnergyModuleBlock.LINKED) != linked) {
            level.setBlock(pos, state.setValue(ZeroPointEnergyModuleBlock.LINKED, linked), Block.UPDATE_ALL);
        }
        setChanged();
        level.sendBlockUpdated(pos, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Distance to the module this one links with, or 0. */
    public static int findPartner(Level level, BlockPos pos, Direction facing) {
        return scan(level, pos, facing).distance();
    }

    /**
     * Scans the column in front of the emitter. The beam passes through air, glass and anything else that does not
     * fully block the view; the first module or solid block ends the search, and the result says which it was.
     */
    public static ZeroPointLink.Scan scan(Level level, BlockPos pos, Direction facing) {
        int maxRange = ModConfigs.getInt(ModConfigs.SERVER.zpeMaxRange);
        int minGap = ModConfigs.getInt(ModConfigs.SERVER.zpeMinGap);
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 1; i <= maxRange; i++) {
            cursor.move(facing);
            if (!level.isLoaded(cursor) || level.isOutsideBuildHeight(cursor)) return new ZeroPointLink.Scan(0, ZeroPointLink.NO_MODULE, i);
            BlockState state = level.getBlockState(cursor);
            if (state.is(ModBlocks.ZERO_POINT_ENERGY_MODULE.get())) {
                if (state.getValue(ZeroPointEnergyModuleBlock.FACING) != facing.getOpposite()) return new ZeroPointLink.Scan(0, ZeroPointLink.WRONG_FACING, i);
                if (i - 1 < minGap) return new ZeroPointLink.Scan(0, ZeroPointLink.TOO_CLOSE, i);
                return new ZeroPointLink.Scan(i, ZeroPointLink.LINKED, i);
            }
            if (state.canOcclude()) return new ZeroPointLink.Scan(0, ZeroPointLink.BLOCKED, i);
        }
        return new ZeroPointLink.Scan(0, ZeroPointLink.NO_MODULE, maxRange);
    }

    /** The beam's damage type (data/barelyfunctionalcontraptions/damage_type/zero_point_beam.json). */
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> BEAM_DAMAGE =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep.id("zero_point_beam"));
    /** Half the width of the beam's damage area: a little wider than the drawn glow (0.16), so touching it counts. */
    private static final double BEAM_HALF_WIDTH = 0.2;

    /** The space the beam fills, from the floor emitter's tip to the ceiling emitter's, for a pair this far apart. */
    public static net.minecraft.world.phys.AABB beamArea(BlockPos floorPos, int distance) {
        double x = floorPos.getX() + 0.5, z = floorPos.getZ() + 0.5;
        return new net.minecraft.world.phys.AABB(x - BEAM_HALF_WIDTH, floorPos.getY() + 0.5, z - BEAM_HALF_WIDTH,
                x + BEAM_HALF_WIDTH, floorPos.getY() + distance + 0.5, z + BEAM_HALF_WIDTH);
    }

    /**
     * Hurts and ignites every living thing in the beam (only the floor module does this, so each beam hits once).
     * Living things are only hurt twice a second at most by the normal hurt cooldown; creative and spectator players,
     * and anything invulnerable, are left alone by the usual damage rules.
     */
    private void burnEntitiesInBeam(ServerLevel level, BlockPos pos) {
        float damage = (float) ModConfigs.getDouble(ModConfigs.SERVER.zpeBeamDamage);
        if (damage <= 0) return;
        int burnSeconds = ModConfigs.getInt(ModConfigs.SERVER.zpeBeamBurnSeconds);
        var targets = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, beamArea(pos, linkDistance),
                e -> e.isAlive() && !e.isSpectator());
        if (targets.isEmpty()) return;
        var source = level.damageSources().source(BEAM_DAMAGE);
        net.minecraft.world.entity.player.Player killer = ModConfigs.getBool(ModConfigs.SERVER.zpeBeamPlayerKill) ? beamPlayer(level, pos) : null;
        for (var target : targets) {
            // Like a player's hit: if this blow (or the fire after it) kills, the mob drops experience and player-only loot.
            if (killer != null && !(target instanceof net.minecraft.world.entity.player.Player)) target.setLastHurtByPlayer(killer, 100);
            if (target.hurtServer(level, source, damage) && burnSeconds > 0) target.igniteForSeconds(burnSeconds);
        }
    }

    /** Name of the fake player beam kills are credited to. */
    public static final String BEAM_PLAYER_NAME = "[Zero Point Beam]";

    /** The fake player beam kills are credited to, so they count as player kills; one per level, at this module. */
    public static net.neoforged.neoforge.common.util.FakePlayer beamPlayer(ServerLevel level, BlockPos pos) {
        java.util.UUID uuid = java.util.UUID.nameUUIDFromBytes(("barelyfunctionalcontraptions:" + BEAM_PLAYER_NAME)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(uuid, BEAM_PLAYER_NAME));
        player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return player;
    }

    /** The beam's default colour, used until it passes through stained glass. */
    public static final int BEAM_COLOR = 0xFF3FC8FF;

    /**
     * Colour sections of a beam going {@code distance} blocks up from {@code pos}, with a beacon's rules: the beam
     * starts {@link #BEAM_COLOR}; the first block with a beacon colour (stained glass or panes, or a modded block that
     * colours beacon beams) replaces it, and each later one with a different colour starts a section with the average
     * of the two. Returned as (start, colour) pairs; empty when not linked.
     */
    public static int[] beamSections(Level level, BlockPos pos, int distance) {
        if (distance <= 0) return new int[0];
        it.unimi.dsi.fastutil.ints.IntArrayList out = new it.unimi.dsi.fastutil.ints.IntArrayList();
        out.add(0);
        out.add(BEAM_COLOR);
        boolean tinted = false;
        int color = BEAM_COLOR;
        for (int i = 1; i < distance; i++) {
            BlockPos at = pos.above(i);
            Integer glass = level.getBlockState(at).getBeaconColorMultiplier(level, at, pos);
            if (glass == null || (tinted && glass == color)) continue;
            color = tinted ? net.minecraft.util.ARGB.average(color, glass) : glass;
            tinted = true;
            out.add(i);
            out.add(color);
        }
        return out.toIntArray();
    }

    /**
     * Most FE a module gives out per tick (pushed and pulled): {@code maxOutput}, raised to the pair's whole output
     * when that is larger, so one module can always deliver everything the pair makes.
     */
    public static int outputLimit(int pairOutput) {
        return Math.max(ModConfigs.getInt(ModConfigs.SERVER.zpeMaxOutput), pairOutput);
    }

    /** This module's output limit; see {@link #outputLimit(int)}. */
    public int getOutputLimit() {
        return outputLimit(getPairOutput());
    }

    /** FE/t a pair this far apart makes in total (0 when not linked). */
    public static int pairOutput(int distance) {
        if (distance <= 0) return 0;
        long rate = (long) ModConfigs.getInt(ModConfigs.SERVER.zpeEnergyPerBlock) * (distance - 1);
        return (int) Math.min(Integer.MAX_VALUE, rate);
    }

    /** Pushes from the pair's pool (this buffer first, then the partner's) into every neighbouring receiver. */
    private void pushEnergy(ServerLevel level, BlockPos pos) {
        int budget = Math.min(pairEnergy.getAmountAsInt(), getOutputLimit());
        long pushed = 0;
        for (Direction dir : Direction.values()) {
            if (budget <= 0) break;
            BlockPos target = pos.relative(dir);
            if (level.getBlockEntity(target) instanceof ZeroPointEnergyModuleBlockEntity) continue;
            EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, target, dir.getOpposite());
            if (handler == null) continue;
            try (Transaction tx = Transaction.openRoot()) {
                int inserted = handler.insert(budget, tx);
                if (inserted <= 0) continue;
                int taken = pairEnergy.extract(inserted, tx);
                if (taken != inserted) continue; // should not happen: budget never exceeds the pool; roll back
                tx.commit();
                budget -= inserted;
                pushed += inserted;
            }
        }
        lastTickPushed = pushed;
    }

    /** The linked partner's block entity, or null when unlinked, unloaded, or the partner doesn't see this link. */
    public @org.jspecify.annotations.Nullable ZeroPointEnergyModuleBlockEntity partner() {
        if (linkDistance <= 0 || level == null) return null;
        Direction facing = getBlockState().getValue(ZeroPointEnergyModuleBlock.FACING);
        BlockPos other = worldPosition.relative(facing, linkDistance);
        if (!level.isLoaded(other)) return null;
        return level.getBlockEntity(other) instanceof ZeroPointEnergyModuleBlockEntity p && p.linkDistance == linkDistance ? p : null;
    }

    /** The energy capability on every face: the pair's pooled energy, output only. */
    public EnergyHandler getEnergyHandler() {
        return pairEnergy;
    }

    public int getLinkDistance() {
        return linkDistance;
    }

    /** FE/t this module adds to its own buffer (half the pair's output). */
    public int getGenerating() {
        return generating;
    }

    /** The whole pair's FE/t. */
    public int getPairOutput() {
        return pairOutput(linkDistance);
    }

    /** FE in this module's own buffer (not counting the partner's). */
    public int getEnergyStored() {
        return energy.getAmountAsInt();
    }

    /** FE in the pair's pool: this buffer plus the partner's. */
    public int getPairEnergyStored() {
        return pairEnergy.getAmountAsInt();
    }

    public long getLastTickPushed() {
        return lastTickPushed;
    }

    public ZeroPointLink getLink() {
        return link;
    }

    /** The beam's colour sections as (start, colour) pairs; see {@link #beamSections}. */
    public int[] getBeamSections() {
        return beamSections;
    }

    /** FE/t that left this module's buffer, averaged over the last second. */
    public int getOutRate() {
        return outRate;
    }

    /** FE/t that left the whole pair's pool, averaged over the last second. */
    public int getPairOutRate() {
        ZeroPointEnergyModuleBlockEntity p = partner();
        return p == null ? outRate : outRate + p.outRate;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("block.barelyfunctionalcontraptions.zero_point_energy_module");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory,
                                                                         net.minecraft.world.entity.player.Player player) {
        return new com.jax3.barelyfunctionalcontraptions.sheep.menu.ZeroPointModuleMenu(containerId, inventory, getBlockPos(), data);
    }

    // ------------------------------------------------------------------ persistence and sync

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("energy").ifPresent(energy::deserialize);
        linkDistance = Math.max(0, input.getIntOr("LinkDistance", 0));
        generating = Math.max(0, input.getIntOr("Generating", 0));
        beamSections = input.getIntArray("BeamSections").filter(a -> a.length % 2 == 0).orElse(new int[0]);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output.child("energy"));
        output.putInt("LinkDistance", linkDistance);
        output.putInt("Generating", generating);
        output.putIntArray("BeamSections", beamSections);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    /** Output-only buffer: neighbours may pull up to {@code maxOutput} FE/t, nothing can be inserted from outside. */
    private final class Storage extends SimpleEnergyHandler {
        Storage() {
            // Only reached through PairEnergy, which applies the output limit; no limit of its own on taking energy.
            super(ModConfigs.getInt(ModConfigs.SERVER.zpeCapacity), 0, Integer.MAX_VALUE);
        }

        void refreshLimits() {
            this.capacity = ModConfigs.getInt(ModConfigs.SERVER.zpeCapacity);
            if (this.energy > this.capacity) set(this.capacity);
        }

        void generate(int amount) {
            set((int) Math.min(capacity, (long) energy + amount));
        }

        void drain(int amount) {
            set(Math.max(0, energy - amount));
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            if (energy < previousAmount) outThisSecond += previousAmount - energy; // pushed or pulled, by either module
            setChanged();
        }
    }

    /**
     * Both buffers of a linked pair as one output-only handler, so the pair's full output can be taken from either
     * module. Takes from this module's buffer first, then the partner's, at most {@code maxOutput} FE per call.
     * Unlinked, it is just this module's buffer.
     */
    private final class PairEnergy implements EnergyHandler {
        @Override
        public long getAmountAsLong() {
            ZeroPointEnergyModuleBlockEntity p = partner();
            return energy.getAmountAsLong() + (p == null ? 0 : p.energy.getAmountAsLong());
        }

        @Override
        public long getCapacityAsLong() {
            ZeroPointEnergyModuleBlockEntity p = partner();
            return energy.getCapacityAsLong() + (p == null ? 0 : p.energy.getCapacityAsLong());
        }

        @Override
        public int insert(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            int limit = Math.min(amount, getOutputLimit());
            if (limit <= 0) return 0;
            int taken = energy.extract(limit, transaction);
            ZeroPointEnergyModuleBlockEntity p = partner();
            if (taken < limit && p != null) taken += p.energy.extract(limit - taken, transaction);
            return taken;
        }
    }
}
