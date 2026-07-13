package immortality.block;

import immortality.block.entity.JadeInfusionAltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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

public class JadeInfusionAltarBlock extends BaseEntityBlock {
	public static final MapCodec<JadeInfusionAltarBlock> CODEC = simpleCodec(JadeInfusionAltarBlock::new);

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	public JadeInfusionAltarBlock(Properties properties) {
		super(properties);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new JadeInfusionAltarBlockEntity(pos, state);
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
		if (be instanceof JadeInfusionAltarBlockEntity altar) {
			ItemStack held = player.getMainHandItem();
			ItemStack current = altar.getItem();
			
			if (altar.isCrafting()) {
				return InteractionResult.CONSUME;
			}

			if (current.isEmpty() && !held.isEmpty()) {
				altar.setItem(held.split(1));
				return InteractionResult.SUCCESS;
			} else if (!current.isEmpty()) {
				if (held.isEmpty() && player.isShiftKeyDown()) {
					if (player instanceof ServerPlayer serverPlayer) {
						altar.tryStartRitual(serverPlayer);
					}
					return InteractionResult.SUCCESS;
				} else {
					player.getInventory().placeItemBackInInventory(current);
					altar.setItem(ItemStack.EMPTY);
					return InteractionResult.SUCCESS;
				}
			}
		}
		return InteractionResult.PASS;
	}


	@Nullable
	@Override
	public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
		return createTickerHelper(type, immortality.Immortality.JADE_INFUSION_ALTAR_ENTITY, JadeInfusionAltarBlockEntity::tick);
	}
}
