/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ben657.fasttravelwaypoints;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 *
 * @author BEN_S
 */
public class FTWEvents implements Listener {

    FastTravelWaypoints plugin;

    public FTWEvents(FastTravelWaypoints plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onMove(PlayerMoveEvent e) {
        
        Player player = e.getPlayer();
        for (int i = 0; i < FastTravelWaypoints.waypoints.size(); i++) {
            Waypoint point = FastTravelWaypoints.waypoints.get(i);
            if (point.loc.distance(player.getLocation()) < FastTravelWaypoints.activateDistance) {
                boolean newFind = point.tryFind(player.getName(), false);
                if (newFind) {
                    player.sendMessage(plugin.foundPoint + point.name);
                    plugin.saveWaypoints();
                }
            }

        }
    }
}
