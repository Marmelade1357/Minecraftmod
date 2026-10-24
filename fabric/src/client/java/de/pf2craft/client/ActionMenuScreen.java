package de.pf2craft.client;

import de.pf2craft.core.combat.BasicAction;
import de.pf2craft.core.combat.BasicActions;
import de.pf2craft.core.combat.Condition;
import de.pf2craft.core.spell.CastingType;
import de.pf2craft.core.spell.SpellcastingEntry;
import de.pf2craft.core.spell.Spellbook;
import de.pf2craft.network.ActionRequestPayload;
import de.pf2craft.network.CombatSyncPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The action menu (key V): categories on the left, the actions of the chosen category on the right with
 * favorites first. A click only sends a request; the server checks everything again. Sized for ~640x360
 * scaled pixels.
 */
public class ActionMenuScreen extends Screen {
    private static final int PANEL_W = 340;
    private static final int PANEL_H = 190;
    private static final int CATEGORY_W = 100;
    private static final int ROW_H = 20;
    private static final int STAR_W = 14;
    private static final int PAD = 8;
    private static final int HEADER = 24;
    private static final int COLOR_PANEL = 0xD0101010;
    private static final int COLOR_BORDER = 0xFF606060;
    private static final int COLOR_SELECTED = 0xFF5A4A10;
    private static final int COLOR_HOVER = 0x40FFFFFF;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GREY = 0xFF808080;
    private static final int COLOR_GOLD = 0xFFFFC800;

    private enum Category {
        BASIC("basic", true),
        CLASS("class", true),
        SPELLS("spells", true),
        ITEMS("items", false);

        private final String key;
        private final boolean enabled;

        Category(String key, boolean enabled) {
            this.key = key;
            this.enabled = enabled;
        }
    }

    /** One castable line of the spell category: the spell as listed in an entry, at the rank it would be cast. */
    private record SpellRow(int entry, String spell, int rank) {
    }

    private Category category = Category.BASIC;
    private int spellScroll;
    private int left;
    private int top;
    private int panelW;

    public ActionMenuScreen() {
        super(Text.translatable("screen.pf2craft.action_menu.title"));
    }

    /**
     * Who acts: the own player when it is their turn, a GM the NPC that is up (the exact hit points in the
     * sync only go to GMs), otherwise nobody.
     */
    public static Optional<UUID> actor(MinecraftClient client) {
        if (client.player == null) {
            return Optional.empty();
        }
        Optional<CombatSyncPayload.Entry> current = ClientCombat.current();
        if (current.isEmpty()) {
            return Optional.empty();
        }
        CombatSyncPayload.Entry entry = current.get();
        Optional<UUID> controlled = ClientControl.controlled();
        if (controlled.isPresent()) {
            // While controlling, the controlled NPC is the actor (and only on its turn).
            return controlled.get().equals(entry.id()) ? controlled : Optional.empty();
        }
        if (entry.id().equals(client.player.getUuid())) {
            return Optional.of(entry.id());
        }
        return entry.npc() && entry.hp() >= 0 ? Optional.of(entry.id()) : Optional.empty();
    }

    /** Only dims the game behind the window; the vanilla background blur would smear the text. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        panelW = Math.min(PANEL_W, width - 16);
        left = (width - panelW) / 2;
        top = Math.max(8, (height - PANEL_H) / 2);
    }

    private int rowY(int index) {
        return top + HEADER + index * (ROW_H + 2);
    }

    private int listX() {
        return left + PAD + CATEGORY_W + PAD;
    }

    private int listW() {
        return panelW - (listX() - left) - PAD;
    }

    /** Favorites first, otherwise in the order of the definitions. */
    private List<BasicAction> sortedActions() {
        List<BasicAction> result = new ArrayList<>();
        for (BasicAction a : BasicActions.ALL) {
            if (ClientActions.favorites().contains(a.id())) {
                result.add(a);
            }
        }
        for (BasicAction a : BasicActions.ALL) {
            if (!result.contains(a)) {
                result.add(a);
            }
        }
        return result;
    }

    /** Why the action cannot be used right now, empty if it can. */
    private Optional<Text> unavailable(BasicAction action) {
        Optional<UUID> actor = actor(client);
        Optional<CombatSyncPayload> combat = ClientCombat.get();
        if (actor.isEmpty() || combat.isEmpty()) {
            return Optional.of(Text.translatable("screen.pf2craft.action_menu.not_your_turn"));
        }
        if (combat.get().actionsLeft() < action.cost()) {
            return Optional.of(Text.translatable("screen.pf2craft.action_menu.no_actions"));
        }
        if ("stride".equals(action.id()) && combat.get().strideBlocksLeft() > 0) {
            return Optional.of(Text.translatable("screen.pf2craft.action_menu.stride_active"));
        }
        // The sync carries the actor's conditions (own ones for a player, all of them for a GM).
        var conditions = ClientConditions.of(actor.get());
        if ("stand_up".equals(action.id()) && !conditions.containsKey(Condition.PRONE)) {
            return Optional.of(Text.translatable("screen.pf2craft.action_menu.not_prone"));
        }
        if ("escape".equals(action.id()) && !conditions.containsKey(Condition.GRABBED)) {
            return Optional.of(Text.translatable("screen.pf2craft.action_menu.not_grabbed"));
        }
        if (action.needsShield()) {
            boolean ownShield = client.player != null && actor.get().equals(client.player.getUuid())
                    && ClientSheets.get(client.player.getUuid()).map(s -> s.sheet().shield().isPresent())
                            .orElse(false);
            if (!ownShield) {
                return Optional.of(Text.translatable("screen.pf2craft.action_menu.no_shield"));
            }
        }
        return Optional.empty();
    }

    /** The own spells of all entries (only for the player's own character), cantrips first. */
    private List<SpellRow> spellRows() {
        if (client.player == null) {
            return List.of();
        }
        Optional<Spellbook> book = ClientSpells.own(client);
        if (book.isEmpty()) {
            return List.of();
        }
        List<SpellRow> rows = new ArrayList<>();
        for (int i = 0; i < book.get().entries().size(); i++) {
            SpellcastingEntry entry = book.get().entries().get(i);
            for (var byRank : entry.spellsByRank().entrySet()) {
                int listed = byRank.getKey();
                if (listed < 0 || listed >= SpellcastingEntry.RANKS) {
                    continue;
                }
                for (String spell : byRank.getValue()) {
                    rows.add(new SpellRow(i, spell, castRank(book.get(), i, entry, listed)));
                }
            }
        }
        return rows;
    }

    /** Spontaneous spells can be heightened: the menu uses the lowest rank (from the listed one) with a slot. */
    private static int castRank(Spellbook book, int index, SpellcastingEntry entry, int listed) {
        if (entry.type() == CastingType.SPONTANEOUS && listed > 0) {
            for (int r = listed; r < SpellcastingEntry.RANKS; r++) {
                if (book.slotsLeft(index, r) > 0) {
                    return r;
                }
            }
        }
        return listed;
    }

    private int visibleSpellRows() {
        return (PANEL_H - HEADER - PAD) / (ROW_H + 2);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(left, top, left + panelW, top + PANEL_H, COLOR_PANEL);
        context.drawBorder(left, top, panelW, PANEL_H, COLOR_BORDER);
        context.drawTextWithShadow(textRenderer, title.copy().formatted(Formatting.GOLD, Formatting.BOLD),
                left + PAD, top + 8, COLOR_WHITE);

        Text tooltip = null;
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            Category c = categories[i];
            int y = rowY(i);
            boolean hover = inside(mouseX, mouseY, left + PAD, y, CATEGORY_W, ROW_H);
            if (c == category) {
                context.fill(left + PAD, y, left + PAD + CATEGORY_W, y + ROW_H, COLOR_SELECTED);
            } else if (hover && c.enabled) {
                context.fill(left + PAD, y, left + PAD + CATEGORY_W, y + ROW_H, COLOR_HOVER);
            }
            context.drawBorder(left + PAD, y, CATEGORY_W, ROW_H, COLOR_BORDER);
            Text name = Text.translatable("screen.pf2craft.action_menu.category." + c.key);
            context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(name.getString(), CATEGORY_W - 8),
                    left + PAD + 4, y + 6, c.enabled ? COLOR_WHITE : COLOR_GREY);
            if (hover && !c.enabled) {
                tooltip = Text.translatable("screen.pf2craft.action_menu.soon");
            }
        }

        int x = listX();
        int w = listW();
        switch (category) {
            case BASIC -> {
                List<BasicAction> actions = sortedActions();
                for (int i = 0; i < actions.size(); i++) {
                    BasicAction a = actions.get(i);
                    int y = rowY(i);
                    Optional<Text> reason = unavailable(a);
                    boolean hover = inside(mouseX, mouseY, x, y, w, ROW_H);
                    if (hover && reason.isEmpty()) {
                        context.fill(x, y, x + w, y + ROW_H, COLOR_HOVER);
                    }
                    context.drawBorder(x, y, w, ROW_H, COLOR_BORDER);
                    boolean favorite = ClientActions.favorites().contains(a.id());
                    context.drawTextWithShadow(textRenderer, favorite ? "★" : "☆", x + 4, y + 6,
                            favorite ? COLOR_GOLD : COLOR_GREY);
                    context.drawTextWithShadow(textRenderer,
                            Text.translatable("pf2craft.combat.action.name." + a.id()), x + 4 + STAR_W, y + 6,
                            reason.isEmpty() ? COLOR_WHITE : COLOR_GREY);
                    String cost = "◆".repeat(a.cost());
                    context.drawTextWithShadow(textRenderer, cost, x + w - 4 - textRenderer.getWidth(cost), y + 6,
                            reason.isEmpty() ? COLOR_GOLD : COLOR_GREY);
                    if (hover && reason.isPresent() && mouseX > x + STAR_W) {
                        tooltip = reason.get();
                    }
                }
            }
            case SPELLS -> tooltip = renderSpells(context, mouseX, mouseY, x, w);
            case CLASS -> context.drawTextWithShadow(textRenderer,
                    Text.translatable("screen.pf2craft.action_menu.none_yet"), x + 4, top + HEADER + 6, COLOR_GREY);
            default -> context.drawTextWithShadow(textRenderer,
                    Text.translatable("screen.pf2craft.action_menu.soon"), x + 4, top + HEADER + 6, COLOR_GREY);
        }
        if (tooltip != null) {
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
        }
    }

    /** Draws the spell rows; returns the tooltip of the hovered row, if any. */
    private Text renderSpells(DrawContext context, int mouseX, int mouseY, int x, int w) {
        List<SpellRow> rows = spellRows();
        Optional<Spellbook> book = ClientSpells.own(client);
        if (rows.isEmpty() || book.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.pf2craft.spells.none"), x + 4,
                    top + HEADER + 6, COLOR_GREY);
            return null;
        }
        Text tooltip = null;
        int shown = visibleSpellRows();
        spellScroll = Math.max(0, Math.min(spellScroll, rows.size() - shown));
        for (int i = 0; i < shown && spellScroll + i < rows.size(); i++) {
            SpellRow row = rows.get(spellScroll + i);
            int y = rowY(i);
            Optional<Text> reason = ClientSpells.unavailable(client, book.get(), row.entry(), row.rank());
            boolean hover = inside(mouseX, mouseY, x, y, w, ROW_H);
            if (hover && reason.isEmpty()) {
                context.fill(x, y, x + w, y + ROW_H, COLOR_HOVER);
            }
            context.drawBorder(x, y, w, ROW_H, COLOR_BORDER);
            int color = reason.isEmpty() ? COLOR_WHITE : COLOR_GREY;
            String cost = "◆◆";
            int costW = textRenderer.getWidth(cost);
            Text rank = row.rank() == 0 ? Text.translatable("screen.pf2craft.spells.cantrip")
                    : Text.translatable("screen.pf2craft.spells.rank_short", row.rank());
            int rankW = textRenderer.getWidth(rank);
            context.drawTextWithShadow(textRenderer,
                    textRenderer.trimToWidth(row.spell(), w - costW - rankW - 20), x + 4, y + 6, color);
            context.drawTextWithShadow(textRenderer, rank, x + w - 8 - costW - rankW, y + 6, COLOR_GREY);
            context.drawTextWithShadow(textRenderer, cost, x + w - 4 - costW, y + 6,
                    reason.isEmpty() ? COLOR_GOLD : COLOR_GREY);
            if (hover) {
                tooltip = reason.orElse(Text.translatable("screen.pf2craft.spells.server_decides"));
            }
        }
        return tooltip;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (category == Category.SPELLS) {
            spellScroll = Math.max(0, spellScroll - (int) Math.signum(verticalAmount));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Category[] categories = Category.values();
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].enabled && inside(mouseX, mouseY, left + PAD, rowY(i), CATEGORY_W, ROW_H)) {
                    category = categories[i];
                    return true;
                }
            }
            if (category == Category.BASIC) {
                List<BasicAction> actions = sortedActions();
                for (int i = 0; i < actions.size(); i++) {
                    BasicAction a = actions.get(i);
                    if (!inside(mouseX, mouseY, listX(), rowY(i), listW(), ROW_H)) {
                        continue;
                    }
                    if (mouseX < listX() + STAR_W) {
                        ClientActions.toggleFavorite(a.id());
                    } else if (unavailable(a).isEmpty()) {
                        choose(a);
                    }
                    return true;
                }
            }
            if (category == Category.SPELLS) {
                List<SpellRow> rows = spellRows();
                Optional<Spellbook> book = ClientSpells.own(client);
                for (int i = 0; i < visibleSpellRows() && spellScroll + i < rows.size(); i++) {
                    SpellRow row = rows.get(spellScroll + i);
                    if (inside(mouseX, mouseY, listX(), rowY(i), listW(), ROW_H)) {
                        if (book.isPresent()
                                && ClientSpells.unavailable(client, book.get(), row.entry(), row.rank()).isEmpty()) {
                            ClientSpells.cast(row.entry(), row.spell(), row.rank());
                            close();
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void choose(BasicAction action) {
        Optional<UUID> actor = actor(client);
        if (actor.isPresent() && ClientPlayNetworking.canSend(ActionRequestPayload.ID)) {
            ClientPlayNetworking.send(new ActionRequestPayload(actor.get(), action.id(), -1));
        }
        close();
    }
}
