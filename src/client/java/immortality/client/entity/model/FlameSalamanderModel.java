package immortality.client.entity.model;

import immortality.Immortality;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class FlameSalamanderModel extends EntityModel<LivingEntityRenderState> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Immortality.id("flame_salamander"), "main");

	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;

	public FlameSalamanderModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.rightFrontLeg = root.getChild("right_front_leg");
		this.leftFrontLeg = root.getChild("left_front_leg");
		this.rightHindLeg = root.getChild("right_hind_leg");
		this.leftHindLeg = root.getChild("left_hind_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-3.0F, -3.0F, -5.0F, 6.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)),
			PartPose.offset(0.0F, 19.0F, -6.0F)
		);

		root.addOrReplaceChild("body", CubeListBuilder.create()
			.texOffs(0, 16).addBox(-4.0F, -3.0F, -6.0F, 8.0F, 6.0F, 12.0F, new CubeDeformation(0.0F)),
			PartPose.offset(0.0F, 19.0F, 0.0F)
		);

		root.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
			.texOffs(36, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
			PartPose.offset(-4.0F, 21.0F, -4.0F)
		);

		root.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
			.texOffs(36, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
			PartPose.offset(4.0F, 21.0F, -4.0F)
		);

		root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create()
			.texOffs(36, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
			PartPose.offset(-4.0F, 21.0F, 4.0F)
		);

		root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create()
			.texOffs(36, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
			PartPose.offset(4.0F, 21.0F, 4.0F)
		);

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);
		float walkPos = state.walkAnimationPos;
		float walkSpeed = state.walkAnimationSpeed;

		this.head.xRot = state.xRot * ((float) Math.PI / 180.0F);
		this.head.yRot = state.yRot * ((float) Math.PI / 180.0F);

		this.rightFrontLeg.xRot = (float) Math.cos(walkPos * 0.6662F) * 1.4F * walkSpeed;
		this.leftFrontLeg.xRot = (float) Math.cos(walkPos * 0.6662F + (float) Math.PI) * 1.4F * walkSpeed;
		this.rightHindLeg.xRot = (float) Math.cos(walkPos * 0.6662F + (float) Math.PI) * 1.4F * walkSpeed;
		this.leftHindLeg.xRot = (float) Math.cos(walkPos * 0.6662F) * 1.4F * walkSpeed;
	}
}
