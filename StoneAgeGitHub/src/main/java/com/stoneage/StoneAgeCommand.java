package com.stoneage;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class StoneAgeCommand implements CommandExecutor {

    private final StoneAgeCore plugin;

    public StoneAgeCommand(StoneAgeCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "=== StoneAge Core v1.0 ===");
            sender.sendMessage(ChatColor.YELLOW + "/stoneage reload " + ChatColor.WHITE + "- โหลดค่าใหม่");
            sender.sendMessage(ChatColor.YELLOW + "/stoneage info " + ChatColor.WHITE + "- ข้อมูลระบบ");
            return true;
        }

        if (!sender.hasPermission("stoneage.admin")) {
            sender.sendMessage(ChatColor.RED + "คุณไม่มีสิทธิ์ใช้คำสั่งนี้!");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "โหลดค่าใหม่สำเร็จ!");
                break;
            case "info":
                sender.sendMessage(ChatColor.GOLD + "=== ข้อมูลระบบ ===");
                sender.sendMessage(ChatColor.YELLOW + "กล่องศพที่ active: " + plugin.getGraveManager().getActiveGraves());
                sender.sendMessage(ChatColor.YELLOW + "เวอร์ชัน: 1.0.0");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "คำสั่งไม่ถูกต้อง!");
        }
        return true;
    }
}
