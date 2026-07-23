package immortality.client.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateMixin {
	@Accessor("layers")
	ItemStackRenderState.LayerRenderState[] immortality_getLayers();

	@Accessor("activeLayerCount")
	int immortality_getActiveLayerCount();
}
