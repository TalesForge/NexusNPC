package com.talesforge.nexusnpc.client.gui.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A vertically scrolling container for ordinary widgets (buttons, edit boxes, sliders...).
 * <p>
 * Children are added with their Y relative to the TOP OF THE CONTENT (0 = first row) and their X in
 * absolute screen coordinates (the panel never scrolls sideways). On every frame the panel moves each
 * child to its scrolled position and draws it clipped to the panel. Events are forwarded only to the
 * child under the mouse / the focused child, and a child scrolled out of view cannot be clicked or hovered.
 * <p>
 * This is what lets every screen fit any window size or GUI scale: content that does not fit simply scrolls.
 */
public class ScrollPanel extends AbstractWidget {
    private static final int BAR_WIDTH = 4;
    private static final int SCROLL_STEP = 14;

    private final List<AbstractWidget> children = new ArrayList<>();
    private final Map<AbstractWidget, Integer> offsets = new IdentityHashMap<>();
    private int contentHeight = 0;
    private double scroll = 0;

    @Nullable private AbstractWidget focusedChild;
    @Nullable private AbstractWidget pressedChild;
    private boolean draggingBar = false;
    private boolean blurPending = false;

    public ScrollPanel(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    // ================= content =================

    /** Add a widget whose current Y is relative to the top of the content. */
    public <T extends AbstractWidget> T add(T widget) {
        children.add(widget);
        offsets.put(widget, widget.getY());
        contentHeight = Math.max(contentHeight, widget.getY() + widget.getHeight());
        return widget;
    }

    public void clear() {
        children.clear();
        offsets.clear();
        focusedChild = null;
        pressedChild = null;
        contentHeight = 0;
        scroll = 0;
    }

    /** Content may extend below the last widget (e.g. bottom padding). Never shrinks below the widgets. */
    public void setContentHeight(int height) {
        contentHeight = Math.max(contentHeight, height);
    }

    public int getContentHeight() { return contentHeight; }

    public double getScroll() { return scroll; }

    public void setScroll(double value) {
        scroll = Mth.clamp(value, 0, maxScroll());
    }

    private int maxScroll() { return Math.max(0, contentHeight - getHeight()); }

    private boolean hasBar() { return maxScroll() > 0; }

    // ================= layout =================

    private void layoutChildren() {
        scroll = Mth.clamp(scroll, 0, maxScroll());
        int top = getY() - (int) scroll;
        for (AbstractWidget child : children) {
            child.setY(top + offsets.get(child));
        }
    }

    private boolean inView(AbstractWidget child) {
        return child.getY() + child.getHeight() > getY() && child.getY() < getY() + getHeight();
    }

    // ================= rendering =================

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (blurPending) {   // The panel really lost focus (another widget was clicked): drop the text cursor too
            blurPending = false;
            setFocusedChild(null);
        }
        layoutChildren();
        boolean over = mouseX >= getX() && mouseX < getX() + getWidth() && mouseY >= getY() && mouseY < getY() + getHeight();
        int mx = over ? mouseX : Integer.MIN_VALUE / 2;   // Nothing is "hovered" through the header/footer
        int my = over ? mouseY : Integer.MIN_VALUE / 2;

        g.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());
        for (AbstractWidget child : children) {
            if (child.visible && inView(child)) child.render(g, mx, my, partialTick);
        }
        g.disableScissor();

        if (hasBar()) {
            int barX = getX() + getWidth() - BAR_WIDTH;
            g.fill(barX, getY(), barX + BAR_WIDTH, getY() + getHeight(), UiTheme.SCROLL_TRACK);
            int thumbH = Math.max(16, (int) ((long) getHeight() * getHeight() / contentHeight));
            int thumbY = getY() + (int) ((getHeight() - thumbH) * (scroll / maxScroll()));
            g.fill(barX, thumbY, barX + BAR_WIDTH, thumbY + thumbH, UiTheme.SCROLL_THUMB);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}

    // ================= input =================

    private boolean overBar(double mx, double my) {
        return hasBar() && mx >= getX() + getWidth() - BAR_WIDTH - 2 && mx < getX() + getWidth()
                && my >= getY() && my < getY() + getHeight();
    }

    private void dragBarTo(double my) {
        int thumbH = Math.max(16, (int) ((long) getHeight() * getHeight() / contentHeight));
        double track = Math.max(1, getHeight() - thumbH);
        double ratio = Mth.clamp((my - getY() - thumbH / 2.0) / track, 0, 1);
        scroll = ratio * maxScroll();
    }

    private void setFocusedChild(@Nullable AbstractWidget child) {
        if (focusedChild == child) return;
        if (focusedChild != null) focusedChild.setFocused(false);
        focusedChild = child;
        if (child != null) child.setFocused(true);
    }

    /**
     * Screen re-focuses a widget it just clicked by sending setFocused(false) and then setFocused(true) to it. Dropping
     * the child's focus on that first call would undo the click we just handled (the text field would work exactly once),
     * so losing focus is only recorded here and applied on the next frame, unless the panel is focused again by then.
     */
    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        blurPending = !focused;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        layoutChildren();

        if (button == 0 && overBar(mouseX, mouseY)) {
            draggingBar = true;
            dragBarTo(mouseY);
            return true;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            AbstractWidget child = children.get(i);
            if (!child.visible || !child.active || !inView(child) || !child.isMouseOver(mouseX, mouseY)) continue;
            setFocusedChild(child);
            pressedChild = child;
            if (child.mouseClicked(mouseX, mouseY, button)) return true;
            pressedChild = null;
            return true;   // Clicked on a widget that did not want it: still ours, swallow it
        }
        setFocusedChild(null);   // Clicked on empty space: drop keyboard focus
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingBar = false;
        AbstractWidget child = pressedChild;
        pressedChild = null;
        return child != null && child.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingBar) {
            dragBarTo(mouseY);
            return true;
        }
        return pressedChild != null && pressedChild.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        layoutChildren();
        // A child that scrolls by itself (multi-line text box) gets the wheel first
        for (AbstractWidget child : children) {
            if (child.visible && child.active && inView(child) && child.isMouseOver(mouseX, mouseY)
                    && child.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }
        if (hasBar()) {
            scroll = Mth.clamp(scroll - scrollY * SCROLL_STEP, 0, maxScroll());
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return focusedChild != null && focusedChild.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return focusedChild != null && focusedChild.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return focusedChild != null && focusedChild.charTyped(codePoint, modifiers);
    }
}
