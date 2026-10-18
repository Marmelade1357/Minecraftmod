package de.pf2craft.client;

import de.pf2craft.network.AttackConfirmPayload;
import de.pf2craft.network.OpenAttackScreenPayload;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Opened by the server when the current combatant clicks a target: pick the attack (1st to 3rd, bonus with the
 * multiple attack penalty), the suggested one is highlighted. Escape cancels. The server re-checks everything.
 * Sized for ~640x360 scaled pixels.
 */
public class AttackScreen extends Screen {
    private static final int PANEL_W = 220;
    private static final int BUTTON_H = 20;
    private static final int GAP = 4;

    private final UUID attackerId;
    private final int targetEntityId;
    private final List<OpenAttackScreenPayload.StrikeOption> options;
    private final int suggested;
    private int selected;

    public AttackScreen(OpenAttackScreenPayload payload) {
        super(Text.translatable("screen.pf2craft.attack.title_multi"));
        this.attackerId = payload.attackerId();
        this.targetEntityId = payload.targetEntityId();
        this.options = payload.options();
        this.suggested = Math.max(1, Math.min(3, payload.suggestedNumber()));
    }

    @Override
    protected void init() {
        clearChildren();
        if (options.isEmpty()) {
            close();
            return;
        }
        int w = Math.min(PANEL_W, width - 16);
        int x = (width - w) / 2;
        int y = height / 2 - 48;
        if (options.size() > 1) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pf2craft.attack.strike",
                            options.get(selected).name()), b -> {
                        selected = (selected + 1) % options.size();
                        init();
                    })
                    .dimensions(x, y, w, BUTTON_H).build());
        }
        int rowY = y + (options.size() > 1 ? BUTTON_H + GAP : 0) + 14;
        List<Integer> bonuses = options.get(selected).bonuses();
        for (int n = 1; n <= 3 && n <= bonuses.size(); n++) {
            int number = n;
            Text label = Text.translatable("screen.pf2craft.attack.button", n, signed(bonuses.get(n - 1)));
            if (n == suggested) {
                label = label.copy().formatted(Formatting.YELLOW, Formatting.BOLD);
            }
            ButtonWidget button = addDrawableChild(ButtonWidget.builder(label, b -> confirm(number))
                    .dimensions(x, rowY, w, BUTTON_H).build());
            button.active = n >= suggested; // the real attack count is the floor
            rowY += BUTTON_H + GAP;
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pf2craft.attack.cancel"), b -> close())
                .dimensions(x, rowY + 4, w, BUTTON_H).build());
    }

    private void confirm(int number) {
        AttackConfirmPayload payload = new AttackConfirmPayload(attackerId, targetEntityId, selected, number);
        if (ClientPlayNetworking.canSend(AttackConfirmPayload.ID)) {
            ClientPlayNetworking.send(payload);
        }
        close();
    }

    private static String signed(int bonus) {
        return bonus >= 0 ? "+" + bonus : String.valueOf(bonus);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        if (options.isEmpty()) {
            return;
        }
        Text heading = options.size() == 1
                ? Text.translatable("screen.pf2craft.attack.title", options.get(0).name())
                : title;
        int y = height / 2 - 48 - 14;
        context.drawCenteredTextWithShadow(textRenderer, heading, width / 2, y, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
