package com.noah.minecraftagent.server.bot;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public final class BotActionController {
    private final BotEntity bot;
    private final ServerPlayerInteractionManager interactionManager;

    // Look state
    private Vec3d lookTarget;
    private static final float MAX_TURN_SPEED = 20.0F;

    // Move state
    private Vec3d moveTarget;
    private boolean moving;
    private static final double MOVE_SPEED = 0.15;
    private static final double ARRIVAL_THRESHOLD = 0.25;

    // Break state
    private BlockPos breakingPos;
    private int breakTick;
    private static final int BREAK_TIMEOUT = 40;

    public BotActionController(BotEntity bot) {
        this.bot = bot;
        this.interactionManager = bot.interactionManager;
    }

    // ── per-tick processing ────────────────────────────────

    public void tick() {
        tickLook();
        tickMove();
        tickBreak();
    }

    // ── Look ───────────────────────────────────────────────

    public void lookAt(double x, double y, double z) {
        this.lookTarget = new Vec3d(x, y, z);
    }

    public void snapLookAt(double x, double y, double z) {
        double eyeY = bot.getY() + bot.getStandingEyeHeight();
        double dx = x - bot.getX();
        double dy = y - eyeY;
        double dz = z - bot.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        double hDist = Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, hDist));
        applyRotation(yaw, pitch);
        this.lookTarget = null;
    }

    public void stopLook() {
        this.lookTarget = null;
    }

    public boolean isLooking() {
        return lookTarget != null;
    }

    private void tickLook() {
        if (lookTarget == null) return;

        double eyeY = bot.getY() + bot.getStandingEyeHeight();
        double dx = lookTarget.x - bot.getX();
        double dy = lookTarget.y - eyeY;
        double dz = lookTarget.z - bot.getZ();

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        double hDist = Math.sqrt(dx * dx + dz * dz);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, hDist));

        float yawDiff = MathHelper.wrapDegrees(targetYaw - bot.getHeadYaw());
        float pitchDiff = targetPitch - bot.getPitch();

        float stepYaw = MathHelper.clamp(yawDiff, -MAX_TURN_SPEED, MAX_TURN_SPEED);
        float stepPitch = MathHelper.clamp(pitchDiff, -MAX_TURN_SPEED, MAX_TURN_SPEED);

        applyRotation(bot.getHeadYaw() + stepYaw, bot.getPitch() + stepPitch);
    }

    private void applyRotation(float newYaw, float newPitch) {
        bot.prevYaw = bot.getYaw();
        bot.prevPitch = bot.getPitch();
        bot.prevHeadYaw = bot.getHeadYaw();
        bot.prevBodyYaw = bot.bodyYaw;

        bot.setYaw(newYaw);
        bot.setPitch(MathHelper.clamp(newPitch, -90.0F, 90.0F));
        bot.setHeadYaw(newYaw);
        bot.bodyYaw = newYaw;
    }

    // ── Walk ───────────────────────────────────────────────

    public void walkTo(double x, double y, double z) {
        this.moveTarget = new Vec3d(x, y, z);
        this.moving = true;
    }

    public void walkTo(Vec3d target) {
        this.moveTarget = target;
        this.moving = true;
    }

    public void stopWalk() {
        this.moveTarget = null;
        this.moving = false;
    }

    public boolean isMoving() {
        return moving;
    }

    private void tickMove() {
        if (!moving || moveTarget == null) return;

        Vec3d current = bot.getPos();
        Vec3d delta = moveTarget.subtract(current);
        double dist = delta.length();

        if (dist < ARRIVAL_THRESHOLD) {
            bot.setPosition(moveTarget.x, moveTarget.y, moveTarget.z);
            moving = false;
            moveTarget = null;
            return;
        }

        Vec3d step = delta.normalize().multiply(MOVE_SPEED);
        Vec3d before = bot.getPos();
        bot.move(MovementType.SELF, step);
        Vec3d after = bot.getPos();

        if (after.squaredDistanceTo(before) < 0.0001) {
            jump();
            bot.move(MovementType.SELF, step);
            if (bot.getPos().squaredDistanceTo(before) < 0.0001) {
                moving = false;
                moveTarget = null;
            }
        }
    }

    // ── Mine Block ─────────────────────────────────────────

    public void mineBlock(BlockPos pos) {
        if (bot.getWorld().getBlockState(pos).isAir()) return;

        Vec3d center = pos.toCenterPos();
        lookAt(center.x, center.y, center.z);

        interactionManager.processBlockBreakingAction(
                pos,
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                Direction.UP,
                bot.getWorld().getTopY(),
                0
        );

        this.breakingPos = pos;
        this.breakTick = 0;
    }

    public void cancelBreak() {
        if (breakingPos != null) {
            interactionManager.processBlockBreakingAction(
                    breakingPos,
                    PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                    Direction.UP,
                    bot.getWorld().getTopY(),
                    0
            );
            breakingPos = null;
            breakTick = 0;
        }
    }

    private void tickBreak() {
        if (breakingPos == null) return;

        breakTick++;
        if (breakTick > BREAK_TIMEOUT) {
            cancelBreak();
            return;
        }

        if (bot.getWorld().getBlockState(breakingPos).isAir()) {
            breakingPos = null;
            breakTick = 0;
        }
    }

    // ── Place Block ────────────────────────────────────────

    public void placeBlock(BlockPos pos, Direction face) {
        Vec3d center = pos.toCenterPos();
        lookAt(center.x, center.y, center.z);

        Vec3i normal = face.getVector();
        Vec3d hitVec = new Vec3d(
                pos.getX() + 0.5 + normal.getX() * 0.5,
                pos.getY() + 0.5 + normal.getY() * 0.5,
                pos.getZ() + 0.5 + normal.getZ() * 0.5
        );
        BlockHitResult hit = new BlockHitResult(hitVec, face, pos, false);

        interactionManager.interactBlock(
                bot,
                bot.getWorld(),
                bot.getMainHandStack(),
                Hand.MAIN_HAND,
                hit
        );
    }

    // ── Use Item ───────────────────────────────────────────

    public void useItem() {
        interactionManager.interactItem(bot, bot.getWorld(), bot.getMainHandStack(), Hand.MAIN_HAND);
    }

    // ── Attack Entity ──────────────────────────────────────

    public void attackEntity(Entity target) {
        bot.attack(target);
        bot.swingHand(Hand.MAIN_HAND);
    }

    // ── Jump ───────────────────────────────────────────────

    public void jump() {
        if (bot.isOnGround()) {
            bot.jump();
        }
    }

    // ── Sneak ──────────────────────────────────────────────

    public void sneak(boolean on) {
        bot.setSneaking(on);
    }

    // ── Queries ────────────────────────────────────────────

    public boolean isBusy() {
        return moving || lookTarget != null || breakingPos != null;
    }

    public void cancelAll() {
        stopLook();
        stopWalk();
        cancelBreak();
    }

    public Vec3d getMoveTarget() {
        return moveTarget;
    }

    public Vec3d getLookTarget() {
        return lookTarget;
    }
}
