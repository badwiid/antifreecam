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
import org.bukkit.entity.Player;

public final class PacketListener extends SimplePacketListenerAbstract {

    private final PlayerCacheHolder<PacketCache> playerCacheHolder;
    private final int chunkCutoff;
    private final int positionCutoff;

    public PacketListener(PlayerCacheHolder<PacketCache> playerCacheHolder, int chunkCutoff, int positionCutoff) {
        this.playerCacheHolder = playerCacheHolder;
        this.chunkCutoff = chunkCutoff;
        this.positionCutoff = positionCutoff;
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        switch (event.getPacketType()) {
            case CHUNK_DATA -> handleChunkDataPacket(event);
            case BLOCK_CHANGE -> handleBlockChangePacket(event);
            case MULTI_BLOCK_CHANGE -> handleMultiBlockChange(event);
        }
    }

    private void handleChunkDataPacket(PacketPlaySendEvent event) {
        Player player = event.getPlayer();
        if (player.getLocation().getBlockY() < positionCutoff) {
            return;
        }

        PacketPlaySendEvent clonedEvent = event.clone();
        WrapperPlayServerChunkData wrapperPlayServerChunkData = new WrapperPlayServerChunkData(event);
        Column column = wrapperPlayServerChunkData.getColumn();

        playerCacheHolder.get(player).addPacket(clonedEvent, WrapperPlayServerChunkData::new, wrapper -> ChunkPosition.fromColumn(wrapper.getColumn()));

        BaseChunk[] newChunks = removeChunks(column.getChunks());
        TileEntity[] newTileEntities = removeTileEntities(column.getTileEntities());

        Column newColumn = new Column(
                column.getX(), column.getZ(), column.isFullChunk(),
                newChunks, newTileEntities,
                column.getHeightmaps()
        );
        wrapperPlayServerChunkData.setColumn(newColumn);

        event.markForReEncode(true);
    }

    private void handleBlockChangePacket(PacketPlaySendEvent event) {
        Player player = event.getPlayer();
        if (player.getLocation().getBlockY() < positionCutoff) {
            return;
        }

        PacketPlaySendEvent clonedEvent = event.clone();
        WrapperPlayServerBlockChange wrapperPlayServerBlockChange = new WrapperPlayServerBlockChange(event);
        playerCacheHolder.get(player).addPacket(clonedEvent, WrapperPlayServerBlockChange::new, wrapper -> ChunkPosition.fromBlockPosition(wrapper.getBlockPosition()));

        if(wrapperPlayServerBlockChange.getBlockPosition().y < toYCoordinate(chunkCutoff)) {
            event.setCancelled(true);
        }
    }

    private void handleMultiBlockChange(PacketPlaySendEvent event) {
        Player player = event.getPlayer();
        if (player.getLocation().getBlockY() < positionCutoff) {
            return;
        }

        PacketPlaySendEvent clonedEvent = event.clone();
        WrapperPlayServerMultiBlockChange wrapper = new WrapperPlayServerMultiBlockChange(event);

        playerCacheHolder.get(player).addPacket(clonedEvent, WrapperPlayServerMultiBlockChange::new, it -> ChunkPosition.fromChunkPosition(wrapper.getChunkPosition()));

        if (wrapper.getChunkPosition().y < positionCutoff) {
            event.setCancelled(true);
        }
    }

    private BaseChunk[] removeChunks(BaseChunk[] originalChunks) {
        BaseChunk[] chunks = new BaseChunk[originalChunks.length];
        for (int i = 0; i < originalChunks.length; i++) {
            if (i < chunkCutoff) {
                chunks[i] = createEmptyChunk();
            } else {
                chunks[i] = originalChunks[i];
            }
        }
        return chunks;
    }

    private TileEntity[] removeTileEntities(TileEntity[] tileEntities) {
        int tileEntityCount = countTileEntities(tileEntities);
        TileEntity[] newTileEntities = new TileEntity[tileEntityCount];
        int counter = 0;
        for (TileEntity tileEntity : tileEntities) {
            if (tileEntity.getY() >= toYCoordinate(chunkCutoff)) {
                newTileEntities[counter] = tileEntity;
                counter++;
            }
        }
        return newTileEntities;
    }

    private int countTileEntities(TileEntity[] tileEntities) {
        int count = 0;
        for (TileEntity tileEntity : tileEntities) {
            if (tileEntity.getY() >= toYCoordinate(chunkCutoff)) {
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

    private int toYCoordinate(int chunkIndex) {
        return (chunkIndex << 4) - 64;
    }
}
