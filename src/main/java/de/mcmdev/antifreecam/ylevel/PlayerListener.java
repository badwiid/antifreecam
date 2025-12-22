package de.mcmdev.antifreecam.ylevel;

import de.mcmdev.antifreecam.api.PlayerCacheHolder;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class PlayerListener implements Listener {

    private final PlayerCacheHolder<PacketCache> playerCacheHolder;
    private final int positionCutoff;

    public PlayerListener(PlayerCacheHolder<PacketCache> playerCacheHolder, int positionCutoff) {
        this.playerCacheHolder = playerCacheHolder;
        this.positionCutoff = positionCutoff;
    }

    @EventHandler
    private void onPlayerChunkUnload(PlayerChunkUnloadEvent event)  {
        playerCacheHolder.get(event.getPlayer()).removeChunk(event.getChunk());
    }

    @EventHandler
    private void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        if (event.getTo().getBlockY() >= positionCutoff) {
            return;
        }
        playerCacheHolder.get(event.getPlayer()).resendChunks(event.getPlayer());
    }

}
