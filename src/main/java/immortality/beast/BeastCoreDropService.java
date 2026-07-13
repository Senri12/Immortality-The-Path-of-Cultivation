package immortality.beast;

import immortality.Immortality;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class BeastCoreDropService {
	private static final Map<EntityType<?>, DropRule> DROP_RULES = new HashMap<>();

	static {
		register(EntityType.ENDERMAN, Immortality.SPIRIT_BEAST_CORE, 0.60F);
		register(EntityType.PHANTOM, Immortality.SPIRIT_BEAST_CORE, 0.35F);
		register(EntityType.WITCH, Immortality.SPIRIT_BEAST_CORE, 0.25F);

		register(EntityType.BLAZE, Immortality.FLAME_BEAST_CORE, 0.65F);
		register(EntityType.MAGMA_CUBE, Immortality.FLAME_BEAST_CORE, 0.35F);
		register(EntityType.GHAST, Immortality.FLAME_BEAST_CORE, 0.50F);

		register(EntityType.RAVAGER, Immortality.EARTH_BEAST_CORE, 1.00F);
		register(EntityType.IRON_GOLEM, Immortality.EARTH_BEAST_CORE, 0.45F);
		register(EntityType.HOGLIN, Immortality.EARTH_BEAST_CORE, 0.30F);
	}

	private BeastCoreDropService() {
	}

	public static void handleDeath(LivingEntity entity, DamageSource source) {
		if (entity.level().isClientSide()) {
			return;
		}

		DropRule rule = DROP_RULES.get(entity.getType());
		if (rule == null) {
			return;
		}

		if (!(source.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		if (entity.getRandom().nextFloat() >= rule.chance()) {
			return;
		}

		ItemStack stack = new ItemStack(rule.item());
		ItemEntity dropped = entity.drop(stack, false, true);
		if (dropped != null) {
			player.sendSystemMessage(Component.translatable("message.immortality.core_obtained", stack.getHoverName()));
		}
	}

	private static void register(EntityType<?> entityType, Item item, float chance) {
		DROP_RULES.put(entityType, new DropRule(item, chance));
	}

	private record DropRule(Item item, float chance) {
	}
}
