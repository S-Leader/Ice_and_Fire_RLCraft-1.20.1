package com.github.alexthe666.iceandfire.client.model.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class ModelSilverArmor extends ArmorModelBase {
    private static final float SILVER_INNER_MODEL_OFFSET = 0.2F;
    private static final float SILVER_OUTER_MODEL_OFFSET = 0.5F;
    private static final ModelPart INNER_MODEL = createMesh(new CubeDeformation(SILVER_INNER_MODEL_OFFSET), 0.0F).getRoot().bake(64, 64);
    private static final ModelPart OUTER_MODEL = createMesh(new CubeDeformation(SILVER_OUTER_MODEL_OFFSET), 0.0F).getRoot().bake(64, 64);

    private final ModelPart robeLower;
    private final ModelPart robeLowerBack;

    public ModelSilverArmor(boolean inner) {
        super(getBakedModel(inner));
        this.robeLower = this.body.getChild("robeLower");
        this.robeLowerBack = this.body.getChild("robeLowerBack");
    }

    public static MeshDefinition createMesh(CubeDeformation deformation, float offset) {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(deformation, offset);
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.getChild("head").addOrReplaceChild("faceGuard", CubeListBuilder.create().texOffs(30, 47).addBox(-4.5F, -3.0F, -6.1F, 9, 9, 8, deformation), PartPose.offset(0.0F, -5.1F, 1.9F));
        partdefinition.getChild("head").addOrReplaceChild("helmWingR", CubeListBuilder.create().texOffs(2, 37).addBox(-0.5F, -1.0F, 0.0F, 1, 4, 6, deformation), PartPose.offsetAndRotation(-3.0F, -6.3F, 1.3F, 0.5235987755982988F, -0.4363323129985824F, -0.05235987755982988F));
        partdefinition.getChild("head").addOrReplaceChild("helmWingL", CubeListBuilder.create().texOffs(2, 37).mirror().addBox(-0.5F, -1.0F, 0.0F, 1, 4, 6, deformation), PartPose.offsetAndRotation(3.0F, -6.3F, 1.3F, 0.5235987755982988F, 0.4363323129985824F, 0.05235987755982988F));

        partdefinition.getChild("hat").addOrReplaceChild("crest", CubeListBuilder.create().texOffs(18, 32).addBox(0.0F, -0.5F, 0.0F, 1, 9, 9, CubeDeformation.NONE), PartPose.offsetAndRotation(0.0F, -7.9F, -0.1F, 1.2292353921796064F, 0.0F, 0.0F));

        partdefinition.getChild("body").addOrReplaceChild("robeLowerBack", CubeListBuilder.create().texOffs(4, 55).mirror().addBox(-4.0F, 0.0F, -2.5F, 8, 8, 1, deformation), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.0F, (float) Math.PI, 0.0F));
        partdefinition.getChild("body").addOrReplaceChild("robeLower", CubeListBuilder.create().texOffs(4, 55).addBox(-4.0F, 0.0F, -2.5F, 8, 8, 1, deformation), PartPose.offset(0.0F, 12.0F, 0.0F));
        return meshdefinition;
    }

    @Override
    public void setupAnim(@NotNull LivingEntity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        this.robeLower.z = this.crouching ? -1.0F : 0.0F;
        this.robeLower.y = 12.0F;
        this.robeLower.xRot = Math.min(0.0F, Math.min(this.leftLeg.xRot, this.rightLeg.xRot)) - this.body.xRot;
        this.robeLowerBack.xRot = -Math.max(this.leftLeg.xRot, this.rightLeg.xRot);

        if (this.crouching) {
            this.robeLowerBack.xRot /= 4.0F;
        } else {
            this.robeLower.xRot /= 3.0F;
            this.robeLowerBack.xRot /= 3.0F;
        }
    }

    public static ModelPart getBakedModel(boolean inner) {
        return inner ? INNER_MODEL : OUTER_MODEL;
    }

}
