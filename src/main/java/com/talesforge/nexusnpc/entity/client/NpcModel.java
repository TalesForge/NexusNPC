package com.talesforge.nexusnpc.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.entity.custom.NpcEntity;
import com.talesforge.nexusnpc.npc.NpcRegistries;
import com.talesforge.nexusnpc.npc.model.HeightProfile;
import com.talesforge.nexusnpc.npc.model.NpcModelType;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class NpcModel<T extends NpcEntity> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "npc"), "main");

    private final ModelPart body;
    private final ModelPart legL;
    private final ModelPart legR;
    private final ModelPart armR;
    private final ModelPart armL;
    private final ModelPart head;

    public NpcModel(ModelPart root) {
        this.body = root.getChild("Body");
        this.legL = this.body.getChild("LegL");
        this.legR = this.body.getChild("LegR");
        this.armR = this.body.getChild("ArmR");
        this.armL = this.body.getChild("ArmL");
        this.head = this.body.getChild("Head");
    }

    // ---- Height: which rows of each 12 px part may be repeated (see HeightProfile) ----
    // A part is cut into [kept top rows][band that is repeated / cropped][kept bottom rows]. The bands sit in the
    // plain middle of each part, away from collars, belts, hems and shoes, so repeating them is the least visible.
    private static final int TOP_ROWS = 3;
    private static final int BAND_ROWS = 6;
    private static final int PART_ROWS = 12;

    public static final ModelLayerLocation LAYER_LOCATION_SLIM =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "npc_slim"), "main");

    /** The normal-height geometry (Standard arms). */
    public static LayerDefinition createBodyLayer() {
        return createBodyLayer(HeightProfile.NONE, false);
    }

    /** Standard (4 px) arms at the given height. */
    public static LayerDefinition createBodyLayer(HeightProfile profile) {
        return createBodyLayer(profile, false);
    }

    /**
     * The geometry for an NPC whose limbs and torso are {@code profile} pixels longer than normal. Nothing is scaled:
     * the head, hands and feet are untouched, and the extra length is filled by repeating a band of the texture,
     * so texels stay square and the same size at every height.
     */
    public static LayerDefinition createBodyLayer(HeightProfile profile, boolean slim) {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        float legLength = PART_ROWS + profile.legs();
        float torsoLength = PART_ROWS + profile.torso();
        float hipY = -legLength;                      // The body part's origin is the ground
        float shoulderY = -(legLength + torsoLength);

        PartDefinition Body = partdefinition.addOrReplaceChild("Body",
                sliced(CubeListBuilder.create(), 16, 16, -4.0F, shoulderY, -2.0F, 8.0F, 4.0F, profile.torso()),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        Body.addOrReplaceChild("LegL",
                sliced(CubeListBuilder.create(), 16, 48, -2.0F, 0.0F, -2.0F, 4.0F, 4.0F, profile.legs()),
                PartPose.offset(2.0F, hipY, 0.0F));
        Body.addOrReplaceChild("LegR",
                sliced(CubeListBuilder.create(), 0, 16, -2.0F, 0.0F, -2.0F, 4.0F, 4.0F, profile.legs()),
                PartPose.offset(-2.0F, hipY, 0.0F));
        // Slim arms are 3 px wide instead of 4. The shoulder pivots stay where they are and the arm keeps its inner edge
        // flush with the torso, so only the outer edge moves in. (The texture layout differs too: see the slim skins.)
        float armWidth = slim ? 3.0F : 4.0F;
        float armRightX = slim ? -2.0F : -3.0F;
        Body.addOrReplaceChild("ArmR",
                sliced(CubeListBuilder.create(), 40, 16, armRightX, -2.0F, -2.0F, armWidth, 4.0F, profile.arms()),
                PartPose.offset(-5.0F, shoulderY + 2.0F, 0.0F));
        Body.addOrReplaceChild("ArmL",
                sliced(CubeListBuilder.create(), 32, 48, -1.0F, -2.0F, -2.0F, armWidth, 4.0F, profile.arms()),
                PartPose.offset(5.0F, shoulderY + 2.0F, 0.0F));
        Body.addOrReplaceChild("Head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, shoulderY, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    /**
     * Adds one 12 px tall box as three stacked cubes: the top rows, a band of {@link #BAND_ROWS} rows that is repeated
     * (or cropped) to the wanted length, and the bottom rows. Each cube's texture offset is moved so that it reads
     * exactly the texture rows it stands for; the caps that end up inside the part are never visible.
     * The top cube keeps the original offset, so the visible top cap (shoulders) is always the right one.
     *
     * @param u,v   texture offset of the ORIGINAL box
     * @param y     where the box starts (its top)
     * @param extra pixels to add to the band (negative removes rows from it)
     */
    private static CubeListBuilder sliced(CubeListBuilder builder, int u, int v, float x, float y, float z,
                                          float width, float depth, float extra) {
        int bottomRows = PART_ROWS - TOP_ROWS - BAND_ROWS;
        float band = Math.max(1.0F, BAND_ROWS + extra);   // Never collapse the band completely

        builder.texOffs(u, v).addBox(x, y, z, width, TOP_ROWS, depth);
        float cursor = y + TOP_ROWS;

        float remaining = band;
        while (remaining > 0.001F) {
            float tile = Math.min(remaining, BAND_ROWS);
            builder.texOffs(u, v + TOP_ROWS).addBox(x, cursor, z, width, tile, depth);
            cursor += tile;
            remaining -= tile;
        }

        builder.texOffs(u, v + TOP_ROWS + BAND_ROWS).addBox(x, cursor, z, width, bottomRows, depth);
        return builder;
    }

    @Override
    public void setupAnim(NpcEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.applyHeadRotation(netHeadYaw, headPitch);

        // Longer legs cover more ground per swing: slow the cycle down by the same factor or the feet would slide
        NpcModelType type = NpcRegistries.model(entity.getModelId());
        float legRatio = (PART_ROWS + HeightProfile.of(type, entity.getNpcHeight()).legs()) / PART_ROWS;
        this.animateWalk(NpcAnimations.ANIM_NPC_WALK, limbSwing, limbSwingAmount, 2f / Math.max(0.5f, legRatio), 2.5f);
        this.animate(entity.idleAnimationState, NpcAnimations.ANIM_NPC_IDLE, ageInTicks, 1f);
    }

    private void applyHeadRotation(float headYaw, float headPitch) {
        headYaw = Mth.clamp(headYaw, -30f, 30f);
        headPitch = Mth.clamp(headPitch, -25f, 45f);

        this.head.yRot = headYaw * ((float)Math.PI / 180f);
        this.head.xRot = headPitch * ((float)Math.PI / 180f);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    @Override
    public ModelPart root() {
        return body;
    }
}
