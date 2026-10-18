package de.pf2craft.client;

import de.pf2craft.core.monster.DamagePart;
import de.pf2craft.core.monster.MonsterData;
import de.pf2craft.core.monster.MonsterStrike;
import de.pf2craft.core.monster.NpcJson;
import de.pf2craft.core.monster.NpcStats;
import de.pf2craft.core.monster.Resistance;
import de.pf2craft.network.NpcActionPayload;
import de.pf2craft.network.NpcEditPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * GM-only stat block of one NPC with edit controls. Every change goes to the server, which answers with
 * a fresh sheet that rebuilds this screen. Sized for ~640x360 scaled pixels; the read-only part (strikes,
 * resistances, abilities) scrolls.
 */
public class NpcSheetScreen extends Screen {
    /** Only dims the game behind the window; the vanilla background blur would smear the text. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);
    }

    private static final int PANEL_W = 340;
    private static final int PANEL_H = 280;
    private static final int HEADER_H = 14;
    private static final int ROW_STEP = 22;
    private static final int ROW_H = 18;
    private static final int LABEL_W = 68;
    private static final int SET_W = 46;
    private static final int STEP_W = 16;
    private static final int FOOTER_H = 20;
    private static final int STATUS_H = 11;
    private static final int LINE_H = 10;
    private static final int SCROLLBAR_W = 6;
    private static final int HP_STEP_SHIFT = 5;
    private static final long CONFIRM_MS = 3000;
    private static final Pattern DICE = Pattern.compile("(\\d*)d(\\d+)");
    /** Edit fields in two columns: server field name and its label key suffix. */
    private static final String[][] GRID = {
            {"maxhp", "maxhp"}, {"level", "level"},
            {"ac", "ac"}, {"perception", "perception"},
            {"fortitude", "fortitude"}, {"reflex", "reflex"},
            {"will", "will"}, {"speed", "speed"}};

    private final int entityId;
    private NpcStats stats;
    private final List<OrderedText> infoLines = new ArrayList<>();
    private TextFieldWidget nameField;
    private ButtonWidget removeButton;
    private int left;
    private int top;
    private int panelW;
    private int infoTop;
    private int infoBottom;
    private int statusY;
    private int scroll;
    private Text status = Text.empty();
    /** When the first "remove" click happened, or 0 if no confirmation is pending. */
    private long removeAskedAt;

    public NpcSheetScreen(int entityId, String json) {
        super(Text.empty());
        this.entityId = entityId;
        this.stats = parse(json);
    }

    public int entityId() {
        return entityId;
    }

    private static NpcStats parse(String json) {
        try {
            return NpcJson.read(json);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** The server sent a fresh stat block after a change. */
    public void update(String json) {
        NpcStats parsed = parse(json);
        if (parsed != null) {
            stats = parsed;
            clearAndInit();
        }
    }

    /** Shows a server answer (saved, error ...) in the status line. */
    public void onServerMessage(Text message) {
        status = message;
    }

    @Override
    protected void init() {
        panelW = Math.min(PANEL_W, width - 8);
        int panelH = Math.min(PANEL_H, height - 8);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        int footerY = top + panelH - FOOTER_H;
        statusY = footerY - STATUS_H - 2;
        infoBottom = statusY - 2;
        if (stats == null) {
            return;
        }
        MonsterData base = stats.base();

        int y = top + HEADER_H + 4;
        // Name (the labels are drawn in render)
        nameField = new TextFieldWidget(textRenderer, left + LABEL_W, y, panelW - LABEL_W - SET_W - 4, ROW_H,
                Text.translatable("screen.pf2craft.npc.sheet.name"));
        nameField.setMaxLength(32);
        nameField.setText(stats.displayName());
        addDrawableChild(nameField);
        addDrawableChild(setButton(left + panelW - SET_W, y, () -> sendEdit("name", nameField.getText())));

        // Hit points: - / + and a field
        y += ROW_STEP;
        int x = left + LABEL_W;
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> changeHp(-1))
                .dimensions(x, y, STEP_W, ROW_H)
                .tooltip(Tooltip.of(Text.translatable("screen.pf2craft.npc.sheet.hp_minus"))).build());
        x += STEP_W + 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> changeHp(1))
                .dimensions(x, y, STEP_W, ROW_H)
                .tooltip(Tooltip.of(Text.translatable("screen.pf2craft.npc.sheet.hp_plus"))).build());
        x += STEP_W + 4;
        TextFieldWidget hpField = numberField(x, y, panelW - (x - left) - SET_W - 4, String.valueOf(stats.currentHp()));
        addDrawableChild(hpField);
        addDrawableChild(setButton(left + panelW - SET_W, y, () -> sendEdit("hp", hpField.getText())));

        // The numeric fields in two columns
        y += ROW_STEP;
        int colW = (panelW - 6) / 2;
        for (int i = 0; i < GRID.length; i++) {
            String field = GRID[i][0];
            int cellX = left + (i % 2) * (colW + 6);
            int cellY = y + (i / 2) * ROW_STEP;
            int fieldW = colW - LABEL_W - SET_W - 4;
            TextFieldWidget input = numberField(cellX + LABEL_W, cellY, fieldW, String.valueOf(value(base, field)));
            addDrawableChild(input);
            addDrawableChild(setButton(cellX + colW - SET_W, cellY, () -> sendEdit(field, input.getText())));
        }
        infoTop = y + (GRID.length / 2) * ROW_STEP + 2;

        addFooter(footerY);
        buildInfo(base);
        clampScroll();
    }

    private ButtonWidget setButton(int x, int y, Runnable action) {
        return ButtonWidget.builder(Text.translatable("screen.pf2craft.npc.sheet.set"), b -> action.run())
                .dimensions(x, y, SET_W, ROW_H).build();
    }

    private TextFieldWidget numberField(int x, int y, int w, String value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, w, ROW_H, Text.empty());
        field.setMaxLength(5);
        field.setTextPredicate(text -> text.chars().allMatch(c -> (c >= '0' && c <= '9') || c == '-'));
        field.setText(value);
        return field;
    }

    private static int value(MonsterData m, String field) {
        return switch (field) {
            case "maxhp" -> m.hp();
            case "level" -> m.level();
            case "ac" -> m.ac();
            case "perception" -> m.perception();
            case "fortitude" -> m.fortitude();
            case "reflex" -> m.reflex();
            case "will" -> m.will();
            case "speed" -> m.speed();
            default -> 0;
        };
    }

    private void addFooter(int footerY) {
        int third = (panelW - 8) / 3;
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pf2craft.npc.sheet.template"),
                        b -> send(new NpcActionPayload(entityId, "save_template")))
                .dimensions(left, footerY, third + 20, FOOTER_H).build());
        removeButton = addDrawableChild(ButtonWidget.builder(removeLabel(), b -> remove())
                .dimensions(left + third + 24, footerY, third - 20, FOOTER_H).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(left + 2 * third + 8, footerY, panelW - 2 * third - 8, FOOTER_H).build());
    }

    private Text removeLabel() {
        return removeAskedAt == 0
                ? Text.translatable("screen.pf2craft.npc.sheet.remove")
                : Text.translatable("screen.pf2craft.npc.sheet.remove_confirm").formatted(Formatting.RED);
    }

    private void remove() {
        if (removeAskedAt == 0) {
            removeAskedAt = System.currentTimeMillis();
            removeButton.setMessage(removeLabel());
            return;
        }
        send(new NpcActionPayload(entityId, "remove"));
        close();
    }

    @Override
    public void tick() {
        if (removeAskedAt != 0 && System.currentTimeMillis() - removeAskedAt > CONFIRM_MS) {
            removeAskedAt = 0;
            if (removeButton != null) {
                removeButton.setMessage(removeLabel());
            }
        }
    }

    private void changeHp(int sign) {
        int step = hasShiftDown() ? HP_STEP_SHIFT : 1;
        int hp = MathHelper.clamp(stats.currentHp() + sign * step, 0, stats.base().hp());
        sendEdit("hp", String.valueOf(hp));
    }

    private void sendEdit(String field, String value) {
        send(new NpcEditPayload(entityId, field, value.trim()));
    }

    private void send(CustomPayload payload) {
        if (!ClientPlayNetworking.canSend(payload.getId())) {
            status = Text.translatable("screen.pf2craft.dice.server_missing").formatted(Formatting.RED);
            return;
        }
        ClientPlayNetworking.send(payload);
    }

    // --- read-only part ---------------------------------------------------------------------

    private void buildInfo(MonsterData m) {
        infoLines.clear();
        int wrapW = panelW - SCROLLBAR_W - 4;
        heading("strikes", wrapW);
        if (m.strikes().isEmpty()) {
            none(wrapW);
        }
        for (MonsterStrike strike : m.strikes()) {
            String damage = strike.damage().stream().map(NpcSheetScreen::damage)
                    .collect(Collectors.joining(", "));
            add(Text.literal(strike.name() + " " + signed(strike.attackBonus()) + "  ")
                    .append(Text.literal(damage).formatted(Formatting.GRAY)), wrapW);
        }
        resistanceBlock("resistances", m.resistances(), wrapW);
        resistanceBlock("weaknesses", m.weaknesses(), wrapW);
        heading("immunities", wrapW);
        if (m.immunities().isEmpty()) {
            none(wrapW);
        } else {
            add(Text.literal(m.immunities().stream().map(NpcSheetScreen::type).map(Text::getString)
                    .collect(Collectors.joining(", "))), wrapW);
        }
        heading("abilities", wrapW);
        if (m.abilities().isEmpty()) {
            none(wrapW);
        } else {
            add(Text.literal(String.join(", ", m.abilities())), wrapW);
        }
    }

    private void resistanceBlock(String key, List<Resistance> list, int wrapW) {
        heading(key, wrapW);
        if (list.isEmpty()) {
            none(wrapW);
            return;
        }
        add(Text.literal(list.stream().map(r -> type(r.type()).getString() + " " + r.value())
                .collect(Collectors.joining(", "))), wrapW);
    }

    private void heading(String key, int wrapW) {
        add(Text.translatable("screen.pf2craft.npc.sheet." + key).formatted(Formatting.GOLD), wrapW);
    }

    private void none(int wrapW) {
        add(Text.translatable("screen.pf2craft.npc.sheet.none").formatted(Formatting.DARK_GRAY), wrapW);
    }

    private void add(Text text, int wrapW) {
        infoLines.addAll(textRenderer.wrapLines(text, wrapW));
    }

    /** E.g. "1W6+2 Hieb"; the die letter and the damage type follow the language. */
    private static String damage(DamagePart d) {
        String bonus = d.bonus() == 0 ? "" : (d.bonus() > 0 ? "+" : "") + d.bonus();
        String amount;
        Matcher m = DICE.matcher(d.dice());
        if (d.dice().isEmpty()) {
            amount = String.valueOf(d.bonus());
        } else if (m.matches()) {
            amount = m.group(1) + Text.translatable("screen.pf2craft.dice.die", m.group(2)).getString() + bonus;
        } else {
            amount = d.dice() + bonus;
        }
        return amount + " " + type(d.type()).getString();
    }

    private static MutableText type(String type) {
        return Text.translatableWithFallback("pf2craft.damage.type." + type, type);
    }

    private static String signed(int value) {
        return (value >= 0 ? "+" : "") + value;
    }

    private int visibleLines() {
        return Math.max(1, (infoBottom - infoTop) / LINE_H);
    }

    private int maxScroll() {
        return Math.max(0, infoLines.size() - visibleLines());
    }

    private void clampScroll() {
        scroll = MathHelper.clamp(scroll, 0, maxScroll());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseY >= infoTop && mouseY < infoBottom) {
            scroll -= (int) Math.signum(vertical) * 2;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (nameField != null && nameField.isFocused()) {
                sendEdit("name", nameField.getText());
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (stats == null) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("screen.pf2craft.npc.sheet.invalid").formatted(Formatting.RED),
                    width / 2, height / 2, 0xFFFFFF);
            return;
        }
        MonsterData m = stats.base();
        Text header = Text.translatable("screen.pf2craft.npc.sheet.title", stats.displayName(), m.name(), m.level());
        context.drawCenteredTextWithShadow(textRenderer, header, width / 2, top + 2, 0xFFFFFF);

        int y = top + HEADER_H + 4;
        rowLabel(context, Text.translatable("screen.pf2craft.npc.sheet.name"), left, y);
        y += ROW_STEP;
        rowLabel(context, Text.translatable("screen.pf2craft.npc.sheet.hp", stats.currentHp(), m.hp()), left, y);
        y += ROW_STEP;
        int colW = (panelW - 6) / 2;
        for (int i = 0; i < GRID.length; i++) {
            rowLabel(context, Text.translatable("screen.pf2craft.npc.sheet." + GRID[i][1]),
                    left + (i % 2) * (colW + 6), y + (i / 2) * ROW_STEP);
        }

        context.fill(left, infoTop, left + panelW, infoBottom, 0x80000000);
        int lines = visibleLines();
        for (int i = 0; i < lines && scroll + i < infoLines.size(); i++) {
            context.drawTextWithShadow(textRenderer, infoLines.get(scroll + i), left + 3,
                    infoTop + 2 + i * LINE_H, 0xFFFFFF);
        }
        if (maxScroll() > 0) {
            int trackH = infoBottom - infoTop;
            int thumbH = Math.max(10, trackH * lines / infoLines.size());
            int thumbY = infoTop + (trackH - thumbH) * scroll / maxScroll();
            context.fill(left + panelW - SCROLLBAR_W + 1, thumbY, left + panelW, thumbY + thumbH, 0xFFAAAAAA);
        }
        if (!status.getString().isEmpty()) {
            context.drawTextWithShadow(textRenderer,
                    Language.getInstance().reorder(textRenderer.trimToWidth(status, panelW)), left,
                    statusY + 1, 0xFFFFFF);
        }
    }

    private void rowLabel(DrawContext context, Text text, int x, int y) {
        context.drawTextWithShadow(textRenderer, text.copy().formatted(Formatting.GRAY), x, y + 5, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
