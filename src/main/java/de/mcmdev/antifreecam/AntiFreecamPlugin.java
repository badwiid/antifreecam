package de.mcmdev.antifreecam;

import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiFreecamPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        int positionCutoff = getConfig().getInt("position-cutoff", 0);
        int chunkCutoff = getConfig().getInt("chunk-cutoff", 4);

        ChunkCacheMap chunkCacheMap = new ChunkCacheMap();
        PacketEvents.getAPI().getEventManager().registerListeners(new PacketListener(chunkCacheMap, chunkCutoff, positionCutoff));
        Bukkit.getPluginManager().registerEvents(new PlayerListener(chunkCacheMap, positionCutoff), this);
    }
}
