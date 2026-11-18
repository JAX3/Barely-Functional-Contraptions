package com.jax3.barelyfunctionalcontraptions.sheep.registry;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusion;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ResourcefulSheep.MOD_ID);

    /** Infusion state of a sheep. Persisted with the entity (only when non-default) and synced to tracking clients. */
    public static final Supplier<AttachmentType<SheepInfusion>> SHEEP_INFUSION = REGISTER.register("sheep_infusion",
            () -> AttachmentType.builder(() -> SheepInfusion.NONE)
                    .serialize(SheepInfusion.CODEC, infusion -> !infusion.equals(SheepInfusion.NONE))
                    .sync(SheepInfusion.STREAM_CODEC)
                    .build());

    /** Bred stats of a sheep (yield, growth, endurance). Persisted once they differ from a wild sheep's, and synced. */
    public static final Supplier<AttachmentType<com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats>> SHEEP_STATS = REGISTER.register("sheep_stats",
            () -> AttachmentType.builder(() -> com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats.WILD)
                    .serialize(com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats.CODEC, stats -> !stats.isWild())
                    .sync(com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats.STREAM_CODEC)
                    .build());

    /** The player whose skin a sheep wears (fed a Melon Slice). Persisted only when set, and synced to tracking clients. */
    public static final Supplier<AttachmentType<com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepSkin>> SHEEP_SKIN = REGISTER.register("sheep_skin",
            () -> AttachmentType.builder(() -> com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepSkin.NONE)
                    .serialize(com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepSkin.CODEC, skin -> skin.profile().isPresent())
                    .sync(com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepSkin.STREAM_CODEC)
                    .build());

    /** How tired a sheep is from the Sheep Treadmill. Persisted while it isn't fresh; the server alone needs it. */
    public static final Supplier<AttachmentType<com.jax3.barelyfunctionalcontraptions.sheep.entity.TreadmillWalker>> TREADMILL_WALKER = REGISTER.register("treadmill_walker",
            () -> AttachmentType.builder(() -> com.jax3.barelyfunctionalcontraptions.sheep.entity.TreadmillWalker.FRESH)
                    .serialize(com.jax3.barelyfunctionalcontraptions.sheep.entity.TreadmillWalker.CODEC,
                            walker -> !walker.equals(com.jax3.barelyfunctionalcontraptions.sheep.entity.TreadmillWalker.FRESH))
                    .build());

    private ModAttachments() {}
}
