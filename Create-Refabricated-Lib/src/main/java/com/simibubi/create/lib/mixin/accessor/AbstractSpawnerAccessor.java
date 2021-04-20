package com.simibubi.create.lib.mixin.accessor;

import net.minecraft.util.WeightedSpawnerEntity;

import net.minecraft.world.spawner.AbstractSpawner;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(AbstractSpawner.class)
public interface AbstractSpawnerAccessor {
	@Accessor("potentialSpawns")
	List<WeightedSpawnerEntity> create$potentialSpawns();

	@Accessor("spawnData")
	WeightedSpawnerEntity create$spawnData();
}