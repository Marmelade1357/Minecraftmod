package de.pf2craft.client;

import de.pf2craft.network.CombatControlPayload;
import de.pf2craft.network.CombatStartPayload;
import de.pf2craft.network.OpenCombatScreenPayload;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import net.minecraft.util.math.MathHelper;

/**
 * GM window opened by the combat bell. Before a fight: two ticked lists (characters left, NPCs right) and the
 * "roll initiative" button. While a fight runs: skip the turn or end the combat (second click confirms).
 * Sized for ~640x360 scaled pixels.
 */
public class CombatSetupScreen extends Screen {
    private static final int PANEL_W = 420;
    private static final int PANEL_H = 280;
    private static final int COLUMN_GAP = 8;
    private static final int HEADER_H = 12;
    private static final int ROW_H = 14;
    private static final int BOX = 10;
    private static final int BUTTON_H = 20;
    private static final int BOTTOM_H = 62;

    private final boolean running;
    private final List<OpenCombatScreenPayload.Candidate> players = new ArrayList<>();
    private final List<OpenCombatScreenPayload.Candidate> npcs = new ArrayList<>();
    private final Set<UUID> ticked = new HashSet<>();
    private int playerScroll;
    private int npcScroll;
    private boolean confirmEnd;
    private ButtonWidget endButton;
    private int left;
    private int top;
    private int panelW;
    private int colW;
    private int listTop;
    private int listBottom;

    public CombatSetupScreen(OpenCombatScreenPayload payload) {
        super(Text.translatable("screen.pf2craft.combat.title"));
        this.running = payload.running();
        for (OpenCombatScreenPayload.Candidate c : payload.candidates()) {
            (c.npc() ? npcs : players).add(c);
            ticked.add(c.id());
        }
    }

    @Override
    protected void init() {
        panelW = Math.min(PANEL_W, width - 8);
        int panelH = Math.min(PANEL_H, height - 8);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        colW = (panelW - COLUMN_GAP) / 2;
        listTop = top + 22 + HEADER_H;
        listBottom = top + panelH - BOTTOM_H;

        if (running) {
            int w = Math.min(200, panelW - 20);
            int x = (width - w) / 2;
            int y = top + 40;
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pf2craft.combat.skip"), b -> {
                send(new CombatControlPayload("skip"));
            }).dimensions(x, y, w, BUTTON_H).build());
            endButton = addDrawableChild(ButtonWidget.builder(endLabel(), b -> onEnd())
                    .dimensions(x, y + 28, w, BUTTON_H).build());
            return;
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pf2craft.combat.add_npc"),
                        b -> {
                            if (client != null) {
                                client.setScreen(new NpcSpawnScreen());
                            }
                        })
                .dimensions(left + colW + COLUMN_GAP, listBottom + 12, colW, BUTTON_H).build());
        int startW = Math.min(180, panelW - 20);
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pf2craft.combat.start"), b -> onStart())
                .dimensions((width - startW) / 2, listBottom + 38, startW, BUTTON_H).build());
    }

    private Text endLabel() {
        return confirmEnd
                ? Text.translatable("screen.pf2craft.combat.end_confirm").formatted(Formatting.RED)
                : Text.translatable("screen.pf2craft.combat.end");
    }

    private void onEnd() {
        if (!confirmEnd) {
            confirmEnd = true;
            endButton.setMessage(endLabel());
            return;
        }
        send(new CombatControlPayload("end"));
        close();
    }

    private void onStart() {
        List<UUID> ids = new ArrayList<>();
        for (OpenCombatScreenPayload.Candidate c : players) {
            if (ticked.contains(c.id())) {
                ids.add(c.id());
            }
        }
        for (OpenCombatScreenPayload.Candidate c : npcs) {
            if (ticked.contains(c.id())) {
                ids.add(c.id());
            }
        }
        if (ids.size() > CombatStartPayload.MAX_PARTICIPANTS) {
            ids = ids.subList(0, CombatStartPayload.MAX_PARTICIPANTS);
        }
        if (send(new CombatStartPayload(ids))) {
            close();
        }
    }

    private boolean send(net.minecraft.network.packet.CustomPayload payload) {
        if (client == null || !ClientPlayNetworking.canSend(payload.getId())) {
            return false;
        }
        ClientPlayNetworking.send(payload);
        return true;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom - listTop) / ROW_H);
    }

    private int columnLeft(boolean npcColumn) {
        return npcColumn ? left + colW + COLUMN_GAP : left;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top + 6, 0xFFFFFF);
        if (running) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("screen.pf2craft.combat.running").formatted(Formatting.GRAY),
                    width / 2, top + 24, 0xFFFFFF);
        } else {
            drawColumn(context, mouseX, mouseY, false);
            drawColumn(context, mouseX, mouseY, true);
            context.drawTextWithShadow(textRenderer,
                    Language.getInstance().reorder(textRenderer.trimToWidth(
                            Text.translatable("screen.pf2craft.combat.add_npc_hint").formatted(Formatting.GRAY),
                            colW)),
                    columnLeft(true), listBottom + 2, 0xFFFFFF);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawColumn(DrawContext context, int mouseX, int mouseY, boolean npcColumn) {
        List<OpenCombatScreenPayload.Candidate> list = npcColumn ? npcs : players;
        int x = columnLeft(npcColumn);
        Text header = Text.translatable(npcColumn ? "screen.pf2craft.combat.npcs" : "screen.pf2craft.combat.players")
                .formatted(Formatting.GOLD);
        context.drawTextWithShadow(textRenderer, header, x, top + 22, 0xFFFFFF);
        context.fill(x, listTop, x + colW, listBottom, 0x80000000);
        context.drawBorder(x, listTop, colW, listBottom - listTop, 0xFF606060);
        if (list.isEmpty()) {
            context.drawTextWithShadow(textRenderer,
                    Language.getInstance().reorder(textRenderer.trimToWidth(
                            Text.translatable("screen.pf2craft.combat.none").formatted(Formatting.GRAY),
                            colW - 8)), x + 4, listTop + 4, 0xFFFFFF);
            return;
        }
        int scroll = npcColumn ? npcScroll : playerScroll;
        int rows = visibleRows();
        for (int i = 0; i < rows && scroll + i < list.size(); i++) {
            OpenCombatScreenPayload.Candidate c = list.get(scroll + i);
            int y = listTop + 1 + i * ROW_H;
            boolean hover = mouseX >= x && mouseX < x + colW && mouseY >= y && mouseY < y + ROW_H;
            if (hover) {
                context.fill(x + 1, y, x + colW - 1, y + ROW_H, 0x40FFFFFF);
            }
            int bx = x + 4;
            int by = y + (ROW_H - BOX) / 2;
            context.drawBorder(bx, by, BOX, BOX, 0xFFC0C0C0);
            if (ticked.contains(c.id())) {
                context.fill(bx + 2, by + 2, bx + BOX - 2, by + BOX - 2, 0xFF55FF55);
            }
            context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(c.name(), colW - BOX - 14),
                    bx + BOX + 4, y + 3, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (running || button != 0 || mouseY < listTop || mouseY >= listBottom) {
            return false;
        }
        for (boolean npcColumn : new boolean[] {false, true}) {
            int x = columnLeft(npcColumn);
            if (mouseX >= x && mouseX < x + colW) {
                List<OpenCombatScreenPayload.Candidate> list = npcColumn ? npcs : players;
                int index = (npcColumn ? npcScroll : playerScroll) + (int) ((mouseY - listTop - 1) / ROW_H);
                if (index >= 0 && index < list.size()) {
                    UUID id = list.get(index).id();
                    if (!ticked.remove(id)) {
                        ticked.add(id);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (running) {
            return false;
        }
        boolean npcColumn = mouseX >= columnLeft(true);
        List<OpenCombatScreenPayload.Candidate> list = npcColumn ? npcs : players;
        int max = Math.max(0, list.size() - visibleRows());
        int scroll = MathHelper.clamp((npcColumn ? npcScroll : playerScroll) - (int) Math.signum(vertical), 0, max);
        if (npcColumn) {
            npcScroll = scroll;
        } else {
            playerScroll = scroll;
        }
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
