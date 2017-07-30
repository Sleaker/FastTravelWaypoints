/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ben657.fasttravelwaypoints;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import bsh.EvalError;
import bsh.Interpreter;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.api.ExperienceAPI;
import java.io.IOException;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.scheduler.BukkitRunnable;

/**
 *
 * @author BEN_S
 */
public class FastTravelWaypoints extends JavaPlugin {

    Economy econ;
    File settingsFile;
    File waypointsFile;
    FileConfiguration settingsConfig;
    FileConfiguration waypointsConfig;
    String adminPerm = "FTW.admin";
    String playerPerm = "FTW.player";
    String noPerm = ChatColor.RED + "[FTW] Sorry, you do not have permission to do that.";
    String created = ChatColor.YELLOW + "[FTW] A new waypoint has been created.";
    String deleted = ChatColor.YELLOW + "[FTW] The waypoint has been deleted.";
    String nonExistant = ChatColor.RED + "[FTW] The waypoint given does not exist.";
    String teleported = ChatColor.YELLOW + "[FTW] You have been teleported to the given waypoint.";
    String noMoney = ChatColor.RED + "[FTW] You do not have enough money to go there.";
    String foundPoint = ChatColor.YELLOW + "[FTW] You have found a new waypoint: ";
    String notFoundPoint = ChatColor.RED + "[FTW] You have not found that waypoint.";
    public static ArrayList<Waypoint> waypoints;
    public static double worldToWorldPrice;
    public static double activateDistance;
    public static String priceEqn;
    public mcMMO mmo;

    @Override
    public void onEnable() {

        mmo = (mcMMO) getServer().getPluginManager().getPlugin("mcMMO");
        if (mmo != null ) getLogger().info("Connected to mcMMO");
        RegisteredServiceProvider<Economy> econProv = getServer().getServicesManager().getRegistration(Economy.class);
        if (econProv != null) {
            econ = econProv.getProvider();
        }

        getLogger().log(Level.INFO, "Loading settings and waypoints.");
        waypoints = new ArrayList<Waypoint>();

        settingsFile = new File(getDataFolder(), "settings.yml");
        waypointsFile = new File(getDataFolder(), "waypoints.yml");
        settingsConfig = new YamlConfiguration();
        waypointsConfig = new YamlConfiguration();
        loadWaypoints();
        loadSettings();
        getLogger().log(Level.INFO, "Loaded " + waypoints.size() + " waypoints.");

        BukkitRunnable foundCheck = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    for (int i = 0; i < waypoints.size(); i++) {
                        Waypoint point = waypoints.get(i);
                        if (!player.getLocation().getWorld().getName().equals(point.loc.getWorld().getName())) {
                            continue;
                        }
                        if (point.loc.distance(player.getLocation()) < activateDistance) {
                            boolean newFind = point.tryFind(player.getName(), false);
                            if (newFind) {
                                player.sendMessage(foundPoint + point.name);
                            }
                        }
                    }
                }
            }
        };

        foundCheck.runTaskTimer(this, 20, 20);
    }

    @Override
    public void onDisable() {
        saveWaypoints();
    }

    public void saveWaypoints() {
        try {
            String[] sections = waypointsConfig.getConfigurationSection("").getKeys(false).toArray(new String[0]);
            for (String s : sections) {
                waypointsConfig.set(s, null);
                waypointsConfig.save(waypointsFile);
            }
        } catch (IOException e) {
            this.getLogger().log(Level.SEVERE, e.getMessage());
        }
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint point = waypoints.get(i);
            waypointsConfig.set(point.name + ".X", point.loc.getBlockX());
            waypointsConfig.set(point.name + ".Y", point.loc.getBlockY());
            waypointsConfig.set(point.name + ".Z", point.loc.getBlockZ());
            waypointsConfig.set(point.name + ".world", point.loc.getWorld().getName());
            waypointsConfig.set(point.name + ".foundBy", point.foundBy);
        }
        try {
            waypointsConfig.save(waypointsFile);
        } catch (IOException e) {
            this.getLogger().log(Level.SEVERE, e.getMessage());
        }
    }

    public void loadWaypoints() {
        if (!waypointsFile.exists()) {
            waypointsFile.getParentFile().mkdirs();
            try {
                waypointsFile.createNewFile();
            } catch (IOException e) {
                this.getLogger().log(Level.SEVERE, e.getMessage());
            }
        }
        try {
            waypointsConfig.load(waypointsFile);
        } catch (IOException e) {
            this.getLogger().log(Level.SEVERE, e.getMessage());
        } catch (InvalidConfigurationException e) {
            this.getLogger().log(Level.SEVERE, e.getMessage());
        }
        Object[] keys = waypointsConfig.getKeys(false).toArray();
        for (Object keyObj : keys) {
            String key = keyObj.toString();
            int locX = waypointsConfig.getInt(key + ".X");
            int locY = waypointsConfig.getInt(key + ".Y");
            int locZ = waypointsConfig.getInt(key + ".Z");
            String world = waypointsConfig.getString(key + ".world");
            Location loc = new Location(getServer().getWorld(world), locX, locY, locZ);
            List<String> foundBy = waypointsConfig.getStringList(key + ".foundBy");
            Waypoint point = new Waypoint(loc, key);
            point.foundBy = foundBy;
            waypoints.add(point);
        }
    }

    public void loadSettings() {
        if (!settingsFile.exists()) {
            settingsFile.getParentFile().mkdirs();
            try {
                settingsFile.createNewFile();
            } catch (IOException e) {
                this.getLogger().log(Level.SEVERE, e.getMessage());
            }
        }
        try {
            settingsConfig.load(settingsFile);
            settingsConfig.addDefault("priceEqn", "[DISTANCE]*5");
            settingsConfig.addDefault("worldToWorldPrice", 200);
            settingsConfig.addDefault("activateRadius", 5);
            settingsConfig.options().copyDefaults(true);
            settingsConfig.save(settingsFile);
        } catch (IOException e) {
            this.getLogger().log(Level.SEVERE, e.getMessage());
        } catch (InvalidConfigurationException e) {
            this.getLogger().log(Level.SEVERE, e.getMessage());
        }
        priceEqn = settingsConfig.getString("priceEqn", "10");
        if (priceEqn.contains("[PWRLVL]") && mmo == null) {
            System.out.println("[FTW] You have added an mcMMO specific variable to your price equation, but mcMMO is not loaded.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        worldToWorldPrice = settingsConfig.getDouble("worldToWorldPrice", 200);
        activateDistance = settingsConfig.getDouble("activateRadius", 5);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if ((command.getName().equalsIgnoreCase("FTW") && args.length == 0) || (command.getName().equalsIgnoreCase("FTW") && args.length == 1 && args[0].equalsIgnoreCase("list"))) {
                player.sendMessage(ChatColor.YELLOW + "[FTW] Current waypoints:");
                for (int i = 0; i < waypoints.size(); i++) {
                    Waypoint point = waypoints.get(i);
                    if (!point.tryFind(player.getName(), true)) {
                        player.sendMessage(ChatColor.AQUA + point.name + " - price: " + getPrice(player, point));
                    }
                }
                return true;

            } else if (command.getName().equalsIgnoreCase("FTW") && args.length == 2 && args[0].equalsIgnoreCase("list") && args[1].equalsIgnoreCase("all")) {
                if (player.hasPermission(adminPerm)) {
                    player.sendMessage(ChatColor.YELLOW + "[FTW] Current waypoints:");
                    for (int i = 0; i < waypoints.size(); i++) {
                        Waypoint point = waypoints.get(i);
                        if (!point.tryFind(player.getName(), true)) {
                            player.sendMessage(ChatColor.AQUA + point.name + " - price: " + getPrice(player, point));
                        }
                    }
                }
                else
                {
                    player.sendMessage(noPerm);
                }
                return true;
            } else if (command.getName().equalsIgnoreCase("FTW") && args[0].equalsIgnoreCase("create") && args.length == 2) {
                if (player.hasPermission(adminPerm)) {
                    Waypoint point = new Waypoint(player.getLocation(), args[1]);
                    waypoints.add(point);
                    player.sendMessage(created);
                    return true;
                } else {
                    player.sendMessage(noPerm);
                    return true;
                }
            } else if (command.getName().equalsIgnoreCase("FTW") && args[0].equalsIgnoreCase("delete") && args.length == 2) {
                if (player.hasPermission(adminPerm)) {
                    Waypoint point = getWaypointFromName(args[1]);
                    if (point != null) {
                        waypoints.remove(point);
                        player.sendMessage(deleted);
                        System.out.println(waypoints.size());
                        return true;
                    } else {
                        player.sendMessage(nonExistant);
                        return true;
                    }
                } else {
                    player.sendMessage(noPerm);
                    return true;
                }
            } else if (command.getName().equalsIgnoreCase("FTW") && args.length == 1) {
                Waypoint point = getWaypointFromName(args[0]);
                if (point != null) {
                    if ((player.hasPermission(playerPerm) && !point.tryFind(player.getName(), true)) || player.hasPermission(adminPerm)) {
                        int distance = (int) player.getLocation().distance(point.loc);
                        double price = getPrice(player, point);
                        EconomyResponse r = econ.withdrawPlayer(player.getName(), price);
                        if (r.type == EconomyResponse.ResponseType.FAILURE) {
                            player.sendMessage(noMoney);
                            return true;
                        }
                        player.teleport(point.loc);
                        player.sendMessage(teleported + " It cost: " + price);
                        return true;
                    } else if (point.tryFind(player.getName(), true)) {
                        player.sendMessage(notFoundPoint);
                        return true;
                    } else {
                        player.sendMessage(noPerm);
                        return true;
                    }
                } else {
                    player.sendMessage(nonExistant);
                    return true;
                }
            }
        }
        return false;
    }

    public Waypoint getWaypointFromName(String name) {
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint point = waypoints.get(i);
            if (point.name.equalsIgnoreCase(name)) {
                return point;
            }
        }
        return null;
    }

    public boolean isWaypontInRegion(String region, Waypoint point) {
        return false;
    }

    public boolean isPlayerInRegion(String region, Player player) {
        return false;
    }

    public double getPrice(Player player, Waypoint point) {
        int crossWorld = 0;
        if (!player.getLocation().getWorld().getName().equals(point.loc.getWorld().getName())) {
            crossWorld = 1;
            point.loc.setWorld(player.getLocation().getWorld());
        }
        String priceStr = priceEqn.replace("[DISTANCE]", String.valueOf(player.getLocation().distance(point.loc)));
        priceStr = priceStr.replaceAll("[XWORLD]", String.valueOf(crossWorld));
        priceStr = priceStr.replaceAll("[LVL]", String.valueOf(player.getLevel()));
        if (mmo != null) {
            priceStr = priceStr.replaceAll("[PWRLVL]", String.valueOf(ExperienceAPI.getPowerLevel(player)));
        } else if (priceStr.contains("[PWRLVL")) {
            getLogger().log(Level.SEVERE, "mcMMO power level used in calculation but not loaded.");
        }
        Interpreter interp = new Interpreter();
        try {
            interp.eval("price=" + priceStr);
            return Math.round(Double.parseDouble(String.valueOf(interp.get("price"))));
        } catch (EvalError ex) {
            this.getLogger().log(Level.SEVERE, ex.getMessage());
        }
        return 0;
    }
}
