package com.talesforge.masternpc.npc;

import com.talesforge.masternpc.npc.attitude.NpcAttitudes;
import com.talesforge.masternpc.npc.behavior.NpcBehaviors;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record NpcSettings(String name, ResourceLocation attitude, ResourceLocation behavior,
                          String skin, double maxHealth, double damage, double speed) {

    public static final NpcSettings DEFAULT = new NpcSettings("", NpcAttitudes.DEFAULT_ID,
            NpcBehaviors.DEFAULT_ID, NpcSkins.DEFAULT, 20.0, 2.0, 0.25);

    // In 1.21.1, StreamCodec.composite supports a maximum of 6 fields; we have 7, so we write it manually
    public static final StreamCodec<FriendlyByteBuf, NpcSettings> STREAM_CODEC =
            StreamCodec.of(NpcSettings::write, NpcSettings::read);

    private static void write(FriendlyByteBuf buf, NpcSettings s) {
        buf.writeUtf(s.name, 32);
        buf.writeResourceLocation(s.attitude);
        buf.writeResourceLocation(s.behavior);
        buf.writeUtf(s.skin, 256);
        buf.writeDouble(s.maxHealth);
        buf.writeDouble(s.damage);
        buf.writeDouble(s.speed);
    }

    private static NpcSettings read(FriendlyByteBuf buf) {
        return new NpcSettings(buf.readUtf(32), buf.readResourceLocation(),
                buf.readResourceLocation(), buf.readUtf(256),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}