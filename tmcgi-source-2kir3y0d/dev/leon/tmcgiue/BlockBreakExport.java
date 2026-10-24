//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.SortedSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.BlockDestructionProgress;

final class BlockBreakExport {
    private static final int MAX_BLOCKS = 16;
    private static int texturesGeneration = -1;
    private static int listGeneration = -1;
    private static String lastList = "";

    private BlockBreakExport() {
    }

    static void update(ClientLevel level, BridgeConnection connection) {
        int generation = connection.generation();
        if (generation != texturesGeneration) {
            texturesGeneration = generation;

            for(int stage = 0; stage < 10; ++stage) {
                byte[] png = EntityCapture.readResource(Identifier.fromNamespaceAndPath("minecraft", "textures/block/destroy_stage_" + stage + ".png"));
                if (png.length > 0) {
                    connection.sendUrgent(BridgeProtocol.blockTexture("block/destroy_stage_" + stage, png, new byte[0], new byte[0], new byte[0]));
                }
            }
        }

        int[] blocks = new int[64];
        int count = 0;
        StringBuilder list = new StringBuilder();
        ObjectIterator var6 = level.destructionProgress().long2ObjectEntrySet().iterator();

        while(var6.hasNext()) {
            Long2ObjectMap.Entry<SortedSet<BlockDestructionProgress>> entry = (Long2ObjectMap.Entry)var6.next();
            SortedSet<BlockDestructionProgress> progresses = (SortedSet)entry.getValue();
            if (progresses != null && !progresses.isEmpty() && count < 16) {
                BlockPos pos = BlockPos.of(entry.getLongKey());
                int stage = Math.max(0, Math.min(9, ((BlockDestructionProgress)progresses.last()).getProgress()));
                blocks[count * 4] = pos.getX();
                blocks[count * 4 + 1] = pos.getY();
                blocks[count * 4 + 2] = pos.getZ();
                blocks[count * 4 + 3] = stage;
                list.append(pos.getX()).append(',').append(pos.getY()).append(',').append(pos.getZ()).append(',').append(stage).append(';');
                ++count;
            }
        }

        String now = list.toString();
        if (generation != listGeneration || !now.equals(lastList)) {
            listGeneration = generation;
            lastList = now;
            connection.sendUrgent(BridgeProtocol.blockBreak(blocks, count));
        }

    }
}
