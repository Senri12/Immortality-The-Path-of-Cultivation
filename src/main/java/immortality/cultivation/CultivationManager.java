package immortality.cultivation;

import immortality.Immortality;
import immortality.beast.BeastCoreDefinition;
import immortality.manual.InsightUnlockService;
import immortality.manual.ManualAccessService;
import immortality.manual.ManualDefinition;
import immortality.manual.ManualRegistry;
import immortality.network.BreakthroughScreenPayload;
import immortality.network.AltarResearchScreenPayload;
import immortality.network.CultivationSyncPayload;
import immortality.network.ManualActionPayload;
import immortality.network.ManualScreenPayload;
import immortality.network.QiFocusScreenPayload;
import immortality.network.QiFocusActionPayload;
import immortality.network.ResearchActionPayload;
import immortality.network.ResearchLinkPayload;
import immortality.network.ResearchStudyScreenPayload;
import immortality.network.TechniqueActionPayload;
import immortality.network.TechniqueScreenPayload;
import immortality.technique.TechniqueDefinition;
import immortality.technique.TechniqueService;
import immortality.technique.TechniqueRegistry;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import immortality.item.SpiritualBlueprintComponent;

public final class CultivationManager {
	private CultivationManager() {
	}

	public static CultivationData get(ServerPlayer player) {
		return ((CultivatorAccess) player).immortality$getCultivationData();
	}

	public static void tickPlayer(ServerPlayer player) {
		CultivationData data = get(player);
		applyQiFocusBenefits(player, data);
		applySpiritualModifiers(player);
		if (data.breakthroughCooldown() > 0) {
			data.setBreakthroughCooldown(data.breakthroughCooldown() - 1);
		}
		if (data.techniqueCooldown() > 0) {
			data.setTechniqueCooldown(data.techniqueCooldown() - 1);
			if (data.techniqueCooldown() == 0 || data.techniqueCooldown() % 20 == 0) {
				sync(player);
			}
		}

		boolean meditating = isMeditating(player);
		if (meditating) {
			data.setMeditationTicks(data.meditationTicks() + 1);
			if (data.meditationTicks() % 40 == 0) {
				int gain = data.stage() == CultivationStage.MORTAL ? 2 : 4 + data.stage().tier();
				gain += (int) Math.round(TechniqueService.qiGainBonus(data));
				int actualGain = data.addQi(gain);
				data.addPurity(0.005D + TechniqueService.purityBonus(data));
				data.addStability(0.004D + data.body().stabilityBonus() * 0.1D + TechniqueService.stabilityBonus(data));
				if (actualGain > 0) {
					player.displayClientMessage(Component.translatable("message.immortality.qi_gain", actualGain), true);
				}
				sync(player);
			}
		} else if (data.meditationTicks() != 0) {
			data.setMeditationTicks(0);
			sync(player);
		}
	}

	public static void completeNextResearch(ServerPlayer player) {
		CultivationData data = get(player);
		ResearchDefinition next = ResearchRegistry.all().stream()
			.filter(definition -> canResearch(player, data, definition, false))
			.min(Comparator.comparing(ResearchDefinition::qiCost))
			.orElse(null);
		if (next == null) {
			player.sendSystemMessage(Component.translatable("message.immortality.research_none_available"));
			return;
		}
		completeResearch(player, next);
	}

	public static void useManual(ServerPlayer player, String manualId) {
		useManual(player, manualId, true);
	}

	public static void openManualScreen(ServerPlayer player, String manualId) {
		ServerPlayNetworking.send(player, new ManualScreenPayload(buildManualScreenData(player, manualId)));
	}

	public static void openTechniqueScreen(ServerPlayer player) {
		ServerPlayNetworking.send(player, new TechniqueScreenPayload(buildTechniqueScreenData(player)));
	}

	public static void openEffectsScreen(ServerPlayer player) {
		ServerPlayNetworking.send(player, new QiFocusScreenPayload(buildEffectsScreenData(player)));
	}

	public static void handleManualAction(ServerPlayer player, ManualActionPayload payload) {
		if (ManualActionPayload.ACTION_COMPREHEND.equals(payload.action())) {
			useManual(player, payload.manualId(), true);
			openManualScreen(player, payload.manualId());
		}
	}

	public static void handleTechniqueAction(ServerPlayer player, TechniqueActionPayload payload) {
		CultivationData data = get(player);
		switch (payload.action()) {
			case TechniqueActionPayload.ACTION_CYCLE_NEXT -> cycleTechnique(player, data);
			case TechniqueActionPayload.ACTION_CLEAR -> {
				data.setActiveTechnique(TechniqueRegistry.NONE_ID);
				player.sendSystemMessage(Component.translatable("message.immortality.technique.cleared"));
				sync(player);
			}
			case TechniqueActionPayload.ACTION_SET -> {
				if (!data.knowsTechnique(payload.techniqueId())) {
					player.sendSystemMessage(Component.translatable("message.immortality.technique.unknown", payload.techniqueId()));
					return;
				}
				data.setActiveTechnique(payload.techniqueId());
				TechniqueDefinition active = TechniqueService.activeTechnique(data);
				player.sendSystemMessage(Component.translatable("message.immortality.technique.active", active.titleComponent()));
				sync(player);
			}
			case TechniqueActionPayload.ACTION_INVOKE -> invokeTechnique(player, data);
		}
	}

	public static void handleQiFocusAction(ServerPlayer player, QiFocusActionPayload payload) {
		CultivationData data = get(player);
		QiFocus focus = QiFocus.byId(payload.focusId());
		data.toggleQiFocus(focus);
		player.sendSystemMessage(Component.translatable("message.immortality.focus.changed", focusedPartsText(data)));
		sync(player);
		openEffectsScreen(player);
	}

	private static void useManual(ServerPlayer player, String manualId, boolean notify) {
		CultivationData data = get(player);
		ManualDefinition manual = ManualRegistry.get(manualId);
		data.setManual(manual.id());
		String unlockedInsight = InsightUnlockService.unlockNextInsight(data, manual);
		for (String techniqueId : manual.grantedTechniques()) {
			if (!data.knowsTechnique(techniqueId)) {
				data.learnTechnique(techniqueId);
				if (TechniqueRegistry.NONE_ID.equals(data.activeTechniqueId())) {
					data.setActiveTechnique(techniqueId);
				}
				if (notify) {
					player.sendSystemMessage(Component.translatable("message.immortality.technique.learned", Component.translatable("technique.immortality." + techniqueId)));
				}
			}
		}
		if (notify) {
			player.sendSystemMessage(Component.translatable("message.immortality.manual.equipped", manual.titleComponent()));
			if (unlockedInsight != null) {
				player.sendSystemMessage(Component.translatable("message.immortality.insight.unlocked", Component.translatable("insight.immortality." + unlockedInsight)));
			} else {
				player.sendSystemMessage(Component.translatable("message.immortality.manual.no_new_insights"));
			}
		}
		sync(player);
	}

	public static void openResearchScreen(ServerPlayer player) {
		ServerPlayNetworking.send(player, new AltarResearchScreenPayload(buildResearchScreenData(player)));
	}

	public static void openStudyScreen(ServerPlayer player) {
		ServerPlayNetworking.send(player, new ResearchStudyScreenPayload(buildStudyScreenData(player)));
	}

	public static void openBreakthroughScreen(ServerPlayer player) {
		ServerPlayNetworking.send(player, new BreakthroughScreenPayload(buildBreakthroughScreenData(player)));
	}

	public static void handleResearchAction(ServerPlayer player, ResearchActionPayload payload) {
		if (ResearchActionPayload.ACTION_PREPARE.equals(payload.action())) {
			prepareResearch(player, payload.researchId());
			openResearchScreen(player);
			return;
		}
		if (ResearchActionPayload.ACTION_STUDY.equals(payload.action())) {
			ResearchDefinition definition = ResearchRegistry.get(payload.researchId());
			if (definition == null) {
				player.sendSystemMessage(Component.translatable("message.immortality.research_unknown", payload.researchId()));
			} else {
				completeResearch(player, definition);
			}
			openResearchScreen(player);
			return;
		}
		if (ResearchActionPayload.ACTION_BREAKTHROUGH.equals(payload.action())) {
			tryBreakthrough(player);
			openBreakthroughScreen(player);
			return;
		}
		if (ResearchActionPayload.ACTION_OPEN_BREAKTHROUGH.equals(payload.action())) {
			openBreakthroughScreen(player);
			return;
		}
		if (ResearchActionPayload.ACTION_OPEN_RESEARCH.equals(payload.action())) {
			openResearchScreen(player);
			return;
		}
		if (ResearchActionPayload.ACTION_OPEN_TECHNIQUES.equals(payload.action())) {
			openTechniqueScreen(player);
			return;
		}
		if (ResearchActionPayload.ACTION_OPEN_EFFECTS.equals(payload.action())) {
			openEffectsScreen(player);
		}
	}

	public static void handleResearchLink(ServerPlayer player, ResearchLinkPayload payload) {
		ResearchDefinition definition = ResearchRegistry.get(payload.researchId());
		if (definition == null) {
			player.sendSystemMessage(Component.translatable("message.immortality.research_unknown", payload.researchId()));
			return;
		}
		CultivationData data = get(player);
		if (!definition.id().equals(data.preparedResearchId())) {
			player.sendSystemMessage(Component.translatable("message.immortality.research.not_prepared"));
			openStudyScreen(player);
			return;
		}
		if (!canResearch(player, data, definition, true)) {
			return;
		}
		if (!isValidAspectBoard(data, definition, payload.placements())) {
			player.sendSystemMessage(Component.translatable("message.immortality.research.invalid_aspect_chain"));
			openStudyScreen(player);
			return;
		}
		completeResearch(player, definition);
		openStudyScreen(player);
	}

	public static void completeResearch(ServerPlayer player, ResearchDefinition definition) {
		CultivationData data = get(player);
		if (!canResearch(player, data, definition, true)) {
			return;
		}

		data.addQi(-definition.qiCost());
		if (definition.requiredItemId() != null) {
			consumeItem(player, definition.requiredItemId());
		}
		data.learn(definition.id());
		if (definition.id().equals(data.preparedResearchId())) {
			data.setPreparedResearchId("");
		}
		if (definition.rewardType() == ResearchRewardType.BODY && definition.rewardValue() != null) {
			data.setBody(definition.rewardValue());
		}
		if (definition.rewardType() == ResearchRewardType.SKILL && definition.rewardValue() != null) {
			data.learnTechnique(definition.rewardValue());
			if (TechniqueRegistry.NONE_ID.equals(data.activeTechniqueId())) {
				data.setActiveTechnique(definition.rewardValue());
			}
			player.sendSystemMessage(Component.translatable("message.immortality.technique.learned", Component.translatable("technique.immortality." + definition.rewardValue())));
		}
		player.sendSystemMessage(Component.translatable("message.immortality.research_completed", definition.titleComponent()));
		sync(player);
	}

	public static void tryBreakthrough(ServerPlayer player) {
		CultivationData data = get(player);
		BreakthroughResult result = BreakthroughService.attempt(player, data);
		switch (result.outcome()) {
			case SUCCESS -> player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
			case FLAWED_SUCCESS -> player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 0));
			case FAILURE -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
			case DEVIATION -> {
				player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 300, 0));
				player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 1));
			}
		}
		player.sendSystemMessage(Component.translatable("message.immortality.breakthrough.result", result.message(), percent(result.chance())));
		sync(player);
	}

	public static MutableComponent buildStatusText(ServerPlayer player) {
		CultivationData data = get(player);
		return Component.translatable(
			"message.immortality.status",
			data.stage().displayNameComponent(),
			data.currentQi(),
			data.maxQi(),
			percent(data.purity()),
			percent(data.stability()),
			percent(data.deviation()),
			data.body().displayNameComponent(),
			data.manual().titleComponent(),
			TechniqueService.activeTechnique(data).titleComponent()
		);
	}

	public static void sync(ServerPlayer player) {
		ServerPlayNetworking.send(player, new CultivationSyncPayload(get(player).toNbt()));
	}

	public static void debugSetStage(ServerPlayer player, CultivationStage stage) {
		CultivationData data = get(player);
		data.setStage(stage);
		data.setCurrentQi(Math.min(data.currentQi(), data.maxQi()));
		player.sendSystemMessage(Component.translatable("message.immortality.debug.stage_set", stage.displayNameComponent()));
		sync(player);
	}

	public static void debugAdvanceStage(ServerPlayer player, int steps) {
		CultivationStage[] values = CultivationStage.values();
		CultivationData data = get(player);
		int index = Math.max(0, Math.min(values.length - 1, data.stage().ordinal() + steps));
		debugSetStage(player, values[index]);
	}

	public static void debugSetQi(ServerPlayer player, int amount) {
		CultivationData data = get(player);
		data.setCurrentQi(amount);
		player.sendSystemMessage(Component.translatable("message.immortality.debug.qi_set", data.currentQi(), data.maxQi()));
		sync(player);
	}

	public static void debugAddQi(ServerPlayer player, int amount) {
		CultivationData data = get(player);
		int applied = data.addQi(amount);
		player.sendSystemMessage(Component.translatable("message.immortality.debug.qi_added", applied, data.currentQi(), data.maxQi()));
		sync(player);
	}

	public static void debugSetBody(ServerPlayer player, String bodyId) {
		CultivationData data = get(player);
		data.setBody(bodyId);
		player.sendSystemMessage(Component.translatable("message.immortality.debug.body_set", data.body().displayNameComponent()));
		sync(player);
	}

	public static void debugGrantTechnique(ServerPlayer player, String techniqueId) {
		CultivationData data = get(player);
		data.learnTechnique(techniqueId);
		if (TechniqueRegistry.NONE_ID.equals(data.activeTechniqueId())) {
			data.setActiveTechnique(techniqueId);
		}
		player.sendSystemMessage(Component.translatable("message.immortality.debug.technique_granted", Component.translatable("technique.immortality." + techniqueId)));
		sync(player);
	}

	public static void debugGrantAllTechniques(ServerPlayer player) {
		for (TechniqueDefinition definition : TechniqueRegistry.all()) {
			if (!TechniqueRegistry.NONE_ID.equals(definition.id())) {
				debugGrantTechnique(player, definition.id());
			}
		}
	}

	public static void debugGrantInsight(ServerPlayer player, String insightId) {
		CultivationData data = get(player);
		data.learnInsight(insightId);
		player.sendSystemMessage(Component.translatable("message.immortality.debug.insight_granted", Component.translatable("insight.immortality." + insightId)));
		sync(player);
	}

	public static void debugGrantAllInsights(ServerPlayer player) {
		Set<String> insightIds = new LinkedHashSet<>();
		for (ManualDefinition definition : ManualRegistry.all()) {
			insightIds.addAll(definition.insightPool());
		}
		for (String insightId : insightIds) {
			debugGrantInsight(player, insightId);
		}
	}

	public static void debugUnlockResearch(ServerPlayer player, ResearchDefinition definition) {
		CultivationData data = get(player);
		if (!data.knows(definition.id())) {
			data.learn(definition.id());
		}
		if (definition.rewardType() == ResearchRewardType.BODY && definition.rewardValue() != null) {
			data.setBody(definition.rewardValue());
		}
		if (definition.rewardType() == ResearchRewardType.SKILL && definition.rewardValue() != null) {
			data.learnTechnique(definition.rewardValue());
			if (TechniqueRegistry.NONE_ID.equals(data.activeTechniqueId())) {
				data.setActiveTechnique(definition.rewardValue());
			}
		}
		player.sendSystemMessage(Component.translatable("message.immortality.debug.research_granted", definition.titleComponent()));
		sync(player);
	}

	public static void debugUnlockAllResearches(ServerPlayer player) {
		for (ResearchDefinition definition : ResearchRegistry.all()) {
			debugUnlockResearch(player, definition);
		}
	}

	public static void debugSetManual(ServerPlayer player, String manualId) {
		CultivationData data = get(player);
		data.setManual(manualId);
		player.sendSystemMessage(Component.translatable("message.immortality.debug.manual_set", data.manual().titleComponent()));
		sync(player);
	}

	public static void debugPrepareResearch(ServerPlayer player, String researchId) {
		get(player).setPreparedResearchId(researchId);
		player.sendSystemMessage(Component.translatable("message.immortality.debug.research_prepared", Component.translatable("research.immortality." + researchId + ".title")));
		sync(player);
	}

	public static void debugSetQiFocus(ServerPlayer player, QiFocus focus) {
		CultivationData data = get(player);
		data.toggleQiFocus(focus);
		player.sendSystemMessage(Component.translatable("message.immortality.focus.changed", focusedPartsText(data)));
		sync(player);
	}

	public static boolean isMeditating(ServerPlayer player) {
		BlockState state = player.level().getBlockState(player.blockPosition().below());
		return state.is(Immortality.MEDITATION_MAT) && player.isShiftKeyDown() && player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D;
	}

	private static void applyStageEffect(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier) {
		if (amplifier < 0) {
			player.removeEffect(effect);
			return;
		}
		MobEffectInstance current = player.getEffect(effect);
		if (current == null || current.getAmplifier() != amplifier || !current.isInfiniteDuration()) {
			player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, true, false, true));
		}
	}

	private static void applyStageFlight(ServerPlayer player, CultivationStage stage, boolean torsoFocused) {
		boolean shouldFly = stage.grantsFlight() && torsoFocused;
		boolean currentlyCanFly = player.getAbilities().mayfly;
		if (shouldFly == currentlyCanFly) {
			return;
		}
		if (shouldFly) {
			player.getAbilities().mayfly = true;
			player.onUpdateAbilities();
			return;
		}
		if (!player.isCreative() && !player.isSpectator()) {
			player.getAbilities().mayfly = false;
			player.getAbilities().flying = false;
			player.onUpdateAbilities();
		}
	}

	private static void applyQiFocusBenefits(ServerPlayer player, CultivationData data) {
		int tier = data.stage().tier();
		applyStageEffect(player, MobEffects.STRENGTH, data.hasQiFocus(QiFocus.HANDS) && tier > 0 ? tier - 1 : -1);
		applyStageEffect(player, MobEffects.SPEED, data.hasQiFocus(QiFocus.LEGS) && tier > 0 ? tier - 1 : -1);
		applyStageEffect(player, MobEffects.JUMP_BOOST, data.hasQiFocus(QiFocus.LEGS) && tier > 0 ? Math.max(0, tier / 2) : -1);
		applyStageEffect(player, MobEffects.NIGHT_VISION, data.hasQiFocus(QiFocus.HEAD) && tier > 0 ? 0 : -1);
		applyStageFlight(player, data.stage(), data.hasQiFocus(QiFocus.TORSO));
		if (data.hasQiFocus(QiFocus.HEAD) && tier > 0 && player.tickCount % 20 == 0) {
			int radius = spiritualSightRadius(data.stage());
			for (var entity : player.level().getEntities(player, player.getBoundingBox().inflate(radius), candidate -> candidate instanceof net.minecraft.world.entity.LivingEntity && candidate != player)) {
				if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
					living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, false, true));
				}
			}
		}
	}

	private static final Identifier VIGOR_ID = Identifier.fromNamespaceAndPath("immortality", "vigor");

	private static void applySpiritualModifiers(ServerPlayer player) {
		int vigorLvl = 0;
		for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
			ItemStack stack = player.getItemBySlot(slot);
			if (stack != null && !stack.isEmpty()) {
				SpiritualBlueprintComponent blueprint = stack.get(Immortality.SPIRITUAL_BLUEPRINT);
				if (blueprint != null) {
					boolean broken = stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1 && blueprint.hasFlag(SpiritualBlueprintComponent.TEMPERED);
					if (!broken) {
						vigorLvl += blueprint.getModifierLevel("vigor");
					}
				}
			}
		}

		var attributeInstance = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
		if (attributeInstance != null) {
			attributeInstance.removeModifier(VIGOR_ID);
			if (vigorLvl > 0) {
				double healthAmount = vigorLvl * 4.0D;
				net.minecraft.world.entity.ai.attributes.AttributeModifier modifier = new net.minecraft.world.entity.ai.attributes.AttributeModifier(
					VIGOR_ID,
					healthAmount,
					net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
				);
				attributeInstance.addTransientModifier(modifier);
			}
		}
	}

	private static int spiritualSightRadius(CultivationStage stage) {
		if (stage.tier() <= 0) {
			return 0;
		}
		return Math.min(100, 10 + (stage.tier() - 1) * 9);
	}

	public static CompoundTag buildResearchScreenData(ServerPlayer player) {
		CultivationData data = get(player);
		CompoundTag root = new CompoundTag();
		root.putString("Stage", data.stage().name());
		root.putInt("CurrentQi", data.currentQi());
		root.putInt("MaxQi", data.maxQi());
		root.putDouble("Purity", data.purity());
		root.putDouble("Stability", data.stability());
		root.putDouble("Deviation", data.deviation());
		root.putString("Body", data.bodyId());
		root.putString("Manual", data.manualId());
		root.putString("PreparedResearch", data.preparedResearchId());

		ListTag researches = new ListTag();
		for (ResearchDefinition definition : ResearchRegistry.all()) {
			CompoundTag entry = new CompoundTag();
			boolean known = data.knows(definition.id());
			boolean stageMet = data.stage().tier() >= definition.requiredStage().tier();
			boolean prerequisitesMet = definition.prerequisites().stream().allMatch(data::knows);
			boolean insightsMet = definition.requiredInsights().stream().allMatch(data::knowsInsight);
			boolean manualMet = ManualAccessService.allowsResearch(data, definition);
			boolean qiMet = data.currentQi() >= definition.qiCost();
			boolean itemMet = definition.requiredItemId() == null || hasItem(player, definition.requiredItemId());
			boolean available = !known && stageMet && prerequisitesMet && insightsMet && manualMet;

			entry.putString("Id", definition.id());
			entry.putBoolean("Known", known);
			entry.putBoolean("Available", available);
			entry.putBoolean("Prepared", definition.id().equals(data.preparedResearchId()));
			entry.putBoolean("StageMet", stageMet);
			entry.putBoolean("PrerequisitesMet", prerequisitesMet);
			entry.putBoolean("InsightsMet", insightsMet);
			entry.putBoolean("ManualMet", manualMet);
			entry.putBoolean("QiMet", qiMet);
			entry.putBoolean("ItemMet", itemMet);
			entry.putInt("QiCost", definition.qiCost());
			entry.putString("Category", definition.category());
			entry.putString("AspectStart", definition.aspectStart());
			entry.putString("AspectEnd", definition.aspectEnd());
			entry.putString("StudyBoardId", definition.studyBoardId());
			entry.putInt("Column", definition.column());
			entry.putInt("Row", definition.row());
			entry.putString("RequiredStage", definition.requiredStage().name());
			entry.putString("RequiredItemId", definition.requiredItemId() == null ? "" : definition.requiredItemId());

			ListTag prerequisites = new ListTag();
			for (String prerequisite : definition.prerequisites()) {
				prerequisites.add(StringTag.valueOf(prerequisite));
			}
			entry.put("Prerequisites", prerequisites);
			ListTag insights = new ListTag();
			for (String insight : definition.requiredInsights()) {
				insights.add(StringTag.valueOf(insight));
			}
			entry.put("RequiredInsights", insights);
			researches.add(entry);
		}
		root.put("Researches", researches);
		ListTag aspects = new ListTag();
		for (String aspectId : availableAspectIds(data)) {
			aspects.add(StringTag.valueOf(aspectId));
		}
		root.put("AvailableAspects", aspects);

		return root;
	}

	public static CompoundTag buildStudyScreenData(ServerPlayer player) {
		CultivationData data = get(player);
		CompoundTag root = new CompoundTag();
		root.putString("PreparedResearch", data.preparedResearchId());
		root.putString("Stage", data.stage().name());
		root.putInt("CurrentQi", data.currentQi());
		root.putInt("MaxQi", data.maxQi());
		root.putString("Manual", data.manualId());

		ResearchDefinition definition = data.preparedResearchId().isBlank() ? null : ResearchRegistry.get(data.preparedResearchId());
		if (definition != null) {
		StudyBoardDefinition board = resolveStudyBoard(definition);
			root.putString("ResearchId", definition.id());
			root.putString("AspectStart", definition.aspectStart());
			root.putString("AspectEnd", definition.aspectEnd());
			root.putString("StudyBoardId", definition.studyBoardId());
			root.putInt("QiCost", definition.qiCost());
			root.putBoolean("QiMet", data.currentQi() >= definition.qiCost());
			root.putString("RequiredItemId", definition.requiredItemId() == null ? "" : definition.requiredItemId());
			root.putBoolean("ItemMet", definition.requiredItemId() == null || hasItem(player, definition.requiredItemId()));
			if (board != null) {
				root.putString("BoardVictoryMode", board.victoryMode());
				root.putInt("BoardWidth", board.width());
				root.putInt("BoardHeight", board.height());
				root.putInt("BoardQiLimit", board.qiLimit());
				root.putInt("YinYangTolerance", board.yinYangTolerance());
				root.put("BoardStarts", writePoints(board.starts()));
				root.put("BoardFinishes", writePoints(board.finishes()));
				root.put("BoardRequiredNodes", writePoints(board.requiredNodes()));
				root.put("BoardBlocked", writePoints(List.copyOf(board.blocked())));
				root.put("BoardEffects", writeEffects(board.effects().values().stream().toList()));
			}
		}

		ListTag aspects = new ListTag();
		for (String aspectId : availableAspectIds(data)) {
			aspects.add(StringTag.valueOf(aspectId));
		}
		root.put("AvailableAspects", aspects);
		return root;
	}

	public static CompoundTag buildManualScreenData(ServerPlayer player, String manualId) {
		CultivationData data = get(player);
		ManualDefinition manual = ManualRegistry.get(manualId);
		CompoundTag root = new CompoundTag();
		root.putString("ManualId", manual.id());
		root.putString("ActiveManualId", data.manualId());
		root.putString("ActiveTechnique", data.activeTechniqueId());
		root.putString("Stage", data.stage().name());
		root.putBoolean("IsActive", data.manualId().equals(manual.id()));
		root.putDouble("BreakthroughBonus", manual.breakthroughBonus());
		root.putDouble("DeviationModifier", manual.deviationModifier());
		root.putString("MaxStage", manual.maxStage().name());

		ListTag insights = new ListTag();
		for (String insightId : manual.insightPool()) {
			CompoundTag entry = new CompoundTag();
			entry.putString("Id", insightId);
			entry.putBoolean("Known", data.knowsInsight(insightId));
			insights.add(entry);
		}
		root.put("Insights", insights);

		ListTag researches = new ListTag();
		for (ResearchDefinition definition : ResearchRegistry.all()) {
			if (!manual.allowsResearch(definition.id())) {
				continue;
			}
			CompoundTag entry = new CompoundTag();
			entry.putString("Id", definition.id());
			entry.putBoolean("Known", data.knows(definition.id()));
			entry.putBoolean("InsightReady", definition.requiredInsights().stream().allMatch(data::knowsInsight));
			entry.putString("RequiredStage", definition.requiredStage().name());
			researches.add(entry);
		}
		root.put("Researches", researches);
		ListTag techniques = new ListTag();
		for (String techniqueId : manual.grantedTechniques()) {
			CompoundTag entry = new CompoundTag();
			entry.putString("Id", techniqueId);
			entry.putBoolean("Known", data.knowsTechnique(techniqueId));
			entry.putBoolean("Active", techniqueId.equals(data.activeTechniqueId()));
			techniques.add(entry);
		}
		root.put("Techniques", techniques);
		return root;
	}

	public static CompoundTag buildBreakthroughScreenData(ServerPlayer player) {
		CultivationData data = get(player);
		BreakthroughPreview preview = BreakthroughService.preview(player, data);
		BreakthroughCoreSelection selectedCore = BreakthroughService.selectCore(player, data);
		BeastCoreDefinition coreDefinition = selectedCore.definition();

		CompoundTag root = new CompoundTag();
		root.putString("Stage", data.stage().name());
		root.putString("NextStage", data.stage().next().name());
		root.putInt("CurrentQi", data.currentQi());
		root.putInt("RequiredQi", preview.requiredQi());
		root.putDouble("Purity", data.purity());
		root.putDouble("Stability", data.stability());
		root.putDouble("Deviation", data.deviation());
		root.putString("Body", data.bodyId());
		root.putString("ActiveTechnique", data.activeTechniqueId());
		root.putBoolean("PeakReached", preview.peakReached());
		root.putBoolean("CooldownActive", preview.cooldownActive());
		root.putBoolean("ResearchMet", preview.researchMet());
		root.putBoolean("QiMet", preview.qiMet());
		root.putBoolean("CoreRequired", preview.coreRequired());
		root.putBoolean("CoreMet", preview.coreMet());
		root.putDouble("Chance", preview.chance());
		root.putDouble("BaseChance", preview.baseChance());
		root.putDouble("PurityBonus", preview.purityBonus());
		root.putDouble("StabilityBonus", preview.stabilityBonus());
		root.putDouble("DeviationPenalty", preview.deviationPenalty());
		root.putDouble("BodyBonus", preview.bodyBonus());
		root.putDouble("ManualBonus", preview.manualBonus());
		root.putDouble("TechniqueBonus", preview.techniqueBonus());
		root.putDouble("CoreBonus", preview.coreBonus());
		root.putString("RequiredResearchId", preview.requiredResearchId() == null ? "" : preview.requiredResearchId());
		root.putString("CoreItemId", preview.coreItemId());
		root.putString("CoreName", coreDefinition != null ? coreDefinition.displayNameComponent().getString() : "");
		root.putString("CoreBodyCompatibility", coreDefinition != null && coreDefinition.supportsBody(data.bodyId()) ? "good" : coreDefinition != null ? "bad" : "");
		root.putDouble("CoreStabilityBonus", coreDefinition != null ? coreDefinition.stabilityBonus() : 0.0D);
		root.putDouble("CoreDeviationPenalty", preview.coreDeviationPenalty());
		root.putDouble("CorePurityBonus", coreDefinition != null ? coreDefinition.purityBonus() : 0.0D);
		root.putInt("Cooldown", data.breakthroughCooldown());
		return root;
	}

	public static CompoundTag buildTechniqueScreenData(ServerPlayer player) {
		CultivationData data = get(player);
		CompoundTag root = new CompoundTag();
		root.putString("Stage", data.stage().name());
		root.putInt("CurrentQi", data.currentQi());
		root.putInt("MaxQi", data.maxQi());
		root.putString("ActiveTechnique", data.activeTechniqueId());
		root.putInt("TechniqueCooldown", data.techniqueCooldown());
		root.putInt("StageStrengthAmplifier", data.stage().strengthAmplifier());
		root.putInt("StageResistanceAmplifier", data.stage().resistanceAmplifier());
		root.putInt("StageSpeedAmplifier", data.stage().speedAmplifier());
		root.putInt("StageJumpAmplifier", data.stage().jumpAmplifier());
		root.putBoolean("StageFlight", data.stage().grantsFlight());

		ListTag techniques = new ListTag();
		for (String techniqueId : data.knownTechniques().stream().sorted().toList()) {
			TechniqueDefinition definition = TechniqueRegistry.get(techniqueId);
			CompoundTag entry = new CompoundTag();
			entry.putString("Id", techniqueId);
			entry.putBoolean("Active", techniqueId.equals(data.activeTechniqueId()));
			entry.putBoolean("StageMet", data.stage().tier() >= definition.requiredStage().tier());
			entry.putInt("ActivationQiCost", definition.activationQiCost());
			entry.putInt("ActivationCooldownTicks", definition.activationCooldownTicks());
			entry.putInt("ActivationDurationTicks", definition.activationDurationTicks());
			entry.putInt("ActivationAmplifier", definition.activationAmplifier());
			entry.putString("ActivationEffect", definition.activationEffect());
			entry.putDouble("QiGainBonus", definition.qiGainBonus());
			entry.putDouble("PurityBonus", definition.purityBonus());
			entry.putDouble("StabilityBonus", definition.stabilityBonus());
			entry.putDouble("BreakthroughBonus", definition.breakthroughBonus());
			entry.putDouble("DeviationModifier", definition.deviationModifier());
			entry.putString("RequiredStage", definition.requiredStage().name());
			techniques.add(entry);
		}
		root.put("Techniques", techniques);
		return root;
	}

	public static CompoundTag buildEffectsScreenData(ServerPlayer player) {
		CultivationData data = get(player);
		CompoundTag root = new CompoundTag();
		root.putString("Stage", data.stage().name());
		root.putString("Focus", data.qiFocuses().stream().map(QiFocus::name).reduce((left, right) -> left + "," + right).orElse(""));
		root.putInt("CurrentQi", data.currentQi());
		root.putInt("MaxQi", data.maxQi());
		root.putInt("StrengthAmplifier", data.stage().tier());
		root.putInt("SpeedAmplifier", data.stage().tier());
		root.putInt("JumpAmplifier", data.stage().tier() > 0 ? Math.max(1, data.stage().tier() / 2 + 1) : 0);
		root.putBoolean("FlightUnlocked", data.stage().grantsFlight());
		root.putInt("SightRadius", spiritualSightRadius(data.stage()));
		root.putString("ActiveTechnique", TechniqueService.activeTechnique(data).titleKey());
		return root;
	}

	private static Component focusedPartsText(CultivationData data) {
		if (data.qiFocuses().isEmpty()) {
			return QiFocus.NONE.displayName();
		}
		MutableComponent text = Component.empty();
		boolean first = true;
		for (QiFocus focus : List.of(QiFocus.HEAD, QiFocus.HANDS, QiFocus.TORSO, QiFocus.LEGS)) {
			if (!data.hasQiFocus(focus)) {
				continue;
			}
			if (!first) {
				text.append(", ");
			}
			text.append(focus.displayName());
			first = false;
		}
		return text;
	}

	private static boolean canResearch(ServerPlayer player, CultivationData data, ResearchDefinition definition, boolean notify) {
		if (data.knows(definition.id())) {
			if (notify) {
				player.sendSystemMessage(Component.translatable("message.immortality.research_known", definition.titleComponent()));
			}
			return false;
		}
		if (data.stage().tier() < definition.requiredStage().tier()) {
			if (notify) {
				player.sendSystemMessage(Component.translatable("message.immortality.research_required_stage", definition.requiredStage().displayNameComponent()));
			}
			return false;
		}
		for (String prerequisite : definition.prerequisites()) {
			if (!data.knows(prerequisite)) {
				if (notify) {
					ResearchDefinition prerequisiteDefinition = ResearchRegistry.get(prerequisite);
					Component prerequisiteName = prerequisiteDefinition != null ? prerequisiteDefinition.titleComponent() : Component.literal(prerequisite);
					player.sendSystemMessage(Component.translatable("message.immortality.research_missing_prerequisite", prerequisiteName));
				}
				return false;
			}
		}
		if (!ManualAccessService.hasManual(data)) {
			if (notify) {
				player.sendSystemMessage(Component.translatable("message.immortality.manual.required"));
			}
			return false;
		}
		if (!ManualAccessService.allowsResearch(data, definition)) {
			if (notify) {
				player.sendSystemMessage(Component.translatable("message.immortality.manual.blocks_research", data.manual().titleComponent()));
			}
			return false;
		}
		for (String insight : definition.requiredInsights()) {
			if (!data.knowsInsight(insight)) {
				if (notify) {
					player.sendSystemMessage(Component.translatable("message.immortality.insight.required", Component.translatable("insight.immortality." + insight)));
				}
				return false;
			}
		}
		if (data.currentQi() < definition.qiCost()) {
			if (notify) {
				player.sendSystemMessage(Component.translatable("message.immortality.research_not_enough_qi", definition.titleComponent()));
			}
			return false;
		}
		if (definition.requiredItemId() != null && !hasItem(player, definition.requiredItemId())) {
			if (notify) {
				Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(definition.requiredItemId()));
				player.sendSystemMessage(Component.translatable("message.immortality.research_required_item", item.getName()));
			}
			return false;
		}
		return true;
	}

	private static boolean canPrepareResearch(ServerPlayer player, CultivationData data, ResearchDefinition definition, boolean notify) {
		if (data.knows(definition.id())) {
			if (notify) {
				player.sendSystemMessage(Component.translatable("message.immortality.research_known", definition.titleComponent()));
			}
			return false;
		}
		if (data.stage().tier() < definition.requiredStage().tier()) {
			if (notify) {
				player.sendSystemMessage(Component.translatable("message.immortality.research_required_stage", definition.requiredStage().displayNameComponent()));
			}
			return false;
		}
		for (String prerequisite : definition.prerequisites()) {
			if (!data.knows(prerequisite)) {
				if (notify) {
					ResearchDefinition prerequisiteDefinition = ResearchRegistry.get(prerequisite);
					Component prerequisiteName = prerequisiteDefinition != null ? prerequisiteDefinition.titleComponent() : Component.literal(prerequisite);
					player.sendSystemMessage(Component.translatable("message.immortality.research_missing_prerequisite", prerequisiteName));
				}
				return false;
			}
		}
		if (!ManualAccessService.hasManual(data)) {
			if (notify) {
				player.sendSystemMessage(Component.translatable("message.immortality.manual.required"));
			}
			return false;
		}
		if (!ManualAccessService.allowsResearch(data, definition)) {
			if (notify) {
				player.sendSystemMessage(Component.translatable("message.immortality.manual.blocks_research", data.manual().titleComponent()));
			}
			return false;
		}
		for (String insight : definition.requiredInsights()) {
			if (!data.knowsInsight(insight)) {
				if (notify) {
					player.sendSystemMessage(Component.translatable("message.immortality.insight.required", Component.translatable("insight.immortality." + insight)));
				}
				return false;
			}
		}
		return true;
	}

	private static boolean hasItem(ServerPlayer player, String itemId) {
		Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (player.getInventory().getItem(i).is(item)) {
				return true;
			}
		}
		return false;
	}

	private static void consumeItem(ServerPlayer player, String itemId) {
		Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(item)) {
				stack.shrink(1);
				return;
			}
		}
	}

	private static String percent(double value) {
		return Math.round(value * 100.0D) + "%";
	}

	private static boolean isValidAspectChain(CultivationData data, ResearchDefinition definition, List<String> chain) {
		Set<String> available = availableAspectIds(data);
		String previous = definition.aspectStart();
		for (String current : chain) {
			if (!available.contains(current)) {
				return false;
			}
			QiAspectDefinition previousAspect = QiAspectRegistry.get(previous);
			QiAspectDefinition currentAspect = QiAspectRegistry.get(current);
			if (previousAspect == null || currentAspect == null || 
				(!previousAspect.connectsTo(current) && !currentAspect.connectsTo(previous))) {
				return false;
			}
			previous = current;
		}
		QiAspectDefinition lastAspect = QiAspectRegistry.get(previous);
		QiAspectDefinition endAspect = QiAspectRegistry.get(definition.aspectEnd());
		return lastAspect != null && endAspect != null && 
			(lastAspect.connectsTo(definition.aspectEnd()) || endAspect.connectsTo(previous));
	}

	private static boolean isValidAspectBoard(CultivationData data, ResearchDefinition definition, List<ResearchLinkPayload.Placement> placements) {
		StudyBoardDefinition board = resolveStudyBoard(definition);
		if (board == null) {
			return false;
		}
		Set<String> availableAspects = availableAspectIds(data);
		Map<StudyBoardDefinition.BoardPoint, String> placed = new HashMap<>();
		int qiUsage = 0;
		for (ResearchLinkPayload.Placement placement : placements) {
			StudyBoardDefinition.BoardPoint point = new StudyBoardDefinition.BoardPoint(placement.x(), placement.y());
			if (!board.contains(point) || board.isBlocked(point) || board.starts().contains(point) || board.finishes().contains(point)) {
				return false;
			}
			if (!availableAspects.contains(placement.aspectId()) || placed.put(point, placement.aspectId()) != null) {
				return false;
			}
			qiUsage += board.effectAt(point) != null ? board.effectAt(point).qiCost(placement.aspectId()) : 1;
		}
		if (board.qiLimit() > 0 && qiUsage > board.qiLimit()) {
			return false;
		}

		List<StudyBoardDefinition.BoardPoint> points = new java.util.ArrayList<>();
		points.addAll(board.starts());
		points.addAll(placed.keySet());
		points.addAll(board.finishes());
		Map<StudyBoardDefinition.BoardPoint, List<StudyBoardDefinition.BoardPoint>> graph = new HashMap<>();
		for (StudyBoardDefinition.BoardPoint point : points) {
			graph.put(point, new java.util.ArrayList<>());
		}
		for (StudyBoardDefinition.BoardPoint point : points) {
			for (StudyBoardDefinition.BoardPoint neighbor : neighbors(point)) {
				if (graph.containsKey(neighbor)) {
					graph.get(point).add(neighbor);
				}
			}
		}
		for (StudyBoardDefinition.BoardPoint start : board.starts()) {
			if (graph.get(start).isEmpty()) {
				return false;
			}
		}
		for (StudyBoardDefinition.BoardPoint finish : board.finishes()) {
			if (graph.get(finish).isEmpty()) {
				return false;
			}
		}
		for (Map.Entry<StudyBoardDefinition.BoardPoint, List<StudyBoardDefinition.BoardPoint>> entry : graph.entrySet()) {
			StudyBoardDefinition.BoardPoint point = entry.getKey();
			int degree = entry.getValue().size();
			if (board.starts().contains(point) || board.finishes().contains(point)) {
				if (degree < 1) {
					return false;
				}
			} else if (board.requiredNodes().contains(point)) {
				if (degree < 2) {
					return false;
				}
			} else if (degree < 2) {
				return false;
			}
		}
		return switch (board.victoryMode()) {
			case "all_finishes" -> validateAllFinishes(data, definition, board, graph, placed);
			case "required_route" -> validateRequiredRoute(data, definition, board, graph, placed);
			default -> validateSinglePath(data, definition, board, graph, placed);
		};
	}

	private static boolean validateSinglePath(
		CultivationData data,
		ResearchDefinition definition,
		StudyBoardDefinition board,
		Map<StudyBoardDefinition.BoardPoint, List<StudyBoardDefinition.BoardPoint>> graph,
		Map<StudyBoardDefinition.BoardPoint, String> placed
	) {
		TraversalResult result = traverseRoute(definition, board, graph, placed, board.starts().getFirst(), board.finishes().getFirst());
		return result != null
			&& result.visited().size() >= graph.size() - 1
			&& passesYinYangTolerance(board, result.chain(), definition.aspectStart(), definition.aspectEnd())
			&& isValidAspectChain(data, definition, result.chain());
	}

	private static boolean validateRequiredRoute(
		CultivationData data,
		ResearchDefinition definition,
		StudyBoardDefinition board,
		Map<StudyBoardDefinition.BoardPoint, List<StudyBoardDefinition.BoardPoint>> graph,
		Map<StudyBoardDefinition.BoardPoint, String> placed
	) {
		TraversalResult result = traverseRoute(definition, board, graph, placed, board.starts().getFirst(), board.finishes().getFirst());
		if (result == null) {
			return false;
		}
		for (StudyBoardDefinition.BoardPoint required : board.requiredNodes()) {
			if (!result.visited().contains(required)) {
				return false;
			}
		}
		return passesYinYangTolerance(board, result.chain(), definition.aspectStart(), definition.aspectEnd())
			&& isValidAspectChain(data, definition, result.chain());
	}

	private static boolean validateAllFinishes(
		CultivationData data,
		ResearchDefinition definition,
		StudyBoardDefinition board,
		Map<StudyBoardDefinition.BoardPoint, List<StudyBoardDefinition.BoardPoint>> graph,
		Map<StudyBoardDefinition.BoardPoint, String> placed
	) {
		Set<StudyBoardDefinition.BoardPoint> visited = new LinkedHashSet<>();
		List<String> mergedChain = new java.util.ArrayList<>();
		for (StudyBoardDefinition.BoardPoint finish : board.finishes()) {
			TraversalResult result = traverseRoute(definition, board, graph, placed, board.starts().getFirst(), finish);
			if (result == null) {
				return false;
			}
			visited.addAll(result.visited());
			for (String aspectId : result.chain()) {
				if (mergedChain.isEmpty() || !mergedChain.getLast().equals(aspectId)) {
					mergedChain.add(aspectId);
				}
			}
		}
		for (StudyBoardDefinition.BoardPoint required : board.requiredNodes()) {
			if (!visited.contains(required)) {
				return false;
			}
		}
		for (StudyBoardDefinition.BoardPoint finish : board.finishes()) {
			if (!visited.contains(finish)) {
				return false;
			}
		}
		return passesYinYangTolerance(board, mergedChain, definition.aspectStart(), definition.aspectEnd())
			&& isValidAspectChain(data, definition, mergedChain);
	}

	private static TraversalResult traverseRoute(
		ResearchDefinition definition,
		StudyBoardDefinition board,
		Map<StudyBoardDefinition.BoardPoint, List<StudyBoardDefinition.BoardPoint>> graph,
		Map<StudyBoardDefinition.BoardPoint, String> placed,
		StudyBoardDefinition.BoardPoint start,
		StudyBoardDefinition.BoardPoint finish
	) {
		LinkedHashSet<StudyBoardDefinition.BoardPoint> visited = new LinkedHashSet<>();
		visited.add(start);
		List<String> chain = new java.util.ArrayList<>();
		List<String> effectiveSoFar = new java.util.ArrayList<>();
		effectiveSoFar.add(definition.aspectStart());

		return findValidPath(definition, board, graph, placed, start, finish, visited, chain, effectiveSoFar);
	}

	private static TraversalResult findValidPath(
		ResearchDefinition definition,
		StudyBoardDefinition board,
		Map<StudyBoardDefinition.BoardPoint, List<StudyBoardDefinition.BoardPoint>> graph,
		Map<StudyBoardDefinition.BoardPoint, String> placed,
		StudyBoardDefinition.BoardPoint current,
		StudyBoardDefinition.BoardPoint finish,
		LinkedHashSet<StudyBoardDefinition.BoardPoint> visited,
		List<String> chain,
		List<String> effectiveSoFar
	) {
		if (current.equals(finish)) {
			boolean valid = true;
			if ("required_route".equals(board.victoryMode())) {
				for (StudyBoardDefinition.BoardPoint required : board.requiredNodes()) {
					if (!visited.contains(required)) {
						valid = false;
						break;
					}
				}
			}
			if ("single_path".equals(board.victoryMode())) {
				if (visited.size() < graph.size() - 1) {
					valid = false;
				}
			}
			if (valid) {
				return new TraversalResult(new java.util.ArrayList<>(chain), new LinkedHashSet<>(visited));
			}
			return null;
		}

		for (StudyBoardDefinition.BoardPoint next : graph.getOrDefault(current, java.util.Collections.emptyList())) {
			if (visited.contains(next)) {
				continue;
			}

			String originalAspect = placed.get(next);
			if (originalAspect == null && !next.equals(finish)) {
				continue;
			}

			String effectiveAspect = next.equals(finish) ? definition.aspectEnd() : originalAspect;
			StudyBoardDefinition.CellEffect effect = board.effectAt(next);
			if (effect != null && !next.equals(finish)) {
				if (!passesCellRules(effect, originalAspect, next, graph, placed, board, effectiveSoFar)) {
					continue;
				}
				effectiveAspect = effect.transform(originalAspect);
			}

			String prevAspect = effectiveSoFar.get(effectiveSoFar.size() - 1);
			QiAspectDefinition defPrev = QiAspectRegistry.get(prevAspect);
			QiAspectDefinition defNext = QiAspectRegistry.get(effectiveAspect);
			boolean connects = (defPrev != null && defPrev.connectsTo(effectiveAspect)) || (defNext != null && defNext.connectsTo(prevAspect));
			if (!connects) {
				continue;
			}

			visited.add(next);
			if (!next.equals(finish)) {
				chain.add(effectiveAspect);
			}
			effectiveSoFar.add(effectiveAspect);

			TraversalResult res = findValidPath(definition, board, graph, placed, next, finish, visited, chain, effectiveSoFar);
			if (res != null) {
				return res;
			}

			visited.remove(next);
			if (!next.equals(finish)) {
				chain.remove(chain.size() - 1);
			}
			effectiveSoFar.remove(effectiveSoFar.size() - 1);
		}
		return null;
	}

	private static StudyBoardDefinition.BoardPoint chooseCandidate(List<StudyBoardDefinition.BoardPoint> candidates, StudyBoardDefinition.BoardPoint finish) {
		for (StudyBoardDefinition.BoardPoint candidate : candidates) {
			if (candidate.equals(finish)) {
				return candidate;
			}
		}
		return candidates.getFirst();
	}

	private static boolean passesCellRules(
		StudyBoardDefinition.CellEffect effect,
		String originalAspect,
		StudyBoardDefinition.BoardPoint point,
		Map<StudyBoardDefinition.BoardPoint, List<StudyBoardDefinition.BoardPoint>> graph,
		Map<StudyBoardDefinition.BoardPoint, String> placed,
		StudyBoardDefinition board,
		List<String> effectiveSoFar
	) {
		if (effect.requiredAdjacentAspect() != null && !effect.requiredAdjacentAspect().isBlank()) {
			boolean found = false;
			for (StudyBoardDefinition.BoardPoint neighbor : graph.get(point)) {
				if (board.starts().contains(neighbor) && effect.requiredAdjacentAspect().equals(effectiveSoFar.getFirst())) {
					found = true;
					break;
				}
				if (placed.containsKey(neighbor)) {
					StudyBoardDefinition.CellEffect neighborEffect = board.effectAt(neighbor);
					String effective = neighborEffect != null ? neighborEffect.transform(placed.get(neighbor)) : placed.get(neighbor);
					if (effect.requiredAdjacentAspect().equals(effective)) {
						found = true;
						break;
					}
				}
			}
			if (!found) {
				return false;
			}
		}
		if (effect.requiredPolarity() == null || effect.requiredPolarity().isBlank() || "none".equals(effect.requiredPolarity())) {
			return true;
		}
		int yin = 0;
		int yang = 0;
		for (String aspectId : effectiveSoFar) {
			if (isYinFamily(aspectId)) {
				yin++;
			}
			if (isYangFamily(aspectId)) {
				yang++;
			}
		}
		if (isYinFamily(originalAspect)) {
			yin++;
		}
		if (isYangFamily(originalAspect)) {
			yang++;
		}
		return switch (effect.requiredPolarity()) {
			case "yin" -> yin >= yang;
			case "yang" -> yang >= yin;
			case "balanced" -> Math.abs(yin - yang) <= 1;
			default -> true;
		};
	}

	private static boolean passesYinYangTolerance(StudyBoardDefinition board, List<String> chain, String startAspect, String endAspect) {
		if (board.yinYangTolerance() >= 90) {
			return true;
		}
		int yin = isYinFamily(startAspect) ? 1 : 0;
		int yang = isYangFamily(startAspect) ? 1 : 0;
		for (String aspectId : chain) {
			if (isYinFamily(aspectId)) {
				yin++;
			}
			if (isYangFamily(aspectId)) {
				yang++;
			}
		}
		if (isYinFamily(endAspect)) {
			yin++;
		}
		if (isYangFamily(endAspect)) {
			yang++;
		}
		return Math.abs(yin - yang) <= board.yinYangTolerance();
	}

	private static boolean isYinFamily(String aspectId) {
		return Set.of("yin", "water", "mist", "frost", "dream", "abyss", "void", "soul").contains(aspectId);
	}

	private static boolean isYangFamily(String aspectId) {
		return Set.of("yang", "fire", "ember", "blood", "dawn", "thunder", "metal", "karma").contains(aspectId);
	}

	private record TraversalResult(List<String> chain, Set<StudyBoardDefinition.BoardPoint> visited) {
	}

	private static List<StudyBoardDefinition.BoardPoint> neighbors(StudyBoardDefinition.BoardPoint point) {
		int x = point.x();
		int y = point.y();
		if ((y & 1) == 0) {
			return List.of(
				new StudyBoardDefinition.BoardPoint(x - 1, y),
				new StudyBoardDefinition.BoardPoint(x + 1, y),
				new StudyBoardDefinition.BoardPoint(x, y - 1),
				new StudyBoardDefinition.BoardPoint(x, y + 1),
				new StudyBoardDefinition.BoardPoint(x - 1, y - 1),
				new StudyBoardDefinition.BoardPoint(x - 1, y + 1)
			);
		}
		return List.of(
			new StudyBoardDefinition.BoardPoint(x - 1, y),
			new StudyBoardDefinition.BoardPoint(x + 1, y),
			new StudyBoardDefinition.BoardPoint(x, y - 1),
			new StudyBoardDefinition.BoardPoint(x, y + 1),
			new StudyBoardDefinition.BoardPoint(x + 1, y - 1),
			new StudyBoardDefinition.BoardPoint(x + 1, y + 1)
		);
	}

	private static ListTag writePoints(List<StudyBoardDefinition.BoardPoint> points) {
		ListTag list = new ListTag();
		for (StudyBoardDefinition.BoardPoint point : points) {
			CompoundTag tag = new CompoundTag();
			tag.putInt("X", point.x());
			tag.putInt("Y", point.y());
			list.add(tag);
		}
		return list;
	}

	private static ListTag writeEffects(List<StudyBoardDefinition.CellEffect> effects) {
		ListTag list = new ListTag();
		for (StudyBoardDefinition.CellEffect effect : effects) {
			CompoundTag tag = new CompoundTag();
			tag.putInt("X", effect.point().x());
			tag.putInt("Y", effect.point().y());
			tag.putInt("FavoredQiDelta", effect.favoredQiDelta());
			tag.putInt("HostileQiDelta", effect.hostileQiDelta());
			tag.putInt("NeutralQiDelta", effect.neutralQiDelta());
			tag.putString("TransformTo", effect.transformTo() == null ? "" : effect.transformTo());
			tag.putString("RequiredAdjacentAspect", effect.requiredAdjacentAspect() == null ? "" : effect.requiredAdjacentAspect());
			tag.putString("RequiredPolarity", effect.requiredPolarity() == null ? "none" : effect.requiredPolarity());
			ListTag favored = new ListTag();
			for (String aspectId : effect.favoredAspects()) {
				favored.add(StringTag.valueOf(aspectId));
			}
			tag.put("FavoredAspects", favored);
			ListTag hostile = new ListTag();
			for (String aspectId : effect.hostileAspects()) {
				hostile.add(StringTag.valueOf(aspectId));
			}
			tag.put("HostileAspects", hostile);
			list.add(tag);
		}
		return list;
	}

	private static void prepareResearch(ServerPlayer player, String researchId) {
		CultivationData data = get(player);
		ResearchDefinition definition = ResearchRegistry.get(researchId);
		if (definition == null) {
			player.sendSystemMessage(Component.translatable("message.immortality.research_unknown", researchId));
			return;
		}
		if (!canPrepareResearch(player, data, definition, true)) {
			return;
		}
		data.setPreparedResearchId(definition.id());
		player.sendSystemMessage(Component.translatable("message.immortality.research.prepared", definition.titleComponent()));
		sync(player);
	}

	private static Set<String> availableAspectIds(CultivationData data) {
		Set<String> aspects = new LinkedHashSet<>();
		for (QiAspectDefinition definition : QiAspectRegistry.all()) {
			aspects.add(definition.id());
		}
		return aspects;
	}

	private static StudyBoardDefinition resolveStudyBoard(ResearchDefinition definition) {
		StudyBoardDefinition base = StudyBoardRegistry.get(definition.studyBoardId());
		if (base == null) {
			return null;
		}
		return new StudyBoardDefinition(
			base.id(),
			base.width(),
			base.height(),
			definition.studyBoardQiLimit() != null ? definition.studyBoardQiLimit() : base.qiLimit(),
			base.yinYangTolerance(),
			definition.studyVictoryMode() != null && !definition.studyVictoryMode().isBlank() ? definition.studyVictoryMode() : base.victoryMode(),
			definition.studyStarts().isEmpty() ? base.starts() : definition.studyStarts(),
			definition.studyFinishes().isEmpty() ? base.finishes() : definition.studyFinishes(),
			definition.studyRequiredNodes().isEmpty() ? base.requiredNodes() : definition.studyRequiredNodes(),
			base.blocked(),
			base.effects()
		);
	}

	private static void cycleTechnique(ServerPlayer player, CultivationData data) {
		List<String> available = data.knownTechniques().stream()
			.sorted()
			.filter(id -> data.stage().tier() >= TechniqueRegistry.get(id).requiredStage().tier())
			.toList();
		if (available.isEmpty()) {
			player.sendSystemMessage(Component.translatable("message.immortality.technique.none_available"));
			return;
		}
		int currentIndex = available.indexOf(data.activeTechniqueId());
		int nextIndex = (currentIndex + 1 + available.size()) % available.size();
		data.setActiveTechnique(available.get(nextIndex));
		player.sendSystemMessage(Component.translatable("message.immortality.technique.active", TechniqueRegistry.get(data.activeTechniqueId()).titleComponent()));
		sync(player);
	}

	private static void invokeTechnique(ServerPlayer player, CultivationData data) {
		TechniqueDefinition definition = TechniqueService.activeTechnique(data);
		if (TechniqueRegistry.NONE_ID.equals(definition.id())) {
			player.sendSystemMessage(Component.translatable("message.immortality.technique.none_available"));
			return;
		}
		if (data.techniqueCooldown() > 0) {
			player.sendSystemMessage(Component.translatable("message.immortality.technique.cooldown", data.techniqueCooldown() / 20));
			return;
		}
		if (data.currentQi() < definition.activationQiCost()) {
			player.sendSystemMessage(Component.translatable("message.immortality.technique.not_enough_qi", definition.activationQiCost()));
			return;
		}
		if (TechniqueService.invoke(player, data)) {
			player.sendSystemMessage(Component.translatable("message.immortality.technique.invoked", definition.titleComponent()));
			sync(player);
		}
	}
}
