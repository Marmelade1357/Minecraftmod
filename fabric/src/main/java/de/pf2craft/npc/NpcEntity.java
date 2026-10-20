package de.pf2craft.npc;

import de.pf2craft.Pf2Craft;
import de.pf2craft.core.monster.NpcJson;
import de.pf2craft.core.monster.NpcStats;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

/**
 * A GM-controlled NPC. It looks like a villager but never trades, never wanders on its own and cannot be
 * hurt by vanilla damage; only the rules engine changes its hit points. Navigation still works so the
 * GM can send it somewhere.
 */
public class NpcEntity extends VillagerEntity {
    private static final String NBT_KEY = "pf2craft_npc";
    private static final int MAX_NAME_LENGTH = 32;

    private NpcStats stats;
    private boolean controlled; // a GM drives this NPC (see NpcControl); never saved
    private float driveForward;
    private float driveSideways;
    private float driveSpeed;
    private boolean driveJump;

    public NpcEntity(EntityType<? extends VillagerEntity> type, World world) {
        super(type, world);
        setPersistent();
        setCanPickUpLoot(false);
        this.moveControl = new DriveMoveControl(this);
    }

    /**
     * While controlled the GM's input drives the NPC (see NpcControl). The brain never runs for NPCs, so the
     * only own movement logic to replace is the move control, which {@link DriveMoveControl} does.
     */
    public void setControlled(boolean controlled) {
        this.controlled = controlled;
        driveForward = 0;
        driveSideways = 0;
        driveSpeed = 0;
        driveJump = false;
        if (controlled) {
            getNavigation().stop();
        }
    }

    public boolean controlled() {
        return controlled;
    }

    /** The movement the next move-control tick applies: inputs in -1..1, speed as the vanilla movement speed. */
    public void setDrive(float forward, float sideways, float speed, boolean jump) {
        driveForward = forward;
        driveSideways = sideways;
        driveSpeed = speed;
        driveJump = jump;
    }

    private final class DriveMoveControl extends MoveControl {
        DriveMoveControl(NpcEntity npc) {
            super(npc);
        }

        @Override
        public void tick() {
            if (!controlled) {
                super.tick();
                return;
            }
            setMovementSpeed(driveSpeed);
            setForwardSpeed(driveForward);
            setSidewaysSpeed(driveSideways);
            if (driveJump) {
                getJumpControl().setActive();
            }
        }
    }

    public NpcStats stats() {
        return stats;
    }

    public void setStats(NpcStats stats) {
        this.stats = stats;
        setCustomName(Text.literal(stats.displayName()));
        setCustomNameVisible(true);
        updatePose();
    }

    private void updatePose() {
        if (stats != null && stats.defeated()) {
            setPose(EntityPose.SLEEPING);
        } else if (getPose() == EntityPose.SLEEPING) {
            setPose(EntityPose.STANDING);
        }
    }

    @Override
    protected void mobTick() {
        // No brain tick: the villager AI (work, sleep, trade, wander) must never run.
        if (stats == null) {
            discard();
            return;
        }
        // A name tag must not rename the NPC: the stat block owns the name.
        if (getCustomName() == null || !stats.displayName().equals(getCustomName().getString())) {
            setCustomName(Text.literal(stats.displayName()));
        }
        updatePose();
    }

    @Override
    protected Box calculateBoundingBox() {
        // A defeated NPC lies down visually (SLEEPING pose, whose own hitbox is tiny) but keeps the standing
        // hitbox so it stays targetable. LivingEntity.getDimensions is final, hence this hook.
        return getDimensions(EntityPose.STANDING).getBoxAt(getPos());
    }

    @Override
    public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
        // Vanilla would turn a villager into a witch.
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        return ActionResult.PASS;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public boolean isCustomNameVisible() {
        return true;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (stats != null) {
            nbt.putString(NBT_KEY, NpcJson.write(stats));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (!nbt.contains(NBT_KEY)) {
            return;
        }
        try {
            NpcStats read = NpcJson.read(nbt.getString(NBT_KEY));
            int hp = Math.max(0, Math.min(read.base().hp(), read.currentHp()));
            String name = read.displayName();
            if (name == null || name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
                name = read.base().name();
            }
            setStats(new NpcStats(read.base(), name, hp));
        } catch (IllegalArgumentException e) {
            Pf2Craft.LOGGER.error("Dropping NPC with unreadable data: {}", e.getMessage());
            stats = null;
        }
    }
}
