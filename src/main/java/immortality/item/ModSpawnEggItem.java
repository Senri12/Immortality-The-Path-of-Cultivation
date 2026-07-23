package immortality.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class ModSpawnEggItem extends Item {
	private final Supplier<EntityType<? extends Mob>> typeSupplier;

	public ModSpawnEggItem(Supplier<EntityType<? extends Mob>> typeSupplier, Properties properties) {
		super(properties);
		this.typeSupplier = typeSupplier;
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

		BlockPos spawnPos = state.getCollisionShape(level, clickedPos).isEmpty() ? clickedPos : clickedPos.relative(face);

		Mob mob = typeSupplier.get().create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
		if (mob != null) {
			mob.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
			serverLevel.addFreshEntity(mob);
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
			Mob mob = typeSupplier.get().create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
			if (mob != null) {
				mob.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
				serverLevel.addFreshEntity(mob);
				if (!player.hasInfiniteMaterials()) {
					stack.shrink(1);
				}
				return InteractionResult.CONSUME;
			}
		}
		return InteractionResult.SUCCESS;
	}
}
