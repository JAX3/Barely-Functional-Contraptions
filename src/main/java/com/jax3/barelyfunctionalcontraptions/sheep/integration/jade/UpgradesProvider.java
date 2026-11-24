package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades;
import com.jax3.barelyfunctionalcontraptions.sheep.upgrade.UpgradeEffects;
import com.jax3.barelyfunctionalcontraptions.sheep.upgrade.Upgradeable;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;

/**
 * Any machine with upgrade slots: the cards in it, and what they add up to (time and power a job, capacity, range,
 * yield, muffled). Clients don't keep a machine's cards, so the server sends their ids.
 */
public final class UpgradesProvider {
    public static final Identifier UID = ResourcefulSheep.id("upgrades");

    private UpgradesProvider() {}

    public record Data(List<String> cards) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MachineUpgrades.SIZE)), Data::cards, Data::new);
    }

    public enum Server implements StreamServerDataProvider<BlockAccessor, Data> {
        INSTANCE;

        @Override
        public @Nullable Data streamData(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof Upgradeable machine) || machine.upgrades().isEmpty()) return null;
            List<String> ids = new ArrayList<>();
            for (ItemStack card : machine.upgrades().cards()) ids.add(BuiltInRegistries.ITEM.getKey(card.getItem()).toString());
            return new Data(ids);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Data> streamCodec() {
            return Data.CODEC;
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    public enum Client implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            Data data = Server.INSTANCE.decodeFromData(accessor).orElse(null);
            if (data == null || data.cards().isEmpty()) return;
            List<ItemStack> cards = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (String id : data.cards()) {
                Identifier parsed = Identifier.tryParse(id);
                if (parsed == null) continue;
                ItemStack card = new ItemStack(BuiltInRegistries.ITEM.getValue(parsed));
                cards.add(card);
                names.add(card.getHoverName().getString());
            }
            String key = "jade.barelyfunctionalcontraptions.upgrades";
            tooltip.add(Component.translatable(key, String.join(", ", names)).withStyle(ChatFormatting.AQUA));
            Component effects = effects(MachineUpgrades.effectsOf(cards));
            if (effects != null) tooltip.add(effects.copy().withStyle(ChatFormatting.GRAY));
        }

        /** What the cards add up to, the parts that aren't plain: "Time 60%, Power 150%, Range +1". */
        static @Nullable Component effects(UpgradeEffects fx) {
            String key = "jade.barelyfunctionalcontraptions.upgrades.";
            List<Component> parts = new ArrayList<>();
            if (Math.abs(fx.time() - 1) > 1e-6) parts.add(Component.translatable(key + "time", Math.round(fx.time() * 100)));
            if (Math.abs(fx.energy() - 1) > 1e-6) parts.add(Component.translatable(key + "energy", Math.round(fx.energy() * 100)));
            if (Math.abs(fx.capacity() - 1) > 1e-6) parts.add(Component.translatable(key + "capacity", Math.round(fx.capacity() * 100)));
            if (fx.range() > 0) parts.add(Component.translatable(key + "range", fx.range()));
            if (fx.yield() > 0) parts.add(Component.translatable(key + "yield", fx.yield() == Math.floor(fx.yield())
                    ? String.valueOf((int) fx.yield()) : String.valueOf(fx.yield())));
            if (fx.muffled()) parts.add(Component.translatable(key + "muffled"));
            if (parts.isEmpty()) return null;
            var line = Component.empty();
            for (int i = 0; i < parts.size(); i++) {
                if (i > 0) line.append(", ");
                line.append(parts.get(i));
            }
            return line;
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
