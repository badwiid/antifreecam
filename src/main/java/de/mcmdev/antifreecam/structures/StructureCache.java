package de.mcmdev.antifreecam.structures;

import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class StructureCache {

    private final Map<BoundingBox, Set<BoundingBox>> structures;

    public StructureCache() {
        this.structures = new ConcurrentHashMap<>();
    }

    public void add(BoundingBox structureBoundingBox, Set<BoundingBox> boundingBoxesOfPiecesInChunk) {
        structures.computeIfAbsent(structureBoundingBox, boundingBox -> new HashSet<>()).addAll(boundingBoxesOfPiecesInChunk);
    }

    public void checkUpdates(Player player) {
        for (Map.Entry<BoundingBox, Set<BoundingBox>> entry : structures.entrySet()) {
            if (entry.getKey().contains(player.getEyeLocation().toVector())) {
                update(player, entry.getValue());
            }
        }
    }

    private void update(Player player, Set<BoundingBox> boundingBoxes) {
        Set<BlockState> blockStates = new HashSet<>();
        for (BoundingBox boundingBox : boundingBoxes) {
            update(player, boundingBox, blockStates);
        }
        player.sendBlockChanges(blockStates);
    }

    private void update(Player player, BoundingBox boundingBox, Set<BlockState> blockStates) {
        for (Vector vector : new BoundingBoxIterator(boundingBox)) {
            blockStates.add(vector.toLocation(player.getWorld()).getBlock().getState());
        }
    }

}
