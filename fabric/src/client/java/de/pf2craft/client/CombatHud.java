package de.pf2craft.client;

import de.pf2craft.core.character.CharacterMath;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.combat.HealthLabel;
import de.pf2craft.network.CombatSyncPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;

/**
 * The combat overlay: initiative bar at the top, action line above the hotbar, a "your turn" banner and the
 * "TP: x/y" text that replaces hearts and hunger for players with a character.
 */
public final class CombatHud {
    private static final int BAR_TOP = 4;
    private static final int CURRENT_W = 104;
    private static final int CURRENT_H = 36;
    private static final int OTHER_W = 76;
    private static final int OTHER_H = 30;
    private static final int GAP = 3;
    private static final int COLOR_PANEL = 0xA0000000;
    private static final int COLOR_PANEL_CURRENT = 0xC0302800;
    private static final int COLOR_BORDER_CURRENT = 0xFFFFC800;
    private static final int COLOR_BORDER = 0xFF606060;
    private static final int COLOR_GREY = 0xFF808080;
    private static final int COLOR_WHITE = 0xFFFFFFFF;

    private CombatHud() {
    }

    /** The own sheet, if the player has a character. */
    private static Optional<ClientSheets.Synced> ownSheet(MinecraftClient client) {
        return client.player == null ? Optional.empty() : ClientSheets.get(client.player.getUuid());
    }

    /** Whether the vanilla hearts and hunger bar are replaced by the TP text right now. */
    public static boolean replacesVanillaHealth() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && client.interactionManager != null
                && client.interactionManager.hasStatusBars() && ownSheet(client).isPresent();
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.options.hudHidden) {
            return;
        }
        TextRenderer font = client.textRenderer;
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        Optional<ClientSheets.Synced> own = ownSheet(client);
        if (replacesVanillaHealth()) {
            drawHealth(context, font, own.get().sheet(), width / 2 - 91, height - 39);
        }
        Optional<CombatSyncPayload> combat = ClientCombat.get();
        if (combat.isEmpty()) {
            return;
        }
        CombatSyncPayload state = combat.get();
        int barBottom = drawInitiativeBar(context, font, state, width);
        if (ClientCombat.isTurnOf(player.getUuid())) {
            Text banner = Text.translatable("hud.pf2craft.combat.your_turn")
                    .formatted(Formatting.GOLD, Formatting.BOLD);
            context.getMatrices().push();
            context.getMatrices().translate(width / 2.0f, barBottom + 6, 0);
            context.getMatrices().scale(1.5f, 1.5f, 1);
            context.drawCenteredTextWithShadow(font, banner, 0, 0, COLOR_WHITE);
            context.getMatrices().pop();
            drawActions(context, font, state, own.map(s -> s.sheet().speed()).orElse(0), width, height);
        }
    }

    private static void drawHealth(DrawContext context, TextRenderer font, CharacterSheet sheet, int x, int y) {
        int max = CharacterMath.maxHp(sheet);
        int hp = sheet.currentHp();
        Formatting color = hp * 4 <= max ? Formatting.RED : hp * 2 <= max ? Formatting.YELLOW : Formatting.GREEN;
        Text text = Text.translatable("hud.pf2craft.hp", hp, max).formatted(color, Formatting.BOLD);
        context.drawTextWithShadow(font, text, x, y, COLOR_WHITE);
    }

    /** @return the y below the bar */
    private static int drawInitiativeBar(DrawContext context, TextRenderer font, CombatSyncPayload state,
                                         int screenW) {
        List<CombatSyncPayload.Entry> ordered = state.entries();
        int currentIndex = 0;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).current()) {
                currentIndex = i;
            }
        }
        // Current combatant first, then the rest in turn order.
        List<CombatSyncPayload.Entry> slots = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            slots.add(ordered.get((currentIndex + i) % ordered.size()));
        }
        int available = screenW - 16;
        int shown = 0;
        int used = 0;
        for (int i = 0; i < slots.size(); i++) {
            int w = i == 0 ? CURRENT_W : OTHER_W + GAP;
            if (used + w > available) {
                break;
            }
            used += w;
            shown++;
        }
        Text round = Text.translatable("hud.pf2craft.combat.round", state.round()).formatted(Formatting.GOLD);
        if (shown == 0) {
            context.drawCenteredTextWithShadow(font, round, screenW / 2, BAR_TOP, COLOR_WHITE);
            return BAR_TOP + 12;
        }
        int x = (screenW - used) / 2;
        for (int i = 0; i < shown; i++) {
            boolean first = i == 0;
            int w = first ? CURRENT_W : OTHER_W;
            int h = first ? CURRENT_H : OTHER_H;
            drawSlot(context, font, slots.get(i), x, BAR_TOP, w, h, first);
            x += w + GAP;
        }
        int bottom = BAR_TOP + CURRENT_H + 2;
        context.drawCenteredTextWithShadow(font, round, screenW / 2, bottom, COLOR_WHITE);
        return bottom + 10;
    }

    private static void drawSlot(DrawContext context, TextRenderer font, CombatSyncPayload.Entry e, int x, int y,
                                 int w, int h, boolean current) {
        context.fill(x, y, x + w, y + h, current ? COLOR_PANEL_CURRENT : COLOR_PANEL);
        context.drawBorder(x, y, w, h, current ? COLOR_BORDER_CURRENT : COLOR_BORDER);
        boolean grey = !e.active();
        int textW = w - 6;
        context.drawTextWithShadow(font, font.trimToWidth(e.name(), textW), x + 3, y + 3,
                grey ? COLOR_GREY : COLOR_WHITE);
        int line = y + 3 + font.fontHeight + 2;
        if (e.initiative() >= 0) {
            Text ini = Text.translatable("hud.pf2craft.combat.initiative", e.initiative());
            context.drawTextWithShadow(font, ini, x + 3, line, grey ? COLOR_GREY : 0xFFAAAAAA);
            line += font.fontHeight + 1;
        }
        if (e.npc()) {
            Text health = healthText(e);
            context.drawTextWithShadow(font, Language.getInstance().reorder(font.trimToWidth(health, textW)), x + 3, line,
                    grey ? COLOR_GREY : COLOR_WHITE);
        }
    }

    private static Text healthText(CombatSyncPayload.Entry e) {
        if (e.hp() >= 0) {
            // GMs get the exact numbers.
            return Text.translatable("hud.pf2craft.combat.hp_exact", e.hp(), e.maxHp());
        }
        int ordinal = e.label() & 0xFF;
        HealthLabel[] labels = HealthLabel.values();
        if (ordinal >= labels.length) {
            return Text.empty();
        }
        return switch (labels[ordinal]) {
            case UNHURT -> Text.translatable("hud.pf2craft.combat.label.unhurt").formatted(Formatting.GREEN);
            case HURT -> Text.translatable("hud.pf2craft.combat.label.hurt").formatted(Formatting.YELLOW);
            case BADLY_HURT -> Text.translatable("hud.pf2craft.combat.label.badly_hurt").formatted(Formatting.RED);
            case DEFEATED -> Text.translatable("hud.pf2craft.combat.label.defeated").formatted(Formatting.GRAY);
        };
    }

    private static void drawActions(DrawContext context, TextRenderer font, CombatSyncPayload state, int speedFeet,
                                    int screenW, int screenH) {
        int total = Math.max(3, state.actionsLeft());
        StringBuilder diamonds = new StringBuilder();
        for (int i = 0; i < total; i++) {
            diamonds.append(i < state.actionsLeft() ? '◆' : '◇');
        }
        int perStride = speedFeet > 0 ? Math.max(1, speedFeet / 5) : 0;
        Text movement = state.strideBlocksLeft() > 0
                ? Text.translatable("hud.pf2craft.combat.movement", state.strideBlocksLeft(), perStride)
                : Text.translatable("hud.pf2craft.combat.movement_none");
        Text reaction = Text.literal("↺")
                .formatted(state.reactionAvailable() ? Formatting.GREEN : Formatting.DARK_GRAY);
        MutableText line = Text.translatable("hud.pf2craft.combat.actions",
                Text.literal(diamonds.toString()).formatted(Formatting.GOLD), reaction, movement);
        context.drawCenteredTextWithShadow(font, line, screenW / 2, screenH - 75, COLOR_WHITE);
        if (!ClientActions.pending().isEmpty()) {
            Text hint = Text.translatable("hud.pf2craft.action.pending",
                    Text.translatable("pf2craft.combat.action.name." + ClientActions.pending()))
                    .formatted(Formatting.YELLOW);
            context.drawCenteredTextWithShadow(font, hint, screenW / 2, screenH - 87, COLOR_WHITE);
        }
    }
}
