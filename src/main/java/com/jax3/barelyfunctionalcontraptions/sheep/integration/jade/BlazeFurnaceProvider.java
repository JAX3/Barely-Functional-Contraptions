package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.BlazeFurnaceBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.BlazeFurnaceBlock;
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

/** Blaze Sheep Furnace: what it is making and from how many heat sources, its boost, and the waiting boost items. */
public final class BlazeFurnaceProvider {
    public static final Identifier UID = ResourcefulSheep.id("blaze_furnace");

    private BlazeFurnaceProvider() {}

    public record Data(int generating, int heaters, int boostTicks, double multiplier, ItemStack waiting, boolean area) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Data::generating,
                ByteBufCodecs.VAR_INT, Data::heaters,
                ByteBufCodecs.VAR_INT, Data::boostTicks,
                ByteBufCodecs.DOUBLE, Data::multiplier,
                ItemStack.OPTIONAL_STREAM_CODEC, Data::waiting,
                ByteBufCodecs.BOOL, Data::area,
                Data::new);
    }

    public enum Server implements StreamServerDataProvider<BlockAccessor, Data> {
        INSTANCE;

        @Override
        public @Nullable Data streamData(BlockAccessor accessor) {
            return accessor.getBlockEntity() instanceof BlazeFurnaceBlockEntity be
                    ? new Data(be.generating(), be.heaterCount(), be.boostTicks(), be.boostMultiplier(), be.boostItems().copy(), be.showsArea())
                    : null;
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
            if (data == null) return;
            if (data.heaters() > 0) {
                tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.blaze_furnace.heat", data.generating(), data.heaters()).withColor(0xFF9A2E));
            } else {
                tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.blaze_furnace.cold").withColor(0xAAAAAA));
            }
            if (data.boostTicks() > 0) {
                tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.blaze_furnace.boost",
                        BlazeFurnaceBlock.multiplier(data.multiplier()), (data.boostTicks() + 19) / 20).withColor(0xFFD25A));
            }
            if (!data.waiting().isEmpty()) {
                tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.blaze_furnace.waiting",
                        data.waiting().getCount(), data.waiting().getHoverName()).withColor(0xAAAAAA));
            }
            if (data.area()) tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.area").withColor(0xAAAAAA));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
