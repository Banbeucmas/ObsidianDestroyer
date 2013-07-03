package com.drtshock.obsidiandestroyer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author drtshock
 */
public final class ODEntityListener implements Listener {

    private ObsidianDestroyer plugin;
    public ODConfig config;
    public HashMap<Integer, Integer> obsidianDurability = new HashMap<Integer, Integer>();
    private HashMap<Integer, Timer> obsidianTimer = new HashMap<Integer, Timer>();
    private boolean DisplayWarning = true;

    public ODEntityListener(ObsidianDestroyer plugin) {
        this.plugin = plugin;
        this.config = plugin.getODConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityExplode(EntityExplodeEvent event) {

        if (((event == null) || (event.isCancelled())) && (!this.config.getIgnoreCancel())) {
            return;
        }

        if (this.config.getDisabledWorlds().contains(event.getLocation().getWorld().getName())) {
            return;
        }

        int radius = this.config.getRadius();

        if (radius < 0) {
            plugin.getLogger().log(Level.WARNING, "Explosion radius is less than zero. Current value: {0}", radius);
            return;
        }

        Entity detonator = event.getEntity();

        if (detonator == null) {
            return;
        }

        Location detonatorLoc = detonator.getLocation();
        String eventTypeRep = event.getEntity().toString();

        if ((!eventTypeRep.equals("CraftTNTPrimed")) && (!eventTypeRep.equals("CraftCreeper"))
                && (!eventTypeRep.equals("CraftFireball")) && (!eventTypeRep.equals("CraftGhast"))
                && (!eventTypeRep.equals("CraftSnowball"))) {
            return;
        }

        if ((eventTypeRep.equals("CraftTNTPrimed")) && (!this.config.getTntEnabled())) {
            return;
        }

        if ((eventTypeRep.equals("CraftSnowball")) && (!this.config.getCannonsEnabled())) {
            return;
        }

        if ((eventTypeRep.equals("CraftCreeper")) && (!this.config.getCreepersEnabled())) {
            return;
        }

        if (((eventTypeRep.equals("CraftFireball")) || (eventTypeRep.equals("CraftGhast"))) && (!this.config.getGhastsEnabled())) {
            return;
        }

        if (eventTypeRep.equals("CraftSnowball")) {
            Iterator<Block> iter = event.blockList().iterator();
            while (iter.hasNext()) {
                Block block = (org.bukkit.block.Block) iter.next();
                blowBlockUp(block.getLocation());
            }
            return;
        }

        if (config.getExplodeInLiquids()) {
            ExplosionsInLiquid.Handle(event, this.plugin);
        }

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location targetLoc = new Location(detonator.getWorld(), detonatorLoc.getX() + x, detonatorLoc.getY() + y, detonatorLoc.getZ() + z);

                    if (detonatorLoc.distance(targetLoc) <= radius) {
                        if ((detonatorLoc.getBlock().isLiquid()) && (this.config.getWaterProtection())) {
                            return;
                        }
                        blowBlockUp(targetLoc);
                    }
                }
            }
        }
    }

    private void blowBlockUp(Location at) {

        if (at == null) {
            return;
        }

        Block b = at.getBlock();

        if (b.getTypeId() == 49) {
            applyDurability(at, this.config.getoDurability());
        }

        if (b.getTypeId() == 116) {
            applyDurability(at, this.config.geteDurability());
        }

        if (b.getTypeId() == 130) {
            applyDurability(at, this.config.getecDurability());
        }

        if (b.getTypeId() == 145) {
            applyDurability(at, this.config.getaDurability());
        }

        if (b.getTypeId() == 7 && this.config.getBedrockEnabled()) {
            applyDurability(at, this.config.getbDurability());
        }
    }

    private void applyDurability(Location at, int dura) {

        Integer representation = Integer.valueOf(at.getWorld().hashCode() + at.getBlockX() * 2389 + at.getBlockY() * 4027 + at.getBlockZ() * 2053);

        if ((this.config.getDurabilityEnabled()) && (dura > 1)) {
            if (this.obsidianDurability.containsKey(representation)) {
                int currentDurability = ((Integer) this.obsidianDurability.get(representation)).intValue();
                currentDurability++;

                if (checkIfMax(currentDurability, dura)) {
                    dropBlockAndResetTime(representation, at);
                } else {
                    this.obsidianDurability.put(representation, Integer.valueOf(currentDurability));

                    if (this.config.getDurabilityResetTimerEnabled()) {
                        startNewTimer(representation);
                    }
                }
            } else {
                this.obsidianDurability.put(representation, Integer.valueOf(1));

                if (this.config.getDurabilityResetTimerEnabled()) {
                    startNewTimer(representation);
                }

                if (checkIfMax(1, dura)) {
                    dropBlockAndResetTime(representation, at);
                }
            }
        } else {
            destroyBlockAndDropItem(at);
        }
    }

    private void destroyBlockAndDropItem(Location at) {
        if (at == null) {
            return;
        }

        Block b = at.getBlock();

        if ((!b.getType().equals(Material.OBSIDIAN)) && (!b.getType().equals(Material.ENCHANTMENT_TABLE)) && (!b.getType().equals(Material.ENDER_CHEST))
                && (!b.getType().equals(Material.ANVIL)) && (!b.getType().equals(Material.MOB_SPAWNER)) && (!b.getType().equals(Material.BEDROCK))) {
            return;
        }

        double chance = this.config.getChanceToDropBlock();

        if (chance > 1.0D) {
            chance = 1.0D;
        }

        if (chance < 0.0D) {
            chance = 0.0D;
        }

        double random = Math.random();

        if ((chance == 1.0D) || (chance <= random)) {
            ItemStack is = new ItemStack(b.getType(), 1, Short.valueOf(b.getData()).shortValue());

            at.getWorld().dropItemNaturally(at, is);
        }

        b.setTypeId(Material.AIR.getId());
    }

    private boolean checkIfMax(int value, int Dura) {
        return value == Dura;
    }

    private void startNewTimer(Integer representation) {
        if (this.obsidianTimer.get(representation) != null) {
            ((Timer) this.obsidianTimer.get(representation)).cancel();
        }

        // EXPERIMENTAL: Some safety just in case the server is running low on memory.
        // This will prevent a new timer from being created. However, durability will not regenerate
        if (config.getDurabilityTimerSafey()) {
            if (((float) Runtime.getRuntime().freeMemory() + (1024 * 1024 * config.getMinFreeMemoryLimit())) >= Runtime.getRuntime().maxMemory()) {
                if (DisplayWarning) {
                    ObsidianDestroyer.LOG.log(Level.INFO, "Server Memory: {0}MB free out of {1}MB available.", new Object[]{(Runtime.getRuntime().freeMemory() / 1024) / 1024, (Runtime.getRuntime().maxMemory() / 1024) / 1024});
                    ObsidianDestroyer.LOG.log(Level.INFO, "Server is running low on resources.. Let''s not start a new timer, there are {0} other timers running!", this.obsidianTimer.size());
                    DisplayWarning = false;
                }
                return;
            } else {
                DisplayWarning = true;
            }
        }

        Timer timer = new Timer();
        timer.schedule(new ODTimerTask(this.plugin, representation), this.config.getDurabilityResetTime());

        this.obsidianTimer.put(representation, timer);
    }

    private void dropBlockAndResetTime(Integer representation, Location at) {
        this.obsidianDurability.remove(representation);
        destroyBlockAndDropItem(at);

        if (this.config.getDurabilityResetTimerEnabled()) {
            if (this.obsidianTimer.get(representation) != null) {
                ((Timer) this.obsidianTimer.get(representation)).cancel();
            }

            this.obsidianTimer.remove(representation);
        }
    }

    public HashMap<Integer, Integer> getObsidianDurability() {
        return this.obsidianDurability;
    }

    public void setObsidianDurability(HashMap<Integer, Integer> map) {
        if (map == null) {
            return;
        }

        this.obsidianDurability = map;
    }

    public HashMap<Integer, Timer> getObsidianTimer() {
        return this.obsidianTimer;
    }

    public void setObsidianTimer(HashMap<Integer, Timer> map) {
        if (map == null) {
            return;
        }

        this.obsidianTimer = map;
    }
}