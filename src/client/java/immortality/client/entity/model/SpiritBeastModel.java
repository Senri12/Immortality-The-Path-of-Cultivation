package immortality.client.entity.model;

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
import immortality.Immortality;

public class SpiritBeastModel extends EntityModel<LivingEntityRenderState> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Immortality.id("spirit_beast"), "main");

	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart tail;

	public SpiritBeastModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.rightFrontLeg = root.getChild("right_front_leg");
		this.leftFrontLeg = root.getChild("left_front_leg");
		this.rightHindLeg = root.getChild("right_hind_leg");
		this.leftHindLeg = root.getChild("left_hind_leg");
		this.tail = root.getChild("tail");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
			.texOffs(0, 16).addBox(-2.0F, 0.0F, -9.0F, 4.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
			.texOffs(24, 0).addBox(-3.0F, -8.0F, -4.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
			.texOffs(24, 0).addBox(1.0F, -8.0F, -4.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
			PartPose.offset(0.0F, 12.0F, -6.0F)
		);

		partdefinition.addOrReplaceChild("body", CubeListBuilder.create()
			.texOffs(0, 24).addBox(-5.0F, -4.0F, -8.0F, 10.0F, 9.0F, 16.0F, new CubeDeformation(0.0F)),
			PartPose.offset(0.0F, 13.0F, 0.0F)
		);

		partdefinition.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
			.texOffs(36, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
			PartPose.offset(-3.5F, 16.0F, -6.0F)
		);

		partdefinition.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
			.texOffs(36, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
			PartPose.offset(3.5F, 16.0F, -6.0F)
		);

		partdefinition.addOrReplaceChild("right_hind_leg", CubeListBuilder.create()
			.texOffs(36, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
			PartPose.offset(-3.5F, 16.0F, 6.0F)
		);

		partdefinition.addOrReplaceChild("left_hind_leg", CubeListBuilder.create()
			.texOffs(36, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
			PartPose.offset(3.5F, 16.0F, 6.0F)
		);

		partdefinition.addOrReplaceChild("tail", CubeListBuilder.create()
			.texOffs(0, 49).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 10.0F, new CubeDeformation(0.0F)),
			PartPose.offset(0.0F, 10.0F, 8.0F)
		);

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);
		float walkPos = state.walkAnimationPos;
		float walkSpeed = state.walkAnimationSpeed;

		this.rightFrontLeg.xRot = (float) Math.cos(walkPos * 0.6662F) * 1.4F * walkSpeed;
		this.leftFrontLeg.xRot = (float) Math.cos(walkPos * 0.6662F + (float) Math.PI) * 1.4F * walkSpeed;
		this.rightHindLeg.xRot = (float) Math.cos(walkPos * 0.6662F + (float) Math.PI) * 1.4F * walkSpeed;
		this.leftHindLeg.xRot = (float) Math.cos(walkPos * 0.6662F) * 1.4F * walkSpeed;
		this.tail.yRot = (float) Math.cos(walkPos * 0.6662F) * 0.5F * walkSpeed;
	}
}
