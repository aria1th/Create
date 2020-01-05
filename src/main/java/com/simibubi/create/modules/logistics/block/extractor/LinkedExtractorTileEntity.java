package com.simibubi.create.modules.logistics.block.extractor;

import static net.minecraft.state.properties.BlockStateProperties.POWERED;

import java.util.List;

import com.simibubi.create.AllTileEntities;
import com.simibubi.create.foundation.behaviour.base.TileEntityBehaviour;
import com.simibubi.create.foundation.behaviour.linked.LinkBehaviour;
import com.simibubi.create.foundation.behaviour.linked.LinkBehaviour.SlotPositioning;
import com.simibubi.create.modules.logistics.block.belts.AttachedLogisiticalBlock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class LinkedExtractorTileEntity extends ExtractorTileEntity {

	private static LinkBehaviour.SlotPositioning slots;
	public boolean receivedSignal;
	public LinkBehaviour receiver;

	public LinkedExtractorTileEntity() {
		super(AllTileEntities.LINKED_EXTRACTOR.type);
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		if (slots == null)
			slots = new SlotPositioning(LinkedExtractorBlock::getFrequencySlotPosition,
					LinkedExtractorBlock::getFrequencySlotOrientation).scale(.4f);
		receiver = LinkBehaviour.receiver(this, this::setSignal).withSlotPositioning(slots);
		behaviours.add(receiver);
		super.addBehaviours(behaviours);
	}

	public void setSignal(boolean powered) {
		receivedSignal = powered;
	}

	@Override
	public void initialize() {
		super.initialize();
		if (world.isBlockPowered(pos))
			setState(State.LOCKED);
		neighborChanged();
	}

	@Override
	public void tick() {
		super.tick();
		if (world.isRemote)
			return;
		if (receivedSignal != getBlockState().get(POWERED)) {
			setLocked(receivedSignal);
			world.setBlockState(pos, getBlockState().cycle(POWERED));
			return;
		}
	}

	@Override
	public BlockPos getInventoryPos() {
		BlockState blockState = getBlockState();
		Block block = blockState.getBlock();
		if (!(block instanceof ExtractorBlock))
			return null;
		return getPos().offset(AttachedLogisiticalBlock.getBlockFacing(blockState));
	}

}
