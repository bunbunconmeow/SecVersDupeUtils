package org.secverse.SecVerseDupeUtils.cummon;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.inventory.ItemStack;

public class ChestBoatStorage {
    public Location location;
    public ItemStack[] contents;
    public Boat.Type boatType;

    public ChestBoatStorage(Location location, ItemStack[] contents, Boat.Type boatType) {
        this.location = location;
        this.contents = contents;
        this.boatType = boatType;
    }
}
