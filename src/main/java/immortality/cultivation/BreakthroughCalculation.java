package immortality.cultivation;

public record BreakthroughCalculation(
	double baseChance,
	double purityBonus,
	double stabilityBonus,
	double deviationPenalty,
	double bodyBonus,
	double manualBonus,
	double techniqueBonus,
	double coreBonus,
	double finalChance
) {
}
