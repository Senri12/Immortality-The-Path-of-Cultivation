package immortality.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class FloatingItemRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, FloatingItemRenderState> {
	private final ItemModelResolver itemModelResolver;
	private final java.util.function.Function<T, ItemStack> itemGetter;

	public FloatingItemRenderer(BlockEntityRendererProvider.Context context, java.util.function.Function<T, ItemStack> itemGetter) {
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

		ItemStack stack = this.itemGetter.apply(entity);
		this.itemModelResolver.updateForTopItem(
			state.itemState,
			stack,
			ItemDisplayContext.GROUND,
			entity.getLevel(),
			null,
			0
		);
	}

	@Override
	public void submit(FloatingItemRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
		if (state.itemState.isEmpty()) {
			return;
		}

		poseStack.pushPose();
		float hover = (float) Math.sin(state.age * 0.1F) * 0.05F + 1.2F;
		poseStack.translate(0.5F, hover, 0.5F);
		float angle = state.age * 2.5F;
		poseStack.mulPose(Axis.YP.rotationDegrees(angle));
		poseStack.scale(0.6F, 0.6F, 0.6F);

		state.itemState.submit(
			poseStack,
			collector,
			state.lightCoords,
			OverlayTexture.NO_OVERLAY,
			-1
		);

		poseStack.popPose();
	}
}
