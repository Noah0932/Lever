package com.noah.minecraftagent.client;

import com.google.gson.Gson;
import com.noah.minecraftagent.common.context.ContextSnapshot;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public final class ClientContextCollector {
    private static final Gson GSON = new Gson();

    public String collectJson(String lastObservation) {
        MinecraftClient client = MinecraftClient.getInstance();
        ContextSnapshot snapshot = new ContextSnapshot();
        snapshot.lastObservation = lastObservation;
        if (client.player == null || client.world == null) {
            return GSON.toJson(snapshot);
        }
        snapshot.x = client.player.getX();
        snapshot.y = client.player.getY();
        snapshot.z = client.player.getZ();
        snapshot.dimension = client.world.getRegistryKey().getValue().toString();
        snapshot.health = client.player.getHealth();
        HungerManager hunger = client.player.getHungerManager();
        snapshot.hunger = hunger.getFoodLevel();
        snapshot.yaw = client.player.getYaw();
        snapshot.pitch = client.player.getPitch();
        HitResult hit = client.crosshairTarget;
        if (hit instanceof BlockHitResult blockHit) {
            BlockState state = client.world.getBlockState(blockHit.getBlockPos());
            snapshot.crosshairBlock = Registries.BLOCK.getId(state.getBlock()).toString();
        } else if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            snapshot.crosshairEntity = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
        }
        return GSON.toJson(snapshot);
    }
}
