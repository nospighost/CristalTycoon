package de.Main.kristallTycoon.PlayerListenr;

import de.Main.kristallTycoon.KristallTycoon;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.bukkit.Material.*;
import static org.bukkit.enchantments.Enchantment.EFFICIENCY;
import static org.bukkit.enchantments.Enchantment.LOYALTY;

public class PlayerListener implements Listener {

    private final JavaPlugin plugin;
    private static  FileConfiguration growthConfig;
    private static File growthFile;

    // Hier speichern wir die Block-Locations für Spieler, die gerade das Upgrade-Menü offen haben
    private final Map<UUID, Location> upgradeOpenLocations = new HashMap<>();

    public PlayerListener(JavaPlugin plugin, FileConfiguration growthConfig, File growthFile) {
        this.plugin = plugin;
        this.growthConfig = growthConfig;
        PlayerListener.growthFile = growthFile;
    }

    // ----------------------------
    // Block Break: Kristall abbauen = Geld
    // ----------------------------
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material type = block.getType();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Location location = block.getLocation();

        if (type == SMALL_AMETHYST_BUD || type == MEDIUM_AMETHYST_BUD || type == LARGE_AMETHYST_BUD) {
            event.setCancelled(true);
            return;
        }

        if (type == AMETHYST_CLUSTER) {
            int eff = (tool != null && tool.hasItemMeta()) ? tool.getEnchantmentLevel(EFFICIENCY) : 0;
            if (eff < 1) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);
            block.setType(SMALL_AMETHYST_BUD);

            String path = getPath(location);
            int upgradeLevel = growthConfig.getInt(path + ".Level", 0);
            double payout = 1 + upgradeLevel;
            KristallTycoon.eco.depositPlayer(player, payout);
            player.sendMessage("§a+§e" + payout + "§a$ verdient!");

            long now = System.currentTimeMillis();
            saveGrowth(location, SMALL_AMETHYST_BUD.name(), now + getGrowthDelayMillis(SMALL_AMETHYST_BUD), player.getUniqueId());
            growToFinalStage(block, SMALL_AMETHYST_BUD, player.getUniqueId());

            // Location.getWorld().dropItemNaturally(location, new ItemStack(AIR)); // Das droppt nix, also raus damit
        }
    }

    // ----------------------------
    // Block Place: Kristall anpflanzen
    // ----------------------------
    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        ItemStack handItem = player.getInventory().getItemInMainHand();

        if (block.getType() == AMETHYST_CLUSTER) {
            int loyalty = (handItem != null && handItem.hasItemMeta()) ? handItem.getEnchantmentLevel(LOYALTY) : 0;
            if (loyalty < 5) {
                event.setCancelled(true);
                return;
            }

            Location loc = block.getLocation();
            long now = System.currentTimeMillis();
            saveGrowth(loc, SMALL_AMETHYST_BUD.name(), now + getGrowthDelayMillis(SMALL_AMETHYST_BUD), player.getUniqueId());
            block.setType(SMALL_AMETHYST_BUD);
            growToFinalStage(block, SMALL_AMETHYST_BUD, player.getUniqueId());

            player.sendMessage("§aKristall erfolgreich gepflanzt. Wachstum gestartet.");
        }
    }

    // ----------------------------
    // Wachstum durch Phasen
    // ----------------------------
    private void growToFinalStage(Block block, Material current, UUID owner) {
        while (current != AMETHYST_CLUSTER) {
            Material next = getNextStage(current);
            if (next == null) break;
            scheduleGrowthTask(block, current, next, getGrowthTimeSeconds(current), owner);
            current = next;
        }
    }

    private static Material getNextStage(Material mat) {
        switch (mat) {
            case SMALL_AMETHYST_BUD: return MEDIUM_AMETHYST_BUD;
            case MEDIUM_AMETHYST_BUD: return LARGE_AMETHYST_BUD;
            case LARGE_AMETHYST_BUD: return AMETHYST_CLUSTER;
            default: return null;
        }
    }

    private void scheduleGrowthTask(Block block, Material from, Material to, int delay, UUID owner) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (block.getType() == from) {
                    block.setType(to);
                    Location loc = block.getLocation();
                    long next = (to == AMETHYST_CLUSTER) ? 0 : System.currentTimeMillis() + getGrowthDelayMillis(to);
                    saveGrowth(loc, to.name(), next, owner);
                }
            }
        }.runTaskLater(plugin, delay * 20L);
    }

    private static void saveGrowth(Location loc, String stage, long nextGrowthMillis, UUID owner) {
        String path = getPath(loc);
        growthConfig.set(path + ".stage", stage);
        growthConfig.set(path + ".nextGrowth", nextGrowthMillis);
        growthConfig.set(path + ".owner", owner != null ? owner.toString() : null);
        growthConfig.set(path + ".isFullyGrown", stage.equals(AMETHYST_CLUSTER.name()));

        if (!growthConfig.contains(path + ".Level")) {
            growthConfig.set(path + ".Level", 0);
        }

        try {
            growthConfig.save(growthFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getPath(Location loc) {
        // Hier anpassen auf x_y_z Format für deine Config
        return "growth." + loc.getWorld().getName() + "." + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    private static int getGrowthTimeSeconds(Material stage) {
        switch (stage) {
            case SMALL_AMETHYST_BUD: return 3;
            case MEDIUM_AMETHYST_BUD: return 5;
            case LARGE_AMETHYST_BUD: return 7;
            default: return 0;
        }
    }

    private static long getGrowthDelayMillis(Material stage) {
        return getGrowthTimeSeconds(stage) * 1000L;
    }

    // ----------------------------
    // Upgrade-Menü öffnen
    // ----------------------------
    public void openUpgradeGUI(Player player, Location blockLocation) {
        Inventory gui = Bukkit.createInventory(null, 9, "§bUpgrade-Menü");

        String path = getPath(blockLocation);
        int level = growthConfig.getInt(path + ".Level", 0);
        double price = 1000 * Math.pow(2, level + 1);
        int nextPayout = 1 + level + 1;

        ItemStack upgrade = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = upgrade.getItemMeta();

        meta.setDisplayName("§aKristall-Upgrade");
        List<String> lore = new ArrayList<>();
        lore.add("§eAktuelles Level: §b" + level);
        lore.add("§eNach dem Upgrade: §b" + (level + 1));
        lore.add("§eKosten: §6" + price + "$");
        lore.add("§aVerdienst danach: §e" + nextPayout + "$");
        meta.setLore(lore);
        upgrade.setItemMeta(meta);

        gui.setItem(4, upgrade);

        // Location speichern, damit wir im InventoryClickEvent wissen, welcher Block upgegradet wird
        upgradeOpenLocations.put(player.getUniqueId(), blockLocation);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) throws IOException {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTitle().equalsIgnoreCase("§bUpgrade-Menü")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.TOTEM_OF_UNDYING) return;

            Location blockLoc = upgradeOpenLocations.get(player.getUniqueId());
            if (blockLoc == null) {
                player.sendMessage("§cFehler: Block-Location nicht gefunden.");
                player.closeInventory();
                return;
            }

            String path = getPath(blockLoc);
            int level = growthConfig.getInt(path + ".Level", 0);
            int newLevel = level + 1;
            double price = 1000 * Math.pow(2, newLevel);

            if (KristallTycoon.eco.getBalance(player) >= price) {
                KristallTycoon.eco.withdrawPlayer(player, price);
                growthConfig.set(path + ".Level", newLevel);
                growthConfig.save(growthFile);
                player.sendMessage("§aUpgrade erfolgreich! Neuer Verdienst: §e" + (1 + newLevel) + "§a$ pro Kristall.");
            } else {
                player.sendMessage("§cNicht genug Geld! Du brauchst §e" + price + "$");
            }

            upgradeOpenLocations.remove(player.getUniqueId());
            player.closeInventory();
        }
    }

    // ----------------------------
    // Rechtsklick mit Sneaken = Menü öffnen
    // ----------------------------
    @EventHandler(ignoreCancelled = true)
    public void onShiftRightClick(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        Block block = event.getClickedBlock();
        if (block == null || (
                block.getType() != AMETHYST_CLUSTER &&
                        block.getType() != SMALL_AMETHYST_BUD &&
                        block.getType() != MEDIUM_AMETHYST_BUD &&
                        block.getType() != LARGE_AMETHYST_BUD)) {
            return;
        }


        openUpgradeGUI(player, block.getLocation());
    }

    // ----------------------------
    // Nach Server-Start geplante Wachstumsvorgänge starten
    // ----------------------------
    public static void startGrowthTasks(JavaPlugin plugin, FileConfiguration growthConfig) {
        if (!growthConfig.isConfigurationSection("growth")) return;

        for (String world : growthConfig.getConfigurationSection("growth").getKeys(false)) {
            if (!growthConfig.isConfigurationSection("growth." + world)) continue;

            for (String coordKey : growthConfig.getConfigurationSection("growth." + world).getKeys(false)) {
                String path = "growth." + world + "." + coordKey;

                try {
                    String[] parts = coordKey.split("_");
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);

                    World w = Bukkit.getWorld(world);
                    if (w == null) continue;

                    Block block = w.getBlockAt(x, y, z);
                    if (block.getType() == SMALL_AMETHYST_BUD || block.getType() == MEDIUM_AMETHYST_BUD || block.getType() == LARGE_AMETHYST_BUD) {
                        long nextGrowth = growthConfig.getLong(path + ".nextGrowth", 0);
                        if (nextGrowth == 0) continue;

                        long delay = nextGrowth - System.currentTimeMillis();
                        if (delay <= 0) delay = 0;

                        Material current = Material.valueOf(growthConfig.getString(path + ".stage"));

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (block.getType() == current) {
                                    Material next = getNextStage(current);
                                    if (next != null) {
                                        block.setType(next);
                                        saveGrowth(block.getLocation(), next.name(), System.currentTimeMillis() + getGrowthDelayMillis(next), null);
                                    }
                                }
                            }
                        }.runTaskLater(plugin, delay / 50);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
