package com.talesforge.nexusnpc.npc.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * The backpack of an NPC that has no inventory of its own (everything except villagers, piglins and the like):
 * a plain container stored as a data attachment, so ANY mob can carry items. Slot positions are saved, so a shop
 * that was laid out neatly stays that way.
 */
public class NpcInventory extends SimpleContainer implements INBTSerializable<ListTag> {
    public static final int SIZE = 27;

    public NpcInventory() {
        super(SIZE);
    }

    @Override
    public ListTag serializeNBT(HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) continue;
            CompoundTag tag = new CompoundTag();
            tag.putByte("Slot", (byte) slot);
            list.add(stack.save(provider, tag));
        }
        return list;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, ListTag list) {
        clearContent();
        for (Tag entry : list) {
            if (!(entry instanceof CompoundTag tag)) continue;
            int slot = tag.getByte("Slot") & 255;
            if (slot < getContainerSize()) ItemStack.parse(provider, tag).ifPresent(stack -> setItem(slot, stack));
        }
    }
}
