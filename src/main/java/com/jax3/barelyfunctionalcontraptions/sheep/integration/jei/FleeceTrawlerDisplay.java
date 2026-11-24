package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import org.jspecify.annotations.Nullable;

/** A Fleece Trawler page: plain fishing ({@code lure} null), or what a lure of one resource's wool brings up. */
public record FleeceTrawlerDisplay(@Nullable SheepResource lure) {}
