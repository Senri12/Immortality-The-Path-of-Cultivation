package immortality.cultivation;

import immortality.beast.BeastCoreDefinition;
import immortality.beast.BeastCoreRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class BreakthroughService {
	private BreakthroughService() {
	}

	public static BreakthroughPreview preview(ServerPlayer player, CultivationData data) {
		String requiredResearchId = requiredResearchId(data.stage());
		int requiredQi = requiredQi(data.stage());
		BreakthroughCoreSelection core = selectCore(player, data);
		BreakthroughCalculation calculation = BreakthroughCalculator.calculate(data, core.definition());
		return new BreakthroughPreview(
				data.stage() == CultivationStage.VOID_TRIBULANT,
			data.breakthroughCooldown() > 0,
			requiredResearchId,
			requiredQi,
			requiredResearchId == null || data.knows(requiredResearchId),
			data.currentQi() >= requiredQi,
			calculation.finalChance(),
			calculation.baseChance(),
			calculation.purityBonus(),
			calculation.stabilityBonus(),
			calculation.deviationPenalty(),
			calculation.bodyBonus(),
			calculation.manualBonus(),
			calculation.techniqueBonus(),
			calculation.coreBonus(),
			data.stage() == CultivationStage.FOUNDATION_ESTABLISHMENT,
			data.stage() != CultivationStage.FOUNDATION_ESTABLISHMENT || core.present(),
			core.definition() != null ? core.definition().itemId() : "",
			core.definition() != null ? core.definition().breakthroughBonus() : 0.0D,
			core.definition() != null ? core.definition().deviationPenalty() : 0.0D
		);
	}

	public static BreakthroughResult attempt(ServerPlayer player, CultivationData data) {
		BreakthroughCoreSelection core = selectCore(player, data);
		BreakthroughPreview preview = preview(player, data);
		if (preview.cooldownActive()) {
			return new BreakthroughResult(BreakthroughOutcome.FAILURE, 0.0D, Component.translatable("message.immortality.breakthrough.cooldown"));
		}
		if (preview.peakReached()) {
			return new BreakthroughResult(BreakthroughOutcome.FAILURE, 1.0D, Component.translatable("message.immortality.breakthrough.peak"));
		}
		if (data.stage().next().tier() > data.manual().maxStage().tier()) {
			return new BreakthroughResult(BreakthroughOutcome.FAILURE, 0.0D, Component.translatable("message.immortality.breakthrough.manual_limit", data.manual().titleComponent()));
		}

		String missingResearch = preview.researchMet() ? null : preview.requiredResearchId();
		if (missingResearch != null) {
			ResearchDefinition missingDefinition = ResearchRegistry.get(missingResearch);
			Component missingName = missingDefinition != null ? missingDefinition.titleComponent() : Component.literal(missingResearch);
			return new BreakthroughResult(BreakthroughOutcome.FAILURE, 0.0D, Component.translatable("message.immortality.breakthrough.missing_research", missingName));
		}

		int requiredQi = preview.requiredQi();
		if (data.currentQi() < requiredQi) {
			return new BreakthroughResult(BreakthroughOutcome.FAILURE, 0.0D, Component.translatable("message.immortality.breakthrough.not_enough_qi", requiredQi));
		}
		if (preview.coreRequired() && !preview.coreMet()) {
			return new BreakthroughResult(BreakthroughOutcome.FAILURE, 0.0D, Component.translatable("message.immortality.breakthrough.missing_core"));
		}

		double chance = preview.chance();
		double roll = player.getRandom().nextDouble();
		data.setCurrentQi(data.currentQi() - requiredQi);
		consumeCore(player, core);

		if (roll <= chance) {
			boolean flawed = roll > chance - 0.16D;
			data.setStage(data.stage().next());
			data.addStability(flawed ? -0.12D : 0.08D);
			data.addDeviation(flawed ? 0.10D : -0.08D);
			data.addPurity(0.03D);
			applyCoreEffects(data, core.definition(), false);
			return new BreakthroughResult(
				flawed ? BreakthroughOutcome.FLAWED_SUCCESS : BreakthroughOutcome.SUCCESS,
				chance,
				Component.translatable(flawed ? "message.immortality.breakthrough.success_flawed" : "message.immortality.breakthrough.success")
			);
		}

		boolean severe = roll > chance + 0.18D;
		data.addStability(severe ? -0.20D : -0.08D);
		data.addDeviation(severe ? 0.25D : 0.10D);
		applyCoreEffects(data, core.definition(), true);
		data.setBreakthroughCooldown(severe ? 2400 : 1200);
		return new BreakthroughResult(
			severe ? BreakthroughOutcome.DEVIATION : BreakthroughOutcome.FAILURE,
			chance,
			Component.translatable(severe ? "message.immortality.breakthrough.deviation" : "message.immortality.breakthrough.failure")
		);
	}

	public static int requiredQi(CultivationStage stage) {
		return switch (stage) {
			case MORTAL -> 20;
			case QI_GATHERING -> 60;
			case FOUNDATION_ESTABLISHMENT -> 100;
			case CORE_FORMATION -> 140;
			case NASCENT_SOUL -> 190;
			case SPIRIT_SEVERING -> 250;
			case ASCENDANT -> 320;
			case ILLUSORY_YIN -> 400;
			case CORPOREAL_YANG -> 520;
			case NIRVANA_SCRYER -> 680;
			case NIRVANA_CLEANSER -> 900;
			case VOID_TRIBULANT -> 0;
		};
	}

	public static String requiredResearchId(CultivationStage stage) {
		return switch (stage) {
			case MORTAL -> "qi_sense";
			case QI_GATHERING -> "foundation_blueprint";
			case FOUNDATION_ESTABLISHMENT -> "golden_core_method";
			case CORE_FORMATION -> "nascent_soul_seed";
			case NASCENT_SOUL -> "dao_heart_trial";
			case SPIRIT_SEVERING -> "spirit_severing_art";
			case ASCENDANT -> "yin_comprehension";
			case ILLUSORY_YIN -> "yang_manifestation";
			case CORPOREAL_YANG -> "nirvana_karma_thread";
			case NIRVANA_SCRYER -> "nirvana_cycle_method";
			case NIRVANA_CLEANSER -> "void_tribulation_mark";
			case VOID_TRIBULANT -> null;
		};
	}

	public static double calculateChance(CultivationData data, BeastCoreDefinition core) {
		return BreakthroughCalculator.calculate(data, core).finalChance();
	}

	public static BreakthroughCoreSelection selectCore(ServerPlayer player, CultivationData data) {
		boolean required = data.stage() == CultivationStage.FOUNDATION_ESTABLISHMENT;
		BreakthroughCoreSelection best = new BreakthroughCoreSelection(null, ItemStack.EMPTY, -1, required);
		double bestScore = Double.NEGATIVE_INFINITY;

		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
			if (itemId == null) {
				continue;
			}
			BeastCoreDefinition definition = BeastCoreRegistry.byItemId(itemId.toString());
			if (definition == null) {
				continue;
			}
			double score = definition.breakthroughBonus() + definition.stabilityBonus() - definition.deviationPenalty();
			if (definition.supportsBody(data.bodyId())) {
				score += 0.12D;
			} else {
				score -= 0.18D;
			}
			if (score > bestScore) {
				bestScore = score;
				best = new BreakthroughCoreSelection(definition, stack, slot, required);
			}
		}

		return best;
	}

	private static void consumeCore(ServerPlayer player, BreakthroughCoreSelection selection) {
		if (!selection.present()) {
			return;
		}
		player.getInventory().getItem(selection.slot()).shrink(1);
	}

	private static void applyCoreEffects(CultivationData data, BeastCoreDefinition core, boolean failure) {
		if (core == null) {
			return;
		}
		data.addStability(core.stabilityBonus());
		data.addPurity(core.purityBonus());
		double penalty = core.deviationPenalty();
		if (!core.supportsBody(data.bodyId())) {
			penalty += 0.14D;
		}
		data.addDeviation(failure ? penalty : penalty * 0.4D);
	}
}
