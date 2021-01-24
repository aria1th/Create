package com.simibubi.create.content.contraptions.components.structureMovement.bearing;

import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.foundation.render.contraption.RenderedContraption;
import com.simibubi.create.foundation.render.light.ContraptionLighter;
import com.simibubi.create.foundation.render.light.GridAlignedBB;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3i;

import java.util.List;

public class StabilizedLighter extends ContraptionLighter<StabilizedContraption> {
    public StabilizedLighter(StabilizedContraption contraption) {
        super(contraption);
    }

    @Override
    public void tick(RenderedContraption owner) {
        GridAlignedBB contraptionBounds = getContraptionBounds();

        if (!contraptionBounds.sameAs(bounds)) {
            lightVolume.move(contraption.entity.world, contraptionBoundsToVolume(contraptionBounds));
            bounds = contraptionBounds;
        }
    }

    @Override
    public GridAlignedBB getContraptionBounds() {
        GridAlignedBB bb = GridAlignedBB.fromAABB(contraption.bounds);

        bb.translate(contraption.entity.getPosition());

        return bb;
    }
}
