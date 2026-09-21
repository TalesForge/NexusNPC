package com.talesforge.masternpc.api.event;

import com.talesforge.masternpc.entity.custom.NpcEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class NpcInteractEvent extends Event implements ICancellableEvent {
    private final NpcEntity npc;
    private final Player player;
    private final InteractionHand hand;

    public NpcInteractEvent(NpcEntity npc, Player player, InteractionHand hand) {
        this.npc = npc; this.player = player; this.hand = hand;
    }
    public NpcEntity getNpc() { return npc; }
    public Player getPlayer() { return player; }
    public InteractionHand getHand() { return hand; }
}
