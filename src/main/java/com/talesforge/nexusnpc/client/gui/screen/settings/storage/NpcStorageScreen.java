package com.talesforge.nexusnpc.client.gui.screen.settings.storage;

import com.talesforge.nexusnpc.client.gui.NpcEditingScreen;
import com.talesforge.nexusnpc.client.gui.ui.UiTheme;
import com.talesforge.nexusnpc.menu.NpcStorageMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The inventory-and-equipment window of an NPC, laid out like the vanilla player inventory: armor in a column on the
 * left, the mob in the middle (it follows the mouse), the two hands beside it, then the NPC's own inventory and under
 * it the player's. In the free space on the right: the NPC's name and health. Esc / the inventory key closes it and
 * returns to the editor.
 */
public class NpcStorageScreen extends AbstractContainerScreen<NpcStorageMenu> implements NpcEditingScreen {
    private static final int WIDTH = 176;
    private static final int PREVIEW_X = 26, PREVIEW_Y = 8, PREVIEW_W = 50, PREVIEW_H = 72;
    private static final int INFO_X = 102, INFO_W = 66;

    /** Set by the editor right before it asks the server to open the menu, so closing can return there. */
    @Nullable
    private static Screen pendingParent;
    public static void rememberParent(@Nullable Screen screen) { pendingParent = screen; }

    @Nullable
    private final Screen parent;

    public NpcStorageScreen(NpcStorageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.parent = pendingParent;
        pendingParent = null;
        this.imageWidth = WIDTH;
        this.imageHeight = menu.playerInventoryY() + 58 + 18 + 8;
        this.inventoryLabelY = menu.playerInventoryY() - 11;
    }

    @Override
    public void onClose() {
        super.onClose();
        if (parent != null) Minecraft.getInstance().setScreen(parent);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        UiTheme.drawPanel(g, leftPos, topPos, imageWidth, imageHeight);

        // The mob, in a dark box like the vanilla one
        int px = leftPos + PREVIEW_X, py = topPos + PREVIEW_Y;
        g.fill(px, py, px + PREVIEW_W, py + PREVIEW_H, UiTheme.PANEL_BORDER);
        g.fill(px + 1, py + 1, px + PREVIEW_W - 1, py + PREVIEW_H - 1, UiTheme.ROW_DISABLED);
        Mob npc = menu.npc();
        float byHeight = PREVIEW_H * 0.78F / Math.max(0.5F, npc.getBbHeight());
        float byWidth = PREVIEW_W * 0.80F / Math.max(0.4F, npc.getBbWidth());
        int scale = Math.max(8, (int) Math.min(byHeight, byWidth));
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, px, py, px + PREVIEW_W, py + PREVIEW_H, scale, 0.0625F,
                mouseX, mouseY, (LivingEntity) npc);

        // Slots: the NPC's are tinted, the player's are plain
        for (Slot slot : menu.slots) {
            int sx = leftPos + slot.x - 1;
            int sy = topPos + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, UiTheme.PANEL_BORDER);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, menu.isNpcSlot(slot.index) ? UiTheme.ROW_SELECTED : UiTheme.ROW_DISABLED);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Name (up to two lines) and health in the free space on the right of the preview
        Mob npc = menu.npc();
        List<FormattedCharSequence> name = font.split(npc.getDisplayName(), INFO_W);
        int y = 12;
        for (int i = 0; i < Math.min(2, name.size()); i++, y += 10) {
            g.drawString(font, name.get(i), INFO_X, y, UiTheme.TEXT, false);
        }
        g.drawString(font, Component.translatable("gui.nexusnpc.storage.health",
                Mth.ceil(npc.getHealth()), Mth.ceil(npc.getMaxHealth())), INFO_X, y + 4, UiTheme.TEXT_DIM, false);

        g.drawString(font, Component.translatable("gui.nexusnpc.storage.inventory"), 8, NpcStorageMenu.GRID_Y - 11, UiTheme.TEXT_DIM, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, UiTheme.TEXT_DIM, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        // An empty equipment slot may have no icon (the hands): name it
        if (hoveredSlot instanceof NpcStorageMenu.EquipSlot equip && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            g.renderTooltip(font, Component.translatable("gui.nexusnpc.slot." + equip.equipment.getName()), mouseX, mouseY);
        }
    }
}
