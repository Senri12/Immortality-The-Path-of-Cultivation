package immortality.technique;

import immortality.cultivation.CultivationStage;
import net.minecraft.network.chat.Component;

public record TechniqueDefinition(
	String id,
	CultivationStage requiredStage,
	double qiGainBonus,
	double purityBonus,
	double stabilityBonus,
	double breakthroughBonus,
	double deviationModifier,
	int activationQiCost,
	int activationCooldownTicks,
	int activationDurationTicks,
	int activationAmplifier,
	String activationEffect
) {
	public String titleKey() {
		return "technique.immortality." + this.id;
	}

	public String descriptionKey() {
		return "technique.immortality." + this.id + ".description";
	}

	public Component titleComponent() {
		return Component.translatable(titleKey());
	}

	public Component descriptionComponent() {
		return Component.translatable(descriptionKey());
	}
}
