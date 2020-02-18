package com.simibubi.create.modules.logistics.block.extractor;

import static net.minecraft.block.HorizontalBlock.HORIZONTAL_FACING;

import com.simibubi.create.foundation.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.modules.logistics.block.belts.AttachedLogisticalBlock;
import com.simibubi.create.modules.logistics.block.transposer.TransposerBlock;

import net.minecraft.block.BlockState;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.Vec3d;

public class ExtractorSlots {

	static class Filter extends ValueBoxTransform {

		Vec3d offsetForHorizontal = VecHelper.voxelSpace(8f, 10.5f, 14f);
		Vec3d offsetForUpward = VecHelper.voxelSpace(8f, 14.15f, 3.5f);
		Vec3d offsetForDownward = VecHelper.voxelSpace(8f, 1.85f, 3.5f);

		@Override
		protected Vec3d getLocation(BlockState state) {
			Vec3d location = offsetForHorizontal;
			if (state.getBlock() instanceof TransposerBlock)
				location = location.add(0, 2/16f, 0);
			if (AttachedLogisticalBlock.isVertical(state))
				location = state.get(AttachedLogisticalBlock.UPWARD) ? offsetForUpward : offsetForDownward;
			return rotateHorizontally(state, location);
		}

		@Override
		protected Vec3d getOrientation(BlockState state) {
			float yRot = AngleHelper.horizontalAngle(state.get(HORIZONTAL_FACING));
			float zRot = (AttachedLogisticalBlock.isVertical(state)) ? 0 : 90;
			return new Vec3d(0, yRot, zRot);
		}

	}

	public static class Link extends ValueBoxTransform.Dual {

		public Link(boolean first) {
			super(first);
		}

		Vec3d offsetForHorizontal = VecHelper.voxelSpace(11.5f, 4f, 14f);
		Vec3d offsetForUpward = VecHelper.voxelSpace(10f, 14f, 11.5f);
		Vec3d offsetForDownward = VecHelper.voxelSpace(10f, 2f, 11.5f);

		@Override
		protected Vec3d getLocation(BlockState state) {
			Vec3d location = offsetForHorizontal;
			if (state.getBlock() instanceof TransposerBlock)
				location = location.add(0, 2/16f, 0);
			if (!isFirst())
				location = location.add(0, 4/16f, 0);

			if (AttachedLogisticalBlock.isVertical(state)) {
				location = state.get(AttachedLogisticalBlock.UPWARD) ? offsetForUpward : offsetForDownward;
				if (!isFirst())
					location = location.add(-4/16f, 0, 0);
			}

			float yRot = AngleHelper.horizontalAngle(state.get(HORIZONTAL_FACING));
			location = VecHelper.rotateCentered(location, yRot, Axis.Y);
			return location;
		}

		@Override
		protected Vec3d getOrientation(BlockState state) {
			float horizontalAngle = AngleHelper.horizontalAngle(state.get(HORIZONTAL_FACING));
			boolean vertical = AttachedLogisticalBlock.isVertical(state);
			float xRot = vertical ? (state.get(AttachedLogisticalBlock.UPWARD) ? 90 : 270) : 0;
			float yRot = vertical ? horizontalAngle + 180 : horizontalAngle + 270;
			return new Vec3d(xRot, yRot, 0);
		}

	}

}
