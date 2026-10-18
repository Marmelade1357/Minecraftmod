package de.pf2craft.client;

import de.pf2craft.network.RollRequestPayload;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

/**
 * Dice window: click dice to build a pool (right click removes one), set a modifier and an
 * optional label, then roll openly or secretly. The pool is kept between openings.
 */
public class DiceScreen extends Screen {
    /** Only dims the game behind the window; the vanilla background blur would smear the text. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);
    }

    private static final int[] DICE = {4, 6, 8, 10, 12, 20, 100};
    private static final int MAX_PER_DIE = 20;
    private static final int MAX_MODIFIER = 99;
    private static final int BUTTON_W = 30;
    private static final int GAP = 2;
    private static final int PANEL_W = DICE.length * (BUTTON_W + GAP) - GAP;

    private static final Map<Integer, Integer> pool = new LinkedHashMap<>();
    private static int modifier;
    private static boolean secret;
    private static String label = "";

    private final Map<ButtonWidget, Integer> dieButtons = new LinkedHashMap<>();
    private TextFieldWidget labelField;
    private ButtonWidget rollButton;
    private int left;
    private int top;

    public DiceScreen() {
        super(Text.translatable("screen.pf2craft.dice.title"));
    }

    @Override
    protected void init() {
        dieButtons.clear();
        left = (width - PANEL_W) / 2;
        top = height / 2 - 55;

        for (int i = 0; i < DICE.length; i++) {
            int sides = DICE[i];
            ButtonWidget button = ButtonWidget.builder(Text.translatable("screen.pf2craft.dice.die", sides),
                            b -> changeDie(sides, 1))
                    .dimensions(left + i * (BUTTON_W + GAP), top + 20, BUTTON_W, 20)
                    .build();
            dieButtons.put(button, sides);
            addDrawableChild(button);
        }

        int row2 = top + 58;
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> changeModifier(-1))
                .dimensions(left, row2, 20, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> changeModifier(1))
                .dimensions(left + 54, row2, 20, 20).build());

        labelField = new TextFieldWidget(textRenderer, left + 80, row2, PANEL_W - 80, 20,
                Text.translatable("screen.pf2craft.dice.label"));
        labelField.setMaxLength(32);
        labelField.setText(label);
        labelField.setPlaceholder(Text.translatable("screen.pf2craft.dice.label").formatted(Formatting.DARK_GRAY));
        labelField.setChangedListener(text -> label = text);
        addDrawableChild(labelField);

        int row3 = top + 84;
        addDrawableChild(CyclingButtonWidget.onOffBuilder(secret)
                .build(left, row3, 100, 20, Text.translatable("screen.pf2craft.dice.secret"),
                        (button, value) -> secret = value));
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pf2craft.dice.clear"), b -> clear())
                .dimensions(left + 104, row3, 54, 20).build());
        rollButton = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pf2craft.dice.roll"), b -> roll())
                .dimensions(left + 162, row3, PANEL_W - 162, 20).build());
        updateRollButton();
    }

    private void changeDie(int sides, int delta) {
        int count = Math.max(0, Math.min(MAX_PER_DIE, pool.getOrDefault(sides, 0) + delta));
        if (count == 0) {
            pool.remove(sides);
        } else {
            pool.put(sides, count);
        }
        updateRollButton();
    }

    private void changeModifier(int direction) {
        int step = hasShiftDown() ? 5 : 1;
        modifier = Math.max(-MAX_MODIFIER, Math.min(MAX_MODIFIER, modifier + direction * step));
    }

    private void clear() {
        pool.clear();
        modifier = 0;
        updateRollButton();
    }

    private void updateRollButton() {
        if (rollButton != null) {
            rollButton.active = !pool.isEmpty();
        }
    }

    /** E.g. {@code 1d20+2d6-1}; the server parses this again. */
    private static String expression() {
        StringBuilder sb = new StringBuilder();
        for (int sides : DICE) {
            Integer count = pool.get(sides);
            if (count != null) {
                if (!sb.isEmpty()) {
                    sb.append('+');
                }
                sb.append(count).append('d').append(sides);
            }
        }
        if (!sb.isEmpty() && modifier != 0) {
            sb.append(modifier > 0 ? "+" : "").append(modifier);
        }
        return sb.toString();
    }

    private void roll() {
        if (pool.isEmpty() || client == null) {
            return;
        }
        if (!ClientPlayNetworking.canSend(RollRequestPayload.ID)) {
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("screen.pf2craft.dice.server_missing")
                        .formatted(Formatting.RED), false);
            }
            close();
            return;
        }
        ClientPlayNetworking.send(new RollRequestPayload(expression(), label.strip(), secret));
        close();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            for (Map.Entry<ButtonWidget, Integer> entry : dieButtons.entrySet()) {
                if (entry.getKey().isMouseOver(mouseX, mouseY)) {
                    changeDie(entry.getValue(), -1);
                    entry.getKey().playDownSound(client.getSoundManager());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            roll();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = width / 2;
        context.drawCenteredTextWithShadow(textRenderer, title, centerX, top, 0xFFFFFF);

        // Count badges on the die buttons.
        for (Map.Entry<ButtonWidget, Integer> entry : dieButtons.entrySet()) {
            Integer count = pool.get(entry.getValue());
            if (count != null) {
                ButtonWidget b = entry.getKey();
                context.drawTextWithShadow(textRenderer, Text.literal("×" + count).formatted(Formatting.YELLOW),
                        b.getX() + b.getWidth() - 12, b.getY() - 9, 0xFFFFFF);
            }
        }

        String expression = expression();
        Text poolText = expression.isEmpty()
                ? Text.translatable("screen.pf2craft.dice.empty").formatted(Formatting.GRAY)
                : Text.literal(expression).formatted(Formatting.WHITE, Formatting.BOLD);
        context.drawCenteredTextWithShadow(textRenderer, poolText, centerX, top + 45, 0xFFFFFF);

        String mod = (modifier >= 0 ? "+" : "") + modifier;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(mod), left + 37, top + 64, 0xFFFFFF);

        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.pf2craft.dice.hint").formatted(Formatting.DARK_GRAY),
                centerX, top + 110, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
