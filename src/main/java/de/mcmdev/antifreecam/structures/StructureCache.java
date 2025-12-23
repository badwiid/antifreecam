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

    public void add(final BoundingBox structureBoundingBox, final Set<BoundingBox> boundingBoxesOfPiecesInChunk) {
        structures.computeIfAbsent(structureBoundingBox, boundingBox -> new HashSet<>()).addAll(boundingBoxesOfPiecesInChunk);
    }

    public void checkUpdates(final Player player) {
        for (final Map.Entry<BoundingBox, Set<BoundingBox>> entry : structures.entrySet()) {
            if (entry.getKey().contains(player.getEyeLocation().toVector())) {
                update(player, entry.getValue());
            }
        }
    }

    private void update(final Player player, final Set<BoundingBox> boundingBoxes) {
        final Set<BlockState> blockStates = new HashSet<>();
        for (final BoundingBox boundingBox : boundingBoxes) {
            update(player, boundingBox, blockStates);
        }
        player.sendBlockChanges(blockStates);
    }

    private void update(final Player player, final BoundingBox boundingBox, final Set<BlockState> blockStates) {
        for (final Vector vector : new BoundingBoxIterator(boundingBox)) {
            blockStates.add(vector.toLocation(player.getWorld()).getBlock().getState());
        }
    }

}
