package immortality.technique;

import immortality.cultivation.CultivationData;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class TechniqueService {
	private TechniqueService() {
	}

	public static double qiGainBonus(CultivationData data) {
		return activeValue(data, TechniqueDefinition::qiGainBonus);
	}

	public static double purityBonus(CultivationData data) {
		return activeValue(data, TechniqueDefinition::purityBonus);
	}

	public static double stabilityBonus(CultivationData data) {
		return activeValue(data, TechniqueDefinition::stabilityBonus);
	}

	public static double breakthroughBonus(CultivationData data) {
		return activeValue(data, TechniqueDefinition::breakthroughBonus);
	}

	public static double deviationModifier(CultivationData data) {
		return activeValue(data, TechniqueDefinition::deviationModifier);
	}

	public static TechniqueDefinition activeTechnique(CultivationData data) {
		TechniqueDefinition definition = TechniqueRegistry.get(data.activeTechniqueId());
		if (!data.knowsTechnique(definition.id())) {
			return TechniqueRegistry.get(TechniqueRegistry.NONE_ID);
		}
		if (data.stage().tier() < definition.requiredStage().tier()) {
			return TechniqueRegistry.get(TechniqueRegistry.NONE_ID);
		}
		return definition;
	}

	public static boolean canInvoke(CultivationData data) {
		TechniqueDefinition definition = activeTechnique(data);
		return !TechniqueRegistry.NONE_ID.equals(definition.id())
			&& definition.activationQiCost() > 0
			&& data.currentQi() >= definition.activationQiCost()
			&& data.techniqueCooldown() <= 0;
	}

	public static boolean invoke(ServerPlayer player, CultivationData data) {
		TechniqueDefinition definition = activeTechnique(data);
		if (TechniqueRegistry.NONE_ID.equals(definition.id()) || definition.activationQiCost() <= 0 || data.techniqueCooldown() > 0) {
			return false;
		}
		if (data.currentQi() < definition.activationQiCost()) {
			return false;
		}
		data.addQi(-definition.activationQiCost());
		data.setTechniqueCooldown(definition.activationCooldownTicks());
		applyActivation(player, data, definition);
		return true;
	}

	private static void applyActivation(ServerPlayer player, CultivationData data, TechniqueDefinition definition) {
		switch (definition.activationEffect()) {
			case "speed" -> player.addEffect(effect(MobEffects.SPEED, definition));
			case "strength" -> player.addEffect(effect(MobEffects.STRENGTH, definition));
			case "resistance" -> player.addEffect(effect(MobEffects.RESISTANCE, definition));
			case "night_vision" -> player.addEffect(effect(MobEffects.NIGHT_VISION, definition));
			case "regeneration" -> player.addEffect(effect(MobEffects.REGENERATION, definition));
			case "glowing" -> {
				player.addEffect(effect(MobEffects.GLOWING, definition));
				data.addPurity(0.02D);
				data.addDeviation(-0.03D);
			}
			default -> {
			}
		}
	}

	private static MobEffectInstance effect(Holder<net.minecraft.world.effect.MobEffect> effect, TechniqueDefinition definition) {
		return new MobEffectInstance(effect, definition.activationDurationTicks(), definition.activationAmplifier(), false, false, true);
	}

	private static double activeValue(CultivationData data, TechniqueValue value) {
		TechniqueDefinition definition = activeTechnique(data);
		return TechniqueRegistry.NONE_ID.equals(definition.id()) ? 0.0D : value.apply(definition);
	}

	@FunctionalInterface
	private interface TechniqueValue {
		double apply(TechniqueDefinition definition);
	}
}
