package com.jax3.barelyfunctionalcontraptions.mycelial.network;

import com.jax3.barelyfunctionalcontraptions.mycelial.MycelialPower;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Wireless power links made with the Barely Functional Wrench: each one sends a generator's FE to a machine (a block in
 * {@code #barelyfunctionalcontraptions:wireless_power_receivers}). Stored once for the whole world in
 * {@code data/barelyfunctionalcontraptions/power_links.dat}, separately from generator-to-generator links, so machines
 * never become part of a generator network.
 */
public final class PowerLinkData extends SavedData {
    private record Link(GlobalPos generator, GlobalPos machine) {
        static final Codec<Link> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                GlobalPos.CODEC.fieldOf("generator").forGetter(Link::generator),
                GlobalPos.CODEC.fieldOf("machine").forGetter(Link::machine)
        ).apply(instance, Link::new));
    }

    public static final Codec<PowerLinkData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Link.CODEC.listOf().optionalFieldOf("links", List.of()).forGetter(PowerLinkData::toList)
    ).apply(instance, PowerLinkData::fromList));

    public static final SavedDataType<PowerLinkData> TYPE = new SavedDataType<>(
            MycelialPower.id("power_links"), PowerLinkData::new, CODEC, null);

    /** Generator -> machines it powers, in the order they were linked. */
    private final Map<GlobalPos, Set<GlobalPos>> byGenerator = new HashMap<>();
    /** Machine -> generators powering it. */
    private final Map<GlobalPos, Set<GlobalPos>> byMachine = new HashMap<>();

    public PowerLinkData() {
    }

    public static PowerLinkData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public Set<GlobalPos> machinesOf(GlobalPos generator) {
        Set<GlobalPos> set = byGenerator.get(generator);
        return set == null ? Collections.emptySet() : Collections.unmodifiableSet(set);
    }

    public Set<GlobalPos> generatorsOf(GlobalPos machine) {
        Set<GlobalPos> set = byMachine.get(machine);
        return set == null ? Collections.emptySet() : Collections.unmodifiableSet(set);
    }

    public boolean isLinked(GlobalPos generator, GlobalPos machine) {
        return machinesOf(generator).contains(machine);
    }

    public boolean add(GlobalPos generator, GlobalPos machine) {
        if (generator.equals(machine) || isLinked(generator, machine)) return false;
        byGenerator.computeIfAbsent(generator, k -> new LinkedHashSet<>()).add(machine);
        byMachine.computeIfAbsent(machine, k -> new LinkedHashSet<>()).add(generator);
        setDirty();
        return true;
    }

    public boolean remove(GlobalPos generator, GlobalPos machine) {
        boolean removed = removeFrom(byGenerator, generator, machine);
        removeFrom(byMachine, machine, generator);
        if (removed) setDirty();
        return removed;
    }

    /** Removes every link with {@code pos} at either end. Returns how many were removed. */
    public int removeAll(GlobalPos pos) {
        int removed = 0;
        for (GlobalPos machine : List.copyOf(machinesOf(pos))) removed += remove(pos, machine) ? 1 : 0;
        for (GlobalPos generator : List.copyOf(generatorsOf(pos))) removed += remove(generator, pos) ? 1 : 0;
        return removed;
    }

    private static boolean removeFrom(Map<GlobalPos, Set<GlobalPos>> map, GlobalPos key, GlobalPos value) {
        Set<GlobalPos> set = map.get(key);
        if (set == null || !set.remove(value)) return false;
        if (set.isEmpty()) map.remove(key);
        return true;
    }

    private List<Link> toList() {
        List<Link> list = new ArrayList<>();
        byGenerator.forEach((generator, machines) -> machines.forEach(machine -> list.add(new Link(generator, machine))));
        return list;
    }

    private static PowerLinkData fromList(List<Link> list) {
        PowerLinkData data = new PowerLinkData();
        for (Link link : list) {
            if (!link.generator().equals(link.machine())) {
                data.byGenerator.computeIfAbsent(link.generator(), k -> new LinkedHashSet<>()).add(link.machine());
                data.byMachine.computeIfAbsent(link.machine(), k -> new LinkedHashSet<>()).add(link.generator());
            }
        }
        return data;
    }
}
