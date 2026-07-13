package immortality.item;

import immortality.Immortality;
import immortality.block.FormationFlagBlock;
import immortality.block.FormationCoreBlock;
import immortality.block.entity.FormationCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;

public class FormationCompassItem extends Item {
	public FormationCompassItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();

		if (player == null) {
			return InteractionResult.PASS;
		}

		if (player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}

		BlockState state = level.getBlockState(pos);

		// 1. Click on flag block
		if (state.getBlock() instanceof FormationFlagBlock) {
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}

			FormationCompassComponent comp = stack.get(Immortality.FORMATION_FLAGS);
			List<BlockPos> flags = comp != null ? new ArrayList<>(comp.flags()) : new ArrayList<>();

			if (flags.contains(pos)) {
				player.displayClientMessage(Component.translatable("message.immortality.compass.already_registered", pos.getX(), pos.getY(), pos.getZ()), false);
				return InteractionResult.SUCCESS;
			}

			if (flags.size() >= 8) {
				player.displayClientMessage(Component.translatable("message.immortality.compass.max_flags"), false);
				return InteractionResult.SUCCESS;
			}

			flags.add(pos);
			stack.set(Immortality.FORMATION_FLAGS, new FormationCompassComponent(flags));
			level.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
			player.displayClientMessage(Component.translatable("message.immortality.compass.registered", pos.getX(), pos.getY(), pos.getZ(), flags.size()), false);
			return InteractionResult.SUCCESS;
		}

		// 2. Click on core block
		if (state.getBlock() instanceof FormationCoreBlock) {
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}

			net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof FormationCoreBlockEntity core) {
				FormationCompassComponent comp = stack.get(Immortality.FORMATION_FLAGS);
				List<BlockPos> flags = comp != null ? comp.flags() : new ArrayList<>();

				if (flags.isEmpty()) {
					player.displayClientMessage(Component.translatable("message.immortality.compass.no_flags"), false);
					return InteractionResult.SUCCESS;
				}

				if (flags.size() < 3) {
					player.displayClientMessage(Component.translatable("message.immortality.compass.too_few_flags"), false);
					return InteractionResult.SUCCESS;
				}

				boolean bound = core.bindFlags(flags, player);
				if (bound) {
					stack.set(Immortality.FORMATION_FLAGS, null);
					level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
					player.displayClientMessage(Component.translatable("message.immortality.compass.bound_success"), false);
				}
				return InteractionResult.SUCCESS;
			}
		}

		return InteractionResult.PASS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isSecondaryUseActive()) {
			if (!level.isClientSide()) {
				stack.set(Immortality.FORMATION_FLAGS, null);
				level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 1.0F, 0.8F);
				player.displayClientMessage(Component.translatable("message.immortality.compass.cleared"), false);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
}
