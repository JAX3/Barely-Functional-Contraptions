package com.jax3.barelyfunctionalcontraptions.sheep.network;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.StatusPanelMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: give a machine linked to the open Machine Status Panel a name (empty: its own name again). */
public record RenamePanelLinkPayload(int containerId, int link, String name) implements CustomPacketPayload {
    public static final Type<RenamePanelLinkPayload> TYPE = new Type<>(ResourcefulSheep.id("rename_panel_link"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RenamePanelLinkPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RenamePanelLinkPayload::containerId,
            ByteBufCodecs.VAR_INT, RenamePanelLinkPayload::link,
            ByteBufCodecs.stringUtf8(StatusPanelBlockEntity.MAX_NAME * 4), RenamePanelLinkPayload::name,
            RenamePanelLinkPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RenamePanelLinkPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player.containerMenu instanceof StatusPanelMenu menu && menu.containerId == payload.containerId() && menu.stillValid(player)
                    && menu.blockEntity() != null && player.level() instanceof ServerLevel level) {
                menu.blockEntity().rename(level, payload.link(), payload.name());
            }
        });
    }
}
