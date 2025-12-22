package de.mcmdev.antifreecam;

import com.github.retrooper.packetevents.PacketEvents;
import de.mcmdev.antifreecam.api.PlayerCacheHolder;
import de.mcmdev.antifreecam.structures.StructureCache;
import de.mcmdev.antifreecam.structures.StructureHider;
import de.mcmdev.antifreecam.ylevel.PacketCache;
import de.mcmdev.antifreecam.ylevel.PacketListener;
import de.mcmdev.antifreecam.ylevel.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class AntiFreecamPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadYLevelHider();
        loadStructureHider();
    }

    private void loadYLevelHider() {
        final int positionCutoff = getConfig().getInt("position-cutoff", 0);
        final int chunkCutoff = getConfig().getInt("chunk-cutoff", 4);

        final PlayerCacheHolder<PacketCache> packetCacheHolder = new PlayerCacheHolder<>(PacketCache::new);
        PacketEvents.getAPI().getEventManager().registerListeners(new PacketListener(packetCacheHolder, chunkCutoff, positionCutoff));
        Bukkit.getPluginManager().registerEvents(new PlayerListener(packetCacheHolder, positionCutoff), this);
    }

    private void loadStructureHider() {
        final PlayerCacheHolder<StructureCache> structureCacheHolder = new PlayerCacheHolder<>(StructureCache::new);
        Bukkit.getPluginManager().registerEvents(new StructureHider(structureCacheHolder), this);
    }
}
