package immortality.block;

import immortality.block.entity.JadePedestalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

public class JadePedestalBlock extends BaseEntityBlock {
	public static final MapCodec<JadePedestalBlock> CODEC = simpleCodec(JadePedestalBlock::new);

	private static final VoxelShape BASE = Block.box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0);
	private static final VoxelShape PILLAR = Block.box(4.0, 3.0, 4.0, 12.0, 12.0, 12.0);
	private static final VoxelShape TOP = Block.box(1.0, 12.0, 1.0, 15.0, 16.0, 15.0);
	private static final VoxelShape SHAPE = Shapes.or(BASE, PILLAR, TOP);

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	public JadePedestalBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new JadePedestalBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (world.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof JadePedestalBlockEntity pedestal) {
			ItemStack held = player.getMainHandItem();
			ItemStack current = pedestal.getItem();
			
			if (current.isEmpty() && !held.isEmpty()) {
				pedestal.setItem(held.split(1));
				return InteractionResult.SUCCESS;
			} else if (!current.isEmpty()) {
				player.getInventory().placeItemBackInInventory(current);
				pedestal.setItem(ItemStack.EMPTY);
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.PASS;
	}
}
