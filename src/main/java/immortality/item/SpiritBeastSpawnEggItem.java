package immortality.item;

import immortality.Immortality;
import immortality.entity.SpiritBeastEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SpiritBeastSpawnEggItem extends Item {

	public SpiritBeastSpawnEggItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}

		ItemStack stack = context.getItemInHand();
		BlockPos clickedPos = context.getClickedPos();
		Direction face = context.getClickedFace();
		BlockState state = level.getBlockState(clickedPos);

		BlockPos spawnPos;
		if (state.getCollisionShape(level, clickedPos).isEmpty()) {
			spawnPos = clickedPos;
		} else {
			spawnPos = clickedPos.relative(face);
		}

		SpiritBeastEntity beast = Immortality.SPIRIT_BEAST.create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
		if (beast != null) {
			beast.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
			serverLevel.addFreshEntity(beast);
			serverLevel.playSound(null, spawnPos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.7F, 1.2F);

			Player player = context.getPlayer();
			if (player != null && !player.hasInfiniteMaterials()) {
				stack.shrink(1);
			}
			return InteractionResult.CONSUME;
		}

		return InteractionResult.PASS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel serverLevel) {
			BlockPos pos = player.blockPosition().relative(player.getDirection(), 2);
			SpiritBeastEntity beast = Immortality.SPIRIT_BEAST.create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
			if (beast != null) {
				beast.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
				serverLevel.addFreshEntity(beast);
				if (!player.hasInfiniteMaterials()) {
					stack.shrink(1);
				}
				return InteractionResult.CONSUME;
			}
		}
		return InteractionResult.SUCCESS;
	}
}
