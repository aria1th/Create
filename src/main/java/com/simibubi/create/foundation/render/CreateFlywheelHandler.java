package com.simibubi.create.foundation.render;

import com.jozufozu.flywheel.backend.Backend;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionContext;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionRenderDispatcher;
import com.simibubi.create.foundation.render.effects.EffectsContext;

public class CreateFlywheelHandler {
	public static void init() {
		Backend.register(ContraptionContext.INSTANCE);
		Backend.register(EffectsContext.INSTANCE);
		Backend.listeners.renderLayerListener(ContraptionRenderDispatcher::renderLayer);
		Backend.listeners.setupFrameListener(ContraptionRenderDispatcher::beginFrame);
		Backend.listeners.refreshListener($ -> ContraptionRenderDispatcher.invalidateAll());
	}
}
