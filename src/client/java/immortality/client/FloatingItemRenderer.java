package immortality.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import immortality.client.mixin.ItemStackRenderStateMixin;
import immortality.client.mixin.LayerRenderStateMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * Renders a floating, spinning item above a block entity.
 * Uses Mixin accessors to read private LayerRenderState fields, then renders
 * via the static ItemRenderer.renderItem() + MultiBufferSource path — bypassing
 * the broken SubmitNodeCollector.submitItem() in the block entity render context
 * (which doesn't bind the item texture atlas correctly in 1.21.11).
 */
public class FloatingItemRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, FloatingItemRenderState> {
	private final ItemModelResolver itemModelResolver;
	private final Function<T, ItemStack> itemGetter;

	public FloatingItemRenderer(BlockEntityRendererProvider.Context context, Function<T, ItemStack> itemGetter) {
		this.itemModelResolver = context.itemModelResolver();
		this.itemGetter = itemGetter;
	}

	@Override
	public FloatingItemRenderState createRenderState() {
		return new FloatingItemRenderState();
	}

	@Override
	public void extractRenderState(T entity, FloatingItemRenderState state, float partialTick, Vec3 pos, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay) {
		BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, pos, overlay);

		state.age = 0.0f;
		if (entity.getLevel() != null) {
			state.age = (float) entity.getLevel().getGameTime() + partialTick;
		}

		state.itemState.clear();
		ItemStack stack = this.itemGetter.apply(entity);
		if (!stack.isEmpty()) {
			this.itemModelResolver.updateForTopItem(
				state.itemState,
				stack,
				ItemDisplayContext.FIXED,
				entity.getLevel(),
				null,
				0
			);
		}
	}

	@Override
	public void submit(FloatingItemRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
		if (state.itemState.isEmpty()) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.level == null) {
			return;
		}

		// Access private render state data via Mixin accessors
		ItemStackRenderStateMixin stateMixin = (ItemStackRenderStateMixin)(Object) state.itemState;
		int activeCount = stateMixin.immortality_getActiveLayerCount();
		ItemStackRenderState.LayerRenderState[] allLayers = stateMixin.immortality_getLayers();

		if (allLayers == null || activeCount <= 0) {
			return;
		}

		poseStack.pushPose();

		// Position item above block center, with floating bob and spin
		float hover = (float) Math.sin(state.age * 0.1F) * 0.05F + 1.2F;
		poseStack.translate(0.5F, hover, 0.5F);
		float angle = state.age * 2.5F;
		poseStack.mulPose(Axis.YP.rotationDegrees(angle));

		// Render each layer via static ItemRenderer.renderItem() with MultiBufferSource
		// This correctly binds the item texture atlas (unlike SubmitNodeCollector.submitItem()
		// which renders white in the block entity render context in 1.21.11)
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

		for (int i = 0; i < activeCount && i < allLayers.length; i++) {
			ItemStackRenderState.LayerRenderState layer = allLayers[i];
			if (layer == null) continue;

			LayerRenderStateMixin layerMixin = (LayerRenderStateMixin)(Object) layer;
			RenderType renderType = layerMixin.immortality_getRenderType();
			if (renderType == null) continue;

			List<BakedQuad> quads = layer.prepareQuadList();
			if (quads.isEmpty()) continue;

			ItemStackRenderState.FoilType foilType = layerMixin.immortality_getFoilType();
			if (foilType == null) foilType = ItemStackRenderState.FoilType.NONE;

			int[] tints = layerMixin.immortality_getTintLayers();
			if (tints == null) tints = new int[]{ItemRenderer.NO_TINT};

			poseStack.pushPose();

			// Apply the per-layer display transform (defines orientation/scale for FIXED context)
			ItemTransform transform = layerMixin.immortality_getTransform();
			if (transform != null) {
				transform.apply(false, poseStack.last());
			}

			ItemRenderer.renderItem(
				ItemDisplayContext.FIXED,
				poseStack,
				bufferSource,
				LightTexture.FULL_BRIGHT,
				OverlayTexture.NO_OVERLAY,
				tints,
				quads,
				renderType,
				foilType
			);

			// Flush this render type's buffer immediately
			bufferSource.endBatch(renderType);

			poseStack.popPose();
		}

		poseStack.popPose();
	}
}
