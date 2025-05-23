package com.zooi.fairy;

import net.minecraft.block.*;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.stream.Stream;

public class PathfindingUtils {
    public static Result query(BlockView world, BlockPos from, BlockPos to) {
        var result = new Result();
        var gScore = new HashMap<BlockPos, Integer>();
        var fScore = new HashMap<BlockPos, Integer>();
        var openSet = new PriorityQueue<BlockPos>(Comparator.comparingInt(o -> fScore.getOrDefault(o, Integer.MAX_VALUE)));

        openSet.add(from);
        gScore.put(from, 0);
        fScore.put(from, heuristicDistance(from, to));
        int iterationsRemaining = 1024;

        while (!openSet.isEmpty()) {
            if (iterationsRemaining-- <= 0) {
                FairyRings.LOGGER.debug("Out of iterations");
                result.Distance = Integer.MAX_VALUE;
                result.Obstructed = true;
                return result;
            }

            var current = openSet.poll();
            if (current.equals(to)) {
                result.Distance = gScore.get(current);
                result.Obstructed = false;
                return result;
            }

            getNeighbours(current).forEach(neighbour -> {
                if (!isValidNeighbour(world, neighbour))
                    return;

                var tentativeGScore = gScore.getOrDefault(current, Integer.MAX_VALUE) + current.getManhattanDistance(neighbour);
                if (tentativeGScore < gScore.getOrDefault(neighbour, Integer.MAX_VALUE)) {
                    gScore.put(neighbour, tentativeGScore);
                    fScore.put(neighbour, tentativeGScore + heuristicDistance(neighbour, to));
                    if (!openSet.contains(neighbour)) {
                        openSet.add(neighbour);
                    }
                }
            });
        }
        result.Obstructed = true;
        return result;
    }

    private static boolean isValidNeighbour(BlockView world, BlockPos neighbour) {
        var cost = getCost(world, neighbour);
        return cost != Integer.MAX_VALUE;
    }

    public static Stream<BlockPos> getNeighbours(BlockPos p) {
        Stream.Builder<BlockPos> builder = Stream.builder();
        builder.add(p.up());
        builder.add(p.down());
        builder.add(p.west());
        builder.add(p.east());
        builder.add(p.north());
        builder.add(p.south());
        return builder.build();
    }

    public static int getCost(BlockView world, BlockPos pos) {
        var state = world.getBlockState(pos);
        var block = state.getBlock();

        if (block instanceof BedBlock)
            return 0; // beds are transparent

        if (block instanceof DoorBlock || block instanceof FenceGateBlock || block instanceof TrapdoorBlock || block instanceof FluidBlock)
            return Integer.MAX_VALUE;

        if (!state.canPathfindThrough(NavigationType.AIR))
            return Integer.MAX_VALUE;

        // okay, we can definitely pass through this block
        // but we should allow narrow doorways without doors, so
        // open blocks should only count as open if its surrounded
        // by air

        var neighbours = getNeighbours(pos).toList(); // TODO slow
        int openNeighbours = 0;
        for (var neighbour : neighbours)
            if (world.getBlockState(neighbour).canPathfindThrough(NavigationType.AIR))
                openNeighbours++;

        if (openNeighbours <= 3)
            return Integer.MAX_VALUE;

        return 0;
    }

    private static int heuristicDistance(BlockPos from, BlockPos to) {
        return from.getManhattanDistance(to);
    }

    public static class Result {
        public boolean Obstructed;
        public int Distance;
    }
}