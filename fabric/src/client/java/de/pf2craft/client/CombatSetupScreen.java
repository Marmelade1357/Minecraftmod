package de.pf2craft.client;

import de.pf2craft.network.CombatControlPayload;
import de.pf2craft.network.CombatRemovePayload;
import de.pf2craft.network.CombatStartPayload;
import de.pf2craft.network.CombatSyncPayload;
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
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import net.minecraft.util.math.MathHelper;

/**
 * GM window opened by the combat bell. Before a fight: two ticked lists (characters left, NPCs right) and the
 * "roll initiative" button. While a fight runs: skip the turn, end the combat (second click confirms), take
 * participants out (the cross) and add new ones (right list, unticked by default).
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
    private static final int RUNNING_TOP_H = 28;
    private static final int RUNNING_BOTTOM_H = 30;

    private record Row(UUID id, String name) {
    }

    private final boolean running;
    private final List<Row> leftRows = new ArrayList<>();
    private final List<Row> rightRows = new ArrayList<>();
    private final Set<UUID> ticked = new HashSet<>();
    private int leftScroll;
    private int rightScroll;
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
        if (running) {
            Set<UUID> inCombat = new HashSet<>();
            for (CombatSyncPayload.Entry e : ClientCombat.get().map(CombatSyncPayload::entries).orElse(List.of())) {
                leftRows.add(new Row(e.id(), e.name()));
                inCombat.add(e.id());
            }
            for (OpenCombatScreenPayload.Candidate c : payload.candidates()) {
                if (!inCombat.contains(c.id())) {
                    rightRows.add(new Row(c.id(), c.name()));
                }
            }
        } else {
            for (OpenCombatScreenPayload.Candidate c : payload.candidates()) {
                (c.npc() ? rightRows : leftRows).add(new Row(c.id(), c.name()));
                ticked.add(c.id());
            }
        }
    }

    @Override
    protected void init() {
        panelW = Math.min(PANEL_W, width - 8);
        int panelH = Math.min(PANEL_H, height - 8);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        colW = (panelW - COLUMN_GAP) / 2;

        if (running) {
            listTop = top + 22 + RUNNING_TOP_H + HEADER_H;
            listBottom = top + panelH - RUNNING_BOTTOM_H;
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pf2craft.combat.skip"),
                            b -> send(new CombatControlPayload("skip")))
                    .dimensions(left, top + 20, colW, BUTTON_H).build());
            endButton = addDrawableChild(ButtonWidget.builder(endLabel(), b -> onEnd())
                    .dimensions(columnLeft(true), top + 20, colW, BUTTON_H).build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pf2craft.combat.add_selected"),
                            b -> onAdd())
                    .dimensions(columnLeft(true), listBottom + 6, colW, BUTTON_H).build());
            return;
        }
        listTop = top + 22 + HEADER_H;
        listBottom = top + panelH - BOTTOM_H;
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pf2craft.combat.add_npc"),
                        b -> {
                            if (client != null) {
                                client.setScreen(new NpcSpawnScreen());
                            }
                        })
                .dimensions(columnLeft(true), listBottom + 12, colW, BUTTON_H).build());
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

    private List<UUID> tickedIds() {
        List<UUID> ids = new ArrayList<>();
        for (List<Row> rows : List.of(leftRows, rightRows)) {
            for (Row r : rows) {
                if (ticked.contains(r.id())) {
                    ids.add(r.id());
                }
            }
        }
        if (ids.size() > CombatStartPayload.MAX_PARTICIPANTS) {
            ids = ids.subList(0, CombatStartPayload.MAX_PARTICIPANTS);
        }
        return ids;
    }

    private void onStart() {
        if (send(new CombatStartPayload(tickedIds()))) {
            close();
        }
    }

    private void onAdd() {
        List<UUID> ids = new ArrayList<>();
        for (Row r : rightRows) {
            if (ticked.contains(r.id())) {
                ids.add(r.id());
            }
        }
        if (!ids.isEmpty() && send(new CombatStartPayload(ids))) {
            close();
        }
    }

    private boolean send(CustomPayload payload) {
        if (client == null || !ClientPlayNetworking.canSend(payload.getId())) {
            return false;
        }
        ClientPlayNetworking.send(payload);
        return true;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom - listTop) / ROW_H);
    }

    private int columnLeft(boolean right) {
        return right ? left + colW + COLUMN_GAP : left;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top + 6, 0xFFFFFF);
        drawColumn(context, mouseX, mouseY, false);
        drawColumn(context, mouseX, mouseY, true);
        if (!running) {
            context.drawTextWithShadow(textRenderer,
                    Language.getInstance().reorder(textRenderer.trimToWidth(
                            Text.translatable("screen.pf2craft.combat.add_npc_hint").formatted(Formatting.GRAY),
                            colW)),
                    columnLeft(true), listBottom + 2, 0xFFFFFF);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private String headerKey(boolean right) {
        if (running) {
            return right ? "screen.pf2craft.combat.candidates" : "screen.pf2craft.combat.participants";
        }
        return right ? "screen.pf2craft.combat.npcs" : "screen.pf2craft.combat.players";
    }

    private void drawColumn(DrawContext context, int mouseX, int mouseY, boolean right) {
        List<Row> list = right ? rightRows : leftRows;
        int x = columnLeft(right);
        int headerY = listTop - HEADER_H;
        context.drawTextWithShadow(textRenderer,
                Text.translatable(headerKey(right)).formatted(Formatting.GOLD), x, headerY, 0xFFFFFF);
        context.fill(x, listTop, x + colW, listBottom, 0x80000000);
        context.drawBorder(x, listTop, colW, listBottom - listTop, 0xFF606060);
        if (list.isEmpty()) {
            context.drawTextWithShadow(textRenderer,
                    Language.getInstance().reorder(textRenderer.trimToWidth(
                            Text.translatable("screen.pf2craft.combat.none").formatted(Formatting.GRAY),
                            colW - 8)), x + 4, listTop + 4, 0xFFFFFF);
            return;
        }
        boolean removable = running && !right;
        int scroll = right ? rightScroll : leftScroll;
        int rows = visibleRows();
        for (int i = 0; i < rows && scroll + i < list.size(); i++) {
            Row row = list.get(scroll + i);
            int y = listTop + 1 + i * ROW_H;
            boolean hover = mouseX >= x && mouseX < x + colW && mouseY >= y && mouseY < y + ROW_H;
            if (hover) {
                context.fill(x + 1, y, x + colW - 1, y + ROW_H, 0x40FFFFFF);
            }
            int textX = x + 4;
            int textW = colW - 8;
            if (removable) {
                int cx = x + colW - BOX - 5;
                boolean overCross = mouseX >= cx && mouseX < cx + BOX + 2 && hover;
                context.drawTextWithShadow(textRenderer, "✕", cx + 1, y + 3, overCross ? 0xFFFF5555 : 0xFFAA5555);
                textW -= BOX + 6;
            } else {
                int bx = x + 4;
                int by = y + (ROW_H - BOX) / 2;
                context.drawBorder(bx, by, BOX, BOX, 0xFFC0C0C0);
                if (ticked.contains(row.id())) {
                    context.fill(bx + 2, by + 2, bx + BOX - 2, by + BOX - 2, 0xFF55FF55);
                }
                textX = bx + BOX + 4;
                textW -= BOX + 4;
            }
            context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(row.name(), textW), textX, y + 3,
                    0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0 || mouseY < listTop || mouseY >= listBottom) {
            return false;
        }
        for (boolean right : new boolean[] {false, true}) {
            int x = columnLeft(right);
            if (mouseX < x || mouseX >= x + colW) {
                continue;
            }
            List<Row> list = right ? rightRows : leftRows;
            int index = (right ? rightScroll : leftScroll) + (int) ((mouseY - listTop - 1) / ROW_H);
            if (index < 0 || index >= list.size()) {
                return false;
            }
            UUID id = list.get(index).id();
            if (running && !right) {
                if (mouseX >= x + colW - BOX - 5 && send(new CombatRemovePayload(id))) {
                    list.remove(index);
                    clampScrolls();
                }
            } else if (!ticked.remove(id)) {
                ticked.add(id);
            }
            return true;
        }
        return false;
    }

    private void clampScrolls() {
        leftScroll = MathHelper.clamp(leftScroll, 0, Math.max(0, leftRows.size() - visibleRows()));
        rightScroll = MathHelper.clamp(rightScroll, 0, Math.max(0, rightRows.size() - visibleRows()));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        boolean right = mouseX >= columnLeft(true);
        List<Row> list = right ? rightRows : leftRows;
        int max = Math.max(0, list.size() - visibleRows());
        int scroll = MathHelper.clamp((right ? rightScroll : leftScroll) - (int) Math.signum(vertical), 0, max);
        if (right) {
            rightScroll = scroll;
        } else {
            leftScroll = scroll;
        }
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
