package immortality.block;

import immortality.dimension.ImmortalPortalManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ImmortalPortalBlock extends Block {
	public ImmortalPortalBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, net.minecraft.world.entity.InsideBlockEffectApplier insideBlockEffectApplier, boolean canEnter) {
		if (!(world instanceof ServerLevel) || !(entity instanceof ServerPlayer player) || player.isOnPortalCooldown()) {
			return;
		}
		ImmortalPortalManager.teleport(player);
	}

	@Override
	public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
		super.stepOn(world, pos, state, entity);
		if (!(world instanceof ServerLevel) || !(entity instanceof ServerPlayer player) || player.isOnPortalCooldown()) {
			return;
		}
		ImmortalPortalManager.teleport(player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (world.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer serverPlayer && !serverPlayer.isOnPortalCooldown()) {
			ImmortalPortalManager.teleport(serverPlayer);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
		if (random.nextInt(6) != 0) {
			return;
		}
		world.addParticle(
			net.minecraft.core.particles.ParticleTypes.PORTAL,
			pos.getX() + 0.2D + random.nextDouble() * 0.6D,
			pos.getY() + 0.15D + random.nextDouble() * 0.7D,
			pos.getZ() + 0.2D + random.nextDouble() * 0.6D,
			(random.nextDouble() - 0.5D) * 0.15D,
			(random.nextDouble() - 0.5D) * 0.08D,
			(random.nextDouble() - 0.5D) * 0.15D
		);
		if (random.nextInt(40) == 0) {
			world.playLocalSound(
				pos.getX() + 0.5D,
				pos.getY() + 0.5D,
				pos.getZ() + 0.5D,
				net.minecraft.sounds.SoundEvents.PORTAL_AMBIENT,
				net.minecraft.sounds.SoundSource.BLOCKS,
				0.4F,
				0.8F + random.nextFloat() * 0.4F,
				false
			);
		}
	}
}
