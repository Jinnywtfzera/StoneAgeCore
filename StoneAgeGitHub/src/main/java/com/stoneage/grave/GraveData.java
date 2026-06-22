package com.stoneage.grave;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class GraveData {

    private final UUID graveId;
    private final UUID ownerUUID;
    private final String ownerName;
    private final Location location;
    private final long expireAt; // -1 = ไม่หาย
    private final List<ItemStack> originalItems;

    public GraveData(UUID graveId, UUID ownerUUID, String ownerName,
                     Location location, long expireAt, List<ItemStack> originalItems) {
        this.graveId = graveId;
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.location = location;
        this.expireAt = expireAt;
        this.originalItems = originalItems;
    }

    public UUID getGraveId() { return graveId; }
    public UUID getOwnerUUID() { return ownerUUID; }
    public String getOwnerName() { return ownerName; }
    public Location getLocation() { return location; }
    public long getExpireAt() { return expireAt; }
    public List<ItemStack> getOriginalItems() { return originalItems; }

    public boolean isExpired() {
        return expireAt > 0 && System.currentTimeMillis() > expireAt;
    }
}
