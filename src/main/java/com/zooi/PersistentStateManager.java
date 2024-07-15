package com.zooi;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

public class PersistentStateManager extends PersistentState {
    public static final String NBT_PLAYERS = "wellidoor-players";
    public static final String NBT_DESTROYED_FAIRY_RINGS = "wellidoor-destroyed-fairy-rings";

    public static final String NBT_HAUNTING = "wellidoor-haunting";
    public static final String NBT_BED_LOCATIONS = "wellidoor-bed-locations";
    public static final String NBT_SHRINE_CHUNKS = "wellidoor-shrine-chunks";
    public static final String NBT_BROKE_FAIRY_RING = "wellidoor-broke-fairy-ring";

    public HashMap<UUID, PlayerData> players = new HashMap<>();
    public HashSet<ChunkPos> destroyedFairyRings = new HashSet<>();

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {

        var playersNBT = new NbtCompound();
        this.players.forEach((uuid, playerData) -> {
            var player = new NbtCompound();

            player.putFloat(NBT_HAUNTING, playerData.haunting);
            player.putBoolean(NBT_BROKE_FAIRY_RING, playerData.brokeFairyRing);

            var bedLocsNbt = new NbtList();
            playerData.pastBedLocations.forEach(p -> {
                var v = new NbtCompound();
                v.putInt("x", p.getX());
                v.putInt("y", p.getY());
                v.putInt("z", p.getZ());
                bedLocsNbt.add(v);
            });
            player.put(NBT_BED_LOCATIONS, bedLocsNbt);

            var shrinesNbt = new NbtList();
            playerData.usedShrineChunks.forEach(s -> {
                shrinesNbt.add(NbtLong.of(s));
            });
            player.put(NBT_SHRINE_CHUNKS, shrinesNbt);

            playersNBT.put(uuid.toString(), player);
        });
        nbt.put(NBT_PLAYERS, playersNBT);

        var destroyedFairyRingsNBT = new NbtList();
        this.destroyedFairyRings.forEach(loc -> {
            var b = new NbtCompound();
            b.putInt("x", loc.x);
            b.putInt("z", loc.z);
            destroyedFairyRingsNBT.add(b);
        });
        nbt.put(NBT_DESTROYED_FAIRY_RINGS, destroyedFairyRingsNBT);

        return nbt;
    }

    public static PersistentStateManager createFromNbt(NbtCompound tag) {
        var state = new PersistentStateManager();

        var playersNBT = tag.getCompound(NBT_PLAYERS);
        playersNBT.getKeys().forEach(key -> {
            var playerData = new PlayerData();
            var compound = playersNBT.getCompound(key);

            playerData.haunting = compound.getFloat(NBT_HAUNTING);
            playerData.brokeFairyRing = compound.getBoolean(NBT_BROKE_FAIRY_RING);

            var beds = compound.getList(NBT_BED_LOCATIONS, NbtCompound.COMPOUND_TYPE);
            playerData.pastBedLocations.clear();
            beds.forEach(t -> {
                var c = (NbtCompound) t;
                var v = new Vec3i(c.getInt("x"), c.getInt("y"), c.getInt("z"));
                playerData.pastBedLocations.add(v);
            });

            var shrines = compound.getList(NBT_SHRINE_CHUNKS, NbtCompound.LONG_TYPE);
            playerData.pastBedLocations.clear();
            shrines.forEach(t -> {
                var c = (NbtLong) t;
                playerData.usedShrineChunks.add(c.longValue());
            });

            var uuid = UUID.fromString(key);
            state.players.put(uuid, playerData);
        });

        var destroyedFairyRingsNBT = tag.getList(NBT_DESTROYED_FAIRY_RINGS, NbtCompound.COMPOUND_TYPE);
        state.destroyedFairyRings.clear();
        destroyedFairyRingsNBT.forEach(t -> {
            var c = (NbtCompound) t;
            state.destroyedFairyRings.add(new ChunkPos(c.getInt("x"), c.getInt("z")));
        });

        return state;
    }

    public static PersistentStateManager getServerState(MinecraftServer server) {
        var persistentStateManager = Objects.requireNonNull(server.getWorld(World.OVERWORLD)).getPersistentStateManager();
        var state = persistentStateManager.getOrCreate(PersistentStateManager::createFromNbt, PersistentStateManager::new, FairyRings.MOD_ID);
        state.markDirty();
        return state;
    }

    public static PlayerData getPlayerState(LivingEntity player) {
        var serverState = getServerState(Objects.requireNonNull(player.getWorld().getServer()));
        return serverState.getStateFor(player);
    }

    public PlayerData getStateFor(LivingEntity player) {
        return players.computeIfAbsent(player.getUuid(), uuid -> new PlayerData());
    }

    public static void markDirty(MinecraftServer server) {
        var serverState = getServerState(server);
        serverState.markDirty();
    }
}