/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ben657.fasttravelwaypoints;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Set;
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
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

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
    public static ArrayList<Waypoint> waypoints;
    public static double pricePerBlock;

    @Override
    public void onEnable() {
        getServer().getPluginManager().addPermission(new Permission(adminPerm));
        getServer().getPluginManager().addPermission(new Permission(playerPerm));

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
    }

    @Override
    public void onDisable() {
        saveWaypoints();
    }

    public void saveWaypoints() {
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint point = waypoints.get(i);
            waypointsConfig.set(point.name + ".X", point.loc.getBlockX());
            waypointsConfig.set(point.name + ".Y", point.loc.getBlockY());
            waypointsConfig.set(point.name + ".Z", point.loc.getBlockZ());
            waypointsConfig.set(point.name + ".world", point.loc.getWorld().getName());
        }
        try {
            waypointsConfig.save(waypointsFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadWaypoints() {
        if (!waypointsFile.exists()) {
            waypointsFile.getParentFile().mkdirs();
            try {
                waypointsFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            waypointsConfig.load(waypointsFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Object[] keys = waypointsConfig.getKeys(false).toArray();
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i].toString();
            int locX = waypointsConfig.getInt(key + ".X");
            int locY = waypointsConfig.getInt(key + ".Y");
            int locZ = waypointsConfig.getInt(key + ".Z");
            String world = waypointsConfig.getString(key + ".world");
            Location loc = new Location(getServer().getWorld(world), locX, locY, locZ);
            waypoints.add(new Waypoint(loc, key));
        }
    }

    public void loadSettings() {
        if (!settingsFile.exists()) {
            settingsFile.getParentFile().mkdirs();
            try {
                settingsFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            settingsConfig.load(settingsFile);
            settingsConfig.addDefault("pricePerBlock", 10);
            settingsConfig.options().copyDefaults(true);
            settingsConfig.save(settingsFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        pricePerBlock = settingsConfig.getDouble("pricePerBlock", 10);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (command.getName().equalsIgnoreCase("FTW") && args[0].equalsIgnoreCase("create") && args.length == 2) {
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
                    if (player.hasPermission(playerPerm)) {
                        int distance = (int) player.getLocation().distance(point.loc);
                        EconomyResponse r = econ.withdrawPlayer(player.getName(), pricePerBlock * distance);
                        if (r.type == EconomyResponse.ResponseType.FAILURE) {
                            player.sendMessage(noMoney);
                            return true;
                        }
                        player.teleport(point.loc);
                        player.sendMessage(teleported + " It cost: " + pricePerBlock * distance);
                        return true;
                    } else {
                        player.sendMessage(nonExistant);
                        return true;
                    }
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
}
