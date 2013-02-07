/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ben657.fasttravelwaypoints;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;

/**
 *
 * @author BEN_S
 */
public class Waypoint {

    public Location loc;
    public String name;
    public List<String> foundBy;

    public Waypoint(Location loc, String name) {
        this.loc = loc;
        this.name = name;
        foundBy = new ArrayList<String>();
    }

    public boolean tryFind(String player, boolean tp) {
        if (foundBy.contains(player)) {
            return false;
        }
        if (!tp) {
            foundBy.add(player);
        }
        return true;
    }
}
