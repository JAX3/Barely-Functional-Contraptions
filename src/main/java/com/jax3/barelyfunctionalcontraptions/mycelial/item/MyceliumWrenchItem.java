package com.jax3.barelyfunctionalcontraptions.mycelial.item;

import com.jax3.barelyfunctionalcontraptions.mycelial.config.ServerConfig;
import com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks;
import com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * The Barely Functional Wrench: links machines to a Machine Status Panel and crates to a Crate Controller, flips
 * Energy Meters, sets pipe sides, and (see {@link WrenchDismantling}) dismantles the mod's blocks. Everything is
 * decided on the server; what it's linking is stored on the item stack.
 */
public class MyceliumWrenchItem extends Item {
    public MyceliumWrenchItem(Properties properties) {
        super(properties);
    }

    /**
     * Blocks that open a GUI on right-click (status panels, crate controllers, pipes, the Energy Meter) are handled
     * here, before the block is used. Generators already pass wrench clicks on to {@link #useOn}.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        // Machine Status Panel: clicking one starts (or finishes) linking machines to it; while linking, clicking a
        // machine links or unlinks it, before anything else the wrench or the machine would do.
        InteractionResult panel = linkToPanel(stack, context);
        if (panel != InteractionResult.PASS) return panel;
        // Crate Controller: clicking one starts (or finishes) linking crates to its store.
        InteractionResult controller = linkToController(stack, context);
        if (controller != InteractionResult.PASS) return controller;
        // Energy Meter: the wrench flips it (sneak: turns it).
        if (context.getPlayer() != null && context.getLevel().getBlockState(context.getClickedPos()).getBlock()
                instanceof com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock) {
            if (!context.getLevel().isClientSide()) {
                com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.turnWithWrench(context.getLevel(), context.getClickedPos(), context.getPlayer());
            }
            return InteractionResult.SUCCESS;
        }
        // Fluid and Item Pipes: the wrench sets each side's mode and filter.
        if (context.getPlayer() != null && context.getLevel().getBlockState(context.getClickedPos()).getBlock()
                instanceof com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock) {
            var hit = new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside());
            InteractionResult result = com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock.useWrench(
                    context.getLevel(), context.getClickedPos(), context.getPlayer(), context.getHand(), hit);
            if (result != InteractionResult.PASS) return result;
        }
        return InteractionResult.PASS;
    }

    private InteractionResult linkToPanel(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        boolean onPanel = level.getBlockEntity(pos) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity;
        GlobalPos linking = stack.get(ModDataComponents.LINKING_PANEL.get());
        if (!onPanel && linking == null) return InteractionResult.PASS;
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        GlobalPos clicked = GlobalPos.of(level.dimension(), pos);
        if (onPanel) {
            boolean sameScreen = linking != null && linking.dimension().equals(level.dimension())
                    && com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.master(level, linking.pos())
                    .equals(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.master(level, pos));
            if (sameScreen) {
                stack.remove(ModDataComponents.LINKING_PANEL.get());
                ok(player, Component.translatable("message.barelyfunctionalcontraptions.status_panel.done"));
            } else {
                stack.remove(ModDataComponents.LINKING_CONTROLLER.get());
                stack.set(ModDataComponents.LINKING_PANEL.get(), clicked);
                ok(player, Component.translatable("message.barelyfunctionalcontraptions.status_panel.linking"));
            }
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.4F);
            return InteractionResult.SUCCESS;
        }
        if (!linking.dimension().equals(level.dimension())
                || !(level.getBlockEntity(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.master(level, linking.pos()))
                instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity panel)) {
            stack.remove(ModDataComponents.LINKING_PANEL.get());
            fail(player, Component.translatable("message.barelyfunctionalcontraptions.status_panel.gone"));
            return InteractionResult.SUCCESS;
        }
        var result = panel.toggleLink(serverLevel, pos);
        Component name = level.getBlockState(pos).getBlock().getName();
        switch (result) {
            case LINKED -> {
                ok(player, Component.translatable("message.barelyfunctionalcontraptions.status_panel.linked", name, panel.links().size(), panel.capacity()));
                if (panel.links().size() >= com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.MISSION_CONTROL_MACHINES
                        && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.award(serverPlayer,
                            com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.MISSION_CONTROL);
                }
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.8F);
            }
            case UNLINKED -> {
                ok(player, Component.translatable("message.barelyfunctionalcontraptions.status_panel.unlinked", name));
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 0.8F, 0.9F);
            }
            case FULL -> fail(player, Component.translatable("message.barelyfunctionalcontraptions.status_panel.full", panel.capacity()));
            case TOO_FAR -> fail(player, Component.translatable("message.barelyfunctionalcontraptions.status_panel.too_far",
                    com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity.range()));
            case NOT_A_MACHINE -> fail(player, Component.translatable("message.barelyfunctionalcontraptions.status_panel.not_a_machine", name));
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult linkToController(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        boolean onController = level.getBlockEntity(pos) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity;
        GlobalPos linking = stack.get(ModDataComponents.LINKING_CONTROLLER.get());
        if (!onController && linking == null) return InteractionResult.PASS;
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        String key = "message.barelyfunctionalcontraptions.crate_controller.";
        if (onController) {
            if (linking != null && linking.dimension().equals(level.dimension()) && linking.pos().equals(pos)) {
                stack.remove(ModDataComponents.LINKING_CONTROLLER.get());
                ok(player, Component.translatable(key + "done"));
            } else {
                stack.set(ModDataComponents.LINKING_CONTROLLER.get(), GlobalPos.of(level.dimension(), pos));
                ok(player, Component.translatable(key + "linking"));
            }
            level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_INSERT, SoundSource.BLOCKS, 0.8F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        if (!linking.dimension().equals(level.dimension())
                || !(level.getBlockEntity(linking.pos()) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity controller)) {
            stack.remove(ModDataComponents.LINKING_CONTROLLER.get());
            fail(player, Component.translatable(key + "gone"));
            return InteractionResult.SUCCESS;
        }
        var result = controller.toggleLink(serverLevel, pos);
        switch (result) {
            case LINKED -> {
                ok(player, Component.translatable(key + "linked", controller.crateCount(), com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity.maxCrates()));
                level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_INSERT, SoundSource.BLOCKS, 0.8F, 1.4F);
            }
            case UNLINKED -> {
                ok(player, Component.translatable(key + "unlinked"));
                level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_PICKUP, SoundSource.BLOCKS, 0.8F, 0.9F);
            }
            case ALREADY_IN -> ok(player, Component.translatable(key + "already_in"));
            case NOT_A_CRATE -> fail(player, Component.translatable(key + "not_a_crate", level.getBlockState(pos).getBlock().getName()));
            case TOO_FAR -> fail(player, Component.translatable(key + "too_far", com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity.range()));
            case FULL -> fail(player, Component.translatable(key + "full", com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity.maxCrates()));
            case TAKEN -> fail(player, Component.translatable(key + "taken"));
        }
        return InteractionResult.SUCCESS;
    }

    private static void ok(Player player, Component message) {
        player.sendOverlayMessage(message.copy().withStyle(ChatFormatting.GREEN));
    }

    private static void fail(Player player, Component message) {
        player.sendOverlayMessage(message.copy().withStyle(ChatFormatting.RED));
    }

    // ------------------------------------------------------------------ appearance

    /** The enchantment glint shows while it's linking machines to a Machine Status Panel or crates to a Crate Controller. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.LINKING_PANEL.get()) || stack.has(ModDataComponents.LINKING_CONTROLLER.get());
    }

    @Override
    @SuppressWarnings("deprecation") // the TooltipDisplay overload is the current (deprecated-for-components) item hook
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.barelyfunctionalcontraptions.mycelium_wrench.desc").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());
        if (ServerConfig.get(ServerConfig.WRENCH_DISMANTLE)) {
            tooltip.accept(Component.translatable("item.barelyfunctionalcontraptions.mycelium_wrench.help.dismantle").withStyle(ChatFormatting.WHITE));
        }
        tooltip.accept(Component.translatable("item.barelyfunctionalcontraptions.mycelium_wrench.flavor").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}
