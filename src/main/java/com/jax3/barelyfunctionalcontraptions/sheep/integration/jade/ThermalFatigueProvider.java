package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.sheep.Sheep;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;

/**
 * A Blaze or Snow sheep's fleece wear from Thermal Reaction Chambers: how much of it is left, or that it is spent and
 * needs infusing again. Only shown once the fleece has started to wear. The wear lives on the server, so it is sent.
 */
public final class ThermalFatigueProvider {
    public static final Identifier UID = ResourcefulSheep.id("thermal_fatigue");

    private ThermalFatigueProvider() {}

    /** Thousandths of the fleece left, or -1 for a fleece that hasn't worn at all (or a sheep that can't react). */
    public enum Server implements StreamServerDataProvider<EntityAccessor, Integer> {
        INSTANCE;

        @Override
        public @Nullable Integer streamData(EntityAccessor accessor) {
            if (!(accessor.getEntity() instanceof Sheep sheep)) return null;
            if (!ThermalReactionChamberBlockEntity.isHotSource(sheep) && !ThermalReactionChamberBlockEntity.isColdSource(sheep)
                    && !ThermalReactionChamberBlockEntity.isSpent(sheep)) return null;
            int worn = sheep.getData(ModAttachments.THERMAL_FATIGUE).ticks();
            int limit = ModConfigs.getInt(ModConfigs.SERVER.thermalFatigueTicks);
            if (worn <= 0 || limit <= 0) return null;
            return (int) Math.max(0, 1000L - 1000L * worn / limit);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Integer> streamCodec() {
            return ByteBufCodecs.VAR_INT.cast();
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    public enum Client implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            Integer left = Server.INSTANCE.decodeFromData(accessor).orElse(null);
            if (left == null) return;
            if (left <= 0) {
                tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.thermal_fatigue.spent").withColor(0xFF5555));
            } else {
                tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.thermal_fatigue.left", (left + 5) / 10).withColor(0xAAAAAA));
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
