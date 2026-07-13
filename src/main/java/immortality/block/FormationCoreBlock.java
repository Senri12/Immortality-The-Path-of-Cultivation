package immortality.block;

import immortality.block.entity.FormationCoreBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class FormationCoreBlock extends BaseEntityBlock {
	public static final MapCodec<FormationCoreBlock> CODEC = simpleCodec(FormationCoreBlock::new);

	@Override
	public MapCodec<FormationCoreBlock> codec() {
		return CODEC;
	}

	public FormationCoreBlock(Properties properties) {
		super(properties);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FormationCoreBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof FormationCoreBlockEntity core) {
			ItemStack handStack = player.getItemInHand(InteractionHand.MAIN_HAND);

			if (isRune(handStack)) {
				if (core.getRune().isEmpty()) {
					core.setRune(handStack.split(1));
					player.displayClientMessage(Component.translatable("message.immortality.core.rune_inserted", core.getRune().getHoverName()), false);
					return InteractionResult.SUCCESS;
				}
			}

			if (handStack.isEmpty() && !core.getRune().isEmpty()) {
				player.setItemInHand(InteractionHand.MAIN_HAND, core.getRune().copy());
				core.setRune(ItemStack.EMPTY);
				player.displayClientMessage(Component.translatable("message.immortality.core.rune_removed"), false);
				return InteractionResult.SUCCESS;
			}

			core.displayStatus(player);
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	private boolean isRune(ItemStack stack) {
		if (stack.isEmpty()) return false;
		return stack.is(immortality.Immortality.SPIRIT_CONVERGENCE_RUNE) ||
			stack.is(immortality.Immortality.TAIJI_SHIELD_RUNE) ||
			stack.is(immortality.Immortality.MIRAGE_CONCEALMENT_RUNE) ||
			stack.is(immortality.Immortality.SWORD_FOREST_RUNE);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, immortality.Immortality.FORMATION_CORE_ENTITY, FormationCoreBlockEntity::tick);
	}
}
