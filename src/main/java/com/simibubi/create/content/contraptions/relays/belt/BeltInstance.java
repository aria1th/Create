package com.simibubi.create.content.contraptions.relays.belt;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.base.KineticTileInstance;
import com.simibubi.create.content.contraptions.base.RotatingData;
import com.simibubi.create.foundation.block.render.SpriteShiftEntry;
import com.simibubi.create.foundation.render.backend.instancing.InstanceKey;
import com.simibubi.create.foundation.render.backend.instancing.InstancedModel;
import com.simibubi.create.foundation.render.backend.instancing.InstancedTileRenderRegistry;
import com.simibubi.create.foundation.render.backend.instancing.InstancedTileRenderer;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.MatrixStacker;

import net.minecraft.item.DyeColor;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class BeltInstance extends KineticTileInstance<BeltTileEntity> {
    public static void register(TileEntityType<? extends BeltTileEntity> type) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                InstancedTileRenderRegistry.instance.register(type, BeltInstance::new));
    }

    private boolean upward;
    private boolean diagonal;
    private boolean sideways;
    private boolean vertical;
    private boolean alongX;
    private boolean alongZ;
    private BeltSlope beltSlope;
    private Direction facing;
    protected ArrayList<InstanceKey<BeltData>> keys;
    protected InstanceKey<RotatingData> pulleyKey;

    public BeltInstance(InstancedTileRenderer<?> modelManager, BeltTileEntity tile) {
        super(modelManager, tile);
    }

    @Override
    protected void init() {
        if (!AllBlocks.BELT.has(lastState))
            return;

        keys = new ArrayList<>(2);

        beltSlope = lastState.get(BeltBlock.SLOPE);
        facing = lastState.get(BeltBlock.HORIZONTAL_FACING);
        upward = beltSlope == BeltSlope.UPWARD;
        diagonal = beltSlope.isDiagonal();
        sideways = beltSlope == BeltSlope.SIDEWAYS;
        vertical = beltSlope == BeltSlope.VERTICAL;
        alongX = facing.getAxis() == Direction.Axis.X;
        alongZ = facing.getAxis() == Direction.Axis.Z;

        BeltPart part = lastState.get(BeltBlock.PART);
        boolean start = part == BeltPart.START;
        boolean end = part == BeltPart.END;
        DyeColor color = tile.color.orElse(null);

        for (boolean bottom : Iterate.trueAndFalse) {
            AllBlockPartials beltPartial = BeltRenderer.getBeltPartial(diagonal, start, end, bottom);
            SpriteShiftEntry spriteShift = BeltRenderer.getSpriteShiftEntry(color, diagonal, bottom);

            InstancedModel<BeltData> beltModel = beltPartial.renderOnBelt(modelManager, lastState);
            Consumer<BeltData> setupFunc = setupFunc(bottom, spriteShift);

            keys.add(beltModel.setupInstance(setupFunc));

            if (diagonal) break;
        }

        if (tile.hasPulley()) {
            InstancedModel<RotatingData> pulleyModel = getPulleyModel();

            pulleyKey = pulleyModel.setupInstance(setupFunc(tile.getSpeed(), getRotationAxis()));
        }
    }

    @Override
    public void onUpdate() {
        DyeColor color = tile.color.orElse(null);

        boolean bottom = true;
        for (InstanceKey<BeltData> key : keys) {

            SpriteShiftEntry spriteShiftEntry = BeltRenderer.getSpriteShiftEntry(color, diagonal, bottom);
            key.modifyInstance(data -> data.setScrollTexture(spriteShiftEntry)
                                           .setColor(tile.network)
                                           .setRotationalSpeed(getScrollSpeed()));
            bottom = false;
        }

        if (pulleyKey != null) {
            updateRotation(pulleyKey, getRotationAxis());
        }
    }

    @Override
    public void updateLight() {
        for (InstanceKey<BeltData> key : keys) {
            key.modifyInstance(this::relight);
        }

        if (pulleyKey != null) pulleyKey.modifyInstance(this::relight);
    }

    @Override
    public void remove() {
        keys.forEach(InstanceKey::delete);
        keys.clear();
        if (pulleyKey != null) pulleyKey.delete();
        pulleyKey = null;
    }

    private float getScrollSpeed() {
        float speed = tile.getSpeed();
        if (((facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) ^ upward) ^
                ((alongX && !diagonal) || (alongZ && diagonal)) ^ (vertical && facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE)) {
            speed = -speed;
        }
        if (sideways && (facing == Direction.SOUTH || facing == Direction.WEST))
            speed = -speed;

        return speed;
    }

    private InstancedModel<RotatingData> getPulleyModel() {
        Direction dir = getOrientation();

        Direction.Axis axis = dir.getAxis();

        Supplier<MatrixStack> ms = () -> {
            MatrixStack modelTransform = new MatrixStack();
            MatrixStacker msr = MatrixStacker.of(modelTransform);
            msr.centre();
            if (axis == Direction.Axis.X)
                msr.rotateY(90);
            if (axis == Direction.Axis.Y)
                msr.rotateX(90);
            msr.rotateX(90);
            msr.unCentre();

            return modelTransform;
        };

        return rotatingMaterial().getModel(AllBlockPartials.BELT_PULLEY, lastState, dir, ms);
    }

    private Direction getOrientation() {
        Direction dir = lastState.get(BeltBlock.HORIZONTAL_FACING)
                                  .rotateY();
        if (beltSlope == BeltSlope.SIDEWAYS)
            dir = Direction.UP;

        return dir;
    }

    private Consumer<BeltData> setupFunc(boolean bottom, SpriteShiftEntry spriteShift) {
        return data -> {
            float rotX = (!diagonal && beltSlope != BeltSlope.HORIZONTAL ? 90 : 0) + (beltSlope == BeltSlope.DOWNWARD ? 180 : 0);
            float rotY = facing.getHorizontalAngle() + (upward ? 180 : 0) + (sideways ? 90 : 0);
            float rotZ = sideways ? 90 : ((vertical && facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) ? 180 : 0);

            BlockPos pos = tile.getPos();
            data.setTileEntity(tile)
                .setBlockLight(world.getLightLevel(LightType.BLOCK, pos))
                .setSkyLight(world.getLightLevel(LightType.SKY, pos))
                .setRotation(rotX, rotY, rotZ)
                .setRotationalSpeed(getScrollSpeed())
                .setRotationOffset(bottom ? 0.5f : 0f)
                .setScrollTexture(spriteShift)
                .setScrollMult(diagonal ? 3f / 8f : 0.5f);
        };
    }

}
