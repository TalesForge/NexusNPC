package com.talesforge.nexusnpc.npc.trade;

import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.List;
import java.util.Optional;

/**
 * The offer an NPC shows and settles. It differs from a vanilla one in two ways:
 * <ul>
 *   <li><b>It decides itself what a payment is.</b> Vanilla compares the offered stack with a fixed cost; here each
 *       payment follows its {@link PaymentRule} (exact data or any variant, wear ignored or undamaged only). The menu asks
 *       {@link #satisfiedBy} for every decision, so overriding it is enough; the cost sent to the client only decides
 *       how the trade is DISPLAYED.</li>
 *   <li><b>It remembers WHAT the player actually paid.</b> The menu takes the payment out of the player's slots and only
 *       then tells the merchant. A shopkeeper that keeps its takings must keep the items exactly as handed over.</li>
 * </ul>
 */
public final class NpcMerchantOffer extends MerchantOffer {
    private final ItemStack template1;
    private final ItemStack template2;
    private final PaymentRule rule1;
    private final PaymentRule rule2;
    private List<ItemStack> lastPayment = List.of();

    public NpcMerchantOffer(TradeOffer trade, int maxUses) {
        super(displayCost(trade.cost1(), trade.rule1()),
                trade.cost2().isEmpty() ? Optional.empty() : Optional.of(displayCost(trade.cost2(), trade.rule2())),
                trade.result(), maxUses, 0, 0.0F);
        this.template1 = trade.cost1();
        this.template2 = trade.cost2();
        this.rule1 = trade.rule1();
        this.rule2 = trade.rule2();
    }

    /** The cost as the trade window shows it: the exact item with its data, or just the plain item for "any variant". */
    private static ItemCost displayCost(ItemStack stack, PaymentRule rule) {
        if (rule.anyVariant()) {
            return new ItemCost(stack.getItemHolder(), stack.getCount(), DataComponentPredicate.EMPTY);
        }
        ItemStack clean = stack.copy();
        clean.remove(DataComponents.DAMAGE);   // Wear has its own rule: never part of "exactly this item"
        return new ItemCost(clean.getItemHolder(), clean.getCount(), DataComponentPredicate.allOf(clean.getComponents()));
    }

    @Override
    public boolean satisfiedBy(ItemStack playerA, ItemStack playerB) {
        if (!rule1.accepts(playerA, template1)) return false;
        return template2.isEmpty() ? playerB.isEmpty() : rule2.accepts(playerB, template2);
    }

    @Override
    public boolean take(ItemStack playerA, ItemStack playerB) {
        if (!satisfiedBy(playerA, playerB)) return false;
        // Copy BEFORE super shrinks the player's stacks: these are the real items, components and all
        ItemStack paidA = playerA.copyWithCount(template1.getCount());
        ItemStack paidB = template2.isEmpty() ? ItemStack.EMPTY : playerB.copyWithCount(template2.getCount());
        if (!super.take(playerA, playerB)) return false;
        lastPayment = List.of(paidA, paidB);
        return true;
    }

    /** The items paid in the last successful trade (then forgotten). */
    public List<ItemStack> takePayment() {
        List<ItemStack> payment = lastPayment;
        lastPayment = List.of();
        return payment;
    }
}
