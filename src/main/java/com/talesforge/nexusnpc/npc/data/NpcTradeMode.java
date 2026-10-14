package com.talesforge.nexusnpc.npc.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Where the goods of an NPC's trades come from. */
public enum NpcTradeMode implements StringRepresentable {
    /** The goods appear from nowhere and payment vanishes: an admin / server shop. */
    INFINITE("infinite"),
    /** Goods are taken from the NPC's inventory and payment is put into it: a real shopkeeper with a stock. */
    STOCK("stock");

    public static final Codec<NpcTradeMode> CODEC = StringRepresentable.fromEnum(NpcTradeMode::values);

    private final String serializedName;

    NpcTradeMode(String serializedName) { this.serializedName = serializedName; }

    @Override
    public String getSerializedName() { return serializedName; }
}
