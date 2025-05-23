package com.zooi.fairy;

import com.mojang.serialization.Codec;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.*;
import net.minecraft.world.*;

import java.util.*;

/**
 * Stores all world-wide Fairy mod data.
 *
 * Public API is unchanged – only the persistence plumbing differs.
 */
public class PersistentStateManager extends PersistentState {
    /* ---------------- keys ---------------- */
    private static final String NBT_PLAYERS                = "wellidoor-players";
    private static final String NBT_DESTROYED_FAIRY_RINGS  = "wellidoor-destroyed-fairy-rings";
    private static final String NBT_HAUNTING               = "wellidoor-haunting";
    private static final String NBT_BED_LOCATIONS          = "wellidoor-bed-locations";
    private static final String NBT_SHRINE_CHUNKS          = "wellidoor-shrine-chunks";
    private static final String NBT_BROKE_FAIRY_RING       = "wellidoor-broke-fairy-ring";

    /* ---------------- data ---------------- */
    public final Map<UUID, PlayerData> players            = new HashMap<>();
    public final Set<ChunkPos>         destroyedFairyRings = new HashSet<>();

    /* -------------------------------------------------------------------------
       §1  –  Serialisation helpers
       ---------------------------------------------------------------------- */

    /** Encode this state to an NBT blob (no side-effects). */
    private NbtCompound toNbt() {
        var nbt = new NbtCompound();

        /* players */
        var playersNBT = new NbtCompound();
        players.forEach((uuid, pd) -> {
            var pTag = new NbtCompound();

            pTag.putFloat(NBT_HAUNTING, pd.haunting);
            pTag.putBoolean(NBT_BROKE_FAIRY_RING, pd.brokeFairyRing);

            var beds = new NbtList();
            pd.pastBedLocations.forEach(v -> {
                var c = new NbtCompound();
                c.putInt("x", v.getX());
                c.putInt("y", v.getY());
                c.putInt("z", v.getZ());
                beds.add(c);
            });
            pTag.put(NBT_BED_LOCATIONS, beds);

            var shrines = new NbtList();
            pd.usedShrineChunks.forEach(s -> shrines.add(NbtLong.of(s)));
            pTag.put(NBT_SHRINE_CHUNKS, shrines);

            playersNBT.put(uuid.toString(), pTag);
        });
        nbt.put(NBT_PLAYERS, playersNBT);

        /* destroyed rings */
        var ringsNBT = new NbtList();
        destroyedFairyRings.forEach(cp -> {
            var c = new NbtCompound();
            c.putInt("x", cp.x);
            c.putInt("z", cp.z);
            ringsNBT.add(c);
        });
        nbt.put(NBT_DESTROYED_FAIRY_RINGS, ringsNBT);

        return nbt;
    }

    /** Re-hydrate from NBT (wrapper param required by new signature but unused here). */
    private static PersistentStateManager fromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup ignored) {
        var state = new PersistentStateManager();

        /* players */
        var playersNBT = tag.getCompoundOrEmpty(NBT_PLAYERS);
        playersNBT.getKeys().forEach(key -> {
            var pd = new PlayerData();
            var pTag = playersNBT.getCompoundOrEmpty(key);

            pd.haunting        = pTag.getFloat(NBT_HAUNTING, 0);
            pd.brokeFairyRing  = pTag.getBoolean(NBT_BROKE_FAIRY_RING, false);

            var beds = pTag.getListOrEmpty(NBT_BED_LOCATIONS);
            beds.forEach(e -> {
                var c = (NbtCompound) e;
                pd.pastBedLocations.add(new Vec3i(c.getInt("x", 0), c.getInt("y", 0), c.getInt("z", 0)));
            });

            var shrines = pTag.getListOrEmpty(NBT_SHRINE_CHUNKS);
            shrines.forEach(e -> pd.usedShrineChunks.add(e.asLong().orElse(0L)));

            state.players.put(UUID.fromString(key), pd);
        });

        /* destroyed rings */
        var ringsNBT = tag.getListOrEmpty(NBT_DESTROYED_FAIRY_RINGS);
        ringsNBT.forEach(e -> {
            var c = (NbtCompound) e;
            state.destroyedFairyRings.add(new ChunkPos(c.getInt("x", 0), c.getInt("z", 0)));
        });

        return state;
    }

    /* -------------------------------------------------------------------------
       §2  –  Codec & PersistentStateType registration
       ---------------------------------------------------------------------- */

    /** Thin wrapper – use NBT as the on-disk format to avoid rewriting all the logic. */
    private static final Codec<PersistentStateManager> CODEC =
            NbtCompound.CODEC.xmap(tag -> fromNbt(tag, null), PersistentStateManager::toNbt);

    /** One immutable descriptor – file will be “wellidoor-state” in the world folder. */
    public static final PersistentStateType<PersistentStateManager> TYPE =
            new PersistentStateType<>(
                    "wellidoor-state",                       // save-file key
                    ctx -> new PersistentStateManager(),     // empty constructor (new world)
                    ctx -> CODEC,                            // <-- serialiser
                    DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES // fixers
            );                                               // :contentReference[oaicite:6]{index=6}

    /* -------------------------------------------------------------------------
       §3  –  Convenience accessors matching the old API
       ---------------------------------------------------------------------- */

    public static PersistentStateManager getServerState(MinecraftServer server) {
        var mgr = Objects.requireNonNull(server.getWorld(World.OVERWORLD)).getPersistentStateManager();
        var state = mgr.getOrCreate(TYPE);                               // new API, no id arg :contentReference[oaicite:7]{index=7}
        state.markDirty();
        return state;
    }

    public static PlayerData getPlayerState(LivingEntity player) {
        return getServerState(Objects.requireNonNull(player.getWorld().getServer()))
                .players.computeIfAbsent(player.getUuid(), u -> new PlayerData());
    }

    public static void markDirty(MinecraftServer server) {
        getServerState(server).markDirty();
    }
}
