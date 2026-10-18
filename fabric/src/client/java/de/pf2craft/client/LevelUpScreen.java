package de.pf2craft.client;

import de.pf2craft.core.character.Ability;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.Rank;
import de.pf2craft.core.progression.LevelUp;
import de.pf2craft.core.progression.LevelUpOptions;
import de.pf2craft.network.LevelUpPayload;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Level-up choices for the own character: skill increase, ability boosts and one feat name per slot.
 * The options come from the server; the skill list only shows what may be raised at the new level
 * (the server validates everything again). Closes on the next sheet sync after sending.
 */
public class LevelUpScreen extends Screen {
    /** Only dims the game behind the window; the vanilla background blur would smear the text. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);
    }

    private static final int PANEL_W = 300;
    private static final int BUTTON_H = 20;
    private static final int FIELD_H = 18;
    private static final int GAP = 4;
    private static final int LINE = 11;
    private static final int SLOT_LABEL_W = 110;
    private static final int MAX_FEATURE_LINES = 3;
    private static final int MAX_FEAT_NAME = 64;
    /** The confirm button is locked this long after a click unless the server answers earlier. */
    private static final long LOCK_MS = 3000;
    private static final String NO_CHOICE = "";

    private final UUID owner;
    private final EnumSet<Ability> boosts = EnumSet.noneOf(Ability.class);
    private final List<String> featNames = new ArrayList<>();
    private final List<ButtonWidget> boostButtons = new ArrayList<>();
    private final List<Integer> slotLabelYs = new ArrayList<>();
    private String skill = NO_CHOICE;
    private LevelUpOptions options;
    private CharacterSheet sheet;
    private boolean noSkillPossible;
    private List<OrderedText> featureLines = List.of();
    private ButtonWidget confirm;
    private Text status = Text.empty();
    private boolean waiting;
    private long sentAt;
    private int left;
    private int top;
    private int boostLabelY;
    private int skillY;
    private int statusY;

    public LevelUpScreen(UUID owner) {
        super(Text.translatable("screen.pf2craft.levelup.title"));
        this.owner = owner;
    }

    @Override
    protected void init() {
        boostButtons.clear();
        slotLabelYs.clear();
        confirm = null;
        Optional<ClientSheets.Synced> synced = ClientSheets.get(owner);
        if (synced.isEmpty() || synced.get().levelUpOptions().isEmpty()) {
            options = null;
            addDrawableChild(ButtonWidget.builder(ScreenTexts.BACK, b -> close())
                    .dimensions((width - 100) / 2, height / 2 + 10, 100, BUTTON_H).build());
            return;
        }
        LevelUpOptions newOptions = synced.get().levelUpOptions().get();
        if (!newOptions.equals(options)) {
            // A different level-up (e.g. after a re-import): start over.
            boosts.clear();
            featNames.clear();
            skill = NO_CHOICE;
        }
        options = newOptions;
        sheet = synced.get().sheet();
        while (featNames.size() < options.featSlots().size()) {
            featNames.add("");
        }

        Text features = options.features().isEmpty()
                ? Text.translatable("screen.pf2craft.levelup.no_features")
                : Text.translatable("screen.pf2craft.levelup.features", String.join(", ", options.features()));
        featureLines = textRenderer.wrapLines(features.copy().formatted(Formatting.GRAY), PANEL_W);
        if (featureLines.size() > MAX_FEATURE_LINES) {
            featureLines = featureLines.subList(0, MAX_FEATURE_LINES);
        }

        int contentH = 2 * LINE + 2 + featureLines.size() * LINE + GAP;
        if (options.skillIncrease()) {
            contentH += BUTTON_H + GAP;
        }
        if (options.abilityBoosts() > 0) {
            contentH += LINE + BUTTON_H + GAP;
        }
        contentH += options.featSlots().size() * (FIELD_H + GAP) + GAP + BUTTON_H + GAP + 2 * LINE;
        left = (width - PANEL_W) / 2;
        top = Math.max(4, (height - contentH) / 2);
        int y = top + 2 * LINE + 2 + featureLines.size() * LINE + GAP;

        noSkillPossible = false;
        if (options.skillIncrease()) {
            skillY = y;
            addSkillButton(y);
            y += BUTTON_H + GAP;
        }
        if (options.abilityBoosts() > 0) {
            boostLabelY = y;
            y += LINE;
            addBoostButtons(y);
            y += BUTTON_H + GAP;
        }
        for (int i = 0; i < options.featSlots().size(); i++) {
            slotLabelYs.add(y + (FIELD_H - 8) / 2);
            int index = i;
            TextFieldWidget field = new TextFieldWidget(textRenderer, left + SLOT_LABEL_W, y,
                    PANEL_W - SLOT_LABEL_W, FIELD_H, slotName(options.featSlots().get(i)));
            field.setMaxLength(MAX_FEAT_NAME);
            field.setText(featNames.get(i));
            field.setPlaceholder(Text.translatable("screen.pf2craft.levelup.feat_placeholder")
                    .formatted(Formatting.DARK_GRAY));
            field.setChangedListener(text -> {
                featNames.set(index, text);
                updateConfirm();
            });
            addDrawableChild(field);
            if (i == 0) {
                setInitialFocus(field);
            }
            y += FIELD_H + GAP;
        }

        y += GAP;
        int buttonW = (PANEL_W - GAP) / 2;
        confirm = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pf2craft.levelup.confirm"),
                b -> submit()).dimensions(left, y, buttonW, BUTTON_H).build());
        addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, b -> close())
                .dimensions(left + PANEL_W - buttonW, y, buttonW, BUTTON_H).build());
        statusY = y + BUTTON_H + GAP;
        updateConfirm();
    }

    private void addSkillButton(int y) {
        List<String> targets = LevelUp.skillIncreaseTargets(sheet, options.newLevel());
        if (targets.isEmpty()) {
            noSkillPossible = true;
            return;
        }
        List<String> values = new ArrayList<>();
        values.add(NO_CHOICE);
        values.addAll(targets);
        if (!values.contains(skill)) {
            skill = NO_CHOICE;
        }
        addDrawableChild(CyclingButtonWidget.builder(this::skillText)
                .values(values)
                .initially(skill)
                .build(left, y, PANEL_W, BUTTON_H, Text.translatable("screen.pf2craft.levelup.skill"),
                        (button, value) -> {
                            skill = value;
                            updateConfirm();
                        }));
    }

    private void addBoostButtons(int y) {
        Ability[] abilities = Ability.values();
        int buttonW = (PANEL_W - (abilities.length - 1) * 2) / abilities.length;
        for (int i = 0; i < abilities.length; i++) {
            Ability ability = abilities[i];
            boostButtons.add(addDrawableChild(ButtonWidget.builder(Text.empty(), b -> toggleBoost(ability))
                    .dimensions(left + i * (buttonW + 2), y, buttonW, BUTTON_H).build()));
        }
        updateBoostButtons();
    }

    private void toggleBoost(Ability ability) {
        if (!boosts.remove(ability) && boosts.size() < options.abilityBoosts()) {
            boosts.add(ability);
        }
        updateBoostButtons();
        updateConfirm();
    }

    private void updateBoostButtons() {
        Ability[] abilities = Ability.values();
        for (int i = 0; i < boostButtons.size(); i++) {
            Ability ability = abilities[i];
            boolean chosen = boosts.contains(ability);
            Text name = Text.translatable("pf2craft.ability." + ability.pathbuilderKey());
            ButtonWidget button = boostButtons.get(i);
            button.setMessage(chosen ? Text.literal("+").append(name).formatted(Formatting.GREEN) : name);
            button.active = chosen || boosts.size() < options.abilityBoosts();
        }
    }

    /** Skill name with the rank change, e.g. "Athletics (E → M)". */
    private Text skillText(String target) {
        if (target.equals(NO_CHOICE)) {
            return Text.translatable("screen.pf2craft.levelup.skill_choose").formatted(Formatting.GRAY);
        }
        MutableText name;
        Rank rank;
        if (target.startsWith("lore:")) {
            int index = Integer.parseInt(target.substring(5));
            name = Text.literal(sheet.lores().get(index).name());
            rank = sheet.lores().get(index).rank();
        } else {
            name = Text.translatable("pf2craft.skill." + target);
            rank = sheet.rank(target);
        }
        return name.append(Text.literal(" (").append(rankLetter(rank)).append(" → ")
                .append(rankLetter(rank.next())).append(")"));
    }

    private static Text rankLetter(Rank rank) {
        return Text.translatable("screen.pf2craft.sheet.rank." + rank.name().toLowerCase(Locale.ROOT));
    }

    /** Slot types come from the class data in English; known ones are translated. */
    private static Text slotName(String slot) {
        return Text.translatableWithFallback(
                "pf2craft.feat_slot." + slot.toLowerCase(Locale.ROOT).replace(' ', '_'), slot);
    }

    private boolean complete() {
        if (options == null) {
            return false;
        }
        if (options.skillIncrease() && skill.equals(NO_CHOICE)) {
            return false;
        }
        if (boosts.size() != options.abilityBoosts()) {
            return false;
        }
        return featNames.stream().noneMatch(String::isBlank);
    }

    private boolean locked() {
        return waiting && System.currentTimeMillis() - sentAt < LOCK_MS;
    }

    private void updateConfirm() {
        if (confirm != null) {
            confirm.active = complete() && !locked();
        }
    }

    private void submit() {
        if (!complete() || locked() || client == null) {
            return;
        }
        if (!ClientPlayNetworking.canSend(LevelUpPayload.ID)) {
            status = Text.translatable("screen.pf2craft.dice.server_missing").formatted(Formatting.RED);
            return;
        }
        List<String> boostKeys = new ArrayList<>();
        for (Ability ability : boosts) {
            boostKeys.add(ability.pathbuilderKey());
        }
        List<String> names = featNames.stream().map(String::strip).toList();
        ClientPlayNetworking.send(new LevelUpPayload(options.skillIncrease() ? skill : NO_CHOICE, boostKeys, names));
        waiting = true;
        sentAt = System.currentTimeMillis();
        status = Text.translatable("screen.pf2craft.levelup.waiting").formatted(Formatting.YELLOW);
        updateConfirm();
    }

    /** Called when the own sheet was synced while this screen is open. */
    public void onSync() {
        Optional<ClientSheets.Synced> synced = ClientSheets.get(owner);
        if (waiting || synced.isEmpty() || synced.get().levelUpOptions().isEmpty()) {
            // Level-up done (or no longer granted): back to the sheet.
            close();
        } else {
            // E.g. the GM changed a rank: rebuild the skill list, keep the choices.
            clearAndInit();
        }
    }

    /** Shows the server's answer (e.g. an error) while the level-up is being processed. */
    public void onServerMessage(Text message) {
        if (waiting) {
            status = message;
            waiting = false;
            updateConfirm();
        }
    }

    @Override
    public void tick() {
        updateConfirm();
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(ClientSheets.get(owner).isPresent() ? new CharacterScreen(owner) : null);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = width / 2;
        if (options == null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.pf2craft.levelup.unavailable"),
                    centerX, height / 2 - 10, 0xFFFFFF);
            return;
        }
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.pf2craft.levelup.heading",
                sheet.level(), options.newLevel()).formatted(Formatting.BOLD), centerX, top, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.pf2craft.levelup.hp",
                options.hpGain()), centerX, top + LINE + 2, 0xFFFFFF);
        int y = top + 2 * LINE + 2;
        for (OrderedText line : featureLines) {
            context.drawCenteredTextWithShadow(textRenderer, line, centerX, y, 0xFFFFFF);
            y += LINE;
        }
        if (noSkillPossible) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.pf2craft.levelup.skill_none")
                    .formatted(Formatting.RED), centerX, skillY + (BUTTON_H - 8) / 2, 0xFFFFFF);
        }
        if (options.abilityBoosts() > 0) {
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.pf2craft.levelup.boosts",
                    boosts.size(), options.abilityBoosts()).formatted(Formatting.GRAY), left, boostLabelY, 0xFFFFFF);
        }
        for (int i = 0; i < slotLabelYs.size(); i++) {
            String label = textRenderer.trimToWidth(slotName(options.featSlots().get(i)), SLOT_LABEL_W - 6).getString();
            context.drawTextWithShadow(textRenderer, label, left, slotLabelYs.get(i), 0xAAAAAA);
        }
        if (!status.getString().isEmpty()) {
            context.drawTextWrapped(textRenderer, status, left, statusY, PANEL_W, 0xFFFFFF);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
