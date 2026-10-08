package com.talesforge.nexusnpc.menu;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.npc.Npcs;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, NexusNPC.MOD_ID);

    /**
     * The client builds its own copy of the menu. The factory reads the NPC's entity id,
     * which the server writes as "extra data" in ServerPayloadHandler#openTradeEditor.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<TradeEditMenu>> TRADE_EDIT =
            MENU_TYPES.register("trade_edit", () -> IMenuTypeExtension.create((containerId, inventory, buf) -> {
                Entity entity = inventory.player.level().getEntity(buf.readVarInt());
                if (!(entity instanceof Mob npc)) {
                    throw new IllegalStateException("Trade editor menu opened for a non-mob entity");
                }
                return new TradeEditMenu(containerId, inventory, npc);
            }));

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }

    private ModMenus() {}
}
