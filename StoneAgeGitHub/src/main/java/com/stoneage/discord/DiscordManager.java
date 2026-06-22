package com.stoneage.discord;

import com.stoneage.StoneAgeCore;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DiscordManager implements Listener {

    private final StoneAgeCore plugin;
    // UUID ของ MC Player → Discord Display Name
    private final Map<UUID, String> discordNames = new HashMap<>();

    public DiscordManager(StoneAgeCore plugin) {
        this.plugin = plugin;
    }

    /**
     * ก่อนเข้าเกม → เช็คยศ Discord
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!plugin.getConfig().getBoolean("discord.enabled", true)) return;

        UUID uuid = event.getUniqueId();

        try {
            // ดึง Discord User ID จาก DiscordSRV
            String discordUserId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid);

            if (discordUserId == null) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                        ChatColor.RED + "คุณยังไม่ได้เชื่อม Discord!\n" +
                                ChatColor.YELLOW + "พิมพ์ /discord link ในเกมเพื่อเชื่อม");
                return;
            }

            // เช็คยศใน Guild
            String guildId = plugin.getConfig().getString("discord.guild-id", "");
            String requiredRoleId = plugin.getConfig().getString("discord.required-role-id", "");

            Guild guild = DiscordUtil.getJda().getGuildById(guildId);
            if (guild == null) {
                plugin.getLogger().warning("ไม่พบ Discord Guild ID: " + guildId);
                return; // ถ้าหา Guild ไม่เจอ ให้ผ่านไปก่อน
            }

            Member member = guild.getMemberById(discordUserId);
            if (member == null) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                        ChatColor.RED + "คุณไม่ได้อยู่ใน Discord Server!\n" +
                                ChatColor.YELLOW + "กรุณาเข้า Discord ก่อน");
                return;
            }

            // เช็คยศ
            if (!requiredRoleId.isEmpty()) {
                Role requiredRole = guild.getRoleById(requiredRoleId);
                boolean hasRole = requiredRole != null && member.getRoles().contains(requiredRole);

                if (!hasRole) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                            ChatColor.RED + "คุณไม่มียศที่ต้องการใน Discord!\n" +
                                    ChatColor.YELLOW + "ติดต่อ Admin เพื่อขอยศ");
                    return;
                }
            }

            // บันทึกชื่อ Discord
            String displayName = member.getEffectiveName(); // ชื่อเล่นหรือชื่อจริงใน Discord
            discordNames.put(uuid, displayName);

        } catch (Exception e) {
            plugin.getLogger().warning("เกิดข้อผิดพลาดในการเช็ค Discord: " + e.getMessage());
            // ถ้าเกิด error ให้ผ่านไปก่อน (เพื่อไม่ให้ server crash)
        }
    }

    /**
     * เมื่อผู้เล่นเข้าเกม → เปลี่ยนชื่อเป็นชื่อ Discord + แสดงข้อความต้อนรับ
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // รอ 1 tick ให้ load เสร็จก่อน
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String discordName = discordNames.getOrDefault(uuid, player.getName());

            // เปลี่ยน Display Name เป็นชื่อ Discord
            player.setDisplayName(ChatColor.WHITE + discordName);
            player.setPlayerListName(ChatColor.WHITE + discordName);

            // ข้อความต้อนรับในแชทเกม (broadcast ให้ทุกคน)
            String welcomeMsg = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("discord.welcome-message",
                            "&a&l✦ &fยินดีต้อนรับคุณ &e{discord_name} &fเข้าสู่เซิร์ฟเวอร์ &a&l✦"))
                    .replace("{discord_name}", discordName)
                    .replace("{mc_name}", player.getName());

            Bukkit.broadcastMessage(welcomeMsg);

            // ยกเลิก join message ปกติ
            event.setJoinMessage(null);

            // แจ้งยอดเงินเริ่มต้น
            if (plugin.getEconomy() != null && !player.hasPlayedBefore()) {
                double startBalance = plugin.getConfig().getDouble("economy.starting-balance", 100);
                plugin.getEconomy().depositPlayer(player, startBalance);
                player.sendMessage(ChatColor.GOLD + "💰 คุณได้รับเงินเริ่มต้น §e" + startBalance +
                        " §6เหรียญ §7ยินดีต้อนรับ!");
            }
        }, 1L);
    }

    /**
     * เมื่อผู้เล่นออกเกม → แสดงข้อความ
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String discordName = discordNames.getOrDefault(player.getUniqueId(), player.getName());

        String leaveMsg = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("discord.leave-message",
                        "&c{discord_name} &fได้ออกจากเซิร์ฟเวอร์แล้ว"))
                .replace("{discord_name}", discordName)
                .replace("{mc_name}", player.getName());

        event.setQuitMessage(leaveMsg);
        discordNames.remove(player.getUniqueId());
    }

    /**
     * ดึงชื่อ Discord ของผู้เล่น
     */
    public String getDiscordName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return discordNames.getOrDefault(uuid, player != null ? player.getName() : "Unknown");
    }

    public Map<UUID, String> getDiscordNames() { return discordNames; }
}
