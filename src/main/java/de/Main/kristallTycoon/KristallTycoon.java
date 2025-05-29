package de.Main.kristallTycoon;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class KristallTycoon extends JavaPlugin {

    public static net.milkbowl.vault.economy.Economy eco;

    private File growthFile;
    private FileConfiguration growthConfig;

    @Override
    public void onEnable() {
        setupEconomy();
        setupGrowthFile();

        //              <---------------- Listener----------------------->

        getServer().getPluginManager().registerEvents(new PlayerListener(this, growthConfig, growthFile), this);


        //              <---------------- Commands ----------------------->

        getServer().getPluginManager().registerEvents(new KristallGUI(), this);
        this.getCommand("kristallshop").setExecutor(new KristallGUI());

        PlayerListener.startGrowthTasks(this, growthConfig);

        getLogger().info("KristallTycoon aktiviert.");
    }

    private void setupGrowthFile() {
        growthFile = new File(getDataFolder(), "growth/growth.yml");

        if (!growthFile.getParentFile().exists()) {
            growthFile.getParentFile().mkdirs();
        }
//a
        if (!growthFile.exists()) {
            try {
                growthFile.createNewFile();
                getLogger().info("growth.yml erstellt.");
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Konnte growth.yml nicht erstellen.", e);
            }
        }

        growthConfig = YamlConfiguration.loadConfiguration(growthFile);
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault nicht gefunden. Plugin deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        var rsp = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp == null) {
            getLogger().severe("Economy-Service nicht gefunden. Plugin deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        eco = rsp.getProvider();
    }

    @Override
    public void onDisable() {
        getLogger().info("KristallTycoon deaktiviert.");
    }
}
