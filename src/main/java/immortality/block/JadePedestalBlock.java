package immortality.block;

import immortality.block.entity.JadePedestalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

public class JadePedestalBlock extends BaseEntityBlock {
	public static final MapCodec<JadePedestalBlock> CODEC = simpleCodec(JadePedestalBlock::new);

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	public JadePedestalBlock(Properties properties) {
		super(properties);
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
