package de.pf2craft.client;

import de.pf2craft.core.combat.Condition;
import de.pf2craft.network.ConditionEditPayload;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Lists the conditions of one combatant. A GM gets - / + buttons for every condition (the server checks the rights
 * again); everyone else only reads. Rebuilds itself when a new combat sync arrives.
 */
public class ConditionEditScreen extends Screen {
    private static final int PANEL_W = 220;
    private static final int ROW_H = 18;
    private static final int STEP_W = 16;
    private static final int VALUE_W = 18;

    private final UUID target;
    private final String targetName;
    private final boolean editable;
    private final Screen parent;
    private int left;
    private int top;

    public ConditionEditScreen(Screen parent, UUID target, String targetName, boolean editable) {
        super(Text.translatable("screen.pf2craft.conditions.title"));
        this.parent = parent;
        this.target = target;
        this.targetName = targetName;
        this.editable = editable;
    }

    public UUID target() {
        return target;
    }

    /** The server sent a fresh combat state. */
    public void refresh() {
        clearAndInit();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);
    }

    @Override
    protected void init() {
        Condition[] all = Condition.values();
        int panelH = 30 + all.length * ROW_H + 30;
        left = (width - PANEL_W) / 2;
        top = Math.max(4, (height - panelH) / 2);
        Map<Condition, Integer> current = ClientConditions.of(target);
        int y = top + 22;
        for (Condition c : all) {
            int value = current.getOrDefault(c, 0);
            if (editable) {
                int max = c.valued() ? 4 : 1;
                int x = left + PANEL_W - 2 * STEP_W - VALUE_W - 4;
                ButtonWidget minus = addDrawableChild(ButtonWidget.builder(Text.literal("-"),
                        b -> send(c, value - 1)).dimensions(x, y, STEP_W, ROW_H - 2).build());
                minus.active = value > 0;
                ButtonWidget plus = addDrawableChild(ButtonWidget.builder(Text.literal("+"),
                        b -> send(c, value + 1)).dimensions(x + STEP_W + VALUE_W + 4, y, STEP_W, ROW_H - 2).build());
                plus.active = value < max;
            }
            y += ROW_H;
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(left + PANEL_W / 2 - 50, y + 6, 100, 20).build());
    }

    private void send(Condition c, int value) {
        ConditionEditPayload payload = new ConditionEditPayload(target, c.id(), value);
        if (ClientPlayNetworking.canSend(payload.getId())) {
            ClientPlayNetworking.send(payload);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.pf2craft.conditions.heading", targetName), width / 2, top + 6, 0xFFFFFF);
        Map<Condition, Integer> current = ClientConditions.of(target);
        int y = top + 22;
        for (Condition c : Condition.values()) {
            int value = current.getOrDefault(c, 0);
            Text name = Text.translatable("pf2craft.condition." + c.id())
                    .formatted(value > 0 ? Formatting.WHITE : Formatting.DARK_GRAY);
            context.drawTextWithShadow(textRenderer, name, left, y + 5, 0xFFFFFF);
            String shown = value == 0 ? "-" : c.valued() ? String.valueOf(value) : "x";
            int valueX = left + PANEL_W - 2 * STEP_W - VALUE_W - 4 + STEP_W + 2;
            if (!editable) {
                valueX = left + PANEL_W - 20;
            }
            context.drawCenteredTextWithShadow(textRenderer, shown, valueX + (editable ? VALUE_W / 2 : 6), y + 5,
                    value > 0 ? 0xFFFFC800 : 0xFF808080);
            y += ROW_H;
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
