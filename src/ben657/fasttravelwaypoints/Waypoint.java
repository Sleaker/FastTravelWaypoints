/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ben657.fasttravelwaypoints;

import org.bukkit.Location;

/**
 *
 * @author BEN_S
 */
public class Waypoint {
    
    public Location loc;
    public String name;
    
    public Waypoint(Location loc, String name){
        this.loc = loc;
        this.name = name;
    }
}
