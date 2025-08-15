package dev.ha1zen;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StarNotify extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private Map<String, String[]> playerMessages = new HashMap<>();
    private Map<String, Boolean> playerNotifications = new HashMap<>();
    private File notifyFile;
    private FileConfiguration notifyConfig;

    @Override
    public void onEnable() {
        notifyFile = new File(getDataFolder(), "notify.yml");
        if (!notifyFile.exists()) {
            notifyFile.getParentFile().mkdirs();
            saveResource("notify.yml", false);
        }
        notifyConfig = YamlConfiguration.loadConfiguration(notifyFile);

        loadPlayerMessages();
        loadPlayerNotifications();

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("mynotify").setExecutor(this);
        getCommand("mynotify").setTabCompleter(this);
        getCommand("notify").setExecutor(this);
        getCommand("notify").setTabCompleter(this);
        getLogger().info("StarNotify запущен! Folia Edition");
    }

    @Override
    public void onDisable() {
        saveMessages();
        saveNotifications();
        getLogger().info("StarNotify отключен! Folia Edition");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (playerNotifications.getOrDefault(player.getName(), true)) {
            String[] messages = playerMessages.getOrDefault(player.getName(), new String[]{"§a[+] §f" + player.getName(), "§c[-] §f" + player.getName()});
            Bukkit.broadcastMessage(messages[0]);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (playerNotifications.getOrDefault(player.getName(), true)) {
            String[] messages = playerMessages.getOrDefault(player.getName(), new String[]{"§a[+] §f" + player.getName(), "§c[-] §f" + player.getName()});
            Bukkit.broadcastMessage(messages[1]);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("mynotify")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Эту команду может использовать только игрок!");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("starnotify.mynotify")) {
                player.sendMessage("§cУ вас нет разрешения на использование этой команды!");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§cИспользование: /mynotify <join|quit> <сообщение|off>");
                return true;
            }

            String type = args[0].toLowerCase();
            String action = args[1].toLowerCase();

            if (!type.equals("join") && !type.equals("quit")) {
                player.sendMessage("§cПервый аргумент должен быть 'join' или 'quit'.");
                return true;
            }

            if (action.equals("off")) {
                String[] defaultMessages = {"§a[+] §f" + player.getName(), "§c[-] §f" + player.getName()};
                playerMessages.put(player.getName(), defaultMessages);
                player.sendMessage("§aСообщения сброшены до стандартных.");
                return true;
            }

            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                messageBuilder.append(args[i]).append(" ");
            }
            String message = messageBuilder.toString().trim();

            if (message.isEmpty()) {
                player.sendMessage("§cВы должны указать сообщение.");
                return true;
            }

            int chars = message.replace(" ", "").length();

            if (chars > 32) {
                player.sendMessage("§cСообщение должно содержать не более 32 символов без учета пробелов!");
                return true;
            }

            String[] cMessages = playerMessages.getOrDefault(player.getName(), new String[]{"", ""});
            if (type.equals("join")) {
                cMessages[0] = "§a[+] §f" + player.getName() + ". " + message;
            } else if (type.equals("quit")) {
                cMessages[1] = "§c[-] §f" + player.getName() + ". " + message;
            }

            playerMessages.put(player.getName(), cMessages);
            player.sendMessage("§aВаше сообщение установлено: " + message);
            return true;
        } else if (command.getName().equalsIgnoreCase("notify")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Эту команду может использовать только игрок!");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("starnotify.notify")) {
                player.sendMessage("§cУ вас нет разрешения на использование этой команды!");
                return true;
            }

            if (args.length != 1) {
                player.sendMessage("§cИспользование: /notify <on|off>");
                return true;
            }

            String action = args[0].toLowerCase();

            if (action.equals("off")) {
                playerNotifications.put(player.getName(), false);
                player.sendMessage("§aОповещения выключены.");
            } else if (action.equals("on")) {
                playerNotifications.put(player.getName(), true);
                player.sendMessage("§aОповещения включены.");
            } else {
                player.sendMessage("§cАргумент должен быть 'on' или 'off'.");
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("mynotify")) {
            if (args.length == 1) {
                List<String> suggestions = new ArrayList<>();
                Collections.addAll(suggestions, "join", "quit");
                return suggestions;
            } else if (args.length == 2) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add("off");
                return suggestions;
            }
        } else if (command.getName().equalsIgnoreCase("notify")) {
            if (args.length == 1) {
                List<String> suggestions = new ArrayList<>();
                Collections.addAll(suggestions, "on", "off");
                return suggestions;
            }
        }
        return Collections.emptyList();
    }

    private void loadPlayerMessages() {
        for (String playerName : notifyConfig.getKeys(false)) {
            String joinMessage = notifyConfig.getString(playerName + ".join");
            String quitMessage = notifyConfig.getString(playerName + ".quit");
            playerMessages.put(playerName, new String[]{joinMessage, quitMessage});
        }
    }

    private void loadPlayerNotifications() {
        for (String playerName : notifyConfig.getKeys(false)) {
            boolean notificationEnabled = notifyConfig.getBoolean(playerName + ".notifications", true);
            playerNotifications.put(playerName, notificationEnabled);
        }
    }

    private void saveMessages() {
        for (Map.Entry<String, String[]> entry : playerMessages.entrySet()) {
            String playerName = entry.getKey();
            String[] messages = entry.getValue();
            notifyConfig.set(playerName + ".join", messages[0]);
            notifyConfig.set(playerName + ".quit", messages[1]);
        }
        try {
            notifyConfig.save(notifyFile);
        } catch (IOException e) {
            getLogger().severe("Не удалось сохранить файл notify.yml");
            e.printStackTrace();
        }
    }

    private void saveNotifications() {
        for (Map.Entry<String, Boolean> entry : playerNotifications.entrySet()) {
            String playerName = entry.getKey();
            boolean notificationsEnabled = entry.getValue();
            notifyConfig.set(playerName + ".notifications", notificationsEnabled);
        }
        try {
            notifyConfig.save(notifyFile);
        } catch (IOException e) {
            getLogger().severe("Не удалось сохранить файл notify.yml");
            e.printStackTrace();
        }
    }
}