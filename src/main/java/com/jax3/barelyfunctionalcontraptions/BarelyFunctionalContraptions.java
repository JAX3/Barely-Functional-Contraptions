package com.jax3.barelyfunctionalcontraptions;

import com.jax3.barelyfunctionalcontraptions.mycelial.MycelialPower;
import com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab;
import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Entry point for Barely Functional Contraptions.
 *
 * <p>The mod is made of two modules that share one mod id, one namespace and one creative tab:</p>
 * <ul>
 *     <li>{@link MycelialPower}: the Mycelial Generator (mushrooms + water → Forge Energy) and the Barely Functional Wrench.</li>
 *     <li>{@link ResourcefulSheep}: resource-infused sheep, resource wool and the FE-powered Mechanical Separator.</li>
 * </ul>
 *
 * <p>They work together: Mycelial Generators power Mechanical Separators directly, and sheep infused with mushroom
 * blocks grow mushroom wool that the separator turns back into generator fuel.</p>
 */
@Mod(BarelyFunctionalContraptions.MOD_ID)
public final class BarelyFunctionalContraptions {
    public static final String MOD_ID = "barelyfunctionalcontraptions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BarelyFunctionalContraptions(IEventBus modBus, ModContainer container) {
        MycelialPower.init(modBus, container);
        ResourcefulSheep.init(modBus, container);
        ContraptionsCreativeTab.REGISTER.register(modBus);
        // GameTests and the screenshot harness are dev tools: released jars leave them out (see build.gradle).
        if (!net.neoforged.fml.loading.FMLEnvironment.isProduction()) {
            devOnly("gametest.DevGameTests", "register", IEventBus.class, modBus);
        }
    }

    /** Calls a dev-only class by name, so production code never refers to the classes the released jar leaves out. */
    public static void devOnly(String className, String method, Class<?> argType, Object arg) {
        try {
            Class<?> type = Class.forName("com.jax3.barelyfunctionalcontraptions." + className);
            if (argType == null) type.getMethod(method).invoke(null);
            else type.getMethod(method, argType).invoke(null, arg);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Dev-only {} is not available", className, e);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
