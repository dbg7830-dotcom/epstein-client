package com.mayheemtest.gui;

import com.mayheemtest.module.AbstractModule;
import com.mayheemtest.module.ModuleManager;
import com.mayheemtest.module.impl.ReachModule;
import com.mayheemtest.module.setting.BooleanSetting;
import com.mayheemtest.module.setting.DoubleSetting;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * ClickGUI
 *
 * Renders modules with their settings. Boolean settings render as pill
 * toggles. Double settings render as sliders.
 *
 * Special case: ReachModule's "Level" slider only renders when blatant
 * mode is ON — keeps the panel clean and prevents text overlap.
 *
 * ── Customising the title ──
 * Edit TITLE_LEFT / TITLE_RIGHT. § colour codes:
 *   §l bold  §3 dark_aqua  §8 dark_gray  §7 gray  §f white  §r reset
 */
public class ClickGUI extends Screen {

    // ── Title ────────────────────────────────────────────────────────────────
    private static final String TITLE_LEFT  = "§l§3Epstein Client";
    private static final String TITLE_RIGHT = "§8| §7Stress Test";

    // ── Layout ───────────────────────────────────────────────────────────────
    private static final int PANEL_X    = 20;
    private static final int PANEL_Y    = 20;
    private static final int PANEL_W    = 260;
    private static final int TITLE_H    = 22;
    private static final int MODULE_H   = 24;
    private static final int SUBTITLE_H = 14;
    private static final int SETTING_H  = 20;
    private static final int SEP_H      = 1;
    private static final int PADDING    = 10;
    private static final int INDENT     = 14;
    private static final int SLIDER_W   = 100;
    private static final int SLIDER_H   = 4;
    private static final int TOGGLE_W   = 28;
    private static final int TOGGLE_H   = 10;

    // ── Colours ──────────────────────────────────────────────────────────────
    private static final int COL_PANEL        = 0xE0101014;
    private static final int COL_TITLE        = 0xFF0D0D18;
    private static final int COL_SEP          = 0xFF1E1E2E;
    private static final int COL_PILL_ON      = 0xFF4ADE80;
    private static final int COL_PILL_OFF     = 0xFF52525B;
    private static final int COL_BADGE_ON_BG  = 0xFF14532D;
    private static final int COL_BADGE_ON_FG  = 0xFF4ADE80;
    private static final int COL_BADGE_OFF_BG = 0xFF27272A;
    private static final int COL_BADGE_OFF_FG = 0xFF71717A;
    private static final int COL_TRACK        = 0xFF27272A;
    private static final int COL_FILL         = 0xFF6366F1;
    private static final int COL_THUMB        = 0xFFE2E8F0;
    private static final int COL_TEXT         = 0xFFE2E8F0;
    private static final int COL_MUTED        = 0xFF71717A;
    private static final int COL_HOVER        = 0x18FFFFFF;
    private static final int COL_TOGGLE_ON    = 0xFF4ADE80;
    private static final int COL_TOGGLE_OFF   = 0xFF3F3F46;
    private static final int COL_TOGGLE_KNOB  = 0xFFE2E8F0;

    // ── State ────────────────────────────────────────────────────────────────
    private DoubleSetting draggingSetting = null;
    private int           draggingSliderX = 0;

    public ClickGUI() {
        super(Text.literal("Epstein Client"));
    }

    @Override
    public boolean shouldPause() { return false; }

    // ── Visible settings helpers ──────────────────────────────────────────────

    /**
     * Returns the list of DoubleSetting that should actually render for a module.
     * For ReachModule: hides the Level slider when blatant mode is OFF.
     */
    private List<DoubleSetting> visibleSliders(AbstractModule mod) {
        List<DoubleSetting> all = mod.getDoubleSettings();
        if (mod instanceof ReachModule reach) {
            if (!reach.blatantMode.getValue()) {
                // Hide Level slider — only show Distance
                return all.stream()
                        .filter(s -> !s.getName().equals("Level"))
                        .toList();
            }
        }
        return all;
    }

    private List<BooleanSetting> visibleToggles(AbstractModule mod) {
        return mod.getBooleanSettings();
    }

    // ── Height ────────────────────────────────────────────────────────────────

    private int computeTotalHeight(List<AbstractModule> modules) {
        int h = TITLE_H + SEP_H;
        for (int i = 0; i < modules.size(); i++) {
            h += MODULE_H + SUBTITLE_H;
            h += visibleSliders(modules.get(i)).size()  * SETTING_H;
            h += visibleToggles(modules.get(i)).size()  * SETTING_H;
            if (i < modules.size() - 1) h += SEP_H;
        }
        return h;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        List<AbstractModule> modules = ModuleManager.get().getModules();
        int totalH = computeTotalHeight(modules);

        ctx.fill(PANEL_X, PANEL_Y, PANEL_X + PANEL_W, PANEL_Y + totalH, COL_PANEL);

        // Title bar
        ctx.fill(PANEL_X, PANEL_Y, PANEL_X + PANEL_W, PANEL_Y + TITLE_H, COL_TITLE);
        ctx.drawTextWithShadow(textRenderer, Text.literal(TITLE_LEFT),
                PANEL_X + PADDING, PANEL_Y + (TITLE_H - 8) / 2, 0xFFFFFFFF);
        int rightW = textRenderer.getWidth(TITLE_RIGHT);
        ctx.drawTextWithShadow(textRenderer, Text.literal(TITLE_RIGHT),
                PANEL_X + PANEL_W - PADDING - rightW, PANEL_Y + (TITLE_H - 8) / 2, 0xFFFFFFFF);
        ctx.fill(PANEL_X, PANEL_Y + TITLE_H,
                 PANEL_X + PANEL_W, PANEL_Y + TITLE_H + SEP_H, COL_SEP);

        int curY = PANEL_Y + TITLE_H + SEP_H;

        for (int i = 0; i < modules.size(); i++) {
            AbstractModule mod = modules.get(i);

            // Hover tint
            if (mouseX >= PANEL_X && mouseX <= PANEL_X + PANEL_W
             && mouseY >= curY    && mouseY <= curY + MODULE_H) {
                ctx.fill(PANEL_X, curY, PANEL_X + PANEL_W, curY + MODULE_H, COL_HOVER);
            }

            // ── Name row ──────────────────────────────────────────────────────
            int pillColor = mod.isEnabled() ? COL_PILL_ON : COL_PILL_OFF;
            int pillX = PANEL_X + PADDING;
            int pillY = curY + (MODULE_H - 10) / 2;
            ctx.fill(pillX, pillY, pillX + 3, pillY + 10, pillColor);

            ctx.drawTextWithShadow(textRenderer, Text.literal(mod.getName()),
                    pillX + 8, curY + (MODULE_H - 8) / 2,
                    mod.isEnabled() ? 0xFFE2E8F0 : 0xFF71717A);

            String badge  = mod.isEnabled() ? "ON" : "OFF";
            int badgeBg   = mod.isEnabled() ? COL_BADGE_ON_BG  : COL_BADGE_OFF_BG;
            int badgeFg   = mod.isEnabled() ? COL_BADGE_ON_FG  : COL_BADGE_OFF_FG;
            int badgeW    = textRenderer.getWidth(badge) + 8;
            int badgeX    = PANEL_X + PANEL_W - PADDING - badgeW;
            int badgeY    = curY + (MODULE_H - 12) / 2;
            ctx.fill(badgeX, badgeY, badgeX + badgeW, badgeY + 12, badgeBg);
            ctx.drawTextWithShadow(textRenderer, Text.literal(badge),
                    badgeX + 4, badgeY + 2, badgeFg);

            curY += MODULE_H;

            // ── Subtitle ──────────────────────────────────────────────────────
            ctx.drawTextWithShadow(textRenderer, Text.literal(mod.getDescription()),
                    PANEL_X + INDENT, curY + (SUBTITLE_H - 8) / 2, COL_MUTED);
            curY += SUBTITLE_H;

            // ── Sliders (filtered) ────────────────────────────────────────────
            for (DoubleSetting s : visibleSliders(mod)) {
                renderSlider(ctx, s, curY);
                curY += SETTING_H;
            }

            // ── Boolean toggles ───────────────────────────────────────────────
            for (BooleanSetting s : visibleToggles(mod)) {
                renderToggle(ctx, s, curY);
                curY += SETTING_H;
            }

            // ── Separator ─────────────────────────────────────────────────────
            if (i < modules.size() - 1) {
                ctx.fill(PANEL_X, curY, PANEL_X + PANEL_W, curY + SEP_H, COL_SEP);
                curY += SEP_H;
            }
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderSlider(DrawContext ctx, DoubleSetting s, int y) {
        int labelX  = PANEL_X + INDENT + 8;
        int sliderX = PANEL_X + PANEL_W - PADDING - SLIDER_W - 46;
        int sliderY = y + (SETTING_H - SLIDER_H) / 2;
        int valueX  = sliderX + SLIDER_W + 6;

        ctx.drawTextWithShadow(textRenderer, Text.literal(s.getName()),
                labelX, y + (SETTING_H - 8) / 2, COL_MUTED);
        ctx.fill(sliderX, sliderY, sliderX + SLIDER_W, sliderY + SLIDER_H, COL_TRACK);

        int fillW = Math.max(0, (int) (SLIDER_W * s.getNormalized()));
        if (fillW > 0)
            ctx.fill(sliderX, sliderY, sliderX + fillW, sliderY + SLIDER_H, COL_FILL);

        int thumbX = sliderX + fillW;
        ctx.fill(thumbX - 1, sliderY - 2, thumbX + 1, sliderY + SLIDER_H + 2, COL_THUMB);
        ctx.drawTextWithShadow(textRenderer, Text.literal(s.toString()),
                valueX, y + (SETTING_H - 8) / 2, COL_TEXT);
    }

    private void renderToggle(DrawContext ctx, BooleanSetting s, int y) {
        int labelX  = PANEL_X + INDENT + 8;
        int toggleX = PANEL_X + PANEL_W - PADDING - TOGGLE_W;
        int toggleY = y + (SETTING_H - TOGGLE_H) / 2;

        ctx.drawTextWithShadow(textRenderer, Text.literal(s.getName()),
                labelX, y + (SETTING_H - 8) / 2, COL_MUTED);

        ctx.fill(toggleX, toggleY, toggleX + TOGGLE_W, toggleY + TOGGLE_H,
                s.getValue() ? COL_TOGGLE_ON : COL_TOGGLE_OFF);

        int knobX = s.getValue() ? toggleX + TOGGLE_W - TOGGLE_H : toggleX;
        ctx.fill(knobX, toggleY, knobX + TOGGLE_H, toggleY + TOGGLE_H, COL_TOGGLE_KNOB);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        double mouseX = click.x();
        double mouseY = click.y();
        List<AbstractModule> modules = ModuleManager.get().getModules();
        int curY = PANEL_Y + TITLE_H + SEP_H;

        for (AbstractModule mod : modules) {
            // Name row → toggle module
            if (inRect(mouseX, mouseY, PANEL_X, curY, PANEL_W, MODULE_H)) {
                mod.toggle();
                return true;
            }
            curY += MODULE_H + SUBTITLE_H;

            // Sliders
            for (DoubleSetting s : visibleSliders(mod)) {
                int sliderX = PANEL_X + PANEL_W - PADDING - SLIDER_W - 46;
                int sliderY = curY + (SETTING_H - SLIDER_H) / 2 - 4;
                if (inRect(mouseX, mouseY, sliderX, sliderY, SLIDER_W, SLIDER_H + 8)) {
                    draggingSetting = s;
                    draggingSliderX = sliderX;
                    s.setNormalized((float) ((mouseX - sliderX) / SLIDER_W));
                    return true;
                }
                curY += SETTING_H;
            }

            // Boolean toggles
            for (BooleanSetting s : visibleToggles(mod)) {
                int toggleX = PANEL_X + PANEL_W - PADDING - TOGGLE_W;
                int toggleY = curY + (SETTING_H - TOGGLE_H) / 2;
                if (inRect(mouseX, mouseY, toggleX - 80, toggleY - 4,
                           80 + TOGGLE_W, TOGGLE_H + 8)) {
                    s.toggle();
                    return true;
                }
                curY += SETTING_H;
            }

            curY += SEP_H;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (click.button() == 0 && draggingSetting != null) {
            draggingSetting.setNormalized((float) ((click.x() - draggingSliderX) / SLIDER_W));
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) draggingSetting = null;
        return super.mouseReleased(click);
    }

    private boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
