package com.jax3.functionalresources.sheep.integration.jei;

import com.jax3.functionalresources.sheep.resource.SheepResource;

/** JEI recipe: feed {@code resource}'s feeding item to a sheep, shear it for resource wool. */
public record InfusionDisplay(SheepResource resource) {}
