package com.simibubi.create.content.contraptions.components.structureMovement;

import static java.util.concurrent.TimeUnit.SECONDS;
import static net.minecraft.entity.Entity.collideBoundingBoxHeuristically;
import static net.minecraft.entity.Entity.horizontalMag;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;

import com.google.common.base.Predicates;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.components.actors.BlockBreakingMovementBehaviour;
import com.simibubi.create.foundation.collision.ContinuousOBBCollider.ContinuousSeparationManifold;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.collision.OrientedBB;
import com.simibubi.create.foundation.networking.AllPackets;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.block.BlockState;
import net.minecraft.block.CocoaBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Direction.AxisDirection;
import net.minecraft.util.ReuseableStream;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.Template.BlockInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.WorldTickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class ContraptionCollider {

	public static DamageSource damageSourceContraptionSuffocate =
		new DamageSource("create.contraption_suffocate").setDamageBypassesArmor();
	public static boolean wasClientPlayerGrounded;
	public static Cache<World, List<WeakReference<ContraptionEntity>>> activeContraptions = CacheBuilder.newBuilder()
		.expireAfterAccess(400, SECONDS)
		.build();

	@SubscribeEvent
	public static void addSpawnedContraptionsToCollisionList(EntityJoinWorldEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof ContraptionEntity))
			return;
		try {
			List<WeakReference<ContraptionEntity>> list = activeContraptions.get(event.getWorld(), ArrayList::new);
			ContraptionEntity contraption = (ContraptionEntity) entity;
			list.add(new WeakReference<>(contraption));
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public static void playerCollisionHappensOnClientTick(ClientTickEvent event) {
		if (event.phase == Phase.START)
			return;
		ClientWorld world = Minecraft.getInstance().world;
		if (world == null)
			return;
		runCollisions(world);
	}

	@SubscribeEvent
	public static void entityCollisionHappensPreWorldTick(WorldTickEvent event) {
		if (event.phase == Phase.END)
			return;
		World world = event.world;
		runCollisions(world);
	}

	@SubscribeEvent
	public static void entitiesWhoJustDismountedGetSentToTheRightLocation(LivingUpdateEvent event) {
		LivingEntity entityLiving = event.getEntityLiving();
		if (entityLiving == null)
			return;
		if (entityLiving.world.isRemote)
			return;
		CompoundNBT data = entityLiving.getPersistentData();
		if (!data.contains("ContraptionDismountLocation"))
			return;
		Vector3d position = VecHelper.readNBT(data.getList("ContraptionDismountLocation", NBT.TAG_DOUBLE));
		if (entityLiving.getRidingEntity() == null)
			entityLiving.setPositionAndUpdate(position.x, position.y, position.z);
		data.remove("ContraptionDismountLocation");
	}

	private static void runCollisions(World world) {
		List<WeakReference<ContraptionEntity>> list = activeContraptions.getIfPresent(world);
		if (list == null)
			return;
		for (Iterator<WeakReference<ContraptionEntity>> iterator = list.iterator(); iterator.hasNext();) {
			WeakReference<ContraptionEntity> weakReference = iterator.next();
			ContraptionEntity contraptionEntity = weakReference.get();
			if (contraptionEntity == null || !contraptionEntity.isAlive()) {
				iterator.remove();
				continue;
			}
			collideEntities(contraptionEntity);
		}
	}

	public static void collideEntities(ContraptionEntity contraptionEntity) {
		World world = contraptionEntity.getEntityWorld();
		Contraption contraption = contraptionEntity.getContraption();
		AxisAlignedBB bounds = contraptionEntity.getBoundingBox();
		Vector3d contraptionPosition = contraptionEntity.getPositionVec();
		Vector3d contraptionRotation = contraptionEntity.getRotationVec();
		Vector3d contraptionMotion = contraptionEntity.stationary ? Vector3d.ZERO
			: contraptionPosition.subtract(contraptionEntity.getPrevPositionVec());
		contraptionEntity.collidingEntities.clear();

		if (contraption == null)
			return;
		if (bounds == null)
			return;

		Vector3d centerOfBlock = VecHelper.getCenterOf(BlockPos.ZERO);
		double conRotX = contraptionRotation.x;
		double conRotY = contraptionRotation.y;
		double conRotZ = contraptionRotation.z;
		Vector3d contraptionCentreOffset = contraptionEntity.stationary ? centerOfBlock : Vector3d.ZERO.add(0, 0.5, 0);
		boolean axisAlignedCollision = contraptionRotation.equals(Vector3d.ZERO);
		Matrix3d rotation = null;

		for (Entity entity : world.getEntitiesWithinAABB((EntityType<?>) null, bounds.grow(2)
			.expand(0, 32, 0), contraptionEntity::canCollideWith)) {
			boolean player = entity instanceof PlayerEntity;
			boolean serverPlayer = player && !world.isRemote;

			// Init matrix
			if (rotation == null) {
				rotation = new Matrix3d().asIdentity();
				if (!axisAlignedCollision) {
					rotation.multiply(new Matrix3d().asXRotation(AngleHelper.rad(-conRotX)));
					rotation.multiply(new Matrix3d().asYRotation(AngleHelper.rad(conRotY)));
					rotation.multiply(new Matrix3d().asZRotation(AngleHelper.rad(-conRotZ)));
				}
			}

			// Transform entity position and motion to local space
			Vector3d entityPosition = entity.getPositionVec();
			AxisAlignedBB entityBounds = entity.getBoundingBox();
			Vector3d centerY = new Vector3d(0, entityBounds.getYSize() / 2, 0);
			Vector3d motion = entity.getMotion();

			Vector3d position = entityPosition.subtract(contraptionCentreOffset)
				.add(centerY);
			position = position.subtract(contraptionPosition);
			position = rotation.transform(position);
			position = position.add(centerOfBlock)
				.subtract(centerY)
				.subtract(entityPosition);

			// Find all potential block shapes to collide with
			AxisAlignedBB localBB = entityBounds.offset(position)
				.grow(1.0E-7D);
			ReuseableStream<VoxelShape> potentialHits =
				getPotentiallyCollidedShapes(world, contraption, localBB.expand(motion));
			if (potentialHits.createStream()
				.count() == 0)
				continue;

			// Prepare entity bounds
			OrientedBB obb = new OrientedBB(localBB);
			obb.setRotation(rotation);
			motion = rotation.transform(motion);
			motion = motion.subtract(contraptionMotion);

//			Vector3d visualizerOrigin = new Vector3d(10, 64, 0);
//			CollisionDebugger.OBB = obb.copy();
//			CollisionDebugger.OBB.move(visualizerOrigin);

			MutableObject<Vector3d> collisionResponse = new MutableObject<>(Vector3d.ZERO);
			MutableObject<Vector3d> allowedMotion = new MutableObject<>(motion);
			MutableBoolean futureCollision = new MutableBoolean(false);
			MutableBoolean surfaceCollision = new MutableBoolean(false);
			Vector3d obbCenter = obb.getCenter();

			// Apply separation maths
			List<AxisAlignedBB> bbs = new ArrayList<>();
			potentialHits.createStream()
				.forEach(shape -> shape.toBoundingBoxList()
					.forEach(bbs::add));

			boolean doHorizontalPass = conRotX == 0 && conRotZ == 0;
			for (boolean horizontalPass : Iterate.trueAndFalse) {

				for (AxisAlignedBB bb : bbs) {
					Vector3d currentResponse = collisionResponse.getValue();
					obb.setCenter(obbCenter.add(currentResponse));
					ContinuousSeparationManifold intersect = obb.intersect(bb, allowedMotion.getValue());

					if (intersect == null)
						continue;
					if ((!horizontalPass || !doHorizontalPass) && surfaceCollision.isFalse())
						surfaceCollision.setValue(intersect.isSurfaceCollision());

					double timeOfImpact = intersect.getTimeOfImpact();
					if (timeOfImpact > 0 && timeOfImpact < 1) {
						futureCollision.setTrue();
						allowedMotion.setValue(intersect.getAllowedMotion(allowedMotion.getValue()));
						continue;
					}

					Vector3d separation = intersect.asSeparationVec(entity.stepHeight);
					if (separation != null && !separation.equals(Vector3d.ZERO))
						collisionResponse.setValue(currentResponse.add(separation));
				}

				if (!horizontalPass || !doHorizontalPass)
					break;

				boolean noVerticalMotionResponse = allowedMotion.getValue().y == motion.y;
				boolean noVerticalCollision = collisionResponse.getValue().y == 0;
				if (noVerticalCollision && noVerticalMotionResponse)
					break;

				// Re-run collisions with horizontal offset
				collisionResponse.setValue(collisionResponse.getValue()
					.mul(1, 0, 1));
				allowedMotion.setValue(allowedMotion.getValue()
					.mul(1, 0, 1)
					.add(0, motion.y, 0));
				continue;
			}

			// Resolve collision
			Vector3d entityMotion = entity.getMotion();
			Vector3d totalResponse = collisionResponse.getValue();
			Vector3d motionResponse = allowedMotion.getValue();
			boolean hardCollision = !totalResponse.equals(Vector3d.ZERO);

			rotation.transpose();
			motionResponse = rotation.transform(motionResponse)
				.add(contraptionMotion);
			totalResponse = rotation.transform(totalResponse);
			rotation.transpose();

			if (futureCollision.isTrue() && !serverPlayer) {
				if (motionResponse.y != entityMotion.y) {
					entity.setMotion(entityMotion.mul(1, 0, 1)
						.add(0, motionResponse.y, 0));
					entityMotion = entity.getMotion();
				}
			}

			Vector3d contactPointMotion = Vector3d.ZERO;
			if (surfaceCollision.isTrue()) {
				entity.fallDistance = 0;
				entity.onGround = true;
				contraptionEntity.collidingEntities.add(entity);
				if (!serverPlayer) 
					contactPointMotion = contraptionEntity.getContactPointMotion(entityPosition);
			}

			if (hardCollision) {
				double motionX = entityMotion.getX();
				double motionY = entityMotion.getY();
				double motionZ = entityMotion.getZ();
				double intersectX = totalResponse.getX();
				double intersectY = totalResponse.getY();
				double intersectZ = totalResponse.getZ();

				double horizonalEpsilon = 1 / 128f;
				if (motionX != 0 && Math.abs(intersectX) > horizonalEpsilon && motionX > 0 == intersectX < 0)
					entityMotion = entityMotion.mul(0, 1, 1);
				if (motionY != 0 && intersectY != 0 && motionY > 0 == intersectY < 0)
					entityMotion = entityMotion.mul(1, 0, 1);
				if (motionZ != 0 && Math.abs(intersectZ) > horizonalEpsilon && motionZ > 0 == intersectZ < 0)
					entityMotion = entityMotion.mul(1, 1, 0);
			}

			if (!hardCollision && surfaceCollision.isFalse())
				continue;

			if (serverPlayer && entity instanceof ServerPlayerEntity) {
				((ServerPlayerEntity) entity).connection.floatingTickCount = 0;
				continue;
			}

			totalResponse = totalResponse.add(contactPointMotion);
			Vector3d allowedMovement = getAllowedMovement(totalResponse, entity);
			contraptionEntity.collidingEntities.add(entity);
			entity.velocityChanged = true;
			entity.setPosition(entityPosition.x + allowedMovement.x, entityPosition.y + allowedMovement.y,
				entityPosition.z + allowedMovement.z);
			entity.setMotion(entityMotion);
			
			if (!serverPlayer && player)
				AllPackets.channel.sendToServer(new ClientMotionPacket(entityMotion, true));
		}

	}

	/** From Entity#getAllowedMovement **/
	static Vector3d getAllowedMovement(Vector3d movement, Entity e) {
		AxisAlignedBB bb = e.getBoundingBox();
		ISelectionContext ctx = ISelectionContext.forEntity(e);
		World world = e.world;
		VoxelShape voxelshape = world.getWorldBorder()
			.getShape();
		Stream<VoxelShape> stream =
			VoxelShapes.compare(voxelshape, VoxelShapes.create(bb.shrink(1.0E-7D)), IBooleanFunction.AND)
				? Stream.empty()
				: Stream.of(voxelshape);
		Stream<VoxelShape> stream1 = world.getEmptyCollisionShapes(e, bb.expand(movement), ImmutableSet.of());
		ReuseableStream<VoxelShape> reuseablestream = new ReuseableStream<>(Stream.concat(stream1, stream));
		Vector3d Vector3d = movement.lengthSquared() == 0.0D ? movement
			: collideBoundingBoxHeuristically(e, movement, bb, world, ctx, reuseablestream);
		boolean flag = movement.x != Vector3d.x;
		boolean flag1 = movement.y != Vector3d.y;
		boolean flag2 = movement.z != Vector3d.z;
		boolean flag3 = e.onGround || flag1 && movement.y < 0.0D;
		if (e.stepHeight > 0.0F && flag3 && (flag || flag2)) {
			Vector3d Vector3d1 = collideBoundingBoxHeuristically(e, new Vector3d(movement.x, (double) e.stepHeight, movement.z),
				bb, world, ctx, reuseablestream);
			Vector3d Vector3d2 = collideBoundingBoxHeuristically(e, new Vector3d(0.0D, (double) e.stepHeight, 0.0D),
				bb.expand(movement.x, 0.0D, movement.z), world, ctx, reuseablestream);
			if (Vector3d2.y < (double) e.stepHeight) {
				Vector3d Vector3d3 = collideBoundingBoxHeuristically(e, new Vector3d(movement.x, 0.0D, movement.z),
					bb.offset(Vector3d2), world, ctx, reuseablestream).add(Vector3d2);
				if (horizontalMag(Vector3d3) > horizontalMag(Vector3d1)) {
					Vector3d1 = Vector3d3;
				}
			}

			if (horizontalMag(Vector3d1) > horizontalMag(Vector3d)) {
				return Vector3d1.add(collideBoundingBoxHeuristically(e, new Vector3d(0.0D, -Vector3d1.y + movement.y, 0.0D),
					bb.offset(Vector3d1), world, ctx, reuseablestream));
			}
		}

		return Vector3d;
	}

	public static void pushEntityOutOfShape(Entity entity, VoxelShape voxelShape, Vector3d positionOffset,
		Vector3d shapeMotion) {
		AxisAlignedBB entityBB = entity.getBoundingBox()
			.offset(positionOffset);
		Vector3d entityMotion = entity.getMotion();

		if (!voxelShape.toBoundingBoxList()
			.stream()
			.anyMatch(entityBB::intersects))
			return;

		AxisAlignedBB shapeBB = voxelShape.getBoundingBox();
		Direction bestSide = Direction.DOWN;
		double bestOffset = 100;
		double finalOffset = 0;

		for (Direction face : Direction.values()) {
			Axis axis = face.getAxis();
			double d = axis == Axis.X ? entityBB.getXSize() + shapeBB.getXSize()
				: axis == Axis.Y ? entityBB.getYSize() + shapeBB.getYSize() : entityBB.getZSize() + shapeBB.getZSize();
			d = d + .5f;

			Vector3d nudge = Vector3d.of(face.getDirectionVec()).scale(d);
			AxisAlignedBB nudgedBB = entityBB.offset(nudge.getX(), nudge.getY(), nudge.getZ());
			double nudgeDistance = face.getAxisDirection() == AxisDirection.POSITIVE ? -d : d;
			double offset = voxelShape.getAllowedOffset(face.getAxis(), nudgedBB, nudgeDistance);
			double abs = Math.abs(nudgeDistance - offset);
			if (abs < Math.abs(bestOffset) && abs != 0) {
				bestOffset = abs;
				finalOffset = abs;
				bestSide = face;
			}
		}

		if (bestOffset != 0) {
			entity.move(MoverType.SELF, Vector3d.of(bestSide.getDirectionVec()).scale(finalOffset));
			boolean positive = bestSide.getAxisDirection() == AxisDirection.POSITIVE;

			double clamped;
			switch (bestSide.getAxis()) {
			case X:
				clamped = positive ? Math.max(shapeMotion.x, entityMotion.x) : Math.min(shapeMotion.x, entityMotion.x);
				entity.setMotion(clamped, entityMotion.y, entityMotion.z);
				break;
			case Y:
				clamped = positive ? Math.max(shapeMotion.y, entityMotion.y) : Math.min(shapeMotion.y, entityMotion.y);
				if (bestSide == Direction.UP)
					clamped = shapeMotion.y;
				entity.setMotion(entityMotion.x, clamped, entityMotion.z);
				entity.handleFallDamage(entity.fallDistance, 1);
				entity.fallDistance = 0;
				entity.onGround = true;
				break;
			case Z:
				clamped = positive ? Math.max(shapeMotion.z, entityMotion.z) : Math.min(shapeMotion.z, entityMotion.z);
				entity.setMotion(entityMotion.x, entityMotion.y, clamped);
				break;
			}
		}
	}

	public static ReuseableStream<VoxelShape> getPotentiallyCollidedShapes(World world, Contraption contraption,
		AxisAlignedBB localBB) {

		double height = localBB.getYSize();
		double width = localBB.getXSize();
		double horizontalFactor = (height > width && width != 0) ? height / width : 1;
		double verticalFactor = (width > height && height != 0) ? width / height : 1;
		AxisAlignedBB blockScanBB = localBB.grow(0.5f);
		blockScanBB = blockScanBB.grow(horizontalFactor, verticalFactor, horizontalFactor);

		BlockPos min = new BlockPos(blockScanBB.minX, blockScanBB.minY, blockScanBB.minZ);
		BlockPos max = new BlockPos(blockScanBB.maxX, blockScanBB.maxY, blockScanBB.maxZ);

		ReuseableStream<VoxelShape> potentialHits = new ReuseableStream<>(BlockPos.getAllInBox(min, max)
			.filter(contraption.blocks::containsKey)
			.map(p -> {
				BlockState blockState = contraption.blocks.get(p).state;
				BlockPos pos = contraption.blocks.get(p).pos;
				VoxelShape collisionShape = blockState.getCollisionShape(world, p);
				return collisionShape.withOffset(pos.getX(), pos.getY(), pos.getZ());
			})
			.filter(Predicates.not(VoxelShape::isEmpty)));

		return potentialHits;
	}

	public static boolean collideBlocks(ContraptionEntity contraptionEntity) {
		if (Contraption.isFrozen())
			return true;
		if (!contraptionEntity.collisionEnabled())
			return false;

		World world = contraptionEntity.getEntityWorld();
		Vector3d motion = contraptionEntity.getMotion();
		Contraption contraption = contraptionEntity.getContraption();
		AxisAlignedBB bounds = contraptionEntity.getBoundingBox();
		Vector3d position = contraptionEntity.getPositionVec();
		BlockPos gridPos = new BlockPos(position);

		if (contraption == null)
			return false;
		if (bounds == null)
			return false;
		if (motion.equals(Vector3d.ZERO))
			return false;

		Direction movementDirection = Direction.getFacingFromVector(motion.x, motion.y, motion.z);

		// Blocks in the world
		if (movementDirection.getAxisDirection() == AxisDirection.POSITIVE)
			gridPos = gridPos.offset(movementDirection);
		if (isCollidingWithWorld(world, contraption, gridPos, movementDirection))
			return true;

		// Other moving Contraptions
		for (ContraptionEntity otherContraptionEntity : world.getEntitiesWithinAABB(ContraptionEntity.class,
			bounds.grow(1), e -> !e.equals(contraptionEntity))) {

			if (!otherContraptionEntity.collisionEnabled())
				continue;

			Vector3d otherMotion = otherContraptionEntity.getMotion();
			Contraption otherContraption = otherContraptionEntity.getContraption();
			AxisAlignedBB otherBounds = otherContraptionEntity.getBoundingBox();
			Vector3d otherPosition = otherContraptionEntity.getPositionVec();

			if (otherContraption == null)
				return false;
			if (otherBounds == null)
				return false;

			if (!bounds.offset(motion)
				.intersects(otherBounds.offset(otherMotion)))
				continue;

			for (BlockPos colliderPos : contraption.getColliders(world, movementDirection)) {
				colliderPos = colliderPos.add(gridPos)
					.subtract(new BlockPos(otherPosition));
				if (!otherContraption.blocks.containsKey(colliderPos))
					continue;
				return true;
			}
		}

		return false;
	}

	public static boolean isCollidingWithWorld(World world, Contraption contraption, BlockPos anchor,
		Direction movementDirection) {
		for (BlockPos pos : contraption.getColliders(world, movementDirection)) {
			BlockPos colliderPos = pos.add(anchor);

			if (!world.isBlockPresent(colliderPos))
				return true;

			BlockState collidedState = world.getBlockState(colliderPos);
			BlockInfo blockInfo = contraption.blocks.get(pos);

			if (blockInfo.state.getBlock() instanceof IPortableBlock) {
				IPortableBlock block = (IPortableBlock) blockInfo.state.getBlock();
				if (block.getMovementBehaviour() instanceof BlockBreakingMovementBehaviour) {
					BlockBreakingMovementBehaviour behaviour =
						(BlockBreakingMovementBehaviour) block.getMovementBehaviour();
					if (!behaviour.canBreak(world, colliderPos, collidedState)
						&& !collidedState.getCollisionShape(world, pos)
							.isEmpty()) {
						return true;
					}
					continue;
				}
			}

			if (AllBlocks.PULLEY_MAGNET.has(collidedState) && pos.equals(BlockPos.ZERO)
				&& movementDirection == Direction.UP)
				continue;
			if (collidedState.getBlock() instanceof CocoaBlock)
				continue;
			if (!collidedState.getMaterial()
				.isReplaceable()
				&& !collidedState.getCollisionShape(world, colliderPos)
					.isEmpty()) {
				return true;
			}

		}
		return false;
	}

}
