package com.stoneage.grave;

import com.stoneage.StoneAgeCore;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GraveListener implements Listener {

    private final StoneAgeCore plugin;

    public GraveListener(StoneAgeCore plugin) {
        this.plugin = plugin;
    }

    /**
     * เมื่อผู้เล่นตาย → เก็บของทั้งหมด สร้างกล่องศพ
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("grave.enabled", true)) return;

        // เก็บ drop items ทั้งหมด
        List<ItemStack> drops = new ArrayList<>(event.getDrops());

        if (drops.isEmpty()) return;

        // ยกเลิก drop ปกติ (ของจะไปอยู่ในกล่องแทน)
        event.getDrops().clear();
        event.setKeepInventory(false);

        // สร้างกล่องศพ
        plugin.getGraveManager().createGrave(
                event.getEntity(),
                drops,
                event.getEntity().getLocation()
        );
    }

    /**
     * เมื่อทุบบล็อก → ถ้าเป็นกล่องศพ ให้ทุบแล้วได้ของข้างใน แต่ไม่ได้กล่อง
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST) return;

        if (!plugin.getGraveManager().isGrave(block.getLocation())) return;

        // ป้องกัน drop chest
        event.setDropItems(false);

        // ดึงของใน Chest ออกมา drop
        if (block.getState() instanceof Chest chest) {
            for (ItemStack item : chest.getInventory().getContents()) {
                if (item != null) {
                    block.getWorld().dropItemNaturally(block.getLocation(), item);
                }
            }
            chest.getInventory().clear();
        }

        // ลบ grave record
        plugin.getGraveManager().onGraveEmptied(block.getLocation());

        event.getPlayer().sendMessage("§7คุณได้ทุบกล่องศพ — ของที่เหลือถูกดรอปออกมา");
    }

    /**
     * เมื่อปิด inventory ของ Chest ที่เป็นกล่องศพ → เช็คว่าว่างหรือยัง
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Chest chest)) return;

        Block block = chest.getBlock();
        if (!plugin.getGraveManager().isGrave(block.getLocation())) return;

        // เช็คว่า inventory ว่างไหม
        boolean isEmpty = true;
        for (ItemStack item : chest.getInventory().getContents()) {
            if (item != null) {
                isEmpty = false;
                break;
            }
        }

        if (isEmpty) {
            // ของหมดแล้ว → ลบกล่องศพ
            plugin.getGraveManager().onGraveEmptied(block.getLocation());
        }
    }
}
