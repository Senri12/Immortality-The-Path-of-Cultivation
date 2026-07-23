package immortality.client.entity.renderer;

import immortality.Immortality;
import immortality.client.entity.model.FrostFoxModel;
import immortality.entity.FrostFoxEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class FrostFoxRenderer extends MobRenderer<FrostFoxEntity, LivingEntityRenderState, FrostFoxModel> {
	private static final Identifier TEXTURE = Immortality.id("textures/entity/frost_fox.png");

	public FrostFoxRenderer(EntityRendererProvider.Context context) {
		super(context, new FrostFoxModel(context.bakeLayer(FrostFoxModel.LAYER_LOCATION)), 0.5F);
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
