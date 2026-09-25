package com.talesforge.masternpc.event;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.api.MasterNpcApi;
import com.talesforge.masternpc.api.NpcTypeEntry;
import com.talesforge.masternpc.entity.custom.NpcEntity;
import com.talesforge.masternpc.npc.NpcRegistries;
import com.talesforge.masternpc.npc.model.NpcModelType;
import net.minecraft.world.entity.EntityDimensions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;

@EventBusSubscriber(modid = MasterNPC.MOD_ID)
public class ModEventBusEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        for (NpcTypeEntry entry : MasterNpcApi.types()) {
            event.put(entry.type().get(), entry.attributes().get().build());
        }
    }

    /**
     * LivingEntity.getDimensions(Pose)/getEyeHeight(Pose, EntityDimensions) are FINAL in
     * 1.21.1 — NeoForge computes size itself and fires this event so mods can override the
     * result, instead of a protected method being the override point. Fired on
     * NeoForge.EVENT_BUS (the game bus), including once inside Entity's own constructor —
     * safe here because NpcEntity#getModelId() only reads synced entity data, which is
     * already defined with its default value by the time any dimension is ever computed.
     */
    @SubscribeEvent
    public static void onSize(EntityEvent.Size event) {
        if (event.getEntity() instanceof NpcEntity npc) {
            NpcModelType model = NpcRegistries.model(npc.getModelId());
            event.setNewSize(EntityDimensions.scalable(model.width(), model.height()), false);
            event.setNewEyeHeight(model.eyeHeight());
        }
    }
}
