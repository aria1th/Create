package com.simibubi.create.foundation.utility.worldWrappers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.jozufozu.flywheel.api.FlywheelWorld;

import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;

import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionWorld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;

import javax.annotation.Nonnull;

public class PlacementSimulationWorld extends WrappedWorld implements FlywheelWorld {
	public Map<BlockPos, BlockState> blocksAdded;
	public Map<BlockPos, BlockEntity> tesAdded;

	public Set<SectionPos> spannedSections;
	public LevelLightEngine lighter;
	public WrappedChunkProvider chunkProvider;
	private final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();

	private final ContraptionWorld contraptionWorld;

	public PlacementSimulationWorld(Level wrapped, Contraption c) {
		this(wrapped, c, new WrappedChunkProvider());
	}

	public PlacementSimulationWorld(Level wrapped, @Nonnull Contraption c, WrappedChunkProvider chunkProvider) {
		super(wrapped, chunkProvider);
		contraptionWorld = c.getContraptionWorld();
		this.chunkProvider = chunkProvider.setPlacementWorld(this);
		spannedSections = new HashSet<>();
		lighter = new LevelLightEngine(chunkProvider, true, false); // blockLight, skyLight
		blocksAdded = new HashMap<>();
		tesAdded = new HashMap<>();
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return lighter;
	}

	public void updateLightSources() {
		for (Map.Entry<BlockPos, BlockState> entry : blocksAdded.entrySet()) {
			BlockPos pos = entry.getKey();
			BlockState state = entry.getValue();
			int light = state.getLightEmission(this, pos);
			if (light > 0) {
				lighter.onBlockEmissionIncrease(pos, light);
			}
		}
	}

	public void setTileEntities(Collection<BlockEntity> tileEntities) {
		tesAdded.clear();
		tileEntities.forEach(te -> tesAdded.put(te.getBlockPos(), te));
	}

	public void clear() {
		blocksAdded.clear();
	}

	@Override
	public boolean setBlock(BlockPos pos, BlockState newState, int flags) {
		blocksAdded.put(pos, newState);

		SectionPos sectionPos = SectionPos.of(pos);
		if (spannedSections.add(sectionPos)) {
			lighter.updateSectionStatus(sectionPos, false);
		}

		if ((flags & Block.UPDATE_SUPPRESS_LIGHT) == 0) {
			lighter.checkBlock(pos);
		}

		return true;
	}

	@Override
	public boolean setBlockAndUpdate(BlockPos pos, BlockState state) {
		return setBlock(pos, state, 0);
	}

	@Override
	public BlockEntity getBlockEntity(BlockPos pos) {
		return tesAdded.get(pos);
	}

	@Override
	public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> condition) {
		return condition.test(getBlockState(pos));
	}

	@Override
	public boolean isLoaded(BlockPos pos) {
		return true;
	}

	@Override
	public boolean isAreaLoaded(BlockPos center, int range) {
		return true;
	}

	public BlockState getBlockState(int x, int y, int z) {
		return getBlockState(scratch.set(x, y, z));
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		BlockState state = blocksAdded.get(pos);
		if (state != null)
			return state;
		return Blocks.AIR.defaultBlockState();
	}

	@Override
	public int getHeight() {
		return contraptionWorld.getHeight();
	}

	@Override
	public int getMinBuildHeight() {
		return contraptionWorld.getMinBuildHeight();
	}

	// Override Starlight's ExtendedWorld interface methods:

	public LevelChunk getChunkAtImmediately(final int chunkX, final int chunkZ) {
		return chunkProvider.getChunk(chunkX, chunkZ, false);
	}

	public ChunkAccess getAnyChunkImmediately(final int chunkX, final int chunkZ) {
		return chunkProvider.getChunk(chunkX, chunkZ);
	}
}
