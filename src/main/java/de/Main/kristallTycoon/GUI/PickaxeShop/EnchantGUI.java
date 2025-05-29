package de.Main.kristallTycoon.GUI.PickaxeShop;

import de.Main.kristallTycoon.KristallTycoon;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnchantGUI implements Listener {

    // Preise für jedes Material und Effizienz-Level manuell setzen (kannst du anpassen!)
    private static final Map<Material, Integer[]> priceMap = new HashMap<>();

    static {
        priceMap.put(Material.STONE_PICKAXE, new Integer[]{100,200,300,400,500,600,700,800,900,1000});
        priceMap.put(Material.IRON_PICKAXE, new Integer[]{200,400,600,800,1000,1200,1400,1600,1800,2000});
        priceMap.put(Material.DIAMOND_PICKAXE, new Integer[]{500,1000,1500,2000,2500,3000,3500,4000,4500,5000});
        priceMap.put(Material.GOLDEN_PICKAXE, new Integer[]{150,300,450,600,750,900,1050,1200,1350,1500});
        priceMap.put(Material.NETHERITE_PICKAXE, new Integer[]{1000,2000,3000,4000,5000,6000,7000,8000,9000,10000});
    }

    public static void openEnchantGUI(Player player, Material pickaxeType) {
        Inventory gui = Bukkit.createInventory(null, 27, "§eWähle deine Pickaxe");

        Integer[] prices = priceMap.get(pickaxeType);
        if (prices == null) {
            player.sendMessage("§cPreise für dieses Material sind nicht gesetzt!");
            return;
        }

        for (int i = 0; i < 10; i++) {
            int effiLevel = i + 1;

            ItemStack pickaxe = new ItemStack(pickaxeType);
            pickaxe.addUnsafeEnchantment(Enchantment.EFFICIENCY, effiLevel); // Effizienz
            pickaxe.addUnsafeEnchantment(Enchantment.UNBREAKING, effiLevel); // Haltbarkeit

            ItemMeta meta = pickaxe.getItemMeta();
            meta.setDisplayName("§a" + pickaxeType.name() + " §7(Effi " + effiLevel + ", Haltb. " + effiLevel + ")");
            List<String> lore = new ArrayList<>();
            int price = prices[i];
            lore.add("§7Kosten: §6" + price + " Kristalle");
            lore.add("§eKlicke zum Kaufen!");
            meta.setLore(lore);
            pickaxe.setItemMeta(meta);

            gui.setItem(i, pickaxe);
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onBuy(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals("§eWähle deine Pickaxe")) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;

        Player p = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();

        int effiLevel = clicked.getEnchantmentLevel(Enchantment.EFFICIENCY);

        Integer[] prices = priceMap.get(clicked.getType());
        if (prices == null) {
            p.sendMessage("§cPreise für dieses Material sind nicht gesetzt!");
            p.closeInventory();
            return;
        }

        int price = prices[effiLevel - 1];

        Economy eco = KristallTycoon.eco;
        if (eco.getBalance(p) >= price) {
            eco.withdrawPlayer(p, price);
            p.getInventory().addItem(clicked.clone());
            p.sendMessage("§aGekauft für §6" + price + " Kristalle§a!");
        } else {
            p.sendMessage("§cNicht genug Kristalle!");
        }
        p.closeInventory();
    }
}
