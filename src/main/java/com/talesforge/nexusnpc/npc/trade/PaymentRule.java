package com.talesforge.nexusnpc.npc.trade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * What an NPC accepts as ONE of the two payments of a trade, besides the item type and amount:
 * <ul>
 *   <li>{@code anyVariant} — false: the item must be identical to the one in the trade (same name, enchantments, mod data
 *       — everything except wear); true: any item of that type is accepted, whatever it carries.</li>
 *   <li>{@code perfectOnly} — false: wear is ignored; true: only an undamaged item is accepted. Exactly how worn an item
 *       is never matters: it is either perfect or it is not.</li>
 * </ul>
 */
public record PaymentRule(boolean anyVariant, boolean perfectOnly) {
    public static final PaymentRule DEFAULT = new PaymentRule(false, false);

    public static final Codec<PaymentRule> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("any_variant", false).forGetter(PaymentRule::anyVariant),
            Codec.BOOL.optionalFieldOf("perfect_only", false).forGetter(PaymentRule::perfectOnly)
    ).apply(i, PaymentRule::new));

    public static final StreamCodec<ByteBuf, PaymentRule> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PaymentRule::anyVariant,
            ByteBufCodecs.BOOL, PaymentRule::perfectOnly,
            PaymentRule::new);

    /** Does {@code offered} (what the player put in a payment slot) satisfy {@code template} (what the trade asks for)? */
    public boolean accepts(ItemStack offered, ItemStack template) {
        if (!offered.is(template.getItem()) || offered.getCount() < template.getCount()) return false;
        if (perfectOnly && offered.isDamaged()) return false;
        return anyVariant || sameDataIgnoringWear(offered, template);
    }

    /** Same item data component by component, except the wear (which has its own rule). Count is irrelevant. */
    static boolean sameDataIgnoringWear(ItemStack a, ItemStack b) {
        Set<DataComponentType<?>> keys = new HashSet<>(a.getComponents().keySet());
        keys.addAll(b.getComponents().keySet());
        for (DataComponentType<?> key : keys) {
            if (key == DataComponents.DAMAGE) continue;
            if (!Objects.equals(a.get(key), b.get(key))) return false;
        }
        return true;
    }
}
