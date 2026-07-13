package immortality.cultivation;

import net.minecraft.network.chat.Component;

public record BodyDefinition(
	String id,
	double breakthroughBonus,
	double stabilityBonus
) {
	public String translationKey() {
		return "body.immortality." + this.id;
	}

	public Component displayNameComponent() {
		return Component.translatable(translationKey());
	}
}
