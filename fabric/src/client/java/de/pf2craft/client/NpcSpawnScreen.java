package de.pf2craft.client;

import de.pf2craft.network.NpcSearchPayload;
import de.pf2craft.network.NpcSearchResultPayload;
import de.pf2craft.network.NpcSpawnPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * GM screen opened by the command staff: search the monster catalog and click a result to spawn it. Searches
 * go to the server (debounced); the list scrolls with the mouse wheel. Sized for ~640x360 scaled pixels.
 */
public class NpcSpawnScreen extends Screen {
    private static final int PANEL_W = 300;
    private static final int PANEL_H = 280;
    private static final int HEADER_H = 14;
    private static final int FIELD_H = 20;
    private static final int SEARCH_W = 60;
    private static final int ROW_H = 14;
    private static final int FOOTER_H = 12;
    private static final int SCROLLBAR_W = 6;
    private static final long DEBOUNCE_MS = 300;
    private static final long REPEAT_MS = 250;

    private final List<NpcSearchResultPayload.Entry> entries = new ArrayList<>();
    private TextFieldWidget searchField;
    private String searchText = "";
    /** When the next search request is due, or -1 if none is pending. */
    private long searchDueAt;
    private long lastSentAt;
    private boolean received;
    private int scroll;
    private int left;
    private int top;
    private int panelW;
    private int listTop;
    private int listBottom;
    private Text status = Text.empty();

    public NpcSpawnScreen() {
        super(Text.translatable("screen.pf2craft.npc.spawn.title"));
    }

    @Override
    protected void init() {
        panelW = Math.min(PANEL_W, width - 8);
        int panelH = Math.min(PANEL_H, height - 8);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;

        int fieldY = top + HEADER_H + 4;
        searchField = new TextFieldWidget(textRenderer, left, fieldY, panelW - SEARCH_W - 4, FIELD_H,
                Text.translatable("screen.pf2craft.npc.spawn.search"));
        searchField.setMaxLength(NpcSearchPayload.MAX_TEXT);
        searchField.setText(searchText);
        searchField.setPlaceholder(Text.translatable("screen.pf2craft.npc.spawn.placeholder")
                .formatted(Formatting.DARK_GRAY));
        searchField.setChangedListener(text -> {
            searchText = text;
            searchDueAt = System.currentTimeMillis() + DEBOUNCE_MS;
        });
        addDrawableChild(searchField);
        setInitialFocus(searchField);
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pf2craft.npc.spawn.search"),
                        b -> sendSearch())
                .dimensions(left + panelW - SEARCH_W, fieldY, SEARCH_W, FIELD_H).build());

        listTop = fieldY + FIELD_H + 6;
        listBottom = top + panelH - FOOTER_H - 4;
        clampScroll();
        if (!received) {
            // First request right away, so the list is not empty when the screen opens.
            searchDueAt = System.currentTimeMillis();
        }
    }

    private void sendSearch() {
        searchDueAt = -1;
        if (client == null || !ClientPlayNetworking.canSend(NpcSearchPayload.ID)) {
            status = Text.translatable("screen.pf2craft.dice.server_missing").formatted(Formatting.RED);
            return;
        }
        lastSentAt = System.currentTimeMillis();
        ClientPlayNetworking.send(new NpcSearchPayload(searchText.trim()));
    }

    public void onResults(List<NpcSearchResultPayload.Entry> results) {
        entries.clear();
        entries.addAll(results);
        received = true;
        scroll = 0;
    }

    /** Shows a server answer (e.g. "Spawned ...") in the footer line. */
    public void onServerMessage(Text message) {
        status = message;
    }

    @Override
    public void tick() {
        long now = System.currentTimeMillis();
        if (searchDueAt >= 0 && now >= searchDueAt && now - lastSentAt >= REPEAT_MS) {
            sendSearch();
        }
    }

    private int visibleRows() {
        return Math.max(1, (listBottom - listTop) / ROW_H);
    }

    private int maxScroll() {
        return Math.max(0, entries.size() - visibleRows());
    }

    private void clampScroll() {
        scroll = MathHelper.clamp(scroll, 0, maxScroll());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseY >= listTop && mouseY < listBottom) {
            scroll -= (int) Math.signum(vertical) * 2;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= left && mouseX < left + panelW - SCROLLBAR_W
                && mouseY >= listTop && mouseY < listBottom) {
            int index = scroll + (int) ((mouseY - listTop) / ROW_H);
            if (index < entries.size() && index < scroll + visibleRows()) {
                spawn(entries.get(index));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void spawn(NpcSearchResultPayload.Entry entry) {
        if (!ClientPlayNetworking.canSend(NpcSpawnPayload.ID)) {
            status = Text.translatable("screen.pf2craft.dice.server_missing").formatted(Formatting.RED);
            return;
        }
        ClientPlayNetworking.send(new NpcSpawnPayload(entry.id()));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchField != null && searchField.isFocused()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            sendSearch();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top + 2, 0xFFFFFF);

        int listW = panelW - SCROLLBAR_W;
        context.fill(left, listTop, left + panelW, listBottom, 0x80000000);
        if (entries.isEmpty() && received) {
            context.drawTextWithShadow(textRenderer,
                    Text.translatable("screen.pf2craft.npc.spawn.none").formatted(Formatting.GRAY),
                    left + 4, listTop + 4, 0xFFFFFF);
        }
        int rows = visibleRows();
        for (int i = 0; i < rows && scroll + i < entries.size(); i++) {
            NpcSearchResultPayload.Entry entry = entries.get(scroll + i);
            int y = listTop + i * ROW_H;
            boolean hover = mouseX >= left && mouseX < left + listW && mouseY >= y && mouseY < y + ROW_H;
            if (hover) {
                context.fill(left, y, left + listW, y + ROW_H, 0x60FFFFFF);
            }
            Text level = Text.translatable("screen.pf2craft.npc.spawn.level", entry.level())
                    .formatted(Formatting.GRAY);
            int levelW = textRenderer.getWidth(level);
            context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(entry.name(), listW - levelW - 12),
                    left + 4, y + 3, 0xFFFFFF);
            context.drawTextWithShadow(textRenderer, level, left + listW - levelW - 4, y + 3, 0xFFFFFF);
        }
        if (maxScroll() > 0) {
            int trackH = listBottom - listTop;
            int thumbH = Math.max(10, trackH * rows / entries.size());
            int thumbY = listTop + (trackH - thumbH) * scroll / maxScroll();
            context.fill(left + panelW - SCROLLBAR_W + 1, thumbY, left + panelW, thumbY + thumbH, 0xFFAAAAAA);
        }
        Text footer = status.getString().isEmpty()
                ? Text.translatable("screen.pf2craft.npc.spawn.hint").formatted(Formatting.GRAY) : status;
        context.drawTextWithShadow(textRenderer, Language.getInstance().reorder(textRenderer.trimToWidth(footer, panelW)),
                left, listBottom + 4, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
