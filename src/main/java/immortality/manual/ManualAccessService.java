package immortality.manual;

import immortality.cultivation.CultivationData;
import immortality.cultivation.ResearchDefinition;

public final class ManualAccessService {
	private ManualAccessService() {
	}

	public static boolean hasManual(CultivationData data) {
		return !ManualRegistry.NONE_ID.equals(data.manualId());
	}

	public static boolean canUseManualForStage(CultivationData data) {
		return data.stage().tier() <= data.manual().maxStage().tier();
	}

	public static boolean allowsResearch(CultivationData data, ResearchDefinition definition) {
		return hasManual(data) && data.manual().allowsResearch(definition.id());
	}
}
