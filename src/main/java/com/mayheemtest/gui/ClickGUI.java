package com.mayheemtest.gui;

import com.mayheemtest.module.AbstractModule;
import com.mayheemtest.module.ModuleManager;
import com.mayheemtest.module.setting.DoubleSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

/**
 * ClickGUI
 *
 * A clean, compact in-game panel listing every module.
 *
 * Layout (per module row)
 * ───────────────────────
 *  [ ● ]  Module name          description text
 *  ────────────────────────────────────────────
 *  Setting name    [━━━━━━━━━━━━━━━] 3.50
 *  ...
 *
 * Colours
 * ───────
 *  Panel bg        0xCC0D0D0F  (near-black, 80% opacity)
 *  Header bg       0xFF181820
 *  Enabled pill    0xFF4ADE80  (green)
 *  Disabled pill   0xFF6B7280  (gray)
 *  Slider track    0xFF2D2D3A
 *  Slider fill     0xFF818CF8  (indigo)
 *  Text primary    0xFFE2E8F0
 *  Text muted      0xFF94A3B8
 *
 * Interaction
 * ───────────
 *  Left-click module row  → toggle module on/off
 *  Left-drag slider       → adjust setting value
 *  Right-click slider     → reset to default (not implemented here – easy add)
 *  Esc / keybind          → close panel
 */
public class ClickGUI extends Screen {

    // ── Layout constants ────────────────────────────────────────────────────
    private static final int PANEL_X       = 20;
    private static final int PANEL_Y       = 20;
    private static final int PANEL_W       = 280;

    private static final int MODULE_H      = 32;   // height of the module header row
    private static final int SETTING_H     = 22;   // height per setting row
    private static final int SLIDER_W      = 110;  // width of the slider bar
    private static final int SLIDER_H      = 4;
    private static final int PADDING       = 10;

    // ── Colours ─────────────────────────────────────────────────────────────
    private static final int COL_PANEL     = 0xCC0D0D0F;
    private static final int COL_HEADER    = 0xFF181820;
    private static final int COL_ENABLED   = 0xFF4ADE80;
    private static final int COL_DISABLED  = 0xFF6B7280;
    private static final int COL_TRACK     = 0xFF2D2D3A;
    private static final int COL_FILL      = 0xFF818CF8;
    private static final int COL_TEXT      = 0xFFE2E8F0;
    private static final int COL_MUTED     = 0xFF94A3B8;
    private static final int COL_HOVER     = 0x22FFFFFF;

    // ── State ────────────────────────────────────────────────────────────────
    private int totalPanelHeight = 0;

    // Active slider drag state
    private DoubleSetting draggingSetting = null;
    private int           draggingSliderX = 0; // left edge of the slider bar being dragged
    private int           draggingSliderY = 0;

    // Track which module rows are "hovered" for the hover tint
    private int hoveredModule = -1;

    public ClickGUI() {
        super(Text.literal("MayheemTest"));
    }

    @Override
    public boolean shouldPause() {
        return false; // keep the game running while the panel is open
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        List<AbstractModule> modules = ModuleManager.get().getModules();

        // Calculate total panel height every frame (settings may be expanded)
        totalPanelHeight = calculatePanelHeight(modules);

        // Panel background
        ctx.fill(PANEL_X, PANEL_Y,
                 PANEL_X + PANEL_W, PANEL_Y + totalPanelHeight,
                 COL_PANEL);

        // Title bar
        ctx.fill(PANEL_X, PANEL_Y,
                 PANEL_X + PANEL_W, PANEL_Y + 20,
                 COL_HEADER);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("MayheemTest  ·  AC Stress Test"),
                PANEL_X + PADDING, PANEL_Y + 6, COL_MUTED);

        int curY = PANEL_Y + 20;

        for (int i = 0; i < modules.size(); i++) {
            AbstractModule mod = modules.get(i);
            boolean hovered = isMouseOverModuleHeader(mouseX, mouseY, curY);
            if (hovered) hoveredModule = i;

            renderModuleRow(ctx, mod, curY, hovered, mouseX, mouseY);
            curY += MODULE_H;

            // Render settings below the module header
            for (DoubleSetting s : mod.getDoubleSettings()) {
                renderSliderRow(ctx, s, curY, mouseX, mouseY);
                curY += SETTING_H;
            }
        }

        // Draw the title bar label on top (so it's never occluded)
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderModuleRow(DrawContext ctx, AbstractModule mod, int y,
                                 boolean hovered, int mouseX, int mouseY) {
        // Hover tint
        if (hovered) {
            ctx.fill(PANEL_X, y, PANEL_X + PANEL_W, y + MODULE_H, COL_HOVER);
        }

        // Enabled indicator pill (4×12 rounded-ish rect — drawContext has no arc,
        // so we fake it with two overlapping rects)
        int pillColor = mod.isEnabled() ? COL_ENABLED : COL_DISABLED;
        int pillX = PANEL_X + PADDING;
        int pillY = y + (MODULE_H - 12) / 2;
        ctx.fill(pillX, pillY, pillX + 4, pillY + 12, pillColor);

        // Module name
        ctx.drawTextWithShadow(textRenderer,
                Text.literal(mod.getName()),
                pillX + 10, y + 7, mod.isEnabled() ? COL_TEXT : COL_MUTED);

        // Description (right-aligned, muted)
        ctx.drawTextWithShadow(textRenderer,
                Text.literal(mod.getDescription()),
                PANEL_X + PANEL_W - PADDING - textRenderer.getWidth(mod.getDescription()),
                y + 7, COL_MUTED);
    }

    private void renderSliderRow(DrawContext ctx, DoubleSetting s, int y,
                                 int mouseX, int mouseY) {
        int labelX  = PANEL_X + PADDING + 14;
        int sliderX = PANEL_X + PANEL_W - PADDING - SLIDER_W - 50;
        int sliderY = y + (SETTING_H - SLIDER_H) / 2;
        int valueX  = sliderX + SLIDER_W + 6;

        // Setting label
        ctx.drawTextWithShadow(textRenderer,
                Text.literal(s.getName()),
                labelX, y + (SETTING_H - 8) / 2, COL_MUTED);

        // Slider track
        ctx.fill(sliderX, sliderY,
                 sliderX + SLIDER_W, sliderY + SLIDER_H,
                 COL_TRACK);

        // Slider fill
        int fillW = (int) (SLIDER_W * s.getNormalized());
        if (fillW > 0) {
            ctx.fill(sliderX, sliderY,
                     sliderX + fillW, sliderY + SLIDER_H,
                     COL_FILL);
        }

        // Slider thumb (small 2px-wide brighter bar at fill edge)
        int thumbX = sliderX + fillW;
        ctx.fill(thumbX - 1, sliderY - 2,
                 thumbX + 1, sliderY + SLIDER_H + 2,
                 COL_TEXT);

        // Value text
        ctx.drawTextWithShadow(textRenderer,
                Text.literal(s.toString()),
                valueX, y + (SETTING_H - 8) / 2, COL_TEXT);
    }

    // ── Input handling ───────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        List<AbstractModule> modules = ModuleManager.get().getModules();
        int curY = PANEL_Y + 20;

        for (AbstractModule mod : modules) {
            // Check module header click → toggle
            if (isMouseOverRect((int) mouseX, (int) mouseY,
                    PANEL_X, curY, PANEL_W, MODULE_H)) {
                // Only toggle if not clicking on a slider below
                mod.toggle();
                return true;
            }
            curY += MODULE_H;

            // Check slider clicks
            for (DoubleSetting s : mod.getDoubleSettings()) {
                int sliderX = PANEL_X + PANEL_W - PADDING - SLIDER_W - 50;
                int sliderY = curY + (SETTING_H - SLIDER_H) / 2 - 4;

                if (isMouseOverRect((int) mouseX, (int) mouseY,
                        sliderX, sliderY, SLIDER_W, SLIDER_H + 8)) {
                    draggingSetting = s;
                    draggingSliderX = sliderX;
                    draggingSliderY = curY;
                    float t = (float) ((mouseX - sliderX) / SLIDER_W);
                    s.setNormalized(t);
                    return true;
                }
                curY += SETTING_H;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double deltaX, double deltaY) {
        if (button == 0 && draggingSetting != null) {
            float t = (float) ((mouseX - draggingSliderX) / SLIDER_W);
            draggingSetting.setNormalized(t);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) draggingSetting = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private int calculatePanelHeight(List<AbstractModule> modules) {
        int h = 20; // title bar
        for (AbstractModule mod : modules) {
            h += MODULE_H;
            h += mod.getDoubleSettings().size() * SETTING_H;
        }
        return h;
    }

    private boolean isMouseOverModuleHeader(int mx, int my, int rowY) {
        return isMouseOverRect(mx, my, PANEL_X, rowY, PANEL_W, MODULE_H);
    }

    private boolean isMouseOverRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
