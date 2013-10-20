package io.snw.obsidiandestroyer;

import io.snw.obsidiandestroyer.commands.ODCommand;
import io.snw.obsidiandestroyer.listeners.BlockListener;
import io.snw.obsidiandestroyer.listeners.PlayerListener;
import io.snw.obsidiandestroyer.util.Metrics;
import io.snw.obsidiandestroyer.util.Updater;
import io.snw.obsidiandestroyer.listeners.EntityExplodeListener;
import io.snw.obsidiandestroyer.managers.ChunkManager;
import io.snw.obsidiandestroyer.managers.ConfigManager;
import io.snw.obsidiandestroyer.managers.HookManager;
import io.snw.obsidiandestroyer.managers.MaterialManager;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ObsidianDestroyer extends JavaPlugin {

    public ObsidianDestroyer plugin;
    public static Logger LOG;
    private static ObsidianDestroyer instance;
    private boolean update = false;
    private String name = "";

    @Override
    public void onEnable() {
        this.plugin = this;
        instance = this;
        LOG = getLogger();
        new ConfigManager();
        new HookManager();
        new MaterialManager();
        new ChunkManager();
        getCommand("od").setExecutor(new ODCommand(this));
        getServer().getPluginManager().registerEvents(new EntityExplodeListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        checkUpdate();
        //startMetrics(); //FIXME: crashes
    }

    @Override
    public void onDisable() {
        ChunkManager.getInstance().save();
    }

    protected void checkUpdate() {
        if (getConfig().getBoolean("checkupdate")) {
            final ObsidianDestroyer plugin = this;
            final File file = this.getFile();
            final Updater.UpdateType updateType = (getConfig().getBoolean("downloadupdate") ? Updater.UpdateType.DEFAULT : Updater.UpdateType.NO_DOWNLOAD);
            getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
                @Override
                public void run() {
                    Updater updater = new Updater(plugin, 43718, file, updateType, false);
                    update = updater.getResult() == Updater.UpdateResult.UPDATE_AVAILABLE;
                    name = updater.getLatestName();
                    if (updater.getResult() == Updater.UpdateResult.SUCCESS) {
                        getLogger().log(Level.INFO, "Successfully updated ObsidianDestroyer to version {0} for next restart!", updater.getLatestName());
                    } else if (updater.getResult() == Updater.UpdateResult.NO_UPDATE) {
                        getLogger().log(Level.INFO, "We didn't find an update!");
                    }
                }
            });
        }
    }

    private void startMetrics() {
        try {
            Metrics metrics = new Metrics(this);

            Metrics.Graph graph = metrics.createGraph("Durability");
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

    public String getLatestVersion() {
        return name;
    }

    public boolean getNeedsUpdate() {
        return update;
    }

    public static ObsidianDestroyer getInstance() {
        return instance;
    }
}
