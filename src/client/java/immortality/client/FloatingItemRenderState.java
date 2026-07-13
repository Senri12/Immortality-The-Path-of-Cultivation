package immortality.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class FloatingItemRenderState extends BlockEntityRenderState {
	public final ItemStackRenderState itemState = new ItemStackRenderState();
	public float age;
}
