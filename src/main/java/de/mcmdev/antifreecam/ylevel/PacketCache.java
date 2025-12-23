package de.mcmdev.antifreecam.ylevel;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.simple.PacketPlaySendEvent;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;

public final class PacketCache {

    private final Map<ChunkPosition, Deque<PacketWrapper<?>>> packets;

    public PacketCache() {
        this.packets = new ConcurrentHashMap<>();
    }

    public <P extends PacketWrapper<P>> void addPacket(
            final PacketPlaySendEvent event,
            final Function<PacketPlaySendEvent, P> cloningFunction,
            final Function<P, ChunkPosition> positionFunction
    ) {
        final P clonedWrapper = cloningFunction.apply(event);
        final ChunkPosition chunkPosition = positionFunction.apply(clonedWrapper);
        packets.computeIfAbsent(chunkPosition, k -> new ConcurrentLinkedDeque<>()).add(clonedWrapper);
    }

    public void removeChunk(final Chunk chunk) {
        packets.remove(new ChunkPosition(chunk.getX(), chunk.getZ()));
    }

    public void resendChunks(final Player player) {
        for (final Iterator<Deque<PacketWrapper<?>>> chunkIterator = packets.values().iterator(); chunkIterator.hasNext(); ) {
            final Deque<PacketWrapper<?>> chunkPackets = chunkIterator.next();
            for (final Iterator<PacketWrapper<?>> packetIterator = chunkPackets.iterator(); packetIterator.hasNext(); ) {
                final PacketWrapper<?> packetWrapper = packetIterator.next();
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packetWrapper);
                packetIterator.remove();
            }
            chunkIterator.remove();
        }
    }

    private ChunkPosition getChunkPosition(final Column column) {
        return new ChunkPosition(column.getX(), column.getZ());
    }

    private ChunkPosition getChunkPosition(final Vector3i blockPosition) {
        return new ChunkPosition(blockPosition.x >> 4, blockPosition.z >> 4);
    }
}
