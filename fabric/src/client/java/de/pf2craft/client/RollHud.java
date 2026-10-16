package de.pf2craft.client;

import de.pf2craft.network.RollResultPayload;
import de.pf2craft.roll.RollMessages;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

/** Shows the latest roll big in the upper middle of the screen for a few seconds. */
public final class RollHud {
    private static final long SHOW_MS = 4000;
    private static final long FADE_MS = 1000;
    private static final float TOTAL_SCALE = 3.0f;

    private static RollResultPayload current;
    private static long shownAt;

    private RollHud() {
    }

    public static void show(RollResultPayload payload) {
        current = payload;
        shownAt = System.currentTimeMillis();
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (current == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - shownAt;
        if (elapsed > SHOW_MS) {
            current = null;
            return;
        }
        float alpha = elapsed < SHOW_MS - FADE_MS ? 1.0f : (SHOW_MS - elapsed) / (float) FADE_MS;
        // Text with an alpha below ~4 is drawn fully opaque by the text renderer.
        int color = (MathHelper.clamp((int) (alpha * 255), 8, 255) << 24) | 0xFFFFFF;

        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        int centerX = context.getScaledWindowWidth() / 2;
        int y = context.getScaledWindowHeight() / 5;

        Text header = current.label().isEmpty()
                ? Text.literal(current.roller())
                : Text.translatable("hud.pf2craft.roll.header", current.roller(), current.label());
        if (current.secret()) {
            header = Text.translatable("pf2craft.roll.secret_prefix").formatted(Formatting.DARK_PURPLE)
                    .append(header.copy().formatted(Formatting.WHITE));
        }
        context.drawCenteredTextWithShadow(font, header, centerX, y, color);

        context.getMatrices().push();
        context.getMatrices().translate(centerX, y + 12, 0);
        context.getMatrices().scale(TOTAL_SCALE, TOTAL_SCALE, 1);
        context.drawCenteredTextWithShadow(font, RollMessages.total(current), 0, 0, color);
        context.getMatrices().pop();

        int belowTotal = y + 14 + (int) (font.fontHeight * TOTAL_SCALE);
        context.drawCenteredTextWithShadow(font, Text.literal(current.breakdown()).formatted(Formatting.GRAY),
                centerX, belowTotal, color);
        if (current.natural20() || current.natural1()) {
            Text natural = current.natural20()
                    ? Text.translatable("hud.pf2craft.roll.natural20").formatted(Formatting.GOLD)
                    : Text.translatable("hud.pf2craft.roll.natural1").formatted(Formatting.RED);
            context.drawCenteredTextWithShadow(font, natural, centerX, belowTotal + 11, color);
        }
    }
}
