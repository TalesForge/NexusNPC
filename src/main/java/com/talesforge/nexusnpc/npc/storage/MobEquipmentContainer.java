package com.talesforge.nexusnpc.npc.storage;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A live view of a mob's own equipment slots as a Container, so an ordinary menu can edit them. Nothing is copied:
 * every read and write goes to the mob, which saves and syncs its equipment by itself (that is vanilla's job and
 * works for every mob type).
 */
public final class MobEquipmentContainer implements Container {
    /** The order of the slots in the menu. */
    public static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    private final Mob mob;

    public MobEquipmentContainer(Mob mob) { this.mob = mob; }

    @Override public int getContainerSize() { return SLOTS.length; }

    @Override
    public boolean isEmpty() {
        for (EquipmentSlot slot : SLOTS) if (!mob.getItemBySlot(slot).isEmpty()) return false;
        return true;
    }

    @Override public ItemStack getItem(int index) { return mob.getItemBySlot(SLOTS[index]); }

    @Override
    public ItemStack removeItem(int index, int amount) {
        ItemStack stack = getItem(index);
        if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack taken = stack.split(amount);
        mob.setItemSlot(SLOTS[index], stack);
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = getItem(index);
        mob.setItemSlot(SLOTS[index], ItemStack.EMPTY);
        return stack;
    }

    @Override public void setItem(int index, ItemStack stack) { mob.setItemSlot(SLOTS[index], stack); }

    /** One item per slot: a stack of swords in a hand makes no sense. */
    @Override public int getMaxStackSize() { return 1; }

    @Override public void setChanged() {}

    @Override public boolean stillValid(Player player) { return mob.isAlive(); }

    @Override
    public void clearContent() {
        for (EquipmentSlot slot : SLOTS) mob.setItemSlot(slot, ItemStack.EMPTY);
    }
}
