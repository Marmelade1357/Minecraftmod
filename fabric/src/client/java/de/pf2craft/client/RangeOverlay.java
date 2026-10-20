package de.pf2craft.client;

import de.pf2craft.network.CombatSyncPayload;
import java.util.Optional;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Draws a flat circle on the ground around the mover that shows how far a Stride reaches: the preview of one
 * Stride before it is chosen, the remaining blocks while it is active. Only in the own turn and while the toggle
 * (key H, config {@code rangeDisplay}) is on. The outline is rebuilt only when anchor or radius change.
 */
public final class RangeOverlay {
    /** What to outline: the block the movement starts from and the radius in blocks. */
    public record Target(BlockPos anchor, int blocks) {
    }

    private static final int MAX_SEGMENTS = 96;
    private static final float HEIGHT = 0.03f;

    private static BlockPos cachedAnchor;
    private static int cachedBlocks = -1;
    private static float[] cachedPoints = new float[0];

    private RangeOverlay() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(RangeOverlay::render);
    }

    /**
     * The area to show right now, empty when nothing should be drawn. A controlled NPC (its position and
     * speed) extends this later; for now only the own player counts.
     */
    public static Optional<Target> target(MinecraftClient client) {
        if (client.player == null || !ClientActions.rangeDisplay()) {
            return Optional.empty();
        }
        Optional<CombatSyncPayload> combat = ClientCombat.get();
        if (combat.isPresent() && ClientControl.isControlling()) {
            CombatSyncPayload state = combat.get();
            // The sync carries no NPC speed, so a controlled NPC only shows an active stride.
            var npc = client.getCameraEntity();
            boolean its = ClientControl.controlled().map(ClientCombat::isTurnOf).orElse(false);
            return npc != null && npc != client.player && its && state.strideBlocksLeft() > 0
                    ? Optional.of(new Target(npc.getBlockPos(), state.strideBlocksLeft())) : Optional.empty();
        }
        if (combat.isEmpty() || !ClientCombat.isTurnOf(client.player.getUuid())) {
            return Optional.empty();
        }
        CombatSyncPayload state = combat.get();
        int blocks;
        if (state.strideBlocksLeft() > 0) {
            blocks = state.strideBlocksLeft();
        } else if (state.actionsLeft() > 0) {
            int speed = ClientSheets.get(client.player.getUuid()).map(s -> s.sheet().speed()).orElse(0);
            blocks = speed > 0 ? Math.max(1, speed / 5) : 0; // unknown speed: nothing to preview
        } else {
            blocks = 0;
        }
        return blocks > 0 ? Optional.of(new Target(client.player.getBlockPos(), blocks)) : Optional.empty();
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        Optional<Target> target = target(client);
        if (target.isEmpty() || context.consumers() == null) {
            return;
        }
        Target t = target.get();
        if (!t.anchor().equals(cachedAnchor) || t.blocks() != cachedBlocks) {
            rebuild(t);
        }
        Vec3d cam = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer lines = context.consumers().getBuffer(RenderLayer.getLines());
        float y = t.anchor().getY() + HEIGHT;
        int n = cachedPoints.length / 2;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float x1 = cachedPoints[2 * i];
            float z1 = cachedPoints[2 * i + 1];
            float x2 = cachedPoints[2 * j];
            float z2 = cachedPoints[2 * j + 1];
            float dx = x2 - x1;
            float dz = z2 - z1;
            float len = (float) Math.sqrt(dx * dx + dz * dz);
            float nx = len == 0 ? 1 : dx / len;
            float nz = len == 0 ? 0 : dz / len;
            lines.vertex(matrix, x1, y, z1).color(255, 200, 0, 255).normal(nx, 0, nz);
            lines.vertex(matrix, x2, y, z2).color(255, 200, 0, 255).normal(nx, 0, nz);
        }
        matrices.pop();
    }

    private static void rebuild(Target t) {
        cachedAnchor = t.anchor();
        cachedBlocks = t.blocks();
        int segments = Math.min(MAX_SEGMENTS, Math.max(16, t.blocks() * 6));
        double radius = t.blocks() + 0.5;
        double cx = t.anchor().getX() + 0.5;
        double cz = t.anchor().getZ() + 0.5;
        float[] points = new float[segments * 2];
        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            points[2 * i] = (float) (cx + radius * Math.cos(angle));
            points[2 * i + 1] = (float) (cz + radius * Math.sin(angle));
        }
        cachedPoints = points;
    }
}
