package de.mcmdev.antifreecam;

import com.github.retrooper.packetevents.PacketEvents;
import de.mcmdev.antifreecam.api.PlayerCacheHolder;
import de.mcmdev.antifreecam.config.ConfigHolder;
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
        saveResource("world.yml", false);
        final ConfigHolder configHolder = new ConfigHolder(getDataFolder());
        loadYLevelHider(configHolder);
        loadStructureHider(configHolder);
    }

    private void loadYLevelHider(final ConfigHolder configHolder) {
        final PlayerCacheHolder<PacketCache> packetCacheHolder = new PlayerCacheHolder<>(PacketCache::new);
        PacketEvents.getAPI().getEventManager().registerListeners(new PacketListener(configHolder, packetCacheHolder));
        Bukkit.getPluginManager().registerEvents(new PlayerListener(configHolder, packetCacheHolder), this);
    }

    private void loadStructureHider(final ConfigHolder configHolder) {
        final PlayerCacheHolder<StructureCache> structureCacheHolder = new PlayerCacheHolder<>(StructureCache::new);
        Bukkit.getPluginManager().registerEvents(new StructureHider(configHolder, structureCacheHolder), this);
    }
}
