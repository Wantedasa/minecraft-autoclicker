package net.samue.autoclicker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * The settings screen (open with F7, or the key you bound).
 *
 * <p>Everything edits {@link ClickerConfig} directly and saves it to
 * {@code config/autoclicker.json}, so the file stays the single source of truth and can be
 * edited by hand as well.
 *
 * <p>Rows are spaced from a single reference height and scaled to the available screen
 * height, so the panel and every widget stay on screen even at a high GUI scale.
 */
public class AutoClickerScreen extends Screen {
    private static final int PANEL_W = 372;
    /** Reference (unscaled) panel height. */
    private static final int PANEL_H = 278;
    private static final int PAD = 14;
    private static final int ROW_H = 20;
    private static final int GAP = 6;

    private static final int TEXT = 0xFFFFFFFF;
    private static final int LABEL = 0xFFD8DEE9;
    private static final int MUTED = 0xFF9AA4B2;
    private static final int GOOD = 0xFF55FF55;
    private static final int DIM = 0xFF8A93A5;

    private final MinecraftClient mc;
    private final ClickerConfig cfg = AutoClickerClient.config;

    private ButtonWidget startButton;

    private int px;
    private int py;
    private int panelH;
    private int rowH;
    private int innerW;
    private int fieldW;
    private double scale = 1.0;

    public AutoClickerScreen() {
        super(Text.literal("Auto Clicker"));
        this.mc = MinecraftClient.getInstance();
    }

    /** Reference row offset -> actual y on this screen. */
    private int y(int offset) {
        return py + (int) Math.round(offset * scale);
    }

    @Override
    protected void init() {
        scale = Math.max(0.78, Math.min(1.0, (this.height - 8.0) / PANEL_H));
        panelH = (int) Math.round(PANEL_H * scale);
        rowH = scale >= 0.9 ? ROW_H : 18;

        px = (this.width - PANEL_W) / 2;
        py = (this.height - panelH) / 2;
        int x0 = px + PAD;
        innerW = PANEL_W - PAD * 2;
        fieldW = (innerW - 3 * GAP) / 4;

        int intervalFieldsY = y(44);
        numberField(x0 + 0 * (fieldW + GAP), intervalFieldsY, fieldW, cfg.hours, v -> cfg.hours = v);
        numberField(x0 + 1 * (fieldW + GAP), intervalFieldsY, fieldW, cfg.minutes, v -> cfg.minutes = v);
        numberField(x0 + 2 * (fieldW + GAP), intervalFieldsY, fieldW, cfg.seconds, v -> cfg.seconds = v);
        numberField(x0 + 3 * (fieldW + GAP), intervalFieldsY, fieldW, cfg.millis, v -> cfg.millis = v);

        int randomY = y(86);
        this.addDrawableChild(CheckboxWidget.builder(Text.literal("Random offset + -"), this.textRenderer)
                .pos(x0, randomY)
                .checked(cfg.randomOffset)
                .callback((box, checked) -> {
                    cfg.randomOffset = checked;
                    cfg.save();
                })
                .build());
        numberField(x0 + 190, randomY, 60, cfg.randomOffsetMs, v -> cfg.randomOffsetMs = v);

        this.addDrawableChild(CyclingButtonWidget
                .builder((ClickerConfig.MouseButton b) -> Text.literal(b.label()),
                        (Supplier<ClickerConfig.MouseButton>) () -> cfg.mouseButton)
                .values(ClickerConfig.MouseButton.values())
                .build(x0, y(114), innerW, rowH, Text.literal("Mouse button"), (btn, value) -> {
                    cfg.mouseButton = value;
                    cfg.save();
                }));

        this.addDrawableChild(CyclingButtonWidget
                .builder((ClickerConfig.ClickType t) -> Text.literal(t.label()),
                        (Supplier<ClickerConfig.ClickType>) () -> cfg.clickType)
                .values(ClickerConfig.ClickType.values())
                .build(x0, y(140), innerW, rowH, Text.literal("Click type"), (btn, value) -> {
                    cfg.clickType = value;
                    cfg.save();
                }));

        this.addDrawableChild(CyclingButtonWidget
                .builder((ClickerConfig.RepeatMode m) -> Text.literal(m.label()),
                        (Supplier<ClickerConfig.RepeatMode>) () -> cfg.repeatMode)
                .values(ClickerConfig.RepeatMode.values())
                .build(x0, y(166), innerW - 96, rowH, Text.literal("Repeat"), (btn, value) -> {
                    cfg.repeatMode = value;
                    cfg.save();
                }));
        numberField(x0 + innerW - 90, y(166), 60, cfg.repeatTimes, v -> cfg.repeatTimes = Math.max(1, v));

        this.addDrawableChild(CheckboxWidget.builder(Text.literal("Keep running when unfocused (tab out)"), this.textRenderer)
                .pos(x0, y(194))
                .checked(cfg.keepGameRunning)
                .callback((box, checked) -> {
                    cfg.keepGameRunning = checked;
                    cfg.save();
                })
                .build());

        startButton = this.addDrawableChild(ButtonWidget.builder(startLabel(), btn -> {
            AutoClickerClient.clicker.toggle(this.mc);
            btn.setMessage(startLabel());
        }).dimensions(x0, y(220), 160, rowH).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> this.close())
                .dimensions(x0 + innerW - 120, y(220), 120, rowH).build());
    }

    private Text startLabel() {
        return Text.literal(AutoClickerClient.clicker.isEnabled() ? "STOP (F6)" : "START (F6)");
    }

    private TextFieldWidget numberField(int x, int fieldY, int w, int value, IntConsumer apply) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, fieldY, w, rowH, Text.literal(""));
        field.setMaxLength(6);
        field.setTextPredicate(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        field.setText(String.valueOf(value));
        field.setChangedListener(s -> {
            try {
                apply.accept(s.isEmpty() ? 0 : Integer.parseInt(s));
                cfg.save();
            } catch (NumberFormatException ignored) {
                // the field is numeric-only, so this cannot really happen
            }
        });
        return this.addDrawableChild(field);
    }

    @Override
    public void tick() {
        if (startButton != null) {
            Text want = startLabel();
            if (!startButton.getMessage().getString().equals(want.getString())) {
                startButton.setMessage(want);
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int x0 = px + PAD;

        // panel first, so it sits behind the widgets
        context.fill(px, py, px + PANEL_W, py + panelH, 0xE610131C);
        outline(context, px, py, PANEL_W, panelH, 0xFF3B4252);

        super.render(context, mouseX, mouseY, delta);

        AutoClicker clicker = AutoClickerClient.clicker;
        boolean on = clicker.isEnabled();

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Auto Clicker"), px + PANEL_W / 2, y(10), TEXT);
        String state = (on ? "ON" : "OFF") + "  ·  " + clicker.getClicksSent() + " clicks";
        context.drawTextWithShadow(this.textRenderer, state,
                px + PANEL_W - PAD - this.textRenderer.getWidth(state), y(20), on ? GOOD : DIM);

        context.drawTextWithShadow(this.textRenderer, Text.literal("Click interval"), x0, y(32), LABEL);
        context.drawTextWithShadow(this.textRenderer, Text.literal("= " + cfg.intervalMillis() + " ms"),
                x0 + this.textRenderer.getWidth("Click interval") + 8, y(32), MUTED);

        String[] subLabels = {"hours", "mins", "secs", "milliseconds"};
        for (int i = 0; i < subLabels.length; i++) {
            int cx = x0 + i * (fieldW + GAP) + fieldW / 2;
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(subLabels[i]), cx, y(70), MUTED);
        }

        String ms = "ms";
        context.drawTextWithShadow(this.textRenderer, ms, x0 + 262, y(92), MUTED);
        String times = "times";
        context.drawTextWithShadow(this.textRenderer, times, x0 + innerW - this.textRenderer.getWidth(times), y(172), MUTED);

        String hint = "F6 = toggle   ·   settings: config/autoclicker.json";
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(hint), px + PANEL_W / 2, y(248), MUTED);
        String note = cfg.clickType == ClickerConfig.ClickType.HOLD
                ? "Hold: button stays pressed every tick (eat / fishing / attack)"
                : "Clicks keep firing while the Minecraft window is unfocused.";
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(note), px + PANEL_W / 2, y(260), DIM);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void outline(DrawContext c, int x, int oy, int w, int h, int color) {
        c.fill(x, oy, x + w, oy + 1, color);
        c.fill(x, oy + h - 1, x + w, oy + h, color);
        c.fill(x, oy, x + 1, oy + h, color);
        c.fill(x + w - 1, oy, x + w, oy + h, color);
    }
}
