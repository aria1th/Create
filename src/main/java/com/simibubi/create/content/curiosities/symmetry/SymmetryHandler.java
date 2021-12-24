package com.simibubi.create.content.curiosities.symmetry;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.curiosities.symmetry.mirror.EmptyMirror;
import com.simibubi.create.content.curiosities.symmetry.mirror.SymmetryMirror;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SymmetryHandler {

	private static int tickCounter = 0;

	public static InteractionResult onBlockPlaced(BlockPlaceContext context) {
		if (context.getLevel()
			.isClientSide())
			return InteractionResult.PASS;
//		if (!(event.getEntity() instanceof Player))
//			return;

		Player player = (Player) context.getPlayer();
		Inventory inv = player.getInventory();
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			if (!inv.getItem(i)
				.isEmpty()
				&& inv.getItem(i)
					.getItem() == AllItems.WAND_OF_SYMMETRY.get()) {
				SymmetryWandItem.apply(player.level, inv.getItem(i), player, context.getClickedPos(), context.getLevel().getBlockState(context.getClickedPos()));
			}
		}
		return InteractionResult.PASS;
	}

	public static void onBlockDestroyed(Level world, Player player, BlockPos pos, BlockState state, /* Nullable */ BlockEntity blockEntity) {
		if (world
			.isClientSide())
			return;

//		Player player = event.getPlayer();
		Inventory inv = player.getInventory();
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			if (!inv.getItem(i)
				.isEmpty() && AllItems.WAND_OF_SYMMETRY.isIn(inv.getItem(i))) {
				SymmetryWandItem.remove(player.level, inv.getItem(i), player, pos);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public static void render(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		Random random = new Random();

		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			ItemStack stackInSlot = player.getInventory()
				.getItem(i);
			if (!AllItems.WAND_OF_SYMMETRY.isIn(stackInSlot))
				continue;
			if (!SymmetryWandItem.isEnabled(stackInSlot))
				continue;
			SymmetryMirror mirror = SymmetryWandItem.getMirror(stackInSlot);
			if (mirror instanceof EmptyMirror)
				continue;

			BlockPos pos = new BlockPos(mirror.getPosition());

			float yShift = 0;
			double speed = 1 / 16d;
			yShift = Mth.sin((float) (AnimationTickHolder.getRenderTime() * speed)) / 5f;

			MultiBufferSource.BufferSource buffer = mc.renderBuffers()
				.bufferSource();
			Camera info = mc.gameRenderer.getMainCamera();
			Vec3 view = info.getPosition();

			PoseStack ms = context.matrixStack();
			ms.pushPose();
			ms.translate(-view.x(), -view.y(), -view.z());
			ms.translate(pos.getX(), pos.getY(), pos.getZ());
			ms.translate(0, yShift + .2f, 0);
			mirror.applyModelTransform(ms);
			BakedModel model = mirror.getModel()
				.get();
			VertexConsumer builder = buffer.getBuffer(RenderType.solid());

			// FIXME VIRTUAL RENDERING
			mc.getBlockRenderer()
				.getModelRenderer()
				.tesselateBlock(player.level, model, Blocks.AIR.defaultBlockState(), pos, ms, builder, true,
					random, Mth.getSeed(pos), OverlayTexture.NO_OVERLAY);

			buffer.endBatch();
			ms.popPose();
		}
	}

	@Environment(EnvType.CLIENT)
	public static void onClientTick(Minecraft client) {
//		if (event.phase == Phase.START)
//			return;
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;

		if (mc.level == null)
			return;
		if (mc.isPaused())
			return;

		tickCounter++;

		if (tickCounter % 10 == 0) {
			for (int i = 0; i < Inventory.getSelectionSize(); i++) {
				ItemStack stackInSlot = player.getInventory()
					.getItem(i);

				if (stackInSlot != null && AllItems.WAND_OF_SYMMETRY.isIn(stackInSlot)
					&& SymmetryWandItem.isEnabled(stackInSlot)) {

					SymmetryMirror mirror = SymmetryWandItem.getMirror(stackInSlot);
					if (mirror instanceof EmptyMirror)
						continue;

					Random r = new Random();
					double offsetX = (r.nextDouble() - 0.5) * 0.3;
					double offsetZ = (r.nextDouble() - 0.5) * 0.3;

					Vec3 pos = mirror.getPosition()
						.add(0.5 + offsetX, 1 / 4d, 0.5 + offsetZ);
					Vec3 speed = new Vec3(0, r.nextDouble() * 1 / 8f, 0);
					mc.level.addParticle(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, speed.x, speed.y, speed.z);
				}
			}
		}

	}

	public static void drawEffect(BlockPos from, BlockPos to) {
		double density = 0.8f;
		Vec3 start = Vec3.atLowerCornerOf(from)
			.add(0.5, 0.5, 0.5);
		Vec3 end = Vec3.atLowerCornerOf(to)
			.add(0.5, 0.5, 0.5);
		Vec3 diff = end.subtract(start);

		Vec3 step = diff.normalize()
			.scale(density);
		int steps = (int) (diff.length() / step.length());

		Random r = new Random();
		for (int i = 3; i < steps - 1; i++) {
			Vec3 pos = start.add(step.scale(i));
			Vec3 speed = new Vec3(0, r.nextDouble() * -40f, 0);

			Minecraft.getInstance().level.addParticle(new DustParticleOptions(new Vector3f(1, 1, 1), 1), pos.x, pos.y,
				pos.z, speed.x, speed.y, speed.z);
		}

		Vec3 speed = new Vec3(0, r.nextDouble() * 1 / 32f, 0);
		Vec3 pos = start.add(step.scale(2));
		Minecraft.getInstance().level.addParticle(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, speed.x, speed.y,
			speed.z);

		speed = new Vec3(0, r.nextDouble() * 1 / 32f, 0);
		pos = start.add(step.scale(steps));
		Minecraft.getInstance().level.addParticle(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, speed.x, speed.y,
			speed.z);
	}

}
