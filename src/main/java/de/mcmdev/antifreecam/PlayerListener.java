package de.mcmdev.antifreecam;

import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {

    private final ChunkCacheMap chunkCacheMap;
    private final int positionCutoff;

    public PlayerListener(ChunkCacheMap chunkCacheMap, int positionCutoff) {
        this.chunkCacheMap = chunkCacheMap;
        this.positionCutoff = positionCutoff;
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event)  {
        chunkCacheMap.prepare(event.getPlayer());
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event)  {
        chunkCacheMap.clear(event.getPlayer());
    }

    @EventHandler
    private void onPlayerChunkUnload(PlayerChunkUnloadEvent event)  {
        chunkCacheMap.get(event.getPlayer()).removeChunk(event.getChunk());
    }

    @EventHandler
    private void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        if (event.getTo().getBlockY() >= positionCutoff) {
            return;
        }
        chunkCacheMap.get(event.getPlayer()).resendChunks(event.getPlayer());
    }

}
