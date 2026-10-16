package de.pf2craft.client;

import de.pf2craft.core.character.Ability;
import de.pf2craft.core.character.CharacterMath;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.CheckId;
import de.pf2craft.core.character.Feat;
import de.pf2craft.core.character.Lore;
import de.pf2craft.core.character.Rank;
import de.pf2craft.core.character.Save;
import de.pf2craft.core.character.Skill;
import de.pf2craft.core.character.Strike;
import de.pf2craft.network.CheckRollPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import net.minecraft.util.math.MathHelper;

/**
 * Character sheet with three tabs (overview, skills, feats). Check rows roll through the server; the
 * tab content scrolls when it does not fit. Layout is sized for ~640×360 scaled pixels (GUI scale 3
 * on 1080p).
 */
public class CharacterScreen extends Screen {
    private enum Tab { OVERVIEW, SKILLS, FEATS }

    private enum Align { LEFT, CENTER, RIGHT }

    /** A piece of text inside the scrollable content; {@code x} is the anchor for the alignment. */
    private record Label(Text text, int x, int relY, int maxWidth, Align align) {
    }

    /** A widget inside the scrollable content. */
    private record Placed(ClickableWidget widget, int relY) {
    }

    private static final int PANEL_W = 340;
    private static final int PANEL_H = 280;
    private static final int HEADER_H = 22;
    private static final int TAB_H = 18;
    private static final int FOOTER_H = 20;
    private static final int SCROLLBAR_W = 6;
    private static final int ROW_H = 14;
    private static final int ROW_STEP = 15;
    private static final int TEXT_STEP = 11;
    private static final int SCROLL_STEP = 15;

    private static Tab lastTab = Tab.OVERVIEW;
    private static boolean secret;

    private final UUID owner;
    private final List<Placed> placed = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();
    private Tab tab = lastTab;
    private int scroll;
    private int contentHeight;
    private int panelW;
    private int left;
    private int top;
    private int contentTop;
    private int contentBottom;

    public CharacterScreen(UUID owner) {
        super(Text.translatable("screen.pf2craft.sheet.title"));
        this.owner = owner;
    }

    public UUID owner() {
        return owner;
    }

    /** Rebuilds the widgets from the cached sheet, keeping tab and scroll position. */
    public void refresh() {
        clearAndInit();
    }

    private boolean isOwnSheet() {
        return client != null && client.player != null && owner.equals(client.player.getUuid());
    }

    @Override
    protected void init() {
        placed.clear();
        labels.clear();
        panelW = Math.min(PANEL_W, width - 8);
        int panelH = Math.min(PANEL_H, height - 8);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        int tabY = top + HEADER_H + 2;
        contentTop = tabY + TAB_H + 6;
        int footerY = top + panelH - FOOTER_H;
        contentBottom = footerY - 6;

        Optional<ClientSheets.Synced> synced = ClientSheets.get(owner);
        if (synced.isEmpty()) {
            return;
        }
        CharacterSheet sheet = synced.get().sheet();

        Tab[] tabs = Tab.values();
        int tabW = (panelW - (tabs.length - 1) * 2) / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            Tab t = tabs[i];
            ButtonWidget button = addDrawableChild(ButtonWidget.builder(
                            Text.translatable("screen.pf2craft.sheet.tab." + t.name().toLowerCase(Locale.ROOT)),
                            b -> switchTab(t))
                    .dimensions(left + i * (tabW + 2), tabY, tabW, TAB_H).build());
            button.active = t != tab;
        }

        addFooter(synced.get(), footerY);

        int contentW = panelW - SCROLLBAR_W;
        switch (tab) {
            case OVERVIEW -> buildOverview(sheet, contentW);
            case SKILLS -> buildSkills(sheet, contentW);
            case FEATS -> buildFeats(sheet, contentW);
        }
        contentHeight = 0;
        for (Placed p : placed) {
            contentHeight = Math.max(contentHeight, p.relY() + p.widget().getHeight());
        }
        for (Label l : labels) {
            contentHeight = Math.max(contentHeight, l.relY() + textRenderer.fontHeight);
        }
        setScroll(scroll);
    }

    private void addFooter(ClientSheets.Synced synced, int footerY) {
        addDrawableChild(CyclingButtonWidget.onOffBuilder(secret)
                .build(left, footerY, 100, FOOTER_H, Text.translatable("screen.pf2craft.sheet.secret"),
                        (button, value) -> secret = value));
        if (!isOwnSheet()) {
            return;
        }
        int right = left + panelW;
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pf2craft.sheet.reimport"),
                        b -> client.setScreen(new ImportScreen()))
                .dimensions(right - 80, footerY, 80, FOOTER_H).build());
        if (synced.sheet().levelUpGranted()) {
            ButtonWidget levelUp = addDrawableChild(ButtonWidget.builder(
                            Text.translatable("screen.pf2craft.sheet.levelup"), b -> openLevelUp())
                    .dimensions(right - 164, footerY, 80, FOOTER_H).build());
            levelUp.active = synced.classSupported();
            levelUp.setTooltip(Tooltip.of(Text.translatable(synced.classSupported()
                    ? "screen.pf2craft.sheet.levelup_available"
                    : "screen.pf2craft.sheet.levelup_unsupported")));
        }
    }

    private void openLevelUp() {
        // TODO(Task 9): open LevelUpScreen for the own sheet.
    }

    private void switchTab(Tab newTab) {
        tab = newTab;
        lastTab = newTab;
        scroll = 0;
        clearAndInit();
    }

    // --- tab content ------------------------------------------------------------------------

    private void buildOverview(CharacterSheet sheet, int contentW) {
        int cellW = contentW / Ability.values().length;
        for (int i = 0; i < Ability.values().length; i++) {
            Ability ability = Ability.values()[i];
            Text text = Text.translatable("pf2craft.ability." + ability.pathbuilderKey()).formatted(Formatting.GRAY)
                    .append(Text.literal(" " + signed(sheet.abilityMod(ability))).formatted(Formatting.WHITE));
            label(text, i * cellW + cellW / 2, 0, cellW - 2, Align.CENTER);
        }

        int colW = (contentW - 6) / 2;
        int rightCol = colW + 6;
        int y = 14;
        for (Save save : Save.values()) {
            checkRow(new CheckId(CheckId.Kind.SAVE, save.id(), 0), null,
                    Text.translatable("pf2craft.save." + save.id()), CharacterMath.save(sheet, save), 0, y, colW);
            y += ROW_STEP;
        }
        checkRow(new CheckId(CheckId.Kind.PERCEPTION, "", 0), null, Text.translatable("pf2craft.perception"),
                CharacterMath.perception(sheet), rightCol, 14, colW);
        label(Text.translatable("screen.pf2craft.sheet.speed", sheet.speed(), CharacterMath.speedBlocks(sheet)),
                rightCol + 4, 14 + ROW_STEP + 3, colW - 4, Align.LEFT);
        label(Text.translatable("screen.pf2craft.sheet.class_dc", CharacterMath.classDc(sheet)),
                rightCol + 4, 14 + 2 * ROW_STEP + 3, colW - 4, Align.LEFT);

        y += 6;
        label(Text.translatable("screen.pf2craft.sheet.strikes").formatted(Formatting.GRAY), 0, y, contentW,
                Align.LEFT);
        y += 12;
        if (sheet.strikes().isEmpty()) {
            label(Text.translatable("screen.pf2craft.sheet.none").formatted(Formatting.DARK_GRAY), 4, y,
                    contentW, Align.LEFT);
        }
        for (int i = 0; i < sheet.strikes().size(); i++) {
            Strike strike = sheet.strikes().get(i);
            Text value = Text.literal(signed(CharacterMath.strikeAttack(sheet, i)) + "  ")
                    .append(damage(strike, CharacterMath.strikeDamageBonus(sheet, i)).copy()
                            .formatted(Formatting.GRAY));
            addCheckRow(new CheckRow(0, 0, contentW, Text.literal(strike.name()), value, null,
                    rollAction(new CheckId(CheckId.Kind.STRIKE, "", i))), y);
            y += ROW_STEP;
        }
    }

    private void buildSkills(CharacterSheet sheet, int contentW) {
        int colW = (contentW - 6) / 2;
        List<Skill> skills = new ArrayList<>(List.of(Skill.values()));
        // Sort by the translated name, so the German list is alphabetical as well.
        skills.sort(Comparator.comparing(skill -> Text.translatable("pf2craft.skill." + skill.id()).getString()));
        int rows = (skills.size() + 1) / 2;
        for (int i = 0; i < skills.size(); i++) {
            Skill skill = skills.get(i);
            checkRow(new CheckId(CheckId.Kind.SKILL, skill.id(), 0), sheet.rank(skill.id()),
                    Text.translatable("pf2craft.skill." + skill.id()), CharacterMath.skill(sheet, skill),
                    (i / rows) * (colW + 6), (i % rows) * ROW_STEP, colW);
        }

        int y = rows * ROW_STEP + 6;
        label(Text.translatable("screen.pf2craft.sheet.lores").formatted(Formatting.GRAY), 0, y, contentW,
                Align.LEFT);
        y += 12;
        List<Lore> lores = sheet.lores();
        if (lores.isEmpty()) {
            label(Text.translatable("screen.pf2craft.sheet.none").formatted(Formatting.DARK_GRAY), 4, y,
                    contentW, Align.LEFT);
        }
        int loreRows = (lores.size() + 1) / 2;
        for (int i = 0; i < lores.size(); i++) {
            Lore lore = lores.get(i);
            checkRow(new CheckId(CheckId.Kind.LORE, "", i), lore.rank(), Text.literal(lore.name()),
                    CharacterMath.lore(sheet, i), (i / loreRows) * (colW + 6), y + (i % loreRows) * ROW_STEP, colW);
        }
    }

    private void buildFeats(CharacterSheet sheet, int contentW) {
        label(Text.translatable("screen.pf2craft.sheet.feats").formatted(Formatting.GRAY), 0, 0, contentW,
                Align.LEFT);
        int y = 12;
        List<Feat> feats = new ArrayList<>(sheet.feats());
        feats.sort(Comparator.comparingInt(Feat::level));
        if (feats.isEmpty()) {
            label(Text.translatable("screen.pf2craft.sheet.none").formatted(Formatting.DARK_GRAY), 4, y,
                    contentW, Align.LEFT);
            y += TEXT_STEP;
        }
        int nameW = contentW * 3 / 5;
        for (Feat feat : feats) {
            label(Text.literal(String.valueOf(feat.level())).formatted(Formatting.GRAY), 14, y, 14, Align.RIGHT);
            label(Text.literal(feat.name()), 20, y, nameW - 24, Align.LEFT);
            label(Text.literal(feat.type()).formatted(Formatting.DARK_GRAY), contentW - 2, y,
                    contentW - nameW - 4, Align.RIGHT);
            y += TEXT_STEP;
        }

        y += 6;
        label(Text.translatable("screen.pf2craft.sheet.specials").formatted(Formatting.GRAY), 0, y, contentW,
                Align.LEFT);
        y += 12;
        List<String> specials = sheet.specials();
        if (specials.isEmpty()) {
            label(Text.translatable("screen.pf2craft.sheet.none").formatted(Formatting.DARK_GRAY), 4, y,
                    contentW, Align.LEFT);
        }
        int colW = (contentW - 6) / 2;
        int rows = (specials.size() + 1) / 2;
        for (int i = 0; i < specials.size(); i++) {
            label(Text.literal(specials.get(i)), (i / rows) * (colW + 6) + 4, y + (i % rows) * TEXT_STEP,
                    colW - 4, Align.LEFT);
        }
    }

    private void label(Text text, int x, int relY, int maxWidth, Align align) {
        labels.add(new Label(text, x, relY, maxWidth, align));
    }

    private void checkRow(CheckId id, Rank rank, Text name, int bonus, int x, int relY, int w) {
        addCheckRow(new CheckRow(x, 0, w, name, Text.literal(signed(bonus)), rank == null ? null : rankLetter(rank),
                rollAction(id)), relY);
    }

    private void addCheckRow(CheckRow row, int relY) {
        row.setX(left + row.getX());
        row.rollable = isOwnSheet();
        row.active = row.rollable;
        placed.add(new Placed(addDrawableChild(row), relY));
    }

    private ButtonWidget.PressAction rollAction(CheckId id) {
        return b -> roll(id);
    }

    private void roll(CheckId id) {
        // The server only rolls checks of the sender's own character.
        if (client == null || !isOwnSheet()) {
            return;
        }
        if (!ClientPlayNetworking.canSend(CheckRollPayload.ID)) {
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("screen.pf2craft.dice.server_missing")
                        .formatted(Formatting.RED), false);
            }
            close();
            return;
        }
        ClientPlayNetworking.send(new CheckRollPayload(id.format(), secret));
        // Close so that the result in the HUD and chat is visible.
        close();
    }

    private static Text rankLetter(Rank rank) {
        Formatting color = switch (rank) {
            case UNTRAINED -> Formatting.DARK_GRAY;
            case TRAINED -> Formatting.WHITE;
            case EXPERT -> Formatting.GREEN;
            case MASTER -> Formatting.AQUA;
            case LEGENDARY -> Formatting.GOLD;
        };
        return Text.translatable("screen.pf2craft.sheet.rank." + rank.name().toLowerCase(Locale.ROOT))
                .formatted(color);
    }

    /** E.g. "d8+4 S"; the die letter follows the language ("W8" in German). */
    private static Text damage(Strike strike, int bonus) {
        String die = strike.damageDie();
        Text text;
        if (die.length() > 1 && (die.charAt(0) == 'd' || die.charAt(0) == 'D')
                && die.substring(1).chars().allMatch(c -> c >= '0' && c <= '9')) {
            text = Text.translatable("screen.pf2craft.dice.die", die.substring(1));
        } else {
            text = Text.literal(die);
        }
        String rest = (bonus != 0 ? signed(bonus) : "")
                + (strike.damageType().isEmpty() ? "" : " " + strike.damageType());
        return text.copy().append(rest);
    }

    private static String signed(int value) {
        return (value >= 0 ? "+" : "") + value;
    }

    // --- scrolling --------------------------------------------------------------------------

    private int maxScroll() {
        return Math.max(0, contentHeight - (contentBottom - contentTop));
    }

    private void setScroll(int value) {
        scroll = MathHelper.clamp(value, 0, maxScroll());
        for (Placed p : placed) {
            ClickableWidget widget = p.widget();
            int y = contentTop + p.relY() - scroll;
            widget.setY(y);
            widget.visible = y >= contentTop && y + widget.getHeight() <= contentBottom;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll() > 0 && mouseY >= contentTop && mouseY < contentBottom) {
            setScroll(scroll - (int) Math.round(verticalAmount * SCROLL_STEP));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // --- rendering --------------------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        Optional<ClientSheets.Synced> synced = ClientSheets.get(owner);
        if (synced.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.pf2craft.sheet.no_data"),
                    width / 2, height / 2 - 4, 0xFFFFFF);
            return;
        }
        renderHeader(context, synced.get().sheet());

        for (Label l : labels) {
            int y = contentTop + l.relY() - scroll;
            if (y < contentTop || y + textRenderer.fontHeight > contentBottom) {
                continue;
            }
            OrderedText text = fit(textRenderer, l.text(), l.maxWidth());
            int w = textRenderer.getWidth(text);
            int x = left + switch (l.align()) {
                case LEFT -> l.x();
                case CENTER -> l.x() - w / 2;
                case RIGHT -> l.x() - w;
            };
            context.drawTextWithShadow(textRenderer, text, x, y, 0xFFFFFF);
        }

        int max = maxScroll();
        if (max > 0) {
            int trackX = left + panelW - 3;
            int trackH = contentBottom - contentTop;
            int thumbH = Math.max(12, trackH * trackH / contentHeight);
            int thumbY = contentTop + (trackH - thumbH) * scroll / max;
            context.fill(trackX, contentTop, trackX + 3, contentBottom, 0x40FFFFFF);
            context.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xC0FFFFFF);
        }
    }

    private void renderHeader(DrawContext context, CharacterSheet sheet) {
        int right = left + panelW;
        int maxHp = CharacterMath.maxHp(sheet);
        Text ac = Text.translatable("screen.pf2craft.sheet.ac", CharacterMath.armorClass(sheet));
        Formatting hpColor = sheet.currentHp() * 4 <= maxHp ? Formatting.RED
                : sheet.currentHp() * 2 <= maxHp ? Formatting.YELLOW : Formatting.WHITE;
        Text hp = Text.translatable("screen.pf2craft.sheet.hp", sheet.currentHp(), maxHp).formatted(hpColor);
        int acW = textRenderer.getWidth(ac);
        int hpW = textRenderer.getWidth(hp);
        context.drawTextWithShadow(textRenderer, ac, right - acW, top, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, hp, right - hpW, top + 11, 0xFFFFFF);

        Text name = Text.literal(sheet.name()).formatted(Formatting.BOLD);
        context.drawTextWithShadow(textRenderer, fit(textRenderer, name, panelW - acW - 8), left, top, 0xFFFFFF);
        Text subtitle = Text.translatable("screen.pf2craft.sheet.subtitle", sheet.ancestry(), sheet.className(),
                sheet.level()).formatted(Formatting.GRAY);
        context.drawTextWithShadow(textRenderer, fit(textRenderer, subtitle, panelW - hpW - 8), left, top + 11,
                0xFFFFFF);
    }

    /** Cuts the text to {@code maxWidth} pixels and adds "..." if it was too long. */
    private static OrderedText fit(TextRenderer renderer, Text text, int maxWidth) {
        if (renderer.getWidth(text) <= maxWidth) {
            return text.asOrderedText();
        }
        StringVisitable cut = renderer.trimToWidth(text, Math.max(0, maxWidth - renderer.getWidth("...")));
        return Language.getInstance().reorder(StringVisitable.concat(cut, StringVisitable.plain("...")));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /**
     * A check row in vanilla button look: optional rank letter, name on the left, bonus on the right.
     * Rows that cannot be rolled (another player's sheet) are drawn as plain text.
     */
    private static final class CheckRow extends ButtonWidget {
        private final Text name;
        private final Text value;
        private final Text rank;
        private boolean rollable = true;

        CheckRow(int x, int y, int width, Text name, Text value, Text rank, PressAction onPress) {
            super(x, y, width, ROW_H, name, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.name = name;
            this.value = value;
            this.rank = rank;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            if (rollable) {
                super.renderWidget(context, mouseX, mouseY, delta);
            } else {
                drawMessage(context, MinecraftClient.getInstance().textRenderer, 0xFFFFFFFF);
            }
        }

        @Override
        public void drawMessage(DrawContext context, TextRenderer renderer, int color) {
            int textY = getY() + (getHeight() - 8) / 2;
            int x = getX() + 4;
            if (rank != null) {
                context.drawTextWithShadow(renderer, rank, x, textY, color);
                x += 10;
            }
            int right = getX() + getWidth() - 4;
            int valueW = renderer.getWidth(value);
            context.drawTextWithShadow(renderer, value, right - valueW, textY, color);
            context.drawTextWithShadow(renderer, fit(renderer, name, right - valueW - 6 - x), x, textY, color);
        }
    }
}
