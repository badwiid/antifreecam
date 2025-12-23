package de.mcmdev.antifreecam.ylevel;

import de.mcmdev.antifreecam.api.PlayerCacheHolder;
import de.mcmdev.antifreecam.config.Config;
import de.mcmdev.antifreecam.config.ConfigHolder;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;

public final class PlayerListener implements Listener {

    private final ConfigHolder configHolder;
    private final PlayerCacheHolder<PacketCache> playerCacheHolder;

    public PlayerListener(final ConfigHolder configHolder, final PlayerCacheHolder<PacketCache> playerCacheHolder) {
        this.configHolder = configHolder;
        this.playerCacheHolder = playerCacheHolder;
    }

    @EventHandler
    private void onPlayerChunkUnload(final PlayerChunkUnloadEvent event) {
        if (configHolder.getConfig(event.getWorld().getName()).isEmpty()) {
            return;
        }
        playerCacheHolder.get(event.getPlayer()).removeChunk(event.getChunk());
    }

    @EventHandler
    private void onPlayerMove(final org.bukkit.event.player.PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        final Optional<Config> configOptional = configHolder.getConfig(event.getTo().getWorld().getName());
        if (configOptional.isEmpty()) return;
        final Config config = configOptional.get();
        if (!config.isEnableYLevelCutoff()) return;
        if (event.getTo().getBlockY() >= config.getRevealHeight()) {
            return;
        }
        playerCacheHolder.get(event.getPlayer()).resendChunks(event.getPlayer());
    }

}
