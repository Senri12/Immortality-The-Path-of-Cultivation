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

public class TribulationLordModel extends EntityModel<LivingEntityRenderState> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Immortality.id("tribulation_lord"), "main");

	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public TribulationLordModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
			.texOffs(24, 0).addBox(-1.0F, -12.0F, -2.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
			PartPose.offset(0.0F, 0.0F, 0.0F)
		);

		root.addOrReplaceChild("body", CubeListBuilder.create()
			.texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
			PartPose.offset(0.0F, 0.0F, 0.0F)
		);

		root.addOrReplaceChild("right_arm", CubeListBuilder.create()
			.texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
			PartPose.offset(-5.0F, 2.0F, 0.0F)
		);

		root.addOrReplaceChild("left_arm", CubeListBuilder.create()
			.texOffs(40, 16).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
			PartPose.offset(5.0F, 2.0F, 0.0F)
		);

		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
			.texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
			PartPose.offset(-1.9F, 12.0F, 0.0F)
		);

		root.addOrReplaceChild("left_leg", CubeListBuilder.create()
			.texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
			PartPose.offset(1.9F, 12.0F, 0.0F)
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

		this.rightArm.xRot = (float) Math.cos(walkPos * 0.6662F + (float) Math.PI) * 1.4F * walkSpeed;
		this.leftArm.xRot = (float) Math.cos(walkPos * 0.6662F) * 1.4F * walkSpeed;
		this.rightLeg.xRot = (float) Math.cos(walkPos * 0.6662F) * 1.4F * walkSpeed;
		this.leftLeg.xRot = (float) Math.cos(walkPos * 0.6662F + (float) Math.PI) * 1.4F * walkSpeed;
	}
}
