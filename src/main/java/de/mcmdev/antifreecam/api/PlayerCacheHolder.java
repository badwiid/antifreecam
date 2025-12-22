package de.mcmdev.antifreecam.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class PlayerCacheHolder<Cache> {

    private final Supplier<Cache> cacheFactory;
    private final Map<UUID, Cache> caches;

    public PlayerCacheHolder(Supplier<Cache> cacheFactory) {
        this.cacheFactory = cacheFactory;
        this.caches = new ConcurrentHashMap<>();

        Bukkit.getPluginManager().registerEvents(new InnerListener(), JavaPlugin.getProvidingPlugin(getClass()));
    }

    public void prepare(Player player) {
        caches.put(player.getUniqueId(), cacheFactory.get());
    }

    public void clear(Player player) {
        caches.remove(player.getUniqueId());
    }

    public Cache get(Player player) {
        return caches.computeIfAbsent(player.getUniqueId(), uuid -> cacheFactory.get());
    }

    public final class InnerListener implements org.bukkit.event.Listener {

        @EventHandler
        public void onJoin(PlayerJoinEvent event)  {
            PlayerCacheHolder.this.prepare(event.getPlayer());
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event)  {
            PlayerCacheHolder.this.clear(event.getPlayer());
        }

    }

}
