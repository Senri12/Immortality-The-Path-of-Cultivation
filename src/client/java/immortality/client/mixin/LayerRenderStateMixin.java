package immortality.client.mixin;

import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public interface LayerRenderStateMixin {
	@Accessor("renderType")
	RenderType immortality_getRenderType();

	@Accessor("foilType")
	ItemStackRenderState.FoilType immortality_getFoilType();

	@Accessor("tintLayers")
	int[] immortality_getTintLayers();

	@Accessor("transform")
	ItemTransform immortality_getTransform();
}
