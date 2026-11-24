package com.jax3.barelyfunctionalcontraptions.sheep.upgrade;

/** A machine's block entity with upgrade slots. */
public interface Upgradeable {
    MachineUpgrades upgrades();

    /** What its installed upgrades do now. */
    default UpgradeEffects upgradeEffects() {
        return upgrades().effects();
    }

    /** Blocks a Range card adds to its working area: from its cards on the server, as last synced on a client. */
    default int rangeBonus() {
        return upgrades().range();
    }

    /** Whether a Muffler card silences it: from its cards on the server, as last synced on a client. */
    default boolean muffled() {
        return upgrades().muffled();
    }
}
