package immortality.beast;

import java.util.List;
import net.minecraft.network.chat.Component;

public record BeastCoreDefinition(
	String id,
	String itemId,
	double breakthroughBonus,
	double stabilityBonus,
	double deviationPenalty,
	double purityBonus,
	List<String> compatibleBodies
) {
	public boolean supportsBody(String bodyId) {
		return this.compatibleBodies.isEmpty() || this.compatibleBodies.contains(bodyId);
	}

	public String translationKey() {
		return "beast_core.immortality." + this.id;
	}

	public Component displayNameComponent() {
		return Component.translatable(translationKey());
	}
}
