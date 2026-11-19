package com.jax3.barelyfunctionalcontraptions.compat;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * A NeoForge loot modifier whose conditions and priority read the same in every version (Minecraft 26.2: a list of
 * {@code conditions}, here optional so the same JSON works on every version). A modifier takes its {@link Base} in its
 * constructor and passes it on.
 */
public abstract class CompatLootModifier extends LootModifier {
    /** The conditions and priority every loot modifier has, in this version's form. */
    public record Base(LootItemCondition[] conditions, int priority) {}

    protected CompatLootModifier(Base base) {
        super(base.conditions(), base.priority());
    }

    protected Base base() {
        return new Base(conditions, priority);
    }

    /** The base fields of a modifier's codec, to follow with {@code .and(...)} for its own. */
    protected static <T extends CompatLootModifier> Products.P1<RecordCodecBuilder.Mu<T>, Base> baseFields(RecordCodecBuilder.Instance<T> i) {
        return i.group(BASE.forGetter(CompatLootModifier::base));
    }

    private static final MapCodec<Base> BASE = RecordCodecBuilder.mapCodec(i -> i.group(
            IGlobalLootModifier.LOOT_CONDITIONS_CODEC.optionalFieldOf("conditions", new LootItemCondition[0]).forGetter(Base::conditions),
            Codec.INT.optionalFieldOf("priority", IGlobalLootModifier.DEFAULT_PRIORITY).forGetter(Base::priority)
    ).apply(i, Base::new));

    /** The codec of a modifier with no fields of its own. */
    protected static <T extends CompatLootModifier> MapCodec<T> simpleCodec(Function<Base, T> constructor) {
        return RecordCodecBuilder.mapCodec(i -> baseFields(i).apply(i, constructor));
    }
}
