package immortality.client.entity.renderer;

import immortality.Immortality;
import immortality.client.entity.model.TribulationLordModel;
import immortality.entity.TribulationLordEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class TribulationLordRenderer extends MobRenderer<TribulationLordEntity, LivingEntityRenderState, TribulationLordModel> {
	private static final Identifier TEXTURE = Immortality.id("textures/entity/tribulation_lord.png");

	public TribulationLordRenderer(EntityRendererProvider.Context context) {
		super(context, new TribulationLordModel(context.bakeLayer(TribulationLordModel.LAYER_LOCATION)), 1.2F);
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
