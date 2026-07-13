package immortality.cultivation;

public record BreakthroughPreview(
	boolean peakReached,
	boolean cooldownActive,
	String requiredResearchId,
	int requiredQi,
	boolean researchMet,
	boolean qiMet,
	double chance,
	double baseChance,
	double purityBonus,
	double stabilityBonus,
	double deviationPenalty,
	double bodyBonus,
	double manualBonus,
	double techniqueBonus,
	double coreBonus,
	boolean coreRequired,
	boolean coreMet,
	String coreItemId,
	double coreBreakthroughBonus,
	double coreDeviationPenalty
) {
	public boolean canAttempt() {
		return !peakReached && !cooldownActive && researchMet && qiMet && coreMet;
	}
}
