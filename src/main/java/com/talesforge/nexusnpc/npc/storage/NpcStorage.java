package com.talesforge.nexusnpc.npc.storage;

import com.talesforge.nexusnpc.npc.Npcs;
import com.talesforge.nexusnpc.npc.quest.ModAttachments;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

/**
 * Everything an NPC carries, for ANY mob.
 * <ul>
 *   <li><b>Equipment</b> — the mob's own vanilla slots (hands, armor). Every mob has them; vanilla saves and syncs them.</li>
 *   <li><b>Inventory</b> — a mob that already has one (villager, piglin: {@link InventoryCarrier}) uses its own,
 *       otherwise a {@link NpcInventory} data attachment gives it 27 slots. Shop stock and takings live here.</li>
 * </ul>
 */
public final class NpcStorage {
    private NpcStorage() {}

    /** The mob's inventory. Creates the attachment on first use for mobs that have none of their own. */
    public static SimpleContainer inventory(Mob mob) {
        if (mob instanceof InventoryCarrier carrier) return carrier.getInventory();
        return mob.getData(ModAttachments.NPC_INVENTORY);
    }

    /** True if looking at the inventory would not create anything. */
    public static boolean hasInventory(Mob mob) {
        return mob instanceof InventoryCarrier || mob.hasData(ModAttachments.NPC_INVENTORY);
    }

    /** Safe on the client too (no attachment needed): how many slots {@link #inventory} has. */
    public static int inventorySize(Mob mob) {
        return mob instanceof InventoryCarrier carrier ? carrier.getInventory().getContainerSize() : NpcInventory.SIZE;
    }

    /** How many items identical to {@code prototype} (same item, same components) the container holds. */
    public static int count(Container container, ItemStack prototype) {
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, prototype)) total += stack.getCount();
        }
        return total;
    }

    /** Removes up to {@code amount} items identical to {@code prototype}; returns how many were actually removed. */
    public static int remove(Container container, ItemStack prototype, int amount) {
        int left = amount;
        for (int i = 0; i < container.getContainerSize() && left > 0; i++) {
            ItemStack stack = container.getItem(i);
            if (!ItemStack.isSameItemSameComponents(stack, prototype)) continue;
            int take = Math.min(left, stack.getCount());
            stack.shrink(take);
            if (stack.isEmpty()) container.setItem(i, ItemStack.EMPTY); else container.setChanged();
            left -= take;
        }
        return amount - left;
    }

    /**
     * An NPC does not lose its gear by default. Vanilla drops each equipment slot with its own chance (8.5% for
     * gear a mob spawned with), so the chances are pinned: 0 = nothing falls, 2 = everything falls, undamaged
     * (vanilla's "always" value). Call whenever the "drop items" switch changes or the mob becomes an NPC.
     */
    public static void applyDropPolicy(Mob mob) {
        float chance = Npcs.dropItems(mob) ? 2.0F : 0.0F;
        for (EquipmentSlot slot : EquipmentSlot.values()) mob.setDropChance(slot, chance);
    }

    /** Moves the whole inventory into drops (only if the mob has one: nothing is created). */
    public static void collectInventoryDrops(Mob mob, Collection<ItemEntity> drops) {
        if (!hasInventory(mob)) return;
        SimpleContainer inventory = inventory(mob);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.removeItemNoUpdate(i);
            if (stack.isEmpty()) continue;
            ItemEntity entity = new ItemEntity(mob.level(), mob.getX(), mob.getY() + 0.5, mob.getZ(), stack);
            entity.setDefaultPickUpDelay();
            drops.add(entity);
        }
    }
}
