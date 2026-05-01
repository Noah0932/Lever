package com.noah.minecraftagent.client.physical;

import com.noah.minecraftagent.client.AgentRuntime;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class PhysicalActionExecutor {

    private PhysicalActionExecutor() {}

    public static void execute(PhysicalAction action, Consumer<AgentRuntime.RuntimeUpdate> publish) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;

        switch (action) {
            case PhysicalAction.LookAt la -> lookAt(client.player, la.x(), la.y(), la.z());
            case PhysicalAction.WalkTo wt -> walkTo(client, wt.x(), wt.y(), wt.z(), publish);
            case PhysicalAction.MineBlock mb -> mineBlock(client, mb.pos(), publish);
            case PhysicalAction.PlaceBlock pb -> placeBlock(client, pb.pos(), pb.face());
            case PhysicalAction.Jump j -> client.player.jump();
            case PhysicalAction.Sneak s -> client.player.setSneaking(s.on());
            case PhysicalAction.UseItem ui -> client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            case PhysicalAction.GetPosition gp -> publishPosition(client.player, publish);
        }
    }

    private static void lookAt(ClientPlayerEntity player, double tx, double ty, double tz) {
        double dx = tx - player.getX();
        double dy = ty - (player.getY() + player.getStandingEyeHeight());
        double dz = tz - player.getZ();
        double hDist = Math.sqrt(dx * dx + dz * dz);
        player.setYaw((float) (MathHelper.atan2(dz, dx) * 57.2958) - 90.0F);
        player.setPitch((float) (-MathHelper.atan2(dy, hDist) * 57.2958));
    }

    private static void walkTo(MinecraftClient client, double tx, double ty, double tz,
                                Consumer<AgentRuntime.RuntimeUpdate> publish) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        lookAt(player, tx, ty, tz);
        double dist = player.squaredDistanceTo(tx, ty, tz);
        client.options.forwardKey.setPressed(dist > 1.5);
        player.setSprinting(dist > 5.0);

        publish.accept(new AgentRuntime.RuntimeUpdate(
                com.noah.minecraftagent.client.AgentStatus.ACTING,
                "Walking to " + (int)tx + "," + (int)ty + "," + (int)tz,
                "", "", false, 0
        ));

        client.options.forwardKey.setPressed(false);
        player.setSprinting(false);
    }

    private static void mineBlock(MinecraftClient client, BlockPos pos,
                                   Consumer<AgentRuntime.RuntimeUpdate> publish) {
        ClientPlayerEntity player = client.player;
        ClientPlayerInteractionManager im = client.interactionManager;
        if (player == null || im == null) return;

        Vec3d center = pos.toCenterPos();
        lookAt(player, center.x, center.y, center.z);

        Direction face = Direction.UP;
        im.attackBlock(pos, face);
        player.swingHand(Hand.MAIN_HAND);

        for (int tick = 0; tick < 60; tick++) {
            if (client.world == null || client.world.getBlockState(pos).isAir()) break;
            im.updateBlockBreakingProgress(pos, face);
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }

        if (client.world != null && !client.world.getBlockState(pos).isAir()) {
            im.attackBlock(pos, face);
        }

        publish.accept(new AgentRuntime.RuntimeUpdate(
                com.noah.minecraftagent.client.AgentStatus.OBSERVING,
                "Mined block " + pos.toShortString(),
                "", "", false, 0
        ));
    }

    private static void placeBlock(MinecraftClient client, BlockPos pos, Direction face) {
        ClientPlayerEntity player = client.player;
        ClientPlayerInteractionManager im = client.interactionManager;
        if (player == null || im == null || client.crosshairTarget == null) return;

        Vec3d center = pos.toCenterPos();
        lookAt(player, center.x, center.y, center.z);

        if (client.crosshairTarget instanceof BlockHitResult bhr) {
            im.interactBlock(player, Hand.MAIN_HAND, bhr);
        }
    }

    private static void publishPosition(ClientPlayerEntity player,
                                         Consumer<AgentRuntime.RuntimeUpdate> publish) {
        String pos = String.format("x=%.1f y=%.1f z=%.1f dim=%s",
                player.getX(), player.getY(), player.getZ(),
                player.getWorld().getRegistryKey().getValue());
        publish.accept(new AgentRuntime.RuntimeUpdate(
                com.noah.minecraftagent.client.AgentStatus.OBSERVING,
                pos, "", "", false, 0
        ));
    }
}
