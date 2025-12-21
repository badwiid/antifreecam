package de.mcmdev.antifreecam;

import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketPlaySendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.PaletteType;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.SingletonPalette;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import org.bukkit.entity.Player;

public final class PacketListener extends SimplePacketListenerAbstract {

    private final ChunkCacheMap chunkCacheMap;
    private final int chunkCutoff;
    private final int positionCutoff;

    public PacketListener(ChunkCacheMap chunkCacheMap, int chunkCutoff, int positionCutoff) {
        this.chunkCacheMap = chunkCacheMap;
        this.chunkCutoff = chunkCutoff;
        this.positionCutoff = positionCutoff;
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
            handleChunkDataPacket(event);
        }
        if(event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
            handleBlockChangePacket(event);
        }
    }

    private void handleChunkDataPacket(PacketPlaySendEvent event) {
        Player player = event.getPlayer();
        if (player.getLocation().getBlockY() < positionCutoff) {
            return;
        }

        PacketPlaySendEvent clone = event.clone();
        WrapperPlayServerChunkData wrapperPlayServerChunkData = new WrapperPlayServerChunkData(event);
        Column column = wrapperPlayServerChunkData.getColumn();

        chunkCacheMap.get(player).addPacket(new WrapperPlayServerChunkData(clone));

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

        PacketPlaySendEvent clone = event.clone();
        WrapperPlayServerBlockChange wrapperPlayServerBlockChange = new WrapperPlayServerBlockChange(event);
        chunkCacheMap.get(player).addPacket(new WrapperPlayServerBlockChange(clone));

        if(wrapperPlayServerBlockChange.getBlockPosition().y < toYCoordinate(chunkCutoff)) {
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
