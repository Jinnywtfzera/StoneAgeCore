package com.stoneage.economy;

import com.stoneage.StoneAgeCore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class EconomyManager {

    private final StoneAgeCore plugin;
    private final Map<Material, Double> sellPrices = new HashMap<>();

    public EconomyManager(StoneAgeCore plugin) {
        this.plugin = plugin;
        loadPrices();
    }

    private void loadPrices() {
        sellPrices.clear();
        if (plugin.getConfig().getConfigurationSection("economy.sell-prices") == null) return;

        for (String key : plugin.getConfig().getConfigurationSection("economy.sell-prices").getKeys(false)) {
            try {
                Material mat = Material.valueOf(key.toUpperCase());
                double price = plugin.getConfig().getDouble("economy.sell-prices." + key, 1.0);
                sellPrices.put(mat, price);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("ไม่รู้จัก Material: " + key);
            }
        }
        plugin.getLogger().info("โหลดราคาขายสำเร็จ: " + sellPrices.size() + " รายการ");
    }

    /**
     * ขายไอเทมที่ถือในมือขวา
     */
    public void sellItemInHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "ถือของก่อนนะ!");
            return;
        }

        Double price = sellPrices.get(item.getType());
        if (price == null) {
            player.sendMessage(ChatColor.RED + "ของชิ้นนี้ขายไม่ได้!");
            return;
        }

        int amount = item.getAmount();
        double total = price * amount;

        // หักของ
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        // เพิ่มเงิน
        if (plugin.getEconomy() != null) {
            plugin.getEconomy().depositPlayer(player, total);
        }

        String currency = plugin.getConfig().getString("economy.currency-name", "เหรียญ");
        player.sendMessage(ChatColor.GREEN + "ขาย §e" + item.getType().name().toLowerCase() +
                " x" + amount + " §aได้รับ §e" + total + " " + currency);
        player.sendMessage(ChatColor.GRAY + "ยอดเงินปัจจุบัน: §e" +
                (plugin.getEconomy() != null ? plugin.getEconomy().getBalance(player) : 0) + " " + currency);
    }

    /**
     * ขายทั้งกระเป๋า
     */
    public void sellAll(Player player) {
        if (plugin.getEconomy() == null) {
            player.sendMessage(ChatColor.RED + "ระบบเงินไม่พร้อมใช้งาน!");
            return;
        }

        double totalEarned = 0;
        int itemsSold = 0;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            Double price = sellPrices.get(item.getType());
            if (price == null) continue;

            double earned = price * item.getAmount();
            totalEarned += earned;
            itemsSold += item.getAmount();
            player.getInventory().setItem(i, null);
        }

        if (itemsSold == 0) {
            player.sendMessage(ChatColor.RED + "ไม่มีของที่ขายได้ในกระเป๋า!");
            return;
        }

        plugin.getEconomy().depositPlayer(player, totalEarned);
        String currency = plugin.getConfig().getString("economy.currency-name", "เหรียญ");
        player.sendMessage(ChatColor.GREEN + "ขายทั้งหมด " + itemsSold + " ชิ้น ได้รับ §e" +
                totalEarned + " " + currency);
    }

    /**
     * เช็คยอดเงิน
     */
    public void checkBalance(Player player) {
        if (plugin.getEconomy() == null) {
            player.sendMessage(ChatColor.RED + "ระบบเงินไม่พร้อมใช้งาน!");
            return;
        }
        String currency = plugin.getConfig().getString("economy.currency-name", "เหรียญ");
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "💰");
        double balance = plugin.getEconomy().getBalance(player);
        player.sendMessage(ChatColor.GOLD + symbol + " ยอดเงินของคุณ: §e" + balance + " " + currency);
    }

    /**
     * แสดงราคาขายทั้งหมด
     */
    public void showPriceList(Player player) {
        String currency = plugin.getConfig().getString("economy.currency-name", "เหรียญ");
        player.sendMessage(ChatColor.GOLD + "=== รายการราคาขาย ===");
        sellPrices.forEach((mat, price) -> {
            player.sendMessage(ChatColor.YELLOW + mat.name().toLowerCase() +
                    ChatColor.WHITE + ": §e" + price + " " + currency + "/ชิ้น");
        });
    }

    public Map<Material, Double> getSellPrices() { return sellPrices; }

    public void reload() { loadPrices(); }
}
