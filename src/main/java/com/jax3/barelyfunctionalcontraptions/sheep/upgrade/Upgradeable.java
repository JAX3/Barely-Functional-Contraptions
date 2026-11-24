package com.jax3.barelyfunctionalcontraptions.sheep.upgrade;

/** A machine's block entity with upgrade slots. */
public interface Upgradeable {
    MachineUpgrades upgrades();

    /** What its installed upgrades do now. */
    default UpgradeEffects upgradeEffects() {
        return upgrades().effects();
    }
}
