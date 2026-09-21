package com.talesforge.masternpc.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.entity.custom.NpcEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class NpcRenderer extends MobRenderer<NpcEntity, NpcModel<NpcEntity>> {

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, new NpcModel<>(context.bakeLayer(NpcModel.LAYER_LOCATION)), 0.25f);
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity entity) {
        return entity.getSkinTexture();
    }

    @Override
    public void render(NpcEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if(entity.isBaby()) {
            poseStack.scale(0.45f, 0.45f, 0.45f);
        } else {
            poseStack.scale(1f, 1f, 1f);
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
