package de.mcmdev.antifreecam.ylevel;

import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketPlaySendEvent;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.PaletteType;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.SingletonPalette;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import de.mcmdev.antifreecam.api.PlayerCacheHolder;
import de.mcmdev.antifreecam.config.Config;
import de.mcmdev.antifreecam.config.ConfigHolder;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class PacketListener extends SimplePacketListenerAbstract {

    private final ConfigHolder configHolder;
    private final PlayerCacheHolder<PacketCache> playerCacheHolder;

    public PacketListener(final ConfigHolder configHolder, final PlayerCacheHolder<PacketCache> playerCacheHolder) {
        this.configHolder = configHolder;
        this.playerCacheHolder = playerCacheHolder;
    }

    @Override
    public void onPacketPlaySend(final PacketPlaySendEvent event) {
        switch (event.getPacketType()) {
            case CHUNK_DATA -> handleChunkDataPacket(event);
            case BLOCK_CHANGE -> handleBlockChangePacket(event);
            case MULTI_BLOCK_CHANGE -> handleMultiBlockChange(event);
        }
    }

    private void handleChunkDataPacket(final PacketPlaySendEvent event) {
        final Player player = event.getPlayer();
        final Optional<Config> configOptional = configHolder.getConfig(player.getWorld().getName());
        if (configOptional.isEmpty()) return;
        final Config config = configOptional.get();
        if (!config.isEnableYLevelCutoff()) return;
        if (player.getLocation().getBlockY() < config.getRevealHeight()) return;
        final int chunkCutoff = toChunkIndex(config.getCutoffHeight(), player.getWorld().getMinHeight());

        final PacketPlaySendEvent clonedEvent = event.clone();
        final WrapperPlayServerChunkData wrapper = new WrapperPlayServerChunkData(event);
        final Column column = wrapper.getColumn();

        playerCacheHolder.get(player).addPacket(clonedEvent, WrapperPlayServerChunkData::new, it -> ChunkPosition.fromColumn(wrapper.getColumn()));

        final BaseChunk[] newChunks = removeChunks(column.getChunks(), chunkCutoff);
        final TileEntity[] newTileEntities = removeTileEntities(column.getTileEntities(), config.getCutoffHeight());

        final Column newColumn = new Column(
                column.getX(), column.getZ(), column.isFullChunk(),
                newChunks, newTileEntities,
                column.getHeightmaps()
        );
        wrapper.setColumn(newColumn);

        event.markForReEncode(true);
    }

    private void handleBlockChangePacket(final PacketPlaySendEvent event) {
        final Player player = event.getPlayer();
        final Optional<Config> configOptional = configHolder.getConfig(player.getWorld().getName());
        if (configOptional.isEmpty()) {
            return;
        }
        final Config config = configOptional.get();
        if (!config.isEnableYLevelCutoff()) return;
        if (player.getLocation().getBlockY() < config.getRevealHeight()) return;

        final PacketPlaySendEvent clonedEvent = event.clone();
        final WrapperPlayServerBlockChange wrapper = new WrapperPlayServerBlockChange(event);
        playerCacheHolder.get(player).addPacket(clonedEvent, WrapperPlayServerBlockChange::new, it -> ChunkPosition.fromBlockPosition(wrapper.getBlockPosition()));

        if (wrapper.getBlockPosition().y < config.getCutoffHeight()) {
            event.setCancelled(true);
        }
    }

    private void handleMultiBlockChange(final PacketPlaySendEvent event) {
        final Player player = event.getPlayer();
        final Optional<Config> configOptional = configHolder.getConfig(player.getWorld().getName());
        if (configOptional.isEmpty()) {
            return;
        }
        final Config config = configOptional.get();
        if (!config.isEnableYLevelCutoff()) return;
        if (player.getLocation().getBlockY() < config.getRevealHeight()) return;

        final PacketPlaySendEvent clonedEvent = event.clone();
        final WrapperPlayServerMultiBlockChange wrapper = new WrapperPlayServerMultiBlockChange(event);

        playerCacheHolder.get(player).addPacket(clonedEvent, WrapperPlayServerMultiBlockChange::new, it -> ChunkPosition.fromChunkPosition(wrapper.getChunkPosition()));

        if (wrapper.getChunkPosition().y < config.getCutoffHeight()) {
            event.setCancelled(true);
        }
    }

    private BaseChunk[] removeChunks(final BaseChunk[] originalChunks, final int chunkCutoff) {
        final BaseChunk[] chunks = new BaseChunk[originalChunks.length];
        for (int i = 0; i < originalChunks.length; i++) {
            if (i < chunkCutoff) {
                chunks[i] = createEmptyChunk();
            } else {
                chunks[i] = originalChunks[i];
            }
        }
        return chunks;
    }

    private TileEntity[] removeTileEntities(final TileEntity[] tileEntities, final int cutoffHeight) {
        final int tileEntityCount = countTileEntities(tileEntities, cutoffHeight);
        final TileEntity[] newTileEntities = new TileEntity[tileEntityCount];
        int counter = 0;
        for (final TileEntity tileEntity : tileEntities) {
            if (tileEntity.getY() >= cutoffHeight) {
                newTileEntities[counter] = tileEntity;
                counter++;
            }
        }
        return newTileEntities;
    }

    private int countTileEntities(final TileEntity[] tileEntities, final int cutoffHeight) {
        int count = 0;
        for (final TileEntity tileEntity : tileEntities) {
            if (tileEntity.getY() >= cutoffHeight) {
                count++;
            }
        }
        return count;
    }

    private BaseChunk createEmptyChunk() {
        return new Chunk_v1_18(
                0,
                new DataPalette(new SingletonPalette(0), null, PaletteType.CHUNK),
                new DataPalette(new SingletonPalette(0), null, PaletteType.BIOME)
        );
    }

    private int toChunkIndex(final int yCoordinate, final int worldMin) {
        return (yCoordinate >> 4) - (worldMin >> 4);
    }
}
