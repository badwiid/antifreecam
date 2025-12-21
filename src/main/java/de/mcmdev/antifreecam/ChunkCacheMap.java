package de.mcmdev.antifreecam;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkCacheMap {

    private final Map<UUID, PlayerPacketCache> caches;

    public ChunkCacheMap() {
        this.caches = new ConcurrentHashMap<>();
    }

    public void prepare(Player player) {
        caches.put(player.getUniqueId(), new PlayerPacketCache());
    }

    public void clear(Player player) {
        caches.remove(player.getUniqueId());
    }

    public PlayerPacketCache get(Player player) {
        return caches.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerPacketCache());
    }

}
