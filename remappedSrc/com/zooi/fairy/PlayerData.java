package com.zooi.fairy;

import net.minecraft.util.math.Vec3i;

import java.util.HashSet;


public class PlayerData {
    public float haunting = 0;
    public boolean brokeFairyRing = false;
    public HashSet<Vec3i> pastBedLocations = new HashSet<>();
    public HashSet<Long> usedShrineChunks = new HashSet<>();
}
