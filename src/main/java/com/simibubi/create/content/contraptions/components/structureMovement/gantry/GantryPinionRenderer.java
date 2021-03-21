package com.simibubi.create.content.contraptions.components.structureMovement.gantry;

import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.contraptions.base.KineticBlockEntity;
import com.simibubi.create.content.contraptions.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.MatrixStacker;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;

public class GantryPinionRenderer extends KineticBlockEntityRenderer {

	public GantryPinionRenderer(BlockEntityRenderDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	protected void renderSafe(KineticBlockEntity te, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
							  int light, int overlay) {
		super.renderSafe(te, partialTicks, ms, buffer, light, overlay);
		BlockState state = te.getCachedState();
		Direction facing = state.get(GantryPinionBlock.FACING);
		Boolean alongFirst = state.get(GantryPinionBlock.AXIS_ALONG_FIRST_COORDINATE);
		Axis rotationAxis = getRotationAxisOf(te);
		BlockPos visualPos = facing.getDirection() == AxisDirection.POSITIVE ? te.getPos()
			: te.getPos()
				.offset(facing.getOpposite());
		float angleForTe = getAngleForTe(te, visualPos, rotationAxis);

		Axis gantryAxis = Axis.X;
		for (Axis axis : Iterate.axes)
			if (axis != rotationAxis && axis != facing.getAxis())
				gantryAxis = axis;

		if (gantryAxis == Axis.Z)
			if (facing == Direction.DOWN)
				angleForTe *= -1;
		if (gantryAxis == Axis.Y)
			if (facing == Direction.NORTH || facing == Direction.EAST)
				angleForTe *= -1;

		ms.push();

		MatrixStacker msr = MatrixStacker.of(ms);

		msr.centre()
			.rotateY(AngleHelper.horizontalAngle(facing))
			.rotateX(facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90)
			.rotateY(alongFirst ^ facing.getAxis() == Axis.Z ? 90 : 0);

		ms.translate(0, -9 / 16f, 0);
		ms.multiply(Vector3f.POSITIVE_X.getRadialQuaternion(-angleForTe));
		ms.translate(0, 9 / 16f, 0);

		msr.unCentre();
		AllBlockPartials.GANTRY_COGS.renderOn(state)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderLayer.getSolid()));

		ms.pop();
	}

	public static float getAngleForTe(KineticBlockEntity te, final BlockPos pos, Axis axis) {
		float time = AnimationTickHolder.getRenderTick();
		float offset = getRotationOffsetForPosition(te, pos, axis);
		return ((time * te.getSpeed() * 3f / 20 + offset) % 360) / 180 * (float) Math.PI;
	}

	@Override
	protected BlockState getRenderedBlockState(KineticBlockEntity te) {
		return shaft(getRotationAxisOf(te));
	}

}
