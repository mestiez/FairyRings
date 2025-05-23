package com.zooi.fairy.mixin;

import com.zooi.fairy.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class PlayerMixin {
    @Inject(at = @At("TAIL"), method = "wakeUp")
    private void wakeUp(CallbackInfo info) {
        var player = (ServerPlayerEntity) (Object) this;

        var world = player.getServerWorld();
        var time = world.getTimeOfDay() % 24000;

        if (time >= 0 && time <= 40) {
            FairyRings.LOGGER.debug("Player woke up");

            var newBedPos = player.getSpawnPointPosition();
            var playerHauntState = PersistentStateManager.getPlayerState(player);
            if (playerHauntState.brokeFairyRing) {
                // pathfind to other beds to find if the player has built a new room or something
                var sameRoom = false;
                for (var oldBedPos : playerHauntState.pastBedLocations) {
                    var p = new BlockPos(oldBedPos);
                    var oldChunk = world.getChunk(p).getPos();
                    if (world.isChunkLoaded(oldChunk.x, oldChunk.z)) {
                        var result = PathfindingUtils.query(world, newBedPos, p);
                        if (!result.Obstructed) {
                            FairyRings.LOGGER.debug("Bed at {} is reachable from here! Player did not build a new room.", oldBedPos);
                            playerHauntState.haunting += HauntingUtils.HauntValues.SLEEP_SAME_ROOM;
                            sameRoom = true;
                        }
                    }
                }

                // add to bed set
                if (!playerHauntState.pastBedLocations.add(newBedPos)) {
                    FairyRings.LOGGER.debug("Player slept in the same bed");
                    playerHauntState.haunting += HauntingUtils.HauntValues.SLEEP_SAME_BED;

                    var bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
                    bolt.setCosmetic(true);
                    var pos = player.getPos().addRandom(world.random, 100f);
                    bolt.setPosition(pos);
                    world.spawnEntity(bolt);
                }

                // check for "sleep under sky" penalty
                var underSky = false;
                if (world.isSkyVisible(newBedPos)) {
                    FairyRings.LOGGER.debug("Player slept under the sky");
                    underSky = true;
                    playerHauntState.haunting += HauntingUtils.HauntValues.SLEEP_UNDER_SKY;
                }

                if (underSky || sameRoom) {
                    player.playSound(FairyRings.SoundEvents.HAUNT_GENERIC, SoundCategory.AMBIENT, 0.3f, player.getRandom().nextFloat() * 0.1f + 0.6f);
                }

                // spawn underground
                if (playerHauntState.haunting > 5 && world.getRandom().nextFloat() >= 0.95f) {
                    var target = CaveUtils.findCaveUnder(world, player.getBlockPos());
                    FairyRings.LOGGER.debug("Attempt to teleport to {}", target);
                    if (target != null) {
                        var t = target.toCenterPos();
                        player.teleport(t.x, t.y, t.z);
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 200));
                    }
                }
            }
        }
    }
}