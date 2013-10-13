/* 
 * Copyright (C) 2013 drtshock
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.drtshock.obsidiandestroyer;

import com.drtshock.obsidiandestroyer.Metrics.Graph;
import com.drtshock.obsidiandestroyer.Updater.UpdateResult;
import com.drtshock.obsidiandestroyer.Updater.UpdateType;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import java.io.File;

/**
 * The ObsidianDestroyer plugin.
 *
 * Allows certain explosions to destroy Obsidian and other blocks.
 *
 * @author drtshock, squidicuz
 *
 */
public final class ObsidianDestroyer extends JavaPlugin {

    private final ODCommands cmdExecutor = new ODCommands(this);
    private ODConfig config = new ODConfig(this);
    private final ODEntityListener entityListener = new ODEntityListener(this);
    private final ODPlayerListener playerListener = new ODPlayerListener(this);
    public static Logger LOG;
    private PluginManager PM;
    public static boolean UPDATE = false;
    public static String NAME = "";
    private static boolean IS_FACTIONS_HOOKED = false;
    private static boolean IS_TOWNY_HOOKED = false;
    private static boolean IS_WORLDGUARD_HOOKED = false;

    @Override
    public void onDisable() {
        config.saveDurabilityToFile();
        saveConfig();
    }

    @Override
    public void onEnable() {
        PM = getServer().getPluginManager();
        LOG = getLogger();
        getCommand("obsidiandestroyer").setExecutor(cmdExecutor);

        config.loadConfig();
        entityListener.setObsidianDurability(config.loadDurabilityFromFile());
        checkFactionsHook();
        checkTownyHook();
        checkWorldGuardGHook();
        startMetrics();

        PM.registerEvents(entityListener, this);
        PM.registerEvents(playerListener, this);

        checkUpdate();
    }

    // PROTECTED
    protected void checkUpdate() {
        if (config.getCheckUpdate()) {
            final ObsidianDestroyer plugin = this;
            final File file = this.getFile();
            final Updater.UpdateType updateType = (config.getDownloadUpdate() ? UpdateType.DEFAULT : UpdateType.NO_DOWNLOAD);
            getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
                @Override
                public void run() {
                    Updater updater = new Updater(plugin, 43718, file, updateType, false);
                    ObsidianDestroyer.UPDATE = updater.getResult() == Updater.UpdateResult.UPDATE_AVAILABLE;
                    ObsidianDestroyer.NAME = updater.getLatestName();
                    if (updater.getResult() == UpdateResult.SUCCESS) {
                        getLogger().log(Level.INFO, "Successfully updated ObsidianDestroyer to version {0} for next restart!", updater.getLatestName());
                    } else if (updater.getResult() == UpdateResult.NO_UPDATE) {
                        getLogger().log(Level.INFO, "We didn't find an update!");
                    }
                }
            });
        }
    }

    private void startMetrics() {
        try {
            Metrics metrics = new Metrics(this);

            Graph graph = metrics.createGraph("Durability");
            graph.addPlotter(new Metrics.Plotter() {
                @Override
                public String getColumnName() {
                    return getConfig().getString("Durability.Obsidian");
                }

                @Override
                public int getValue() {
                    return 1;
                }
            });
        } catch (IOException ex) {
            getLogger().warning("Failed to load metrics :(");
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

    /* ====================================================
     * Hooks to other plugins
     * ==================================================== */
    /**
     * Checks to see if the Factions plugin is active.
     */
    private void checkFactionsHook() {
        Plugin plug = PM.getPlugin("Factions");

        if (plug != null) {
            String[] ver = plug.getDescription().getVersion().split("\\.");
            String version = ver[0] + "." + ver[1];
            if (version.equalsIgnoreCase("1.8")) {
                LOG.info("Factions 1.8.x Found! Enabling hook..");
                IS_FACTIONS_HOOKED = true;
            } else if (version.equalsIgnoreCase("1.6")) {
                LOG.info("Factions found, but v1.6.x is not supported!");
            }
        }
    }

    /**
     * Gets the state of the Factions hook.
     *
     * @return Factions hook state
     */
    public static boolean isHookedFactions() {
        return IS_FACTIONS_HOOKED;
    }

    /**
     * Checks to see if the Towny plugin is active.
     */
    private void checkTownyHook() {
        Plugin plug = PM.getPlugin("Towny");

        if (plug != null) {
            LOG.info("Towny Found! Enabling hook..");
            IS_TOWNY_HOOKED = true;
        }
    }

    /**
     * Gets the state of the Towny hook.
     *
     * @return Towny hook state
     */
    public static boolean isHookedTowny() {
        return IS_TOWNY_HOOKED;
    }

    /**
     * Checks to see if the WorldGuard plugin is active.
     */
    private void checkWorldGuardGHook() {
        Plugin plug = PM.getPlugin("WorldGuard");

        if (plug != null) {
            LOG.info("WorldGuard Found! Enabling hook..");
            IS_WORLDGUARD_HOOKED = true;
        }
    }

    /**
     * Gets the state of the WorldGuard hook.
     *
     * @return WorldGuard hook state
     */
    public static boolean isHookedWorldGuard() {
        return IS_WORLDGUARD_HOOKED;
    }

    /**
     * Gets the WorldGuard plugin
     *
     * @return WorldGuardPlugin
     * @throws Exception
     */
    public WorldGuardPlugin getWorldGuard() throws Exception {
        Plugin plugin = PM.getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            throw new Exception("WorldGuard could not be reached!");
        }

        return (WorldGuardPlugin) plugin;
    }
}
