package de.mcmdev.antifreecam.structures;

import de.mcmdev.antifreecam.api.PlayerCacheHolder;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.math.Position;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;
import org.bukkit.generator.structure.StructurePiece;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class StructureHider implements Listener {

    private static final Set<Structure> HIDDEN_STRUCTURES = Set.of(
            Structure.BURIED_TREASURE,
            Structure.FORTRESS,
            Structure.MINESHAFT,
            Structure.STRONGHOLD,
            Structure.TRIAL_CHAMBERS,
            Structure.TRAIL_RUINS
    );

    private final PlayerCacheHolder<StructureCache> playerCacheHolder;

    public StructureHider(PlayerCacheHolder<StructureCache> playerCacheHolder) {
        this.playerCacheHolder = playerCacheHolder;
    }

    @EventHandler
    private void onPlayerChunkLoad(PlayerChunkLoadEvent event)  {
        BoundingBox chunkBoundingBox = createChunkBoundingBox(event.getChunk());
        Set<GeneratedStructure> structuresToHide = findStructuresToHide(event.getChunk());
        if(structuresToHide.isEmpty()) return;

        StructureCache structureCache = playerCacheHolder.get(event.getPlayer());
        for (GeneratedStructure structure : structuresToHide) {
            Set<BoundingBox> boundingBoxesOfPiecesInChunk = extractStructurePieceBoundingBoxesInChunk(chunkBoundingBox, structure);
            structureCache.add(structure.getBoundingBox(), boundingBoxesOfPiecesInChunk);

            Map<Position, BlockData> blockDataMap = createBlockDataMap(boundingBoxesOfPiecesInChunk);
            event.getPlayer().sendMultiBlockChange(blockDataMap);
        }
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        playerCacheHolder.get(event.getPlayer()).checkUpdates(event.getPlayer());
    }

    private Position toPosition(Vector vector) {
        return Position.block(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    private @NotNull Set<BoundingBox> extractStructurePieceBoundingBoxesInChunk(BoundingBox chunkBoundingBox, GeneratedStructure generatedStructure) {
        return generatedStructure.getPieces().stream()
                .map(StructurePiece::getBoundingBox)
                .filter(boundingBox -> boundingBox.overlaps(chunkBoundingBox))
                .map(boundingBox -> boundingBox.intersection(chunkBoundingBox))
                .collect(Collectors.toSet());
    }

    private Set<GeneratedStructure> findStructuresToHide(Chunk chunk) {
        return chunk.getStructures()
                .stream()
                .filter(generatedStructure -> HIDDEN_STRUCTURES.contains(generatedStructure.getStructure()))
                .collect(Collectors.toSet());
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
