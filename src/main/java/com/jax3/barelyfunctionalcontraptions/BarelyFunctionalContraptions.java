package com.jax3.barelyfunctionalcontraptions;

import com.jax3.barelyfunctionalcontraptions.gametest.IntegrationGameTests;
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
        IntegrationGameTests.register(modBus);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
