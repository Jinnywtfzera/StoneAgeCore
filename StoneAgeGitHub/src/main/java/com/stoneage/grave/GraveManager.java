package com.stoneage.grave;

import com.stoneage.StoneAgeCore;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GraveManager {

    private final StoneAgeCore plugin;
    // UUID ของกล่องศพ → ข้อมูล Grave
    private final Map<UUID, GraveData> activeGraves = new HashMap<>();
    // Location string → UUID ของกล่องศพ (เพื่อเช็คเมื่อมีการทุบ)
    private final Map<String, UUID> locationToGrave = new HashMap<>();

    public GraveManager(StoneAgeCore plugin) {
        this.plugin = plugin;
    }

    /**
     * สร้างกล่องศพที่ตำแหน่งที่ผู้เล่นตาย
     */
    public void createGrave(Player player, List<ItemStack> items, Location deathLocation) {
        Location safeLoc = findSafeLocation(deathLocation);
        if (safeLoc == null) {
            // ถ้าหาตำแหน่งปลอดภัยไม่ได้ ให้ดรอปของตามปกติ
            for (ItemStack item : items) {
                if (item != null) deathLocation.getWorld().dropItemNaturally(deathLocation, item);
            }
            return;
        }

        // วาง Chest
        Block block = safeLoc.getBlock();
        block.setType(org.bukkit.Material.CHEST);

        // ตั้งชื่อ Chest และใส่ของ
        if (block.getState() instanceof Chest chest) {
            String graveName = plugin.getConfig().getString("grave.grave-name", "&c☠ ศพของ {player}")
                    .replace("{player}", player.getName())
                    .replace("&", "§");
            // ตั้งชื่อ chest ผ่าน CustomName (ใช้ PersistentDataContainer แทน)
            org.bukkit.inventory.Inventory inv = chest.getInventory();
            for (ItemStack item : items) {
                if (item != null) inv.addItem(item);
            }
            chest.update();
        }

        // สร้าง GraveData และบันทึก
        UUID graveId = UUID.randomUUID();
        long despawnTime = plugin.getConfig().getLong("grave.despawn-time", 300);
        long expireAt = despawnTime > 0 ? System.currentTimeMillis() + (despawnTime * 1000L) : -1;

        GraveData data = new GraveData(graveId, player.getUniqueId(), player.getName(), safeLoc, expireAt, items);
        activeGraves.put(graveId, data);
        locationToGrave.put(locationKey(safeLoc), graveId);

        // แจ้งผู้เล่น
        player.sendMessage("§c☠ §fกล่องศพของคุณถูกสร้างที่ §e" +
                (int) safeLoc.getX() + ", " + (int) safeLoc.getY() + ", " + (int) safeLoc.getZ());

        if (despawnTime > 0) {
            player.sendMessage("§7กล่องจะหายภายใน §c" + despawnTime + " §7วินาที");
        }

        // แสดง Particle รอบกล่องศพ
        if (plugin.getConfig().getBoolean("grave.show-particles", true)) {
            startParticleTask(graveId, safeLoc);
        }

        // ตั้งเวลาหาย
        if (expireAt > 0) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                removeGrave(graveId, true);
            }, despawnTime * 20L);
        }
    }

    /**
     * เช็คว่า Location นี้เป็นกล่องศพไหม
     */
    public boolean isGrave(Location loc) {
        return locationToGrave.containsKey(locationKey(loc));
    }

    /**
     * ดึง GraveData จาก Location
     */
    public GraveData getGraveAt(Location loc) {
        UUID graveId = locationToGrave.get(locationKey(loc));
        if (graveId == null) return null;
        return activeGraves.get(graveId);
    }

    /**
     * เรียกเมื่อ Chest ว่างหมด (เก็บของครบ) → ลบกล่องทิ้ง
     */
    public void onGraveEmptied(Location loc) {
        String key = locationKey(loc);
        UUID graveId = locationToGrave.get(key);
        if (graveId != null) {
            removeGrave(graveId, false);
        }
    }

    /**
     * ลบกล่องศพ
     * @param dropItems true = ดรอปของที่เหลือ (กรณี despawn)
     */
    public void removeGrave(UUID graveId, boolean dropItems) {
        GraveData data = activeGraves.remove(graveId);
        if (data == null) return;

        locationToGrave.remove(locationKey(data.getLocation()));
        Block block = data.getLocation().getBlock();

        if (block.getState() instanceof Chest chest) {
            if (dropItems) {
                // ดรอปของที่เหลือออกมา
                for (ItemStack item : chest.getInventory().getContents()) {
                    if (item != null) data.getLocation().getWorld().dropItemNaturally(data.getLocation(), item);
                }
            }
            chest.getInventory().clear();
        }

        // ลบบล็อก
        block.setType(org.bukkit.Material.AIR);
    }

    private void startParticleTask(UUID graveId, Location loc) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!activeGraves.containsKey(graveId)) {
                task.cancel();
                return;
            }
            loc.getWorld().spawnParticle(
                    org.bukkit.Particle.SOUL,
                    loc.clone().add(0.5, 1, 0.5),
                    5, 0.3, 0.5, 0.3, 0.01
            );
        }, 0L, 10L);
    }

    private Location findSafeLocation(Location loc) {
        // ลองตำแหน่งเดิมก่อน
        if (isSafe(loc)) return loc;
        // ลองด้านบน
        for (int y = 1; y <= 5; y++) {
            Location up = loc.clone().add(0, y, 0);
            if (isSafe(up)) return up;
        }
        return loc; // คืนตำแหน่งเดิมถ้าหาไม่ได้
    }

    private boolean isSafe(Location loc) {
        org.bukkit.Material mat = loc.getBlock().getType();
        return mat == org.bukkit.Material.AIR || mat == org.bukkit.Material.WATER;
    }

    private String locationKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public int getActiveGraves() { return activeGraves.size(); }

    public void saveAll() {
        // TODO: บันทึก grave data ลง file เพื่อรองรับ server restart
        plugin.getLogger().info("บันทึกข้อมูล Graves: " + activeGraves.size() + " กล่อง");
    }
}
