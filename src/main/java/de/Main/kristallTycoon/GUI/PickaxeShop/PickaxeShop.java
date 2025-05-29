package de.Main.kristallTycoon.GUI.PickaxeShop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PickaxeShop implements CommandExecutor, Listener {

    private static final Material[] PICKAXES = {
            Material.STONE_PICKAXE,
            Material.IRON_PICKAXE,
            Material.DIAMOND_PICKAXE,
            Material.GOLDEN_PICKAXE,
            Material.NETHERITE_PICKAXE
    };

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        openMainShop(player);
        return true;
    }

    public void openMainShop(Player player) {
        Inventory mainShop = Bukkit.createInventory(null, 9, "§cPickaxe Shop");

        for (int i = 0; i < PICKAXES.length; i++) {
            mainShop.setItem(i, createItem(PICKAXES[i]));
        }


        player.openInventory(mainShop);
    }

    private ItemStack createItem(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        String displayName = switch (mat) {
            case STONE_PICKAXE -> "§6Stein Spitzhacke";
            case IRON_PICKAXE -> "§7Eisen Spitzhacke";
            case DIAMOND_PICKAXE -> "§bDiamant Spitzhacke";
            case GOLDEN_PICKAXE -> "§eGoldene Spitzhacke";
            case NETHERITE_PICKAXE -> "§8Netherite Spitzhacke";
            default -> mat.name();
        };

        meta.setDisplayName(displayName);
        List<String> lore = new ArrayList<>();
        lore.add("§7Klicke für Varianten von " + displayName);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals("§cPickaxe Shop")) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;

        Player p = (Player) e.getWhoClicked();
        Material clicked = e.getCurrentItem().getType();

        for (Material mat : PICKAXES) {
            if (clicked == mat) {
                EnchantGUI.openEnchantGUI(p, mat);
                break;
            }
        }
    }
}
