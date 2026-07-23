package immortality.client;

import immortality.beast.BeastCoreRegistry;
import immortality.cultivation.BodyRegistry;
import immortality.cultivation.QiAspectRegistry;
import immortality.manual.ManualRegistry;
import immortality.technique.TechniqueRegistry;
import immortality.network.AltarResearchScreenPayload;
import immortality.network.BreakthroughScreenPayload;
import immortality.network.CultivationSyncPayload;
import immortality.network.ManualScreenPayload;
import immortality.network.QiFocusScreenPayload;
import immortality.network.ResearchStudyScreenPayload;
import immortality.network.TechniqueScreenPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class ImmortalityClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BodyRegistry.bootstrap();
		BeastCoreRegistry.bootstrap();
		TechniqueRegistry.bootstrap();
		ManualRegistry.bootstrap();
		QiAspectRegistry.bootstrap();
		CultivationHudControls.init();
		ClientLifecycleEvents.CLIENT_STARTED.register(ClientHudConfig::load);
		ClientPlayNetworking.registerGlobalReceiver(CultivationSyncPayload.TYPE, (payload, context) ->
			context.client().execute(() -> ClientCultivationState.apply(payload.data()))
		);
		ClientPlayNetworking.registerGlobalReceiver(AltarResearchScreenPayload.TYPE, (payload, context) ->
			context.client().execute(() -> context.client().setScreen(new AltarResearchScreen(payload.data())))
		);
		ClientPlayNetworking.registerGlobalReceiver(BreakthroughScreenPayload.TYPE, (payload, context) ->
			context.client().execute(() -> context.client().setScreen(new BreakthroughScreen(payload.data())))
		);
		ClientPlayNetworking.registerGlobalReceiver(ManualScreenPayload.TYPE, (payload, context) ->
			context.client().execute(() -> context.client().setScreen(new ManualScreen(payload.data())))
		);
		ClientPlayNetworking.registerGlobalReceiver(ResearchStudyScreenPayload.TYPE, (payload, context) ->
			context.client().execute(() -> context.client().setScreen(new ResearchStudyScreen(payload.data())))
		);
		ClientPlayNetworking.registerGlobalReceiver(TechniqueScreenPayload.TYPE, (payload, context) ->
			context.client().execute(() -> context.client().setScreen(new TechniqueScreen(payload.data())))
		);
		ClientPlayNetworking.registerGlobalReceiver(QiFocusScreenPayload.TYPE, (payload, context) ->
			context.client().execute(() -> context.client().setScreen(new QiFocusScreen(payload.data())))
		);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientCultivationState.reset());
		ClientTickEvents.END_CLIENT_TICK.register(CultivationHudControls::tick);
		HudRenderCallback.EVENT.register((graphics, deltaTracker) -> CultivationHudRenderer.render(graphics));

		BlockEntityRendererRegistry.register(
			immortality.Immortality.JADE_PEDESTAL_ENTITY,
			context -> new FloatingItemRenderer<>(context, immortality.block.entity.JadePedestalBlockEntity::getItem)
		);
		BlockEntityRendererRegistry.register(
			immortality.Immortality.JADE_INFUSION_ALTAR_ENTITY,
			context -> new FloatingItemRenderer<>(context, immortality.block.entity.JadeInfusionAltarBlockEntity::getItem)
		);

		BlockRenderLayerMap.putBlocks(
			ChunkSectionLayer.CUTOUT,
			immortality.Immortality.BAMBOO_FLAG,
			immortality.Immortality.JADE_FLAG
		);

		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
			immortality.Immortality.SPIRIT_BEAST,
			immortality.client.entity.renderer.SpiritBeastRenderer::new
		);
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
			immortality.Immortality.TRIBULATION_LORD,
			immortality.client.entity.renderer.TribulationLordRenderer::new
		);
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
			immortality.Immortality.FLAME_SALAMANDER,
			immortality.client.entity.renderer.FlameSalamanderRenderer::new
		);
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
			immortality.Immortality.FROST_FOX,
			immortality.client.entity.renderer.FrostFoxRenderer::new
		);

		net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
			immortality.client.entity.model.SpiritBeastModel.LAYER_LOCATION,
			immortality.client.entity.model.SpiritBeastModel::createBodyLayer
		);
		net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
			immortality.client.entity.model.TribulationLordModel.LAYER_LOCATION,
			immortality.client.entity.model.TribulationLordModel::createBodyLayer
		);
		net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
			immortality.client.entity.model.FlameSalamanderModel.LAYER_LOCATION,
			immortality.client.entity.model.FlameSalamanderModel::createBodyLayer
		);
		net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
			immortality.client.entity.model.FrostFoxModel.LAYER_LOCATION,
			immortality.client.entity.model.FrostFoxModel::createBodyLayer
		);

		net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			immortality.item.SpiritualBlueprintComponent blueprint = stack.get(immortality.Immortality.SPIRITUAL_BLUEPRINT);
			if (blueprint != null) {
				lines.add(net.minecraft.network.chat.Component.literal("§b✦ Духовная Чертежная Схема §7(Эффект растет от стадии)"));
				if (blueprint.hasFlag(immortality.item.SpiritualBlueprintComponent.TEMPERED)) {
					boolean broken = stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
					if (broken) {
						lines.add(net.minecraft.network.chat.Component.literal("  §c⚠️ Дух Сломлен (Неактивен до ремонта)"));
					} else {
						lines.add(net.minecraft.network.chat.Component.literal("  §3✧ Закалено (Неразрушимо)"));
					}
				}
				for (var mod : blueprint.modifiers()) {
					String id = mod.id().toLowerCase();
					int lvl = mod.level();
					String title = switch (id) {
						case "unyielding" -> "§6✦ Непреклонность " + lvl + " §7(Стойкость и Щит при низком HP)";
						case "electrum" -> "§e⚡ Молния Электрума " + lvl + " §7(Призов молнии и цепной разряд)";
						case "vigor" -> "§a❤ Духовная Жила " + lvl + " §7(+" + (lvl * 4) + " к максимальному HP)";
						case "ignis" -> "§c🔥 Пламя Небес " + lvl + " §7(Огненный урон и поджог)";
						case "swift" -> "§b💨 Ветер Скорости " + lvl + " §7(Скорость и Спешка)";
						case "sharp" -> "§d⚔ Духовная Острота " + lvl + " §7(Сквозной урон сквозь броню)";
						default -> "§e✦ " + id.toUpperCase() + " " + lvl;
					};
					lines.add(net.minecraft.network.chat.Component.literal("  " + title));
				}
			}
		});
	}
}
