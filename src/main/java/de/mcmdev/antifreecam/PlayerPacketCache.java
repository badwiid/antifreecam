package de.mcmdev.antifreecam;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.simple.PacketPlaySendEvent;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;

public final class PlayerPacketCache {

    private final Map<ChunkPosition, Deque<PacketWrapper<?>>> packets;

    public PlayerPacketCache() {
        this.packets = new ConcurrentHashMap<>();
    }

    public <P extends PacketWrapper<P>> void addPacket(
            PacketPlaySendEvent event,
            Function<PacketPlaySendEvent, P> cloningFunction,
            Function<P, ChunkPosition> positionFunction
    ) {
        P clonedWrapper = cloningFunction.apply(event);
        ChunkPosition chunkPosition = positionFunction.apply(clonedWrapper);
        packets.computeIfAbsent(chunkPosition, k -> new ConcurrentLinkedDeque<>()).add(clonedWrapper);
    }

    public void removeChunk(Chunk chunk)   {
        packets.remove(new ChunkPosition(chunk.getX(), chunk.getZ()));
    }

    public void resendChunks(Player player) {
        for(Iterator<Deque<PacketWrapper<?>>> chunkIterator = packets.values().iterator(); chunkIterator.hasNext();) {
            Deque<PacketWrapper<?>> chunkPackets = chunkIterator.next();
            for (Iterator<PacketWrapper<?>> packetIterator = chunkPackets.iterator(); packetIterator.hasNext();) {
                PacketWrapper<?> packetWrapper = packetIterator.next();
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packetWrapper);
                packetIterator.remove();
            }
            chunkIterator.remove();
        }
    }

    private ChunkPosition getChunkPosition(Column column) {
        return new ChunkPosition(column.getX(), column.getZ());
    }

    private ChunkPosition getChunkPosition(Vector3i blockPosition) {
        return new ChunkPosition(blockPosition.x >> 4, blockPosition.z >> 4);
    }
}
