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
public class Region {
    
    public Location loc1;
    public Location loc2;
    public String name;

    public Region(Location loc1, Location loc2, String name) {
        this.loc1 = loc1;
        this.loc2 = loc2;
        this.name = name;
    }
    
    
}
