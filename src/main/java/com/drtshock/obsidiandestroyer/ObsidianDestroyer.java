package com.drtshock.obsidiandestroyer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.drtshock.obsidiandestroyer.Metrics.Graph;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

/**
 * The ObsidianDestroyer plugin.
 * 
 * Allows certain explosions to destroy Obsidian.
 * 
 * @author drtshock
 * 
 */
public final class ObsidianDestroyer extends JavaPlugin {

    private final ODCommands cmdExecutor = new ODCommands(this);
    private ODConfig config = new ODConfig(this);
    private final ODEntityListener entityListener = new ODEntityListener(this);
    private final ODJoinListener joinListener = new ODJoinListener(this);
    public static Logger LOG;
    private static PluginManager pm;

    public static boolean UPDATE = false;
    public static String NAME = "";
    
    private static boolean hookedFactions = false;
    private static boolean hookedTowny = false;
    private static boolean hookedWG = false;

    
    @Override
    public void onDisable() {
        config.saveDurabilityToFile();
        saveConfig();
    }

    
    @Override
    public void onEnable() {
        pm = getServer().getPluginManager();
        LOG = getLogger();
        getCommand("obsidiandestroyer").setExecutor(cmdExecutor);
        getCommand("od").setExecutor(cmdExecutor);

        config.loadConfig();
        entityListener.setObsidianDurability(config.loadDurabilityFromFile());
        checkFactionsHook();
        checkTownyHook();
        checkWorldGuardGHook();

        pm.registerEvents(entityListener, this);
        pm.registerEvents(joinListener, this);
        
        startMetrics();

        if(config.getCheckUpdate()) {
            Updater updater = new Updater(this, "obsidiandestroyer", this.getFile(), Updater.UpdateType.NO_DOWNLOAD, false);
            UPDATE = updater.getResult() == Updater.UpdateResult.UPDATE_AVAILABLE; 
            NAME = updater.getLatestVersionString();
        }
    }

    public void startMetrics() { 	
        try {	
            Metrics metrics = new Metrics(this);
            
            Graph graph = metrics.createGraph("Durability");
            
            graph.addPlotter(new Metrics.Plotter("Obsidian Durability Per Server") {

                @Override
                public String getColumnName() {
                    return String.valueOf(config.getoDurability());
                }
            	
                @Override
                public int getValue() {
                    return 1;
                }
            });

            metrics.start();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to submit the stats :-("); // Failed to submit the stats :-(
        }
    }

    /**
     * Returns the config of this plugin.
     * 
     * @return the config of this plugin
     */
    public ODConfig getODConfig() {
        return config;
    }

    /**
     * Returns the entity listener of this plugin.
     * 
     * @return the entity listener of this plugin
     */
    public ODEntityListener getListener() {
        return entityListener;
    }
    
    /**
     * Checks to see if the Factions plugin is active.
     */
    private void checkFactionsHook() {
        Plugin plug = pm.getPlugin("Factions");
		
        if (plug != null) {
            LOG.info("Factions Found! Enabling hook..");
            hookedFactions = true;
        }
    }
    
    /* ====================================================
     * Hooks to other plugins
     * ==================================================== */

    /**
     * Gets the state of the Factions hook.
     * 
     * @return Factions hook state
     */
    public static boolean isHookedFactions() {
        return hookedFactions;
    }
    
    /**
     * Checks to see if the Towny plugin is active.
     */
    private void checkTownyHook() {
        Plugin plug = pm.getPlugin("Towny");
		
        if (plug != null) {
            LOG.info("Towny Found! Enabling hook..");
            hookedTowny = true;
        }
    }
    
    /**
     * Gets the state of the Towny hook.
     * 
     * @return Towny hook state
     */
    public static boolean isHookedTowny() {
        return hookedTowny;
    }
    
    /**
     * Checks to see if the WorldGuard plugin is active.
     */
    private void checkWorldGuardGHook() {
        Plugin plug = pm.getPlugin("WorldGuard");
		
        if (plug != null) {
            LOG.info("WorldGuard Found! Enabling hook..");
            hookedWG = true;
        }
    }
    
    /**
     * Gets the state of the WorldGuard hook.
     * 
     * @return WorldGuard hook state
     */
    public static boolean isHookedWorldGuard() {
        return hookedWG;
    }
    
    /**
     * Gets the WorldGuard plugin
     * 
     * @return WorldGuardPlugin
     * @throws Exception 
     */
    public WorldGuardPlugin getWorldGuard() throws Exception {
        Plugin plugin = pm.getPlugin("WorldGuard");
     
        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            throw new Exception("WorldGuard could not be reached!");
        }
     
        return (WorldGuardPlugin) plugin;
    }
}
