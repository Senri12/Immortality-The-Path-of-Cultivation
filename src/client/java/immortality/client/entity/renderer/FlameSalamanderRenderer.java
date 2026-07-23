package immortality.client.entity.renderer;

import immortality.Immortality;
import immortality.client.entity.model.FlameSalamanderModel;
import immortality.entity.FlameSalamanderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class FlameSalamanderRenderer extends MobRenderer<FlameSalamanderEntity, LivingEntityRenderState, FlameSalamanderModel> {
	private static final Identifier TEXTURE = Immortality.id("textures/entity/flame_salamander.png");

	public FlameSalamanderRenderer(EntityRendererProvider.Context context) {
		super(context, new FlameSalamanderModel(context.bakeLayer(FlameSalamanderModel.LAYER_LOCATION)), 0.6F);
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
