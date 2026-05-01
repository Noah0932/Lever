package com.noah.minecraftagent.server.bot;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class BotDeathHandler {
    private BotDeathHandler() {
    }

    public static void handle(BotEntity bot) {
        BlockPos pos = bot.getBlockPos();
        if (bot.getServerWorld().getBlockState(pos).isAir() || bot.getServerWorld().getBlockState(pos).getBlock() == Blocks.AIR) {
            bot.getServerWorld().setBlockState(pos, Blocks.CHEST.getDefaultState());
            BlockEntity blockEntity = bot.getServerWorld().getBlockEntity(pos);
            if (blockEntity instanceof ChestBlockEntity chest) {
                Inventory inventory = bot.getInventory();
                for (int i = 0; i < 41 && i < inventory.size(); i++) {
                    ItemStack stack = inventory.getStack(i);
                    if (!stack.isEmpty()) {
                        int slot = findEmptySlot(chest);
                        if (slot >= 0 && slot < chest.size()) {
                            chest.setStack(slot, stack.copy());
                        }
                    }
                }
                inventory.clear();
            }
            bot.getServer().getPlayerManager().broadcast(
                    Text.literal("[Lever] ").formatted(Formatting.AQUA)
                            .append(Text.literal(bot.getBotProfile().displayName() + " 已死亡，物品已转移至箱子").formatted(Formatting.WHITE)),
                    false
            );
        }
    }

    private static int findEmptySlot(Inventory inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }
}
