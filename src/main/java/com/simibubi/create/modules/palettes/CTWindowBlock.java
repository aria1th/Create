package com.simibubi.create.modules.palettes;

import com.simibubi.create.AllCTs;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.util.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class CTWindowBlock extends VerticalCTGlassBlock {

	public CTWindowBlock(AllCTs spriteShift, boolean hasAlpha) {
		super(spriteShift, hasAlpha);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public boolean isSideInvisible(BlockState state, BlockState adjacentBlockState, Direction side) {
		return adjacentBlockState.getBlock() instanceof CTGlassBlock
				? (!RenderTypeLookup.canRenderInLayer(state, RenderType.getTranslucent()) && side.getAxis().isHorizontal()
						|| state.getBlock() == adjacentBlockState.getBlock())
				: super.isSideInvisible(state, adjacentBlockState, side);
	}
	
}
