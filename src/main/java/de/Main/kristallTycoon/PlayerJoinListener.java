package de.Main.kristallTycoon;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerJoinListener implements Listener {

    private final String WORLD_NAME;

    public PlayerJoinListener(String worldName) {
        this.WORLD_NAME = worldName;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        World tycoonWorld = Bukkit.getWorld(WORLD_NAME);
        if (tycoonWorld == null) {
            player.sendMessage("Die Tycoon-Welt ist derzeit nicht verfügbar.");
            return;
        }

        if (!player.getWorld().getName().equals(WORLD_NAME)) {
            Location spawnLocation = new Location(tycoonWorld, 0.5, 101, 0.5);
            player.teleport(spawnLocation);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        World tycoonWorld = Bukkit.getWorld(WORLD_NAME);
        if (tycoonWorld == null) {
            player.sendMessage("Die Tycoon-Welt ist derzeit nicht verfügbar.");
            return;
        }

        Location respawnLocation = new Location(tycoonWorld, 0.5, 101, 0.5);
        event.setRespawnLocation(respawnLocation);
    }
}
