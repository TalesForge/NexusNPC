package com.talesforge.nexusnpc.api.event;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class NpcInteractEvent extends Event implements ICancellableEvent {
    private final Mob npc;
    private final Player player;
    private final InteractionHand hand;

    public NpcInteractEvent(Mob npc, Player player, InteractionHand hand) {
        this.npc = npc; this.player = player; this.hand = hand;
    }
    public Mob getNpc() { return npc; }
    public Player getPlayer() { return player; }
    public InteractionHand getHand() { return hand; }
}
