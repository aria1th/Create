package com.simibubi.create.modules.contraptions.relays.belt;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.block.IHaveNoBlockItem;
import com.simibubi.create.foundation.block.IWithTileEntity;
import com.simibubi.create.foundation.utility.AllShapes;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.modules.contraptions.base.HorizontalKineticBlock;
import com.simibubi.create.modules.contraptions.relays.belt.BeltMovementHandler.TransportedEntityInfo;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Direction.AxisDirection;
import net.minecraft.util.Hand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class BeltBlock extends HorizontalKineticBlock implements IHaveNoBlockItem, IWithTileEntity<BeltTileEntity> {

	public static final IProperty<Slope> SLOPE = EnumProperty.create("slope", Slope.class);
	public static final IProperty<Part> PART = EnumProperty.create("part", Part.class);
	public static final BooleanProperty CASING = BooleanProperty.create("casing");

	public BeltBlock() {
		super(Properties.from(Blocks.BROWN_WOOL));
		setDefaultState(getDefaultState().with(SLOPE, Slope.HORIZONTAL).with(PART, Part.START).with(CASING, false));
	}

	@Override
	public boolean hasShaftTowards(IWorldReader world, BlockPos pos, BlockState state, Direction face) {
		if (face.getAxis() != getRotationAxis(state))
			return false;
		BeltTileEntity beltEntity = (BeltTileEntity) world.getTileEntity(pos);
		return beltEntity != null && beltEntity.hasPulley();
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.get(HORIZONTAL_FACING).rotateY().getAxis();
	}

	@Override
	public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos,
			PlayerEntity player) {
		return AllItems.BELT_CONNECTOR.asStack();
	}

	@Override
	public Material getMaterial(BlockState state) {
		return state.get(CASING) ? Material.WOOD : Material.WOOL;
	}

	@SuppressWarnings("deprecation")
	@Override
	public List<ItemStack> getDrops(BlockState state, net.minecraft.world.storage.loot.LootContext.Builder builder) {
		List<ItemStack> drops = super.getDrops(state, builder);
		if (state.get(CASING))
			drops.addAll(AllBlocks.BRASS_CASING.getDefault()
					.getDrops(builder));
		return drops;
	}
	
	@Override
	public void spawnAdditionalDrops(BlockState state, World worldIn, BlockPos pos, ItemStack stack) {
		withTileEntityDo(worldIn, pos, te -> {
			if (worldIn.isRemote)
				return;
			if (te.hasPulley())
				Block.spawnDrops(AllBlocks.SHAFT.get().getDefaultState(), worldIn, pos);
			if (te.isController()) {
				BeltInventory inv = te.getInventory();
				for (TransportedItemStack s : inv.items)
					inv.eject(s);
			}
		});
	}

	@Override
	public boolean isFlammable(BlockState state, IBlockReader world, BlockPos pos, Direction face) {
		return false;
	}

	@Override
	public void onLanded(IBlockReader worldIn, Entity entityIn) {
		super.onLanded(worldIn, entityIn);
		BlockPos entityPosition = entityIn.getPosition();
		BlockPos beltPos = null;

		if (AllBlocks.BELT.typeOf(worldIn.getBlockState(entityPosition)))
			beltPos = entityPosition;
		else if (AllBlocks.BELT.typeOf(worldIn.getBlockState(entityPosition.down())))
			beltPos = entityPosition.down();
		if (beltPos == null)
			return;
		if (!(worldIn instanceof World))
			return;

		onEntityCollision(worldIn.getBlockState(beltPos), (World) worldIn, beltPos, entityIn);
	}

	@Override
	public void onEntityCollision(BlockState state, World worldIn, BlockPos pos, Entity entityIn) {
		BeltTileEntity belt = null;
		belt = (BeltTileEntity) worldIn.getTileEntity(pos);

		if (state.get(SLOPE) == Slope.VERTICAL)
			return;
		if (entityIn instanceof PlayerEntity && entityIn.isSneaking())
			return;
		if (belt == null || belt.getSpeed() == 0)
			return;
		if (entityIn instanceof ItemEntity && entityIn.isAlive()) {
			if (worldIn.isRemote)
				return;
			if (entityIn.getMotion().y > 0)
				return;
			withTileEntityDo(worldIn, pos, te -> {
				ItemEntity itemEntity = (ItemEntity) entityIn;
				IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElse(null);
				if (handler == null)
					return;
				ItemStack remainder = handler.insertItem(0, itemEntity.getItem().copy(), false);
				if (remainder.isEmpty())
					itemEntity.remove();
			});
			return;
		}

		BeltTileEntity controller = (BeltTileEntity) worldIn.getTileEntity(belt.getController());
		if (controller == null || controller.passengers == null)
			return;
		if (controller.passengers.containsKey(entityIn)) {
			TransportedEntityInfo info = controller.passengers.get(entityIn);
			if (info.ticksSinceLastCollision != 0 || pos.equals(entityIn.getPosition()))
				info.refresh(pos, state);
		} else
			controller.passengers.put(entityIn, new TransportedEntityInfo(pos, state));
	}

	@Override
	public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
		updateNeighbouringTunnel(worldIn, pos, state);
		withTileEntityDo(worldIn, pos, te -> {
			te.attachmentTracker.findAttachments(te);
		});
	}

	@Override
	public boolean onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
			BlockRayTraceResult hit) {
		if (player.isSneaking() || !player.isAllowEdit())
			return false;
		ItemStack heldItem = player.getHeldItem(handIn);
		boolean isShaft = heldItem.getItem() == AllBlocks.SHAFT.get().asItem();
		boolean isCasing = heldItem.getItem() == AllBlocks.BRASS_CASING.get().asItem();
		boolean isDye = Tags.Items.DYES.contains(heldItem.getItem());
		boolean isHand = heldItem.isEmpty() && handIn == Hand.MAIN_HAND;

		if (isDye) {
			if (worldIn.isRemote)
				return true;
			withTileEntityDo(worldIn, pos, te -> {
				DyeColor dyeColor = DyeColor.getColor(heldItem);
				if (dyeColor == null)
					return;
				te.applyColor(dyeColor);
			});
			if (!player.isCreative())
				heldItem.shrink(1);
			return true;
		}

		TileEntity te = worldIn.getTileEntity(pos);
		if (te == null || !(te instanceof BeltTileEntity))
			return false;
		BeltTileEntity belt = (BeltTileEntity) te;

		if (isHand) {
			BeltTileEntity controllerBelt = belt.getControllerTE();
			if (controllerBelt == null)
				return false;
			if (worldIn.isRemote)
				return true;
			controllerBelt.getInventory().forEachWithin(belt.index + .5f, .55f, (transportedItemStack) -> {
				player.inventory.placeItemBackInInventory(worldIn, transportedItemStack.stack);
				return Collections.emptyList();
			});
		}

		if (isShaft) {
			if (state.get(PART) != Part.MIDDLE)
				return false;
			if (worldIn.isRemote)
				return true;
			if (!player.isCreative())
				heldItem.shrink(1);
			worldIn.setBlockState(pos, state.with(PART, Part.PULLEY), 2);
			belt.attachKinetics();
			return true;
		}

		if (isCasing) {
			if (state.get(CASING))
				return false;
			if (state.get(SLOPE) == Slope.VERTICAL)
				return false;
			if (!player.isCreative())
				heldItem.shrink(1);
			worldIn.setBlockState(pos, state.with(CASING, true), 2);
			return true;
		}

		return false;
	}

	@Override
	public ActionResultType onWrenched(BlockState state, ItemUseContext context) {
		World world = context.getWorld();
		TileEntity te = world.getTileEntity(context.getPos());
		if (te == null || !(te instanceof BeltTileEntity))
			return ActionResultType.PASS;
		BeltTileEntity belt = (BeltTileEntity) te;
		PlayerEntity player = context.getPlayer();

		if (state.get(CASING)) {
			if (world.isRemote)
				return ActionResultType.SUCCESS;
			world.setBlockState(context.getPos(), state.with(CASING, false), 3);
			if (!player.isCreative())
				player.inventory.placeItemBackInInventory(world, new ItemStack(AllBlocks.BRASS_CASING.block));
			return ActionResultType.SUCCESS;
		}

		if (state.get(PART) == Part.PULLEY) {
			if (world.isRemote)
				return ActionResultType.SUCCESS;
			world.setBlockState(context.getPos(), state.with(PART, Part.MIDDLE), 2);
			belt.detachKinetics();
			belt.attachKinetics();
			if (!player.isCreative())
				player.inventory.placeItemBackInInventory(world, new ItemStack(AllBlocks.SHAFT.block));
			return ActionResultType.SUCCESS;
		}

		return ActionResultType.FAIL;
	}

	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder) {
		builder.add(SLOPE, PART, CASING);
		super.fillStateContainer(builder);
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public PathNodeType getAiPathNodeType(BlockState state, IBlockReader world, BlockPos pos, MobEntity entity) {
		return PathNodeType.DANGER_OTHER;
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		return VoxelShapes.or(BeltShapes.getShape(state), BeltShapes.getCasingShape(state));
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos,
			ISelectionContext context) {
		VoxelShape shape = getShape(state, worldIn, pos, context);
		BeltTileEntity belt = (BeltTileEntity) worldIn.getTileEntity(pos);
		if (belt == null || context.getEntity() == null)
			return shape;
		BeltTileEntity controller = (BeltTileEntity) worldIn.getTileEntity(belt.getController());
		if (controller == null)
			return shape;
		if (controller.passengers == null || !controller.passengers.containsKey(context.getEntity())) {
			return VoxelShapes.combine(AllShapes.BELT_COLLISION_MASK, shape, IBooleanFunction.AND);
		}

		return shape;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new BeltTileEntity();
	}

	@Override
	public boolean canRenderInLayer(BlockState state, BlockRenderLayer layer) {
		return state.get(CASING) && layer == getRenderLayer();
	}

	@Override
	public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		if (worldIn.isRemote)
			return;
		if (state.getBlock() == newState.getBlock())
			return;
		updateNeighbouringTunnel(worldIn, pos, state);

		// Destroy chain
		boolean endWasDestroyed = state.get(PART) == Part.END;
		TileEntity tileEntity = worldIn.getTileEntity(pos);
		if (tileEntity == null)
			return;
		if (!(tileEntity instanceof BeltTileEntity))
			return;
		BeltTileEntity beltEntity = (BeltTileEntity) tileEntity;
		BlockPos controller = beltEntity.getController();
		beltEntity.setSource(null);
		beltEntity.remove();

		int limit = 1000;
		BlockPos toDestroy = controller;
		BlockState destroyedBlock = null;

		do {

			if (!toDestroy.equals(pos)) {
				destroyedBlock = worldIn.getBlockState(toDestroy);
				if (!AllBlocks.BELT.typeOf(destroyedBlock))
					break;

				BeltTileEntity te = (BeltTileEntity) worldIn.getTileEntity(toDestroy);
				if (te.isController()) {
					BeltInventory inv = te.getInventory();
					for (TransportedItemStack stack : inv.items)
						inv.eject(stack);
				}

				te.setSource(null);
				te.remove();

				if (destroyedBlock.get(CASING))
					Block.spawnAsEntity(worldIn, toDestroy, new ItemStack(AllBlocks.BRASS_CASING.get()));
				if (te.hasPulley())
					worldIn.setBlockState(toDestroy, AllBlocks.SHAFT.get().getDefaultState()
							.with(BlockStateProperties.AXIS, getRotationAxis(destroyedBlock)), 3);
				else
					worldIn.destroyBlock(toDestroy, false);

				if (destroyedBlock.get(PART) == Part.END)
					break;
			} else {
				if (endWasDestroyed)
					break;
			}

			Slope slope = state.get(SLOPE);
			Direction direction = state.get(HORIZONTAL_FACING);

			if (slope == Slope.VERTICAL) {
				toDestroy = toDestroy.up(direction.getAxisDirection() == AxisDirection.POSITIVE ? 1 : -1);
				continue;
			}

			toDestroy = toDestroy.offset(direction);
			if (slope != Slope.HORIZONTAL)
				toDestroy = toDestroy.up(slope == Slope.UPWARD ? 1 : -1);

		} while (limit-- > 0);

	}

	private void updateNeighbouringTunnel(World world, BlockPos pos, BlockState beltState) {
		boolean isEnd = beltState.get(PART) != Part.END;
		if (isEnd && beltState.get(PART) != Part.START)
			return;
		int offset = isEnd ? -1 : 1;
		BlockPos tunnelPos = pos.offset(beltState.get(HORIZONTAL_FACING), offset).up();
		if (AllBlocks.BELT_TUNNEL.typeOf(world.getBlockState(tunnelPos)))
			BeltTunnelBlock.updateTunnel(world, tunnelPos);
	}

	public enum Slope implements IStringSerializable {
		HORIZONTAL, UPWARD, DOWNWARD, VERTICAL;

		@Override
		public String getName() {
			return Lang.asId(name());
		}
	}

	public enum Part implements IStringSerializable {
		START, MIDDLE, END, PULLEY;

		@Override
		public String getName() {
			return Lang.asId(name());
		}
	}

	public static List<BlockPos> getBeltChain(World world, BlockPos controllerPos) {
		List<BlockPos> positions = new LinkedList<>();

		BlockState blockState = world.getBlockState(controllerPos);
		if (!AllBlocks.BELT.typeOf(blockState))
			return positions;

		Slope slope = blockState.get(SLOPE);
		Direction direction = blockState.get(HORIZONTAL_FACING);

		int limit = 1000;
		BlockPos current = controllerPos;
		do {
			positions.add(current);

			if (!AllBlocks.BELT.typeOf(world.getBlockState(current)))
				break;
			if (world.getBlockState(current).get(PART) == Part.END)
				break;
			if (slope == Slope.VERTICAL) {
				current = current.up(direction.getAxisDirection() == AxisDirection.POSITIVE ? 1 : -1);
				continue;
			}
			current = current.offset(direction);
			if (slope != Slope.HORIZONTAL)
				current = current.up(slope == Slope.UPWARD ? 1 : -1);
		} while (limit-- > 0);

		return positions;
	}

	@Override
	protected boolean hasStaticPart() {
		return false;
	}

	public static boolean canAccessFromSide(Direction facing, BlockState belt) {
		if (facing == null)
			return true;
		if (!belt.get(BeltBlock.CASING))
			return false;
		Part part = belt.get(BeltBlock.PART);
		if (part != Part.MIDDLE && facing.getAxis() == belt.get(HORIZONTAL_FACING).rotateY().getAxis())
			return false;

		Slope slope = belt.get(BeltBlock.SLOPE);
		if (slope != Slope.HORIZONTAL) {
			if (slope == Slope.DOWNWARD && part == Part.END)
				return true;
			if (slope == Slope.UPWARD && part == Part.START)
				return true;
			Direction beltSide = belt.get(HORIZONTAL_FACING);
			if (slope == Slope.DOWNWARD)
				beltSide = beltSide.getOpposite();
			if (beltSide == facing)
				return false;
		}

		return true;
	}

}
