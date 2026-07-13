package immortality.manual;

import immortality.cultivation.CultivationStage;
import java.util.List;
import net.minecraft.network.chat.Component;

public record ManualDefinition(
	String id,
	String itemId,
	CultivationStage maxStage,
	List<String> allowedResearchIds,
	List<String> grantedTechniques,
	List<String> insightPool,
	double breakthroughBonus,
	double deviationModifier
) {
	public boolean allowsResearch(String researchId) {
		return this.allowedResearchIds.contains("*") || this.allowedResearchIds.contains(researchId);
	}

	public String titleKey() {
		return "manual.immortality." + this.id;
	}

	public Component titleComponent() {
		return Component.translatable(titleKey());
	}
}
