package de.pf2craft.client;

import de.pf2craft.network.ImportCharacterPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

/**
 * Asks for the Pathbuilder 2e id and sends it to the server. Replaced by the character sheet as soon
 * as the server syncs the own sheet; import errors from the server are shown in the status line.
 */
public class ImportScreen extends Screen {
    private static final int PANEL_W = 240;
    private static final int MAX_ID_LENGTH = 10;
    /** The button is locked this long after a click; the server refuses parallel imports anyway. */
    private static final long LOCK_MS = 3000;

    private TextFieldWidget idField;
    private ButtonWidget importButton;
    private String idText = "";
    private Text status = Text.empty();
    private boolean waiting;
    private long sentAt;
    private int left;
    private int top;

    public ImportScreen() {
        super(Text.translatable("screen.pf2craft.import.title"));
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = height / 2 - 45;

        idField = new TextFieldWidget(textRenderer, left, top + 36, PANEL_W, 20,
                Text.translatable("screen.pf2craft.import.id"));
        idField.setMaxLength(MAX_ID_LENGTH);
        idField.setTextPredicate(text -> text.chars().allMatch(c -> c >= '0' && c <= '9'));
        idField.setText(idText);
        idField.setPlaceholder(Text.translatable("screen.pf2craft.import.id").formatted(Formatting.DARK_GRAY));
        idField.setChangedListener(text -> {
            idText = text;
            updateButton();
        });
        addDrawableChild(idField);
        setInitialFocus(idField);

        importButton = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pf2craft.import.button"),
                b -> submit()).dimensions(left, top + 62, PANEL_W, 20).build());
        updateButton();
    }

    private boolean locked() {
        return waiting && System.currentTimeMillis() - sentAt < LOCK_MS;
    }

    private void updateButton() {
        if (importButton != null) {
            importButton.active = !idText.isEmpty() && !locked();
        }
    }

    private void submit() {
        if (idText.isEmpty() || locked() || client == null) {
            return;
        }
        if (!ClientPlayNetworking.canSend(ImportCharacterPayload.ID)) {
            status = Text.translatable("screen.pf2craft.dice.server_missing").formatted(Formatting.RED);
            return;
        }
        ClientPlayNetworking.send(new ImportCharacterPayload(idText));
        waiting = true;
        sentAt = System.currentTimeMillis();
        status = Text.translatable("screen.pf2craft.import.importing").formatted(Formatting.YELLOW);
        updateButton();
    }

    /** Shows the server's import messages (errors, "busy") while an import is running. */
    public void onServerMessage(Text message) {
        if (waiting && message.getContent() instanceof TranslatableTextContent content
                && content.getKey().startsWith("pf2craft.character.")) {
            status = message;
            waiting = false;
            updateButton();
        }
    }

    @Override
    public void tick() {
        updateButton();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = width / 2;
        context.drawCenteredTextWithShadow(textRenderer, title, centerX, top, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.pf2craft.import.hint").formatted(Formatting.GRAY),
                centerX, top + 18, 0xFFFFFF);
        if (!status.getString().isEmpty()) {
            context.drawTextWrapped(textRenderer, status, left, top + 90, PANEL_W, 0xFFFFFF);
        }
    }

    @Override
    public boolean shouldPause() {
        // In singleplayer a paused integrated server would never finish the import.
        return false;
    }
}
