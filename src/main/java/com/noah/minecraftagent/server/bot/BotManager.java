package com.noah.minecraftagent.server.bot;

import com.noah.minecraftagent.common.bot.BotProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BotManager {
    private static final BotManager INSTANCE = new BotManager();
    private final Map<String, BotEntity> botsByUuid = new ConcurrentHashMap<>();
    private final Map<String, BotProfile> botProfiles = new ConcurrentHashMap<>();
    private final Map<String, List<String>> ownerBots = new ConcurrentHashMap<>();
    private int botCounter;

    private BotManager() {
    }

    public static BotManager getInstance() {
        return INSTANCE;
    }

    public int nextBotId() {
        return ++botCounter;
    }

    public BotEntity summon(MinecraftServer server, ServerPlayerEntity owner, BotProfile profile) {
        BotEntity bot = new BotEntity(server, owner.getServerWorld(), profile);
        botsByUuid.put(bot.getUuidAsString(), bot);
        botProfiles.put(bot.getUuidAsString(), profile);
        ownerBots.computeIfAbsent(owner.getUuidAsString(), k -> Collections.synchronizedList(new ArrayList<>())).add(bot.getUuidAsString());
        bot.setOwner(owner);
        bot.updateDisplayName();
        bot.getServerWorld().spawnEntity(bot);
        bot.refreshPositionAndAngles(owner.getX(), owner.getY(), owner.getZ(), owner.getYaw(), owner.getPitch());
        return bot;
    }

    public void remove(BotEntity bot) {
        String uuid = bot.getUuidAsString();
        botsByUuid.remove(uuid);
        BotProfile profile = botProfiles.remove(uuid);
        if (profile != null) {
            List<String> ownedBots = ownerBots.get(profile.ownerUuid);
            if (ownedBots != null) {
                ownedBots.remove(uuid);
                if (ownedBots.isEmpty()) {
                    ownerBots.remove(profile.ownerUuid);
                }
            }
        }
        bot.discard();
    }

    public Optional<BotEntity> getBot(String uuid) {
        return Optional.ofNullable(botsByUuid.get(uuid));
    }

    public Optional<BotEntity> getBotByName(String name) {
        return botsByUuid.values().stream()
                .filter(b -> b.getBotProfile().displayName().equalsIgnoreCase(name))
                .findFirst();
    }

    public BotProfile getProfile(String uuid) {
        return botProfiles.get(uuid);
    }

    public void updateProfile(String uuid, BotProfile profile) {
        botProfiles.put(uuid, profile);
    }

    public List<BotEntity> getBotsByOwner(String ownerUuid) {
        List<String> botUuids = ownerBots.getOrDefault(ownerUuid, Collections.emptyList());
        List<BotEntity> bots = new ArrayList<>();
        for (String uuid : botUuids) {
            BotEntity bot = botsByUuid.get(uuid);
            if (bot != null) bots.add(bot);
        }
        return bots;
    }

    public Collection<BotEntity> getAllBots() {
        return botsByUuid.values();
    }
}
