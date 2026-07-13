package immortality.block;

import immortality.block.entity.FormationFlagBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.phys.shapes.VoxelShape;

public class FormationFlagBlock extends BaseEntityBlock {
	public static final MapCodec<FormationFlagBlock> CODEC = simpleCodec(FormationFlagBlock::new);

	@Override
	public MapCodec<FormationFlagBlock> codec() {
		return CODEC;
	}

	protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

	public FormationFlagBlock(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FormationFlagBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof FormationFlagBlockEntity flag) {
			ItemStack handStack = player.getItemInHand(InteractionHand.MAIN_HAND);
			
			if (handStack.isEmpty()) {
				if (player instanceof ServerPlayer serverPlayer) {
					immortality.cultivation.CultivationData data = immortality.cultivation.CultivationManager.get(serverPlayer);
					int maxCharge = flag.getMaxQi() - flag.getCurrentQi();
					
					if (maxCharge <= 0) {
						player.displayClientMessage(Component.translatable("message.immortality.flag.fully_charged", flag.getCurrentQi()), false);
						return InteractionResult.SUCCESS;
					}

					int toTransfer = Math.min(50, Math.min(data.currentQi(), maxCharge));
					if (toTransfer > 0) {
						data.addQi(-toTransfer);
						flag.addQi(toTransfer);
						immortality.cultivation.CultivationManager.sync(serverPlayer);
						
						level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.3F);
						player.displayClientMessage(Component.translatable("message.immortality.flag.charged", flag.getCurrentQi(), flag.getMaxQi()), false);
					} else {
						player.displayClientMessage(Component.translatable("message.immortality.flag.no_qi", flag.getCurrentQi(), flag.getMaxQi()), false);
					}
				}
				return InteractionResult.SUCCESS;
			} else {
				player.displayClientMessage(Component.translatable("message.immortality.flag.status", flag.getCurrentQi(), flag.getMaxQi()), false);
				return InteractionResult.SUCCESS;
			}
		}

		return InteractionResult.PASS;
	}
}
