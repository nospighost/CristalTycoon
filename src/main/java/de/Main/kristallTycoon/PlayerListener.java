package de.Main.kristallTycoon;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import static org.bukkit.Material.*;
import static org.bukkit.enchantments.Enchantment.EFFICIENCY;
import static org.bukkit.enchantments.Enchantment.LOYALTY;

public class PlayerListener implements Listener {

    private final HashMap<UUID, Integer> upgradeLevels = new HashMap<>();
    public double price = 1000;

    private final JavaPlugin plugin;
    private final FileConfiguration growthConfig;
    private final File growthFile;

    public PlayerListener(JavaPlugin plugin, FileConfiguration growthConfig, File growthFile) {
        this.plugin = plugin;
        this.growthConfig = growthConfig;
        this.growthFile = growthFile;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material type = block.getType();
        ItemStack handItem = player.getInventory().getItemInMainHand();
        Location location = block.getLocation();

        if (type == SMALL_AMETHYST_BUD || type == MEDIUM_AMETHYST_BUD || type == LARGE_AMETHYST_BUD) {
            event.setCancelled(true);
            return;
        }

        if (type == AMETHYST_CLUSTER) {
            int effLevel = (handItem != null && handItem.hasItemMeta()) ? handItem.getEnchantmentLevel(EFFICIENCY) : 0;

            if (effLevel < 1) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);
            block.setType(SMALL_AMETHYST_BUD);

            int upgradeLevel = upgradeLevels.getOrDefault(player.getUniqueId(), 1);
            double payout = 2 + upgradeLevel;
            KristallTycoon.eco.depositPlayer(player, payout);

            long now = System.currentTimeMillis();
            saveGrowth(location, SMALL_AMETHYST_BUD.name(), now + getGrowthDelayMillis(SMALL_AMETHYST_BUD), player.getUniqueId());
            growToFinalStage(block, SMALL_AMETHYST_BUD, player.getUniqueId());

            location.getWorld().dropItemNaturally(location, new ItemStack(STONE));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        ItemStack handItem = player.getInventory().getItemInMainHand();

        if (block.getType() == AMETHYST_CLUSTER) {
            int loyaltyLevel = (handItem != null && handItem.hasItemMeta()) ? handItem.getEnchantmentLevel(LOYALTY) : 0;

            if (loyaltyLevel < 5) {
                event.setCancelled(true);
                return;
            }

            Location loc = block.getLocation();
            long now = System.currentTimeMillis();
            saveGrowth(loc, SMALL_AMETHYST_BUD.name(), now + getGrowthDelayMillis(SMALL_AMETHYST_BUD), player.getUniqueId());
            block.setType(SMALL_AMETHYST_BUD);
            growToFinalStage(block, SMALL_AMETHYST_BUD, player.getUniqueId());

            player.sendMessage("§aKristall erfolgreich platziert. Wachstum gestartet.");
        }
    }

    private void growToFinalStage(Block block, Material start, UUID owner) {
        Material current = start;
        while (current != AMETHYST_CLUSTER) {
            Material next = getNextStage(current);
            if (next == null) break;
            scheduleGrowthTask(block, current, next, getGrowthTimeSeconds(current), owner);
            current = next;
        }
    }

    private Material getNextStage(Material current) {
        switch (current) {
            case SMALL_AMETHYST_BUD: return MEDIUM_AMETHYST_BUD;
            case MEDIUM_AMETHYST_BUD: return LARGE_AMETHYST_BUD;
            case LARGE_AMETHYST_BUD: return AMETHYST_CLUSTER;
            default: return null;
        }
    }

    private void scheduleGrowthTask(Block block, Material from, Material to, int delaySeconds, UUID owner) {
        if (delaySeconds <= 0) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (block.getType() == from) {
                    block.setType(to);
                    Location loc = block.getLocation();

                    boolean isFullyGrown = (to == AMETHYST_CLUSTER);
                    long nextGrowth = isFullyGrown ? 0 : System.currentTimeMillis() + getGrowthDelayMillis(to);
                    saveGrowth(loc, to.name(), nextGrowth, owner);
                }
            }
        }.runTaskLater(plugin, delaySeconds * 20L);
    }

    private void saveGrowth(Location loc, String stage, long nextGrowthMillis, UUID owner) {
        String path = getPath(loc);
        growthConfig.set(path + ".stage", stage);
        growthConfig.set(path + ".nextGrowth", nextGrowthMillis);
        growthConfig.set(path + ".owner", owner != null ? owner.toString() : null);
        growthConfig.set(path + ".world", loc.getWorld().getName());
        growthConfig.set(path + ".x", loc.getBlockX());
        growthConfig.set(path + ".y", loc.getBlockY());
        growthConfig.set(path + ".z", loc.getBlockZ());
        growthConfig.set(path + ".isFullyGrown", stage.equals(AMETHYST_CLUSTER.name()));

        try {
            growthConfig.save(growthFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getPath(Location loc) {
        return "growth." + loc.getWorld().getName() + "." + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    public static void startGrowthTasks(JavaPlugin plugin, FileConfiguration growthConfig) {
        if (growthConfig == null || !growthConfig.isConfigurationSection("growth")) return;

        for (String world : growthConfig.getConfigurationSection("growth").getKeys(false)) {
            for (String key : growthConfig.getConfigurationSection("growth." + world).getKeys(false)) {
                String path = "growth." + world + "." + key;
                String[] coords = key.split("_");
                if (coords.length != 3) continue;

                int x, y, z;
                try {
                    x = Integer.parseInt(coords[0]);
                    y = Integer.parseInt(coords[1]);
                    z = Integer.parseInt(coords[2]);
                } catch (NumberFormatException e) {
                    continue;
                }

                Location loc = new Location(Bukkit.getWorld(world), x, y, z);
                Block block = loc.getBlock();

                Material from;
                try {
                    from = Material.valueOf(growthConfig.getString(path + ".stage", ""));
                } catch (IllegalArgumentException e) {
                    continue;
                    //aaa
                }

                Material to = new PlayerListener(plugin, growthConfig, null).getNextStage(from);
                if (to == null) continue;

                long delayMillis = growthConfig.getLong(path + ".nextGrowth", 0) - System.currentTimeMillis();
                delayMillis = Math.max(0, delayMillis);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (block.getType() == from) {
                            block.setType(to);
                            if (to == AMETHYST_CLUSTER) {
                                growthConfig.set(path + ".isFullyGrown", true);
                                growthConfig.set(path + ".stage", to.name());
                                growthConfig.set(path + ".nextGrowth", 0);
                            } else {
                                growthConfig.set(path + ".stage", to.name());
                                growthConfig.set(path + ".nextGrowth", System.currentTimeMillis() + getGrowthDelayMillis(to));
                                growthConfig.set(path + ".isFullyGrown", false);
                            }

                            try {
                                growthConfig.save(new File(plugin.getDataFolder(), "growth/growth.yml"));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }.runTaskLater(plugin, delayMillis / 50L);
            }
        }
    }

    private static int getGrowthTimeSeconds(Material stage) { //aa
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

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTitle().equalsIgnoreCase("§bUpgrade-Menü")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() != Material.TOTEM_OF_UNDYING) return;

            UUID uuid = player.getUniqueId();
            Location loc = player.getLocation();

            growthConfig.get("growth." + loc.getWorld() + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ());
            int currentLevel = upgradeLevels.getOrDefault(uuid, 0);
            price = 1000 * Math.pow(2, currentLevel);

            if (KristallTycoon.eco.getBalance(player) >= price) {
                KristallTycoon.eco.withdrawPlayer(player, price);
                upgradeLevels.put(uuid, currentLevel + 1);
                player.sendMessage("§aUpgrade erfolgreich! Du erhältst jetzt §e" + (currentLevel + 2) + "§a$ pro Kristall.");
            } else {
                player.sendMessage("§cDu hast nicht genug Geld. Preis: §e" + price + "$");
            }

            player.closeInventory();
        }
    }

    public void openUpgradeGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, "§bUpgrade-Menü");

        ItemStack upgrade = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = upgrade.getItemMeta();
        meta.setDisplayName("§aGeld Upgrade");
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§cZahle " + price);
        lore.add("§bErhalte durch dieses Upgrade mehr Dollar pro Abbau!");
        meta.setLore(lore);
        upgrade.setItemMeta(meta);

        gui.setItem(4, upgrade);

        player.openInventory(gui);
    }

    @EventHandler(ignoreCancelled = true)
    public void onShiftRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != AMETHYST_CLUSTER) return;

        openUpgradeGUI(player);
    }
}
