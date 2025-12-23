package de.mcmdev.antifreecam.ylevel;

import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.Location;

public record ChunkPosition(int x, int z) {

    public static ChunkPosition fromColumn(final Column column) {
        return new ChunkPosition(column.getX(), column.getZ());
    }

    public static ChunkPosition fromBlockPosition(final Vector3i blockPosition) {
        return new ChunkPosition(blockPosition.x >> 4, blockPosition.z >> 4);
    }

    public static ChunkPosition fromChunkPosition(final Vector3i chunkPosition) {
        return new ChunkPosition(chunkPosition.x, chunkPosition.z);
    }

    public static ChunkPosition fromLocation(final Location location) {
        return new ChunkPosition(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

}