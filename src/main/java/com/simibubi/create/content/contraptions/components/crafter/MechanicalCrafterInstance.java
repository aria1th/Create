package com.simibubi.create.content.contraptions.components.crafter;

import java.util.function.Supplier;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.base.RotatingData;
import com.simibubi.create.content.contraptions.base.SingleRotatingInstance;
import com.simibubi.create.foundation.render.backend.instancing.InstancedModel;
import com.simibubi.create.foundation.render.backend.instancing.InstancedTileRenderRegistry;
import com.simibubi.create.foundation.render.backend.instancing.InstancedTileRenderer;
import com.simibubi.create.foundation.utility.MatrixStacker;

import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class MechanicalCrafterInstance extends SingleRotatingInstance {
    public static void register(TileEntityType<? extends KineticTileEntity> type) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                InstancedTileRenderRegistry.instance.register(type, MechanicalCrafterInstance::new));
    }

    public MechanicalCrafterInstance(InstancedTileRenderer<?> modelManager, KineticTileEntity tile) {
        super(modelManager, tile);
    }

    @Override
    protected InstancedModel<RotatingData> getModel() {
        Direction facing = lastState.get(MechanicalCrafterBlock.HORIZONTAL_FACING);

        Supplier<MatrixStack> ms = () -> {
            MatrixStack stack = new MatrixStack();
            MatrixStacker stacker = MatrixStacker.of(stack).centre();

            if (facing.getAxis() == Direction.Axis.X)
                stacker.rotateZ(90);
            else if (facing.getAxis() == Direction.Axis.Z)
                stacker.rotateX(90);

            stacker.unCentre();
            return stack;
        };
        return rotatingMaterial().getModel(AllBlockPartials.SHAFTLESS_COGWHEEL, lastState, facing, ms);
    }
}
