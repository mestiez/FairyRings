package com.zooi.fairy;

import net.minecraft.block.BlockState;
import net.minecraft.block.MushroomPlantBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.world.gen.feature.ConfiguredFeature;

public class MushroomPlantLightBlock extends MushroomPlantBlock {
    public MushroomPlantLightBlock(Settings settings, RegistryKey<ConfiguredFeature<?, ?>> featureKey) {
        super(settings, featureKey);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        var blockPos = pos.down();
        var blockState = world.getBlockState(blockPos);
        return blockState.isIn(BlockTags.MUSHROOM_GROW_BLOCK) || this.canPlantOnTop(blockState, world, blockPos);
    }
}
