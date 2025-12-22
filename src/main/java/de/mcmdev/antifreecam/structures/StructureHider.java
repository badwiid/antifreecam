package de.mcmdev.antifreecam.structures;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.math.Position;
import io.papermc.paper.registry.keys.StructureTypeKeys;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.generator.structure.Structure;
import org.bukkit.generator.structure.StructurePiece;
import org.bukkit.generator.structure.StructureType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StructureHider implements Listener {

    private static final Set<Structure> HIDDEN_STRUCTURES = Set.of(
            Structure.BURIED_TREASURE,
            Structure.FORTRESS,
            Structure.MINESHAFT,
            Structure.STRONGHOLD,
            Structure.TRIAL_CHAMBERS,
            Structure.TRAIL_RUINS
    );

    @EventHandler
    private void onPlayerChunkLoad(PlayerChunkLoadEvent event)  {
        List<BoundingBox> boundingBoxes = extractStructureBoundingBoxes(event.getChunk());
        if(boundingBoxes.isEmpty()) return;

        Map<Position, BlockData> blockDataMap = createBlockDataMap(boundingBoxes);

        event.getPlayer().sendMultiBlockChange(blockDataMap);
    }

    private Position toPosition(Vector vector) {
        return Position.block(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    private @NotNull List<BoundingBox> extractStructureBoundingBoxes(Chunk chunk) {
        BoundingBox chunkBoundingBox = createChunkBoundingBox(chunk);
        return chunk.getStructures()
                .stream()
                .filter(generatedStructure -> HIDDEN_STRUCTURES.contains(generatedStructure.getStructure()))
                .flatMap(generatedStructure -> generatedStructure.getPieces().stream())
                .map(StructurePiece::getBoundingBox)
                .filter(boundingBox -> boundingBox.overlaps(chunkBoundingBox))
                .map(boundingBox -> boundingBox.intersection(chunkBoundingBox))
                .toList();
    }

    private BoundingBox createChunkBoundingBox(Chunk chunk) {
        int worldMin = chunk.getWorld().getMinHeight();
        int worldMax = chunk.getWorld().getMaxHeight();
        return new BoundingBox(chunk.getX() << 4, worldMin, chunk.getZ() << 4, (chunk.getX() << 4) + 15, worldMax, (chunk.getZ() << 4) + 15);
    }

    private Map<Position, BlockData> createBlockDataMap(Collection<BoundingBox> boundingBoxes) {
        Map<Position, BlockData> blockDataMap = new ConcurrentHashMap<>();
        for (BoundingBox boundingBox : boundingBoxes) {
            BoundingBoxIterator boundingBoxIterator = new BoundingBoxIterator(boundingBox);
            for (Vector vector : boundingBoxIterator) {
                if(vector.getBlockY() < 0) {
                    blockDataMap.put(toPosition(vector), Material.DEEPSLATE.createBlockData());
                }   else    {
                    blockDataMap.put(toPosition(vector), Material.STONE.createBlockData());
                }
            }
        }
        return blockDataMap;
    }

}
