package com.talesforge.nexusnpc.menu;

import com.mojang.datafixers.util.Pair;
import com.talesforge.nexusnpc.npc.runtime.NpcEditing;
import com.talesforge.nexusnpc.npc.storage.MobEquipmentContainer;
import com.talesforge.nexusnpc.npc.storage.NpcStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Everything an NPC carries, in one window laid out like the vanilla player inventory: a column of armor slots and
 * the two hands around a preview of the mob on top, the NPC's inventory under them (for a shopkeeper this is the stock
 * and the takings), the player's own inventory at the bottom. The slots are real: what you put in really moves into the
 * NPC and what you take really leaves it. Works for any mob. While it is open the NPC is frozen by the editing lock,
 * exactly like in the trade editor.
 * <p>
 * Slot order: 0-5 equipment (see {@link MobEquipmentContainer#SLOTS}), then the NPC's inventory, then the player's.
 */
public class NpcStorageMenu extends AbstractContainerMenu {
    public static final int EQUIPMENT_SLOTS = MobEquipmentContainer.SLOTS.length;
    private static final int ARMOR_SLOTS = 4;
    /** Top of the NPC's inventory grid. */
    public static final int GRID_Y = 96;

    private final Mob npc;
    private final int inventorySlots;
    private final int playerInventoryY;
    @Nullable private final ServerPlayer serverPlayer;

    public NpcStorageMenu(int containerId, Inventory playerInv, Mob npc) {
        super(ModMenus.NPC_STORAGE.get(), containerId);
        this.npc = npc;
        this.serverPlayer = playerInv.player instanceof ServerPlayer sp ? sp : null;
        this.inventorySlots = NpcStorage.inventorySize(npc);

        // The server edits the real things; the client gets same-sized placeholders that slot syncing fills in
        Container equipment = serverPlayer != null ? new MobEquipmentContainer(npc) : new SimpleContainer(EQUIPMENT_SLOTS);
        Container inventory = serverPlayer != null ? NpcStorage.inventory(npc) : new SimpleContainer(inventorySlots);

        // --- like the vanilla inventory: armor in a column, the hands under the right edge of the preview ---
        for (int i = 0; i < ARMOR_SLOTS; i++) addSlot(new EquipSlot(equipment, i, 8, 8 + i * 18, npc));
        addSlot(new EquipSlot(equipment, 4, 80, 44, npc));   // main hand
        addSlot(new EquipSlot(equipment, 5, 80, 62, npc));   // off hand

        for (int i = 0; i < inventorySlots; i++) {
            addSlot(new Slot(inventory, i, 8 + (i % 9) * 18, GRID_Y + (i / 9) * 18));
        }

        int rows = (inventorySlots + 8) / 9;
        this.playerInventoryY = GRID_Y + rows * 18 + 14;
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(playerInv, 9 + r * 9 + c, 8 + c * 18, playerInventoryY + r * 18));
        for (int c = 0; c < 9; c++)
            addSlot(new Slot(playerInv, c, 8 + c * 18, playerInventoryY + 58));
    }

    public Mob npc() { return npc; }

    public int inventorySlots() { return inventorySlots; }

    public int playerInventoryY() { return playerInventoryY; }

    /** True for slots that belong to the NPC (equipment and its inventory), false for the player's own. */
    public boolean isNpcSlot(int index) { return index < EQUIPMENT_SLOTS + inventorySlots; }

    /**
     * Shift-click. From the NPC to the player's inventory; from the player's inventory first into a matching ARMOR slot
     * (never into a hand: that would swallow every shift-clicked item), then into the NPC's inventory.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int npcEnd = EQUIPMENT_SLOTS + inventorySlots;

        boolean moved;
        if (index < npcEnd) {
            moved = moveItemStackTo(stack, npcEnd, slots.size(), true);
        } else {
            moved = moveItemStackTo(stack, 0, ARMOR_SLOTS, false)
                    || moveItemStackTo(stack, EQUIPMENT_SLOTS, npcEnd, false);
        }
        if (!moved) return ItemStack.EMPTY;

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }

    /** Runs every server tick while open: keeps the NPC frozen (same editing lock as the trade editor). */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (serverPlayer == null) return;
        if (NpcEditing.isEditedBy(npc, serverPlayer)) NpcEditing.ping(npc);
        else if (!NpcEditing.tryStart(npc, serverPlayer)) serverPlayer.closeContainer();  // Someone else took it
    }

    @Override
    public boolean stillValid(Player player) {
        return npc.isAlive() && player.distanceToSqr(npc) <= NpcEditing.MAX_DIST_SQ;
    }

    /** An equipment slot: one item, armor slots only take what can really be worn there, and an empty one shows the vanilla icon. */
    public static final class EquipSlot extends Slot {
        private static final ResourceLocation[] ICONS = {
                InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
                InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
                null, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD
        };

        public final EquipmentSlot equipment;
        private final int slotIndex;
        private final Mob mob;

        EquipSlot(Container container, int index, int x, int y, Mob mob) {
            super(container, index, x, y);
            this.slotIndex = index;
            this.equipment = MobEquipmentContainer.SLOTS[index];
            this.mob = mob;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (equipment.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) return true;   // Anything fits in a hand
            return stack.canEquip(equipment, mob);
        }

        @Override public int getMaxStackSize() { return 1; }

        @Nullable
        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            ResourceLocation icon = ICONS[slotIndex];
            return icon == null ? null : Pair.of(InventoryMenu.BLOCK_ATLAS, icon);
        }
    }
}
