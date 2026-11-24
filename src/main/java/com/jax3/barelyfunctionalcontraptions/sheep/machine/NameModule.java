package com.jax3.barelyfunctionalcontraptions.sheep.machine;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The name an anvil gave the machine's item: shown as its window's title, kept when the machine is broken (its loot
 * table copies {@code minecraft:custom_name}) and saved under {@code CustomName}, as vanilla containers do.
 */
public final class NameModule implements MachineModule {
    private final Component defaultName;
    private @Nullable Component customName;

    public NameModule(Component defaultName) {
        this.defaultName = defaultName;
    }

    /** Its custom name, else its default one. */
    public Component name() {
        return customName != null ? customName : defaultName;
    }

    public @Nullable Component customName() {
        return customName;
    }

    @Override
    public void save(ValueOutput output) {
        output.storeNullable("CustomName", ComponentSerialization.CODEC, customName);
    }

    @Override
    public void load(ValueInput input) {
        customName = input.read("CustomName", ComponentSerialization.CODEC).orElse(null);
    }

    @Override
    public void collectComponents(DataComponentMap.Builder components) {
        components.set(DataComponents.CUSTOM_NAME, customName);
    }

    @Override
    public void applyComponents(DataComponentGetter components) {
        customName = components.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    public void removeComponents(ValueOutput output) {
        output.discard("CustomName");
    }
}
