package com.zooi.fairy;

import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;

public class HauntingUtils {
    public static final double HAUNTING_CHANCE_PER_TICK = 0.0007;

    public static class HauntValues {
        public static final float SLEEP_UNDER_SKY = 0.5f;
        public static final float SLEEP_SAME_ROOM = 1;
        public static final float SLEEP_SAME_BED = 3;
    }

    public static void hauntBurnBed(ServerPlayerEntity player) {
        var world = player.getEntityWorld();
        var spawnPointPosition = player.getSpawnPointPosition();

        if (spawnPointPosition == null)
            return;

        var chunkX = ChunkSectionPos.getSectionCoord(spawnPointPosition.getX());
        var chunkZ = ChunkSectionPos.getSectionCoord(spawnPointPosition.getZ());

        if (world.isChunkLoaded(chunkX, chunkZ)) {
            final var radius = 3;
            var center = new BlockPos(spawnPointPosition);

            for (int x = -radius; x <= radius; x++)
                for (int y = -radius; y <= radius; y++)
                    for (int z = -radius; z <= radius; z++) {
                        if (world.random.nextFloat() > 0.5)
                            continue;
                        var pos = center.add(x, y, z);
                        var blockState = world.getBlockState(pos);
                        if (blockState.isBurnable() && world.isAir(pos.up())) {
                            world.setBlockState(pos.up(), Blocks.FIRE.getDefaultState());
                            return; // one fire is enough
                        }
                    }
        }
    }

    public static void hauntEnvironmentNoises(ServerPlayerEntity player) {
        var world = player.getEntityWorld();
        var center = player.getBlockPos();

        int action = player.getRandom().nextBetween(0, 1);

        switch (action) {
            case 0:
                playSoundOnlyFor(player, player.getPos().addRandom(player.getRandom(), 10), SoundEvents.AMBIENT_CAVE.value(), SoundCategory.AMBIENT, 0.5f, player.getRandom().nextFloat() * 0.1F + 0.9F);
                break;
            case 1: {
                final var radius = 3;
                for (int x = -radius; x <= radius; x++)
                    for (int y = -radius; y <= radius; y++)
                        for (int z = -radius; z <= radius; z++) {
                            if (world.random.nextFloat() > 0.3)
                                continue;

                            var pos = center.add(x, y, z);
                            var state = world.getBlockState(pos);
                            if (state.isAir())
                                continue;

                            var block = state.getBlock();
                            if (block.getTranslationKey().endsWith("planks")) { // TODO slow
                                playSoundOnlyFor(player, pos.toCenterPos(),
                                        FairyRings.SoundEvents.SPOOKY, SoundCategory.BLOCKS, 0.5f, player.getRandom().nextFloat() * 0.1F + 0.9F);
                                return;
                            }
                        }
            }
            break;
        }
    }

    public static void hauntEnvironmentErratic(ServerPlayerEntity player) {
        var world = player.getEntityWorld();
        var playerPosition = player.getBlockPos();

        assert playerPosition != null;

        final var radius = 5;
        var center = new BlockPos(playerPosition);

        for (int x = -radius; x <= radius; x++)
            for (int y = -radius; y <= radius; y++)
                for (int z = -radius; z <= radius; z++) {
                    if (world.random.nextFloat() > 0.1)
                        continue;

                    var pos = center.add(x, y, z);
                    var state = world.getBlockState(pos);
                    var block = state.getBlock();

                    if (block instanceof DoorBlock door) {
                        if (!door.isOpen(state) && door.getBlockSetType().canOpenByHand()) {
                            world.playSound(null, pos, door.getBlockSetType().doorOpen(), SoundCategory.BLOCKS, 1.0F, world.getRandom().nextFloat() * 0.1F + 0.9F);
                            door.setOpen(player, world, state, pos, true);
                        }
                    } else if (block instanceof FenceGateBlock fenceGate && !state.get(FenceGateBlock.OPEN)) {
                        fenceGate.onUse(state, world, pos, player, player.preferredHand, new BlockHitResult(player.getPos(), player.getMovementDirection(), pos, false));
                        world.playSound(null, pos, SoundEvents.BLOCK_FENCE_GATE_OPEN, SoundCategory.BLOCKS);
                    } else if (block instanceof ButtonBlock button) {
                        // TODO get block set type somehow
                        world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS);
                        button.powerOn(state, world, pos);
                    } else if (block instanceof LeverBlock lever) {
                        world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 1.0f, 0.4f);
                        lever.togglePower(state, world, pos);
                    } else if (block instanceof CandleBlock candle) {
                        if (state.get(CandleBlock.LIT))
                            CandleBlock.extinguish(player, state, world, pos);
                    }
                }
    }

    public static void hauntSpoilFood(ServerPlayerEntity player) {
        var world = player.getEntityWorld();
        var inv = player.getInventory();
        for (var stack : inv.main) {
            var item = stack.getItem();
            var food = item.getFoodComponent();

            if (food != null && player.getRandom().nextBoolean()) {
                if (food.isMeat()) { // we spoil the meat
                    var slot = inv.getSlotWithStack(stack);
                    var amount = stack.getCount();
                    inv.removeStack(slot);
                    inv.setStack(slot, new ItemStack(Items.ROTTEN_FLESH, amount));
                }
            }
        }
    }

    public static void hauntPetsHateYou(ServerPlayerEntity player) {
        var world = player.getEntityWorld();
        var pets = world.getOtherEntities(player, player.getBoundingBox().expand(5), e -> {
            if (e instanceof TameableEntity potentialPet)
                return potentialPet.isOwner(player);
            return false;
        });

        for (var pet : pets) {
            if (pet instanceof WolfEntity wolf) {
                wolf.setAngryAt(player.getUuid());
                wolf.setAngerTime(500);
                wolf.setSitting(false);
                FairyRings.LOGGER.debug("Cursed {}'s pet {}", player, wolf.getName());
            }
        }
    }

    public static void hauntMobSpawn(ServerPlayerEntity player) {
        var world = player.getEntityWorld();
        var center = player.getBlockPos();

        int c = 0;
        final var radius = 5;
        for (int x = -radius; x <= radius; x++)
            for (int y = -radius; y <= radius; y++)
                for (int z = -radius; z <= radius; z++) {

                    if (player.getRandom().nextFloat() > 0.01)
                        continue;

                    var pos = center.add(x, y, z);
                    var blockState = world.getBlockState(pos);

                    if (blockState.allowsSpawning(world, pos, EntityType.ZOMBIE)) {
                        var ent = new ZombieEntity(EntityType.ZOMBIE, world);
                        var spawnPos = pos.up().toCenterPos();

                        if (world.isSpaceEmpty(ent.getBoundingBox())) {
                            ent.setCanBreakDoors(true);
                            ent.updatePositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, 0, 0);
                            world.spawnEntity(ent);
                        }
                    }
                }
    }

    public static void hauntEnvironmentLifeDrain(ServerPlayerEntity player) {
        var world = player.getEntityWorld();
        var playerPosition = player.getBlockPos();

        assert playerPosition != null;

        final var radius = 5;
        var center = new BlockPos(playerPosition);

        for (int x = -radius; x <= radius; x++)
            for (int y = -radius; y <= radius; y++)
                for (int z = -radius; z <= radius; z++) {
                    if (world.random.nextFloat() > 0.1)
                        continue;
                    var pos = center.add(x, y, z);
                    var state = world.getBlockState(pos);
                    var block = state.getBlock();

                    // kill saplings
                    if (block instanceof SaplingBlock)
                        world.setBlockState(pos, Blocks.DEAD_BUSH.getDefaultState());

                        // kill grass
                    else if (block == Blocks.GRASS_BLOCK)
                        world.setBlockState(pos, Blocks.DIRT.getDefaultState());

                        // kill leaves
                    else if (block instanceof LeavesBlock)
                        world.breakBlock(pos, true);

                        // kill plants and crops
                    else if (block instanceof PlantBlock)
                        world.breakBlock(pos, true);

                        // kill plants in pots
                    else if (block instanceof FlowerPotBlock pot && block != Blocks.POTTED_DEAD_BUSH) {
                        if (pot.getContent() != Blocks.AIR)
                            world.setBlockState(pos, Blocks.POTTED_DEAD_BUSH.getDefaultState());
                    }
                }
    }

    public static void hauntPhysiological(ServerPlayerEntity player) {
        var rand = player.getServerWorld().getRandom();
        int duration = rand.nextBetween(5, 30) * 20;

        switch (rand.nextBetween(0, 5)) {
            case 0 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, duration));
            case 1 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, duration));
            case 2 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration));
            case 3 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, duration));
            case 4 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.UNLUCK, duration));
            case 5 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, duration));
        }
    }

    public static void playSoundOnlyFor(ServerPlayerEntity player, Vec3d pos, SoundEvent soundEvent, SoundCategory category, float volume, float pitch) {
        var packet = new PlaySoundS2CPacket(
                Registries.SOUND_EVENT.getEntry(soundEvent),
                category,
                pos.x, pos.y, pos.z,
                volume,
                pitch,
                player.getServerWorld().getSeed());

        player.networkHandler.sendPacket(packet);
    }
}
