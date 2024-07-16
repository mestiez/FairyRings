package com.zooi.fairy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableSource;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.loot.LootManager;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.condition.LootConditionTypes;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.loot.function.LootFunctionTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.feature.TreeConfiguredFeatures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FairyRings implements ModInitializer {

    public static final String MOD_ID = "fairy-rings";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static class SoundEvents {
        public static SoundEvent HAUNT_GENERIC = registerSound("haunt_generic");
        public static SoundEvent HAUNT_FIRE = registerSound("haunt_fire");
        public static SoundEvent SPOOKY = registerSound("spooky");
        public static SoundEvent SHRINE_LOW = registerSound("shrine_low");

        private static SoundEvent registerSound(String id) {
            var identifier = Identifier.of(FairyRings.MOD_ID, id);
            return Registry.register(Registries.SOUND_EVENT, identifier, SoundEvent.of(identifier));
        }

        private static void initialise() {
        }
    }

    public static class BlocksItems {
        public static class Blocks {
            public static Block WHITE_MUSHROOM;
        }

        public static class Items {
            public static Item WHITE_MUSHROOM;
        }

        private static void initialise() {
            // white mushroom
            {
                var id = Identifier.of(MOD_ID, "white_mushroom");
                Blocks.WHITE_MUSHROOM = Registry.register(Registries.BLOCK, id,
                        new MushroomPlantLightBlock(
                                AbstractBlock.Settings.create()
                                        .mapColor(MapColor.BROWN)
                                        .noCollision()
                                        .ticksRandomly()
                                        .breakInstantly()
                                        .sounds(BlockSoundGroup.GRASS)
                                        .postProcess(net.minecraft.block.Blocks::always)
                                        .pistonBehavior(PistonBehavior.DESTROY),
                                TreeConfiguredFeatures.HUGE_BROWN_MUSHROOM
                        ));
                var item = new BlockItem(Blocks.WHITE_MUSHROOM, new Item.Settings().food((new FoodComponent.Builder()).hunger(1).saturationModifier(0.3F).build()));
                Items.WHITE_MUSHROOM = Registry.register(Registries.ITEM, id, item);
                CompostingChanceRegistry.INSTANCE.add(Items.WHITE_MUSHROOM, 0.65f);

                ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(content -> {
                    content.add(Items.WHITE_MUSHROOM);
                });
            }
        }
    }

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        ServerLivingEntityEvents.AFTER_DEATH.register(this::onEntityDeath);
        SoundEvents.initialise();
        BlocksItems.initialise();

        PlayerBlockBreakEvents.AFTER.register(this::onPlayerBreakBlock);
        UseBlockCallback.EVENT.register(this::onPlayerUseBlock);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
        });

        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            if (source.isBuiltin() && LootTables.SHIPWRECK_SUPPLY_CHEST.equals(id)) {
                LootPool.Builder poolBuilder = LootPool.builder().with(ItemEntry.builder(Items.WRITTEN_BOOK)).conditionally(new ChanceLootCondition(0.02f));
                poolBuilder.apply(new LootFunction() {
                    @Override
                    public LootFunctionType getType() {
                        return LootFunctionTypes.SET_LORE;
                    }

                    @Override
                    public ItemStack apply(ItemStack itemStack, LootContext lootContext) {
                        Stories.apply(itemStack);
                        return itemStack;
                    }
                });
                tableBuilder.pool(poolBuilder);
            }
        });

        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            if (source.isBuiltin() && LootTables.RUINED_PORTAL_CHEST.equals(id)) {
                LootPool.Builder poolBuilder = LootPool.builder().with(ItemEntry.builder(Items.PAPER)).conditionally(new ChanceLootCondition(0.08f));
                poolBuilder.apply(new LootFunction() {
                    @Override
                    public LootFunctionType getType() {
                        return LootFunctionTypes.SET_NAME;
                    }

                    @Override
                    public ItemStack apply(ItemStack stack, LootContext lootContext) {
                        var options = new String[]{
                                "Disturb the fairy's sacred mark, and restless nights shall build till dark",
                                "Break the fairy's circle bright, and walls you'll build anew each night",
                                "Shatter the circle of the fey, and cursed rooms shall mark each day",
                        };
                        final String chosen = options[lootContext.getRandom().nextBetween(0, 2)];
                        stack.setCustomName(Text.literal(chosen));
                        return stack;
                    }
                });
                tableBuilder.pool(poolBuilder);
            }
        });
    }

    private ActionResult onPlayerUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult blockHitResult) {
        if (world.isClient())
            return ActionResult.PASS;

        var pos = blockHitResult.getBlockPos();
        if (pos == null)
            return ActionResult.PASS;

        var state = world.getBlockState(pos);
        var block = state.getBlock();

        if (block instanceof FlowerPotBlock flowerPot) {
            var playerState = PersistentStateManager.getPlayerState(player);
            if (!playerState.brokeFairyRing)
                return ActionResult.PASS;

            var chunkId = world.getChunk(pos).getPos().toLong();

            if (playerState.usedShrineChunks.contains(chunkId))
                return ActionResult.PASS;

            if (!(flowerPot.getContent() instanceof FlowerBlock) && player.getStackInHand(hand).getItem() instanceof BlockItem blockItem) {
                LOGGER.debug("{} put flower in pot!", player.getName());
                var platform = world.getBlockState(pos.down()).getBlock();
                int value = 0;
                if (platform.equals(Blocks.GOLD_BLOCK)) {
                    value = 5;
                } else if (platform.equals(Blocks.EMERALD_BLOCK)) {
                    value = 3;
                } else if (platform.equals(Blocks.DIAMOND_BLOCK)) {
                    value = 10;
                } else if (platform.equals(Blocks.COPPER_BLOCK)) {
                    value = 2;
                } else if (platform.equals(Blocks.IRON_BLOCK)) {
                    value = 2;
                } else if (platform.equals(Blocks.LAPIS_BLOCK)) {
                    value = 3;
                } else if (platform.equals(Blocks.REDSTONE_BLOCK)) {
                    value = 1;
                } else if (platform.equals(Blocks.NETHERITE_BLOCK)) {
                    value = 20;
                }

                if (value > 0) {
                    var prev = playerState.haunting;
                    playerState.haunting = Math.max(0, playerState.haunting - value * 0.25f);
                    if (prev > playerState.haunting) {
                        playerState.usedShrineChunks.add(chunkId);
                        player.playSound(SoundEvents.SHRINE_LOW, SoundCategory.BLOCKS, 0.6f, player.getRandom().nextFloat() * 0.1f + 1);
                    }
                }
            }
        }

        return ActionResult.PASS;
    }

    // check for fairy ring destruction
    private void onPlayerBreakBlock(World world, PlayerEntity player, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
        if (world.getServer() == null)
            return;

        if (blockState.getBlock() instanceof FlowerbedBlock) { // player broke flower petals so this might be a fairy ring!
            var worldState = PersistentStateManager.getServerState(world.getServer());
            var playerState = worldState.players.get(player.getUuid());
            if (playerState.brokeFairyRing) // no need to check haunted players
                return;

            var registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
            var fairy_ring = registry.get(Identifier.of(MOD_ID, "fairy_ring"));
            final int maxDist = 8;
            var chunksToCheck = new WorldChunk[5];
            chunksToCheck[0] = world.getWorldChunk(blockPos);
            chunksToCheck[1] = world.getWorldChunk(blockPos.north(maxDist));
            chunksToCheck[2] = world.getWorldChunk(blockPos.west(maxDist));
            chunksToCheck[3] = world.getWorldChunk(blockPos.east(maxDist));
            chunksToCheck[4] = world.getWorldChunk(blockPos.south(maxDist));

            for (var chunk : chunksToCheck) {
                if (playerState.brokeFairyRing) // no need to check haunted players
                    return;
                if (worldState.destroyedFairyRings.contains(chunk.getPos()))
                    continue;

                chunk.getStructureStarts().forEach((structure, start) -> {
                    if (structure != fairy_ring)
                        return;

                    var childBroken = start.getChildren().stream().anyMatch(piece -> {
                        var bounds = piece.getBoundingBox();
                        for (int x = bounds.getMinX(); x <= bounds.getMaxX(); x++) {
                            for (int y = bounds.getMinY(); y <= bounds.getMaxY(); y++) {
                                for (int z = bounds.getMinZ(); z <= bounds.getMaxZ(); z++) {
                                    var p = new BlockPos(x, y, z);
                                    var block = world.getBlockState(p);
                                    if (block.getBlock() instanceof FlowerbedBlock) {
                                        return true;
                                    }
                                }
                            }
                        }
                        return false;
                    });
                    LOGGER.debug("Piece of fairy ring destroyed? {}", childBroken);

                    if (childBroken) {
                        player.playSound(SoundEvents.HAUNT_GENERIC, SoundCategory.BLOCKS, 0.5f, player.getRandom().nextFloat() * 0.1f + 1);
                        player.damage(world.getDamageSources().magic(), 1);
//                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 500));
                        player.setFrozenTicks(100);

                        playerState.brokeFairyRing = true;
                        PersistentStateManager.getServerState(world.getServer()).destroyedFairyRings.add(chunk.getPos());
                    }
                });
            }
        }
    }

    private void onEntityDeath(LivingEntity livingEntity, DamageSource damageSource) {
        if (livingEntity instanceof ServerPlayerEntity player) {
            var difficulty = livingEntity.getEntityWorld().getDifficulty();
            var state = PersistentStateManager.getPlayerState(player);

            switch (difficulty) {
                case PEACEFUL -> {
                    state.haunting = 0; // clear haunting on peaceful mode
                }
                case EASY -> {
                    state.haunting *= 0.5f; // lower haunting on death if set to easy
                }
                case NORMAL, HARD -> {
                    state.haunting--; // otherwise just decrement it to prevent the player from dying the whole time
                }
            }
        }
    }

    private void onServerTick(MinecraftServer server) {
        for (var player : server.getPlayerManager().getPlayerList()) {
            var state = PersistentStateManager.getPlayerState(player);
            if (state.brokeFairyRing && state.haunting >= 0.1f) {
                var chance = HauntingUtils.HAUNTING_CHANCE_PER_TICK + (state.haunting * 0.00025);
                chance *= MathHelper.clamp(state.haunting * 0.1f, 0, 1);

                if (player.getRandom().nextDouble() < chance) {
                    triggerHauntingEvent(player, state.haunting);
                }
            }
        }
    }

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("haunt")
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .then(CommandManager.argument("mode", StringArgumentType.string())
                                .executes(context -> handleHauntCommand(context, false))
                                .then(CommandManager.argument("value", IntegerArgumentType.integer())
                                        .executes(context -> handleHauntCommand(context, true))
                                )
                        )
                )
        );

        dispatcher.register(CommandManager.literal("fairyring")
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .then(CommandManager.argument("mode", StringArgumentType.string())
                                .executes(context -> handleFairyRingCommand(context, false))
                                .then(CommandManager.argument("value", StringArgumentType.word())
                                        .executes(context -> handleFairyRingCommand(context, true))
                                )
                        )
                )
        );

        dispatcher.register(CommandManager.literal("detectroom").executes(context -> {
            var src = context.getSource();

            if (!src.isExecutedByPlayer()) {
                src.sendError(Text.literal("You are not a player!"));
                return 0;
            }

            var state = PersistentStateManager.getPlayerState(src.getPlayerOrThrow());
            if (state.pastBedLocations.isEmpty()) {
                src.sendError(Text.literal("No beds found"));
                return 0;
            }

            LOGGER.debug(String.valueOf(state.pastBedLocations.size()));

            var world = src.getWorld();
            for (var bedA : state.pastBedLocations)
                for (var bedB : state.pastBedLocations) {
                    if (bedA != bedB) {
                        var result = PathfindingUtils.query(world, new BlockPos(bedA), new BlockPos(bedB));
                        LOGGER.debug("Obstructed?: {}", result.Obstructed);
                        src.sendFeedback(() -> Text.literal("%s to %s is obstructed?: %s".formatted(bedA, bedB, result.Obstructed)), false);
                    }
                }

            return 1;
        }));
    }

    private int handleFairyRingCommand(CommandContext<ServerCommandSource> context, boolean hasValue) throws
            CommandSyntaxException {
        var player = EntityArgumentType.getPlayer(context, "target");
        var mode = StringArgumentType.getString(context, "mode");
        var value = hasValue ? StringArgumentType.getString(context, "value") : null;

        if (hasValue && !value.equals("true") && !value.equals("false")) {
            context.getSource().sendError(Text.literal("Provided value must be \"true\" or \"false\""));
            return 0;
        }

        var state = PersistentStateManager.getPlayerState(player);
        switch (mode) {
            case "set":
                if (hasValue) {
                    state.brokeFairyRing = value.equals("true");
                    context.getSource().sendFeedback(() -> Text.literal(String.format("%s fairy ring status has been set to %b", player.getName().getString(), state.brokeFairyRing)), false);
                } else {
                    context.getSource().sendError(Text.literal("No value provided"));
                    return 0;
                }
                break;
            case "get":
                if (state.brokeFairyRing)
                    context.getSource().sendFeedback(() -> Text.literal(String.format("%s destroyed a fairy ring", player.getName().getString())), false);
                else
                    context.getSource().sendFeedback(() -> Text.literal(String.format("%s hasn't destroyed a fairy ring", player.getName().getString())), false);
                break;
        }

        return 1;
    }

    private int handleHauntCommand(CommandContext<ServerCommandSource> context, boolean hasValue) throws
            CommandSyntaxException {
        var player = EntityArgumentType.getPlayer(context, "target");
        var mode = StringArgumentType.getString(context, "mode");
        int value = hasValue ? IntegerArgumentType.getInteger(context, "value") : 0;

        var state = PersistentStateManager.getPlayerState(player);
        switch (mode) {
            case "clear":
                state.haunting = 0;
                context.getSource().sendFeedback(() -> Text.literal(String.format("%s has been set to haunt level %f", player.getName().getString(), state.haunting)), false);
                break;
            case "set":
                if (hasValue) {
                    state.haunting = Math.max(0, value);
                    context.getSource().sendFeedback(() -> Text.literal(String.format("%s has been set to haunt level %f", player.getName().getString(), state.haunting)), false);
                } else {
                    context.getSource().sendError(Text.literal("No value provided"));
                }
                break;
            case "get":
                context.getSource().sendFeedback(() -> Text.literal(String.format("%s has haunt level %f", player.getName().getString(), state.haunting)), false);
                break;
        }

        return 1;
    }

    private void triggerHauntingEvent(ServerPlayerEntity player, float hauntLevel) {
        LOGGER.debug("Haunting event for player {} with haunt level {}", player.getName(), hauntLevel);

        var random = player.getRandom();
        var didSomething = false;
        var mute = false;

        if (hauntLevel >= 1) {
            if (random.nextFloat() > 0.9f) {
                didSomething = true;
                HauntingUtils.hauntPhysiological(player);
            }

            if (random.nextFloat() > 0.2f) {
                didSomething = true;
                HauntingUtils.hauntEnvironmentErratic(player);
            }

            if (random.nextFloat() > 0.8f) {
                HauntingUtils.hauntEnvironmentNoises(player);
                mute = true;
            }

            if (random.nextFloat() > 0.8f)
                HauntingUtils.hauntPetsHateYou(player);
        }

        if (hauntLevel >= 5) {
            if (random.nextFloat() > 0.95f) {
                didSomething = true;
                HauntingUtils.hauntBurnBed(player);
                mute = true;
                player.playSound(SoundEvents.HAUNT_FIRE, SoundCategory.AMBIENT, 0.5f, player.getRandom().nextFloat() * 0.1f + 1);
            }
        }

        if (hauntLevel >= 10) {
            HauntingUtils.hauntEnvironmentLifeDrain(player);
            didSomething = true;
            // no need to set didSomething from this point forward!!
            if (random.nextFloat() > 0.5f)
                HauntingUtils.hauntSpoilFood(player);

//            if (player.getServerWorld().getDifficulty() != Difficulty.PEACEFUL && random.nextFloat() > 0.7f)
//                HauntingUtils.hauntMobSpawn(player);
        }

        if (hauntLevel >= 15) {

        }

        if (hauntLevel >= 20) {

        }

        if (hauntLevel >= 25) {

        }

        if (!mute && didSomething && player.getRandom().nextFloat() > 0.7f)
            player.playSound(SoundEvents.HAUNT_GENERIC, SoundCategory.AMBIENT, 0.5f, player.getRandom().nextFloat() * 0.1f + 1);
    }

    private static class ChanceLootCondition implements LootCondition {
        private float chance = 0.1f;

        public ChanceLootCondition(float chance) {
            this.chance = chance;
        }

        @Override
        public LootConditionType getType() {
            return LootConditionTypes.RANDOM_CHANCE;
        }

        @Override
        public boolean test(LootContext lootContext) {
            return lootContext.getRandom().nextDouble() < chance;
        }
    }
}