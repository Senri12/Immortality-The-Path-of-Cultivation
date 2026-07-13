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
	}
}
