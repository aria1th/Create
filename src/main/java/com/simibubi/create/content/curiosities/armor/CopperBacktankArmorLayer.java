package com.simibubi.create.content.curiosities.armor;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Color;

import com.simibubi.create.lib.helper.EntityHelper;

import com.simibubi.create.lib.helper.EntityRendererManagerHelper;
import com.simibubi.create.lib.helper.LivingRendererHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class CopperBacktankArmorLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

	public CopperBacktankArmorLayer(RenderLayerParent<T, M> renderer) {
		super(renderer);
	}

	@Override
	public void render(PoseStack ms, MultiBufferSource buffer, int light, LivingEntity entity, float yaw, float pitch,
		float pt, float p_225628_8_, float p_225628_9_, float p_225628_10_) {
		if (entity.getPose() == Pose.SLEEPING)
			return;
		if (!AllItems.COPPER_BACKTANK.get()
			.isWornBy(entity))
			return;

		M entityModel = getParentModel();
		if (!(entityModel instanceof HumanoidModel))
			return;

		HumanoidModel<?> model = (HumanoidModel<?>) entityModel;
		RenderType renderType = Sheets.cutoutBlockSheet();
		BlockState renderedState = AllBlocks.COPPER_BACKTANK.getDefaultState()
				.setValue(CopperBacktankBlock.HORIZONTAL_FACING, Direction.SOUTH);
		SuperByteBuffer backtank = CachedBufferer.block(renderedState);
		SuperByteBuffer cogs = CachedBufferer.partial(AllBlockPartials.COPPER_BACKTANK_COGS, renderedState);

		ms.pushPose();

		model.body.translateAndRotate(ms);
		ms.translate(-1 / 2f, 10 / 16f, 1f);
		ms.scale(1, -1, -1);

		backtank.forEntityRender()
			.light(light)
			.renderInto(ms, buffer.getBuffer(renderType));

		cogs.centre()
			.rotateY(180)
			.unCentre()
			.translate(0, 6.5f / 16, 11f / 16)
			.rotate(Direction.EAST, AngleHelper.rad(2 * AnimationTickHolder.getRenderTime(entity.level) % 360))
			.translate(0, -6.5f / 16, -11f / 16);

		cogs.forEntityRender()
			.light(light)
			.renderInto(ms, buffer.getBuffer(renderType));

		ms.popPose();
	}

	public static void registerOnAll(EntityRenderDispatcher renderManager) {
		for (EntityRenderer<? extends Player> renderer : EntityRendererManagerHelper.getSkinMap(renderManager).values())
			registerOn(renderer);
		for (EntityRenderer<?> renderer : EntityRendererManagerHelper.getRenderers(renderManager).values())
			registerOn(renderer);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void registerOn(EntityRenderer<?> entityRenderer) {
		if (!(entityRenderer instanceof LivingEntityRenderer))
			return;
		LivingEntityRenderer<?, ?> livingRenderer = (LivingEntityRenderer<?, ?>) entityRenderer;
		if (!(livingRenderer.getModel() instanceof HumanoidModel))
			return;
		CopperBacktankArmorLayer<?, ?> layer = new CopperBacktankArmorLayer<>(livingRenderer);
		LivingRendererHelper.addRenderer(livingRenderer, layer);
	}

	public static void renderRemainingAirOverlay(PoseStack ms, BufferSource buffers, int light, int overlay, float pt) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null)
			return;
		if (player.isSpectator() || player.isCreative())
			return;
		if (!EntityHelper.getExtraCustomData(player)
			.contains("VisualBacktankAir"))
			return;
		if (!player.isEyeInFluid(FluidTags.WATER))
			return;

		int timeLeft = EntityHelper.getExtraCustomData(player)
			.getInt("VisualBacktankAir");

		ms.pushPose();

		Window window = Minecraft.getInstance()
			.getWindow();
		ms.translate(window.getGuiScaledWidth() / 2 + 90, window.getGuiScaledHeight() - 53, 0);

		Component text = new TextComponent(StringUtil.formatTickDuration(timeLeft * 20));
		GuiGameElement.of(AllItems.COPPER_BACKTANK.asStack())
			.at(0, 0)
			.render(ms);
		int color = 0xFF_FFFFFF;
		if (timeLeft < 60 && timeLeft % 2 == 0) {
			color = Color.mixColors(0xFF_FF0000, color, Math.max(timeLeft / 60f, .25f));
		}
		Minecraft.getInstance().font.drawShadow(ms, text, 16, 5, color);
		buffers.endBatch();

		ms.popPose();
	}

}
