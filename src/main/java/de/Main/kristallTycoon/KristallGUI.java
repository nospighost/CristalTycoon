package de.Main.kristallTycoon;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class KristallGUI implements Listener, CommandExecutor {

    public static Inventory BuyKristall;
    private static final int KRISTALL_PREIS = 25000;
    private static final Economy eco = KristallTycoon.eco;

    public static void createGUIs() {
        BuyKristall = Bukkit.createInventory(null, 9, "§dKristalle Kaufen");

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta); //aa
        }
        for (int i = 0; i < 9; i++) {
            BuyKristall.setItem(i, filler);
        }

        int[] slots = {2, 4, 6};
        for (int i = 1; i <= 3; i++) {
            ItemStack kristall = new ItemStack(Material.AMETHYST_CLUSTER, i);
            ItemMeta meta = kristall.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§aKaufe " + i + " §dKristall" + (i > 1 ? "e" : ""));
                meta.addEnchant(Enchantment.LOYALTY, 5, true);
                List<String> lore = new ArrayList<>();
                lore.add(" ");
                lore.add("§bSetze den §dKristall §bauf deine Insel, dass er wächst!");
                lore.add("§bDu kannst durch das Abbauen des Kristalls §aGeld verdienen!");
                lore.add("§bKaufe durch das §aGeld Upgrades §bfür dein Kristall!");
                lore.add(" ");
                lore.add("§eKosten: §f" + (KRISTALL_PREIS * i) + "$");
                meta.setLore(lore);
                kristall.setItemMeta(meta);
            }
            BuyKristall.setItem(slots[i - 1], kristall);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equalsIgnoreCase("§dKristalle Kaufen")) return;
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.AMETHYST_CLUSTER) return;

        int anzahl = clicked.getAmount();
        double kosten = KRISTALL_PREIS * anzahl;

        if (eco.getBalance(player) < kosten) {
            player.sendMessage("§cDu hast nicht genug Geld. Du brauchst §f" + kosten + "§c$.");
            return;
        }

        eco.withdrawPlayer(player, kosten);

        ItemStack kristall = new ItemStack(Material.AMETHYST_CLUSTER, anzahl);
        ItemMeta meta = kristall.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§dWachsender Kristall");
            meta.addEnchant(Enchantment.LOYALTY, 5, true);
            kristall.setItemMeta(meta);
        }

        player.getInventory().addItem(kristall);
        player.sendMessage("§aDu hast erfolgreich §f" + anzahl + " §dKristall" + (anzahl > 1 ? "e" : "") + " §agekauft!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl verwenden.");
            return true;
        }

        Player player = (Player) sender;
        KristallGUI.createGUIs();
        player.openInventory(KristallGUI.BuyKristall);
        return true;
    }
}
