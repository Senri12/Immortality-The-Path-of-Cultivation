package immortality.client.entity.renderer;

import immortality.Immortality;
import immortality.client.entity.model.SpiritBeastModel;
import immortality.entity.SpiritBeastEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class SpiritBeastRenderer extends MobRenderer<SpiritBeastEntity, LivingEntityRenderState, SpiritBeastModel> {
	private static final Identifier TEXTURE = Immortality.id("textures/entity/spirit_beast.png");

	public SpiritBeastRenderer(EntityRendererProvider.Context context) {
		super(context, new SpiritBeastModel(context.bakeLayer(SpiritBeastModel.LAYER_LOCATION)), 0.6F);
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}
}
