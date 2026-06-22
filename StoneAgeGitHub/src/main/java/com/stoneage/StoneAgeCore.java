package com.stoneage;

import com.stoneage.crafting.ChanceCraftingListener;
import com.stoneage.crafting.CraftingGUI;
import com.stoneage.discord.DiscordManager;
import com.stoneage.economy.EconomyManager;
import com.stoneage.grave.GraveListener;
import com.stoneage.grave.GraveManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class StoneAgeCore extends JavaPlugin {

    private static StoneAgeCore instance;
    private Economy economy;
    private GraveManager graveManager;
    private EconomyManager economyManager;
    private DiscordManager discordManager;
    private CraftingGUI craftingGUI;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Setup Vault Economy
        if (!setupEconomy()) {
            getLogger().severe("Vault หรือ Economy Plugin ไม่พบ! กำลังปิดระบบเศรษฐกิจ...");
        }

        // Initialize Managers
        graveManager = new GraveManager(this);
        economyManager = new EconomyManager(this);
        discordManager = new DiscordManager(this);
        craftingGUI = new CraftingGUI(this);

        // Register Listeners
        getServer().getPluginManager().registerEvents(new GraveListener(this), this);
        getServer().getPluginManager().registerEvents(new ChanceCraftingListener(this), this);
        getServer().getPluginManager().registerEvents(discordManager, this);

        // Register Commands
        getCommand("stoneage").setExecutor(new StoneAgeCommand(this));

        getLogger().info("=================================");
        getLogger().info("  StoneAge Core Plugin เริ่มทำงาน!");
        getLogger().info("=================================");
    }

    @Override
    public void onDisable() {
        if (graveManager != null) graveManager.saveAll();
        getLogger().info("StoneAge Core Plugin ปิดทำงาน.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager()
                .getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public static StoneAgeCore getInstance() { return instance; }
    public Economy getEconomy() { return economy; }
    public GraveManager getGraveManager() { return graveManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public DiscordManager getDiscordManager() { return discordManager; }
    public CraftingGUI getCraftingGUI() { return craftingGUI; }
}
