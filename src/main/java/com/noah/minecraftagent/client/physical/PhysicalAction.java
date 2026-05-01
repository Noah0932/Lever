package com.noah.minecraftagent.client.physical;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public sealed interface PhysicalAction {

    record LookAt(double x, double y, double z) implements PhysicalAction {}

    record WalkTo(double x, double y, double z) implements PhysicalAction {}

    record MineBlock(BlockPos pos) implements PhysicalAction {}

    record PlaceBlock(BlockPos pos, Direction face) implements PhysicalAction {}

    record UseItem() implements PhysicalAction {}

    record Jump() implements PhysicalAction {}

    record Sneak(boolean on) implements PhysicalAction {}

    record GetPosition() implements PhysicalAction {}
}
