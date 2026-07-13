package immortality.cultivation;

import immortality.beast.BeastCoreDefinition;
import immortality.technique.TechniqueService;

public final class BreakthroughCalculator {
	private BreakthroughCalculator() {
	}

	public static BreakthroughCalculation calculate(CultivationData data, BeastCoreDefinition core) {
		double baseChance = 0.45D;
		double purityBonus = data.purity() * 0.25D;
		double stabilityBonus = data.stability() * 0.20D + data.body().stabilityBonus() * 0.25D;
		double deviationPenalty = Math.max(0.0D, data.deviation() * 0.30D + data.manual().deviationModifier() + TechniqueService.deviationModifier(data));
		double bodyBonus = data.body().breakthroughBonus();
		double manualBonus = data.manual().breakthroughBonus();
		double techniqueBonus = TechniqueService.breakthroughBonus(data);
		double coreBonus = core != null ? core.breakthroughBonus() : 0.0D;
		double finalChance = clamp(baseChance + purityBonus + stabilityBonus + bodyBonus + manualBonus + techniqueBonus + coreBonus - deviationPenalty);
		return new BreakthroughCalculation(baseChance, purityBonus, stabilityBonus, deviationPenalty, bodyBonus, manualBonus, techniqueBonus, coreBonus, finalChance);
	}

	private static double clamp(double value) {
		return Math.max(0.10D, Math.min(0.95D, value));
	}
}
