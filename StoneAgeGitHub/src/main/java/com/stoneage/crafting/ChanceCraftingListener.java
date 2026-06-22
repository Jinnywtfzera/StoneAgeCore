package com.stoneage.crafting;

import com.stoneage.StoneAgeCore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ChanceCraftingListener implements Listener {

    private final StoneAgeCore plugin;
    private final Random random = new Random();

    // วัสดุที่ถือว่า "ไม้หรือหิน" (ไม่ต้องเสียเงิน)
    private static final Set<Material> FREE_MATERIALS = new HashSet<>(Arrays.asList(
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
            Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
            Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS,
            Material.OAK_WOOD, Material.STICK,
            Material.COBBLESTONE, Material.STONE, Material.GRANITE,
            Material.DIORITE, Material.ANDESITE, Material.BLACKSTONE,
            Material.DEEPSLATE, Material.COBBLED_DEEPSLATE
    ));

    public ChanceCraftingListener(StoneAgeCore plugin) {
        this.plugin = plugin;
    }

    /**
     * ดักจับการคลิก Crafting Table → เปิด GUI แทน
     */
    @EventHandler
    public void onCraftingTableClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.CRAFTING_TABLE) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!player.hasPermission("stoneage.craft")) {
            player.sendMessage(ChatColor.RED + "คุณไม่มีสิทธิ์ใช้โต๊ะคราฟ!");
            return;
        }

        plugin.getCraftingGUI().openCraftingGUI(player);
        player.sendMessage(ChatColor.GOLD + "✦ §eเปิดโต๊ะคราฟยุคหิน §7(วางวัสดุในช่องแล้วกด §aคราฟ§7)");
    }

    /**
     * ดักจับการคลิกใน GUI
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!plugin.getCraftingGUI().isOpen(player)) return;

        Inventory inv = event.getInventory();
        String title = event.getView().getTitle();

        // ตรวจว่าเป็น GUI ของเรา
        String guiTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("crafting.gui-title", "&8[&6โต๊ะคราฟยุคหิน&8]"));
        if (!title.equals(guiTitle)) return;

        int slot = event.getRawSlot();

        // อนุญาตให้คลิกช่องวัสดุ (0,1,9,10)
        if (CraftingGUI.MATERIAL_SLOTS.contains(slot)) {
            // อนุญาตให้วาง/หยิบของได้
            return;
        }

        // ปุ่มคราฟ
        if (slot == CraftingGUI.CRAFT_BUTTON) {
            event.setCancelled(true);
            handleCraft(player, inv);
            return;
        }

        // ปุ่มยกเลิก
        if (slot == CraftingGUI.CANCEL_BUTTON) {
            event.setCancelled(true);
            returnMaterials(player, inv);
            player.closeInventory();
            return;
        }

        // บล็อก slot อื่นๆ
        event.setCancelled(true);
    }

    /**
     * เมื่อปิด GUI → คืนของที่วางไว้
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!plugin.getCraftingGUI().isOpen(player)) return;

        String guiTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("crafting.gui-title", "&8[&6โต๊ะคราฟยุคหิน&8]"));
        if (!event.getView().getTitle().equals(guiTitle)) return;

        returnMaterials(player, event.getInventory());
        plugin.getCraftingGUI().removePlayer(player);
    }

    /**
     * Logic หลักในการคราฟ
     */
    private void handleCraft(Player player, Inventory inv) {
        // ดึงวัสดุที่วางไว้
        List<ItemStack> materials = new ArrayList<>();
        for (int slot : CraftingGUI.MATERIAL_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && isRealItem(item)) {
                materials.add(item.clone());
            }
        }

        if (materials.isEmpty()) {
            player.sendMessage(ChatColor.RED + "ใส่วัสดุก่อนนะ!");
            return;
        }

        // เช็คว่าใช้แต่ไม้/หินไหม
        boolean allFree = materials.stream().allMatch(i -> FREE_MATERIALS.contains(i.getType()));
        int successRate;
        int cost = plugin.getConfig().getInt("crafting.craft-cost", 10);

        if (allFree) {
            successRate = plugin.getConfig().getInt("crafting.wood-stone-success-rate", 75);
            cost = 0;
        } else {
            successRate = plugin.getConfig().getInt("crafting.base-success-rate", 60);
            // เช็คเงิน
            if (plugin.getEconomy() != null) {
                double balance = plugin.getEconomy().getBalance(player);
                if (balance < cost) {
                    player.sendMessage(ChatColor.RED + "เงินไม่พอ! ต้องการ §e" + cost +
                            " §cเหรียญ (มีอยู่: §e" + (int) balance + "§c)");
                    return;
                }
            }
        }

        // ลองคราฟ!
        boolean success = random.nextInt(100) < successRate;

        // หักวัสดุ (ไม่ว่าจะสำเร็จหรือไม่)
        for (int slot : CraftingGUI.MATERIAL_SLOTS) {
            inv.setItem(slot, plugin.getCraftingGUI().createSlotItem(Material.GRAY_STAINED_GLASS_PANE,
                    slot == 0 ? "§7วัสดุ 1" : slot == 1 ? "§7วัสดุ 2" :
                            slot == 9 ? "§7วัสดุ 3" : "§7วัสดุ 4"));
        }

        if (success) {
            // หักเงิน
            if (cost > 0 && plugin.getEconomy() != null) {
                plugin.getEconomy().withdrawPlayer(player, cost);
            }

            // TODO: คำนวณผลลัพธ์จาก recipe (ตอนนี้ให้ demo ด้วย stick)
            // ในการใช้จริงต้องเชื่อมกับ Minecraft Recipe System
            ItemStack result = calculateResult(materials);
            player.getInventory().addItem(result);

            String msg = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("crafting.success-message", "&aคราฟสำเร็จ!"));
            player.sendMessage(msg + " §7(ใช้วัสดุไป " + materials.size() + " ชิ้น" +
                    (cost > 0 ? ", เสียเงิน §e" + cost + "§7)" : ")"));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.2f);

        } else {
            String msg = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("crafting.fail-message", "&cการคราฟล้มเหลว!"));
            player.sendMessage(msg + " §7(วัสดุสูญหาย" +
                    (cost > 0 ? ", §eไม่เสียเงิน§7)" : ")"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }

        // อัปเดต GUI
        int newRate = allFree ?
                plugin.getConfig().getInt("crafting.wood-stone-success-rate", 75) :
                plugin.getConfig().getInt("crafting.base-success-rate", 60);
        inv.setItem(CraftingGUI.RATE_SLOT, plugin.getCraftingGUI().createRateItem(newRate));
    }

    /**
     * คำนวณผลลัพธ์จากวัสดุ (ใช้ Minecraft Recipe ปกติ)
     * ตอนนี้ return วัสดุแรกเป็น placeholder — ควรเชื่อมกับ CraftingManager ในอนาคต
     */
    private ItemStack calculateResult(List<ItemStack> materials) {
        // Logic ง่ายๆ: ใช้ของแรกที่ใส่มาเป็น base
        // ในการใช้จริง ควรใช้ Bukkit.getCraftingRecipe() เพื่อตรวจสอบ recipe จริง
        if (!materials.isEmpty()) {
            ItemStack first = materials.get(0);
            return new ItemStack(first.getType(), 1);
        }
        return new ItemStack(Material.STICK);
    }

    /**
     * คืนของในช่องวัสดุให้ผู้เล่น
     */
    private void returnMaterials(Player player, Inventory inv) {
        for (int slot : CraftingGUI.MATERIAL_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && isRealItem(item)) {
                player.getInventory().addItem(item.clone());
                inv.setItem(slot, null);
            }
        }
    }

    /**
     * เช็คว่า ItemStack เป็นของจริงไม่ใช่ placeholder
     */
    private boolean isRealItem(ItemStack item) {
        if (item == null) return false;
        if (item.getType() == Material.GRAY_STAINED_GLASS_PANE) return false;
        if (item.getType() == Material.BLACK_STAINED_GLASS_PANE) return false;
        if (item.getType() == Material.LIGHT_GRAY_STAINED_GLASS_PANE) return false;
        if (item.getType() == Material.ARROW) return false;
        if (item.getType() == Material.PAPER) return false;
        if (item.getType() == Material.GOLD_NUGGET) return false;
        if (item.getType() == Material.LIME_WOOL) return false;
        if (item.getType() == Material.RED_WOOL) return false;
        return true;
    }
}
