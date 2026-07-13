package immortality.manual;

import immortality.cultivation.CultivationData;

public final class InsightUnlockService {
	private InsightUnlockService() {
	}

	public static String unlockNextInsight(CultivationData data, ManualDefinition manual) {
		for (String insightId : manual.insightPool()) {
			if (!data.knowsInsight(insightId)) {
				data.learnInsight(insightId);
				return insightId;
			}
		}
		return null;
	}
}
