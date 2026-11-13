package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Fluid and Item Pipes: the network a pipe belongs to and how much that network is moving (mB/t or items/t). */
public enum PipeProvider implements IBlockComponentProvider {
    INSTANCE;

    public static final Identifier UID = ResourcefulSheep.id("pipes");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        PipeDataProvider.Data data = PipeDataProvider.INSTANCE.decodeFromData(accessor).orElse(null);
        if (data == null) return;
        tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.pipe.network", data.cables(), data.endpoints())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(accessor.getBlock() instanceof com.jax3.barelyfunctionalcontraptions.sheep.blocks.ItemPipeBlock
                        ? "jade.barelyfunctionalcontraptions.item_pipe.rate" : "jade.barelyfunctionalcontraptions.pipe.rate", fe(data.rate()), fe(data.maxRate()))
                .withColor(data.rate() > 0 ? 0x3FC8FF : 0xAAAAAA));
        boolean items = accessor.getBlock() instanceof com.jax3.barelyfunctionalcontraptions.sheep.blocks.ItemPipeBlock;
        var kind = items ? com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeKind.ITEM : com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeKind.FLUID;
        for (PipeDataProvider.Side entry : data.sides()) {
            var side = entry.side();
            Component dir = Component.translatable("direction.barelyfunctionalcontraptions." + entry.dir().getSerializedName());
            if (side.filter().isEmpty()) {
                tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.pipe.side", dir, side.mode().displayName()).withStyle(ChatFormatting.GOLD));
            } else {
                var names = Component.empty();
                for (int i = 0; i < side.filter().size(); i++) {
                    if (i > 0) names.append(", ");
                    names.append(kind.nameOf().apply(side.filter().get(i)));
                }
                tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.pipe.side_filter", dir, side.mode().displayName(), names).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    private static String fe(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
