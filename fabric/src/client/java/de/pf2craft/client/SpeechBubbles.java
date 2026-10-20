package de.pf2craft.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Speech bubbles over NPCs: one per entity UUID, shown for 5 seconds and drawn as a camera-facing pane with
 * wrapped text. Bubbles disappear with their entity and on disconnect.
 */
public final class SpeechBubbles {
    private static final int LIFETIME_TICKS = 100;
    private static final int WRAP = 24;
    private static final int MAX_LINES = 4;
    private static final float SCALE = 0.025f;
    private static final int PADDING = 3;
    private static final int BACKGROUND = 0xC0101010;
    private static final int FULL_LIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;

    private static final class Bubble {
        final List<String> lines;
        int ticksLeft = LIFETIME_TICKS;

        Bubble(List<String> lines) {
            this.lines = lines;
        }
    }

    private static final Map<UUID, Bubble> BUBBLES = new HashMap<>();

    private SpeechBubbles() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        WorldRenderEvents.AFTER_TRANSLUCENT.register(SpeechBubbles::render);
    }

    public static void show(UUID npc, String text) {
        List<String> lines = wrap(text);
        if (!lines.isEmpty()) {
            BUBBLES.put(npc, new Bubble(lines));
        }
    }

    public static void clear() {
        BUBBLES.clear();
    }

    /** Word wrap to about {@link #WRAP} characters per line, at most {@link #MAX_LINES} lines. */
    static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            while (word.length() > WRAP) { // a word longer than a line is broken up
                if (line.length() > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                lines.add(word.substring(0, WRAP));
                word = word.substring(WRAP);
            }
            if (line.length() > 0 && line.length() + 1 + word.length() > WRAP) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        if (lines.size() > MAX_LINES) {
            lines = new ArrayList<>(lines.subList(0, MAX_LINES));
            String last = lines.get(MAX_LINES - 1);
            lines.set(MAX_LINES - 1, (last.length() >= WRAP ? last.substring(0, WRAP - 1) : last) + "...");
        }
        return lines;
    }

    private static void tick() {
        for (Iterator<Bubble> it = BUBBLES.values().iterator(); it.hasNext(); ) {
            if (--it.next().ticksLeft <= 0) {
                it.remove();
            }
        }
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider consumers = context.consumers();
        if (BUBBLES.isEmpty() || client.world == null || consumers == null) {
            return;
        }
        TextRenderer font = client.textRenderer;
        float delta = context.tickCounter().getTickDelta(false);
        Vec3d cam = context.camera().getPos();
        Entity self = client.getCameraEntity();
        boolean firstPerson = client.options.getPerspective().isFirstPerson();
        Set<UUID> present = new HashSet<>();
        for (Entity entity : client.world.getEntities()) {
            Bubble bubble = BUBBLES.get(entity.getUuid());
            if (bubble == null) {
                continue;
            }
            present.add(entity.getUuid());
            if (entity == self && firstPerson) {
                continue; // do not cover the own view
            }
            Vec3d pos = entity.getLerpedPos(delta).add(0, entity.getHeight() + 0.6, 0);
            draw(context, consumers, font, bubble, pos.subtract(cam));
        }
        BUBBLES.keySet().retainAll(present); // the entity is gone
    }

    private static void draw(WorldRenderContext context, VertexConsumerProvider consumers, TextRenderer font,
                             Bubble bubble, Vec3d offset) {
        MatrixStack matrices = new MatrixStack();
        matrices.translate(offset.x, offset.y, offset.z);
        matrices.multiply(context.camera().getRotation());
        matrices.scale(-SCALE, -SCALE, SCALE);
        int width = 0;
        for (String l : bubble.lines) {
            width = Math.max(width, font.getWidth(l));
        }
        int lineHeight = font.fontHeight + 1;
        int height = lineHeight * bubble.lines.size();
        float x0 = -width / 2f - PADDING;
        float x1 = width / 2f + PADDING;
        float y1 = PADDING;
        float y0 = -height - PADDING; // the pane grows upwards from the anchor
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer pane = consumers.getBuffer(RenderLayer.getTextBackground());
        pane.vertex(matrix, x0, y1, 0f).color(BACKGROUND).light(FULL_LIGHT);
        pane.vertex(matrix, x1, y1, 0f).color(BACKGROUND).light(FULL_LIGHT);
        pane.vertex(matrix, x1, y0, 0f).color(BACKGROUND).light(FULL_LIGHT);
        pane.vertex(matrix, x0, y0, 0f).color(BACKGROUND).light(FULL_LIGHT);
        for (int i = 0; i < bubble.lines.size(); i++) {
            String line = bubble.lines.get(i);
            float x = -font.getWidth(line) / 2f;
            float y = -height + i * lineHeight;
            font.draw(Text.literal(line), x, y, 0xFFFFFFFF, false, matrix, consumers,
                    TextRenderer.TextLayerType.POLYGON_OFFSET, 0, FULL_LIGHT);
        }
    }
}
