package com.stoneage.crafting;

import com.stoneage.StoneAgeCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CraftingGUI {

    private final StoneAgeCore plugin;
    // ผู้เล่นที่กำลังเปิด GUI อยู่ → slot ที่วางวัสดุ
    private final Map<UUID, ItemStack[]> openInventories = new HashMap<>();

    // GUI Layout:
    // [0-8] Row 1: วัสดุ (slot 0-3 = 4 ช่องวัสดุ), slot 4 = ลูกศร, slot 5 = ผลลัพธ์
    // [9] = ช่องแสดงอัตราสำเร็จ
    // [18] = ปุ่มคราฟ, [26] = ปุ่มยกเลิก

    public static final int[] INPUT_SLOTS = {0, 1, 2, 3, 9, 10, 11, 18}; // 8 ช่องวัสดุ (3x3 ยกเว้น center)
    public static final int CENTER_SLOT = 4;
    public static final int ARROW_SLOT = 13;
    public static final int RESULT_SLOT = 22;
    public static final int RATE_SLOT = 15;
    public static final int CRAFT_BUTTON = 24;
    public static final int CANCEL_BUTTON = 26;
    public static final int MONEY_SLOT = 6;

    public CraftingGUI(StoneAgeCore plugin) {
        this.plugin = plugin;
    }

    /**
     * เปิด GUI คราฟให้ผู้เล่น
     */
    public void openCraftingGUI(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("crafting.gui-title", "&8[&6โต๊ะคราฟยุคหิน&8]"));

        Inventory gui = Bukkit.createInventory(null, 27, title);

        // ตกแต่ง GUI
        fillBorder(gui);

        // ช่องวัสดุ (2x2 ตรงกลางซ้าย)
        gui.setItem(0, createSlotItem(Material.GRAY_STAINED_GLASS_PANE, "§7วัสดุ 1"));
        gui.setItem(1, createSlotItem(Material.GRAY_STAINED_GLASS_PANE, "§7วัสดุ 2"));
        gui.setItem(9, createSlotItem(Material.GRAY_STAINED_GLASS_PANE, "§7วัสดุ 3"));
        gui.setItem(10, createSlotItem(Material.GRAY_STAINED_GLASS_PANE, "§7วัสดุ 4"));

        // ลูกศร
        gui.setItem(ARROW_SLOT, createSlotItem(Material.ARROW, "§e→ คราฟ →"));

        // ช่องผลลัพธ์
        gui.setItem(RESULT_SLOT, createSlotItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7ผลลัพธ์"));

        // อัตราสำเร็จ
        int baseRate = plugin.getConfig().getInt("crafting.base-success-rate", 60);
        gui.setItem(RATE_SLOT, createRateItem(baseRate));

        // ช่องเงิน
        int cost = plugin.getConfig().getInt("crafting.craft-cost", 10);
        gui.setItem(MONEY_SLOT, createCostItem(cost));

        // ปุ่มคราฟ
        gui.setItem(CRAFT_BUTTON, createButtonItem(Material.LIME_WOOL, "§a§lคราฟ!", "§7คลิกเพื่อคราฟ"));

        // ปุ่มยกเลิก
        gui.setItem(CANCEL_BUTTON, createButtonItem(Material.RED_WOOL, "§c§lยกเลิก", "§7ปิด GUI"));

        player.openInventory(gui);
        openInventories.put(player.getUniqueId(), new ItemStack[27]);
    }

    private void fillBorder(Inventory gui) {
        ItemStack glass = createSlotItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, glass);
        }
    }

    public ItemStack createSlotItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createButtonItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Collections.singletonList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createRateItem(int rate) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eอัตราสำเร็จ");
            List<String> lore = new ArrayList<>();
            lore.add("§7โอกาสสำเร็จ: §a" + rate + "%");
            lore.add("§7โอกาสล้มเหลว: §c" + (100 - rate) + "%");
            lore.add("");
            lore.add("§7ไม้/หิน: §a" + plugin.getConfig().getInt("crafting.wood-stone-success-rate", 75) + "%");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createCostItem(int cost) {
        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6ค่าใช้จ่าย");
            List<String> lore = new ArrayList<>();
            lore.add("§7ราคา: §e" + cost + " เหรียญ");
            lore.add("§7(ใช้ไม้/หิน = ฟรี แต่ยังมีโอกาสล้มเหลว)");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isOpen(Player player) {
        return openInventories.containsKey(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        openInventories.remove(player.getUniqueId());
    }

    // Slot สำหรับวางวัสดุใน GUI (ช่อง 0,1,9,10)
    public static final Set<Integer> MATERIAL_SLOTS = new HashSet<>(Arrays.asList(0, 1, 9, 10));
}
