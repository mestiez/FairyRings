package com.zooi.fairy;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CaveUtils {
    public static BlockPos findCaveUnder(World world, BlockPos startPos) {
        var chunk = world.getWorldChunk(startPos);

        int minHeight = world.getBottomY();
        int maxHeight = startPos.getY() - 30;

        if (maxHeight < minHeight)
            return null;

        int startX = chunk.getPos().getStartX();
        int startZ = chunk.getPos().getStartZ();

        for (int x = 0; x < 16; x += 2) {
            for (int z = 0; z < 16; z += 2) {
                for (int y = minHeight; y < maxHeight; y += 2) {
                    var blockPos = new BlockPos(startX + x, y, startZ + z);
                    if (isCaveSpace(world, blockPos))
                        return blockPos;
                }
            }
        }

        return null;
    }

    private static boolean isCaveSpace(World world, BlockPos pos) {
        if (!world.isAir(pos)) // we can breathe
            return false;

        if (world.isSkyVisible(pos)) // we are underground
            return false;

        if (!world.isAir(pos.up()) || !world.isAir(pos.down())) // we can exist
            return false;

        if (!world.getBlockState(pos.down(2)).isOpaque()) // we can stand
            return false;

        return true;
    }
}
