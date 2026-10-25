//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

final class SectionTracker {
    private static final long TICK_BUDGET_NANOS = 3000000L;
    private static final double URGENT_RANGE = 64.0;
    private static final int MAX_URGENT_PER_FRAME = 8;
    private static final int GRID = 18;
    private static final int LOADED = 0;
    private static final int UNLOADED = 1;
    private static final int CHANGED = 2;
    private final BridgeConnection connection;
    private int radius;
    private final int fixedRadius;
    private final boolean followRenderDistance;
    private final int maxRadius;
    private final ConcurrentLinkedQueue<Event> events = new ConcurrentLinkedQueue();
    private ClientLevel level;
    private final HashSet<Long> loadedChunks = new HashSet();
    private final HashSet<Long> sentChunks = new HashSet();
    private final HashMap<Long, BitSet> sentSections = new HashMap();
    private final ArrayDeque<Long> chunkQueue = new ArrayDeque();
    private final ArrayDeque<Long> sectionTasks = new ArrayDeque();
    private final ArrayDeque<Long> deepTasks = new ArrayDeque();
    private Vec3 cameraPosition;
    private final LinkedHashSet<Long> urgent = new LinkedHashSet();
    private final LinkedHashSet<Long> lazy = new LinkedHashSet();
    private int revision;
    private final HashSet<String> emissionSent = new HashSet();
    private final HashMap<Long, short[]> sentHeights = new HashMap();
    private final HashSet<Long> heightDirty = new HashSet();
    private boolean polledUrgent;
    private int tickCounter;
    private final BlockExport export;
    private final LinkedHashMap<Long, Deferred> deferred = new LinkedHashMap();
    private int exportsSeen = -1;

    SectionTracker(BridgeConnection connection, int fixedRadius, boolean followRenderDistance, int maxRadius) {
        this.connection = connection;
        this.fixedRadius = fixedRadius;
        this.followRenderDistance = followRenderDistance;
        this.maxRadius = maxRadius;
        this.radius = fixedRadius;
        this.export = new BlockExport(connection);
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (long)chunkX & 4294967295L | (long)chunkZ << 32;
    }

    private static int chunkKeyX(long key) {
        return (int)key;
    }

    private static int chunkKeyZ(long key) {
        return (int)(key >> 32);
    }

    private static long sectionKey(int chunkX, int sectionY, int chunkZ) {
        return (long)(chunkX & 67108863) << 38 | (long)(chunkZ & 67108863) << 12 | (long)(sectionY + 2048 & 4095);
    }

    private static int sectionKeyX(long key) {
        return (int)(key >> 38) << 6 >> 6;
    }

    private static int sectionKeyZ(long key) {
        return (int)(key >> 12 & 67108863L) << 6 >> 6;
    }

    private static int sectionKeyY(long key) {
        return (int)(key & 4095L) - 2048;
    }

    void onChunkLoad(int chunkX, int chunkZ) {
        this.events.add(new Event(0, chunkKey(chunkX, chunkZ)));
    }

    void onChunkUnload(int chunkX, int chunkZ) {
        this.events.add(new Event(1, chunkKey(chunkX, chunkZ)));
    }

    void onBlockChanged(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int sectionY = pos.getY() >> 4;
        int chunkZ = pos.getZ() >> 4;
        this.events.add(new Event(2, sectionKey(chunkX, sectionY, chunkZ)));
        int localX = pos.getX() & 15;
        int localY = pos.getY() & 15;
        int localZ = pos.getZ() & 15;
        if (localX == 0) {
            this.events.add(new Event(2, sectionKey(chunkX - 1, sectionY, chunkZ)));
        }

        if (localX == 15) {
            this.events.add(new Event(2, sectionKey(chunkX + 1, sectionY, chunkZ)));
        }

        if (localY == 0) {
            this.events.add(new Event(2, sectionKey(chunkX, sectionY - 1, chunkZ)));
        }

        if (localY == 15) {
            this.events.add(new Event(2, sectionKey(chunkX, sectionY + 1, chunkZ)));
        }

        if (localZ == 0) {
            this.events.add(new Event(2, sectionKey(chunkX, sectionY, chunkZ - 1)));
        }

        if (localZ == 15) {
            this.events.add(new Event(2, sectionKey(chunkX, sectionY, chunkZ + 1)));
        }

    }

    void tick(Minecraft minecraft) {
        ClientLevel current = minecraft.level;
        if (current != this.level) {
            this.level = current;
            this.resetAll();
            this.connection.send(BridgeProtocol.clear());
            if (current == null) {
                return;
            }
        }

        if (current != null) {
            this.drainEvents();
            if (this.connection.consumeResync()) {
                this.resetSent();
                this.connection.send(BridgeProtocol.clear());
                this.updateRadius(minecraft);
            } else if (this.tickCounter % 10 == 0) {
                this.updateRadius(minecraft);
            }

            ++this.tickCounter;
            long start = System.nanoTime();
            this.retryDeferred(false, start);

            while(System.nanoTime() - start < 3000000L) {
                Long key = this.pollSection();
                if (key == null) {
                    break;
                }

                this.sendSection(sectionKeyX(key), sectionKeyY(key), sectionKeyZ(key), this.polledUrgent);
            }

        }
    }

    private void drainEvents() {
        Event event;
        while((event = (Event)this.events.poll()) != null) {
            switch (event.kind) {
                case 0:
                    this.loadedChunks.add(event.key);
                    break;
                case 1:
                    this.loadedChunks.remove(event.key);
                    break;
                case 2:
                    if (this.sentChunks.contains(chunkKey(sectionKeyX(event.key), sectionKeyZ(event.key)))) {
                        if (this.withinUrgentRange(event.key)) {
                            this.urgent.add(event.key);
                        } else {
                            this.lazy.add(event.key);
                        }

                        this.heightDirty.add(chunkKey(sectionKeyX(event.key), sectionKeyZ(event.key)));
                    }
            }
        }

    }

    void flushUrgent() {
        if (this.level != null && this.connection.isConnected()) {
            this.drainEvents();
            if (!this.heightDirty.isEmpty()) {
                Iterator var1 = this.heightDirty.iterator();

                while(var1.hasNext()) {
                    long key = (Long)var1.next();
                    LevelChunk changed = this.level.getChunkSource().getChunk(chunkKeyX(key), chunkKeyZ(key), ChunkStatus.FULL, false);
                    if (changed != null && this.sentChunks.contains(key)) {
                        this.sendHeightmap(chunkKeyX(key), chunkKeyZ(key), changed);
                    }
                }

                this.heightDirty.clear();
            }

            this.retryDeferred(true, System.nanoTime());

            for(int sent = 0; sent < 8 && !this.urgent.isEmpty(); ++sent) {
                Iterator<Long> it = this.urgent.iterator();
                long key = (Long)it.next();
                it.remove();
                this.sendSection(sectionKeyX(key), sectionKeyY(key), sectionKeyZ(key), true);
            }

        }
    }

    private void retryDeferred(boolean urgentOnly, long start) {
        if (!this.deferred.isEmpty()) {
            int finishedNow = this.export.finishedCount();
            if (urgentOnly || finishedNow != this.exportsSeen) {
                List<Long> ready = new ArrayList();
                Iterator var6 = this.deferred.entrySet().iterator();

                while(true) {
                    Map.Entry entry;
                    Deferred waiting;
                    do {
                        if (!var6.hasNext()) {
                            boolean all = true;
                            Iterator var13 = ready.iterator();

                            while(var13.hasNext()) {
                                long key = (Long)var13.next();
                                if (!urgentOnly && System.nanoTime() - start >= 3000000L) {
                                    all = false;
                                    break;
                                }

                                Deferred section = (Deferred)this.deferred.remove(key);
                                if (section != null && this.level != null && this.sentChunks.contains(chunkKey(section.chunkX(), section.chunkZ()))) {
                                    this.finishSection(section.chunkX(), section.sectionY(), section.chunkZ(), section.index(), section.urgent(), section.palette(), section.paletteStates(), section.indices(), section.emitterNames(), section.emitterInfos());
                                }
                            }

                            if (!urgentOnly && all) {
                                this.exportsSeen = finishedNow;
                            }

                            return;
                        }

                        entry = (Map.Entry)var6.next();
                        waiting = (Deferred)entry.getValue();
                    } while(urgentOnly && !waiting.urgent());

                    boolean done = true;
                    Iterator var10 = waiting.waiting().iterator();

                    while(var10.hasNext()) {
                        String name = (String)var10.next();
                        if (!this.export.isReady(name)) {
                            done = false;
                            break;
                        }
                    }

                    if (done) {
                        ready.add((Long)entry.getKey());
                    }
                }
            }
        }
    }

    private void resetSent() {
        this.export.reset();
        this.deferred.clear();
        this.exportsSeen = -1;
        this.emissionSent.clear();
        this.sentHeights.clear();
        this.heightDirty.clear();
        this.sentChunks.clear();
        this.sentSections.clear();
        this.chunkQueue.clear();
        this.sectionTasks.clear();
        this.deepTasks.clear();
        this.urgent.clear();
        this.lazy.clear();
    }

    private void resetAll() {
        this.loadedChunks.clear();
        this.resetSent();
        this.events.clear();
    }

    private Long pollSection() {
        this.polledUrgent = false;
        Long key;
        Iterator it;
        if (!this.urgent.isEmpty()) {
            it = this.urgent.iterator();
            key = (Long)it.next();
            it.remove();
            this.polledUrgent = true;
            return key;
        } else {
            while(this.sectionTasks.isEmpty() && !this.chunkQueue.isEmpty()) {
                long chunk = (Long)this.chunkQueue.peek();
                if (this.cameraPosition != null) {
                    double bestDistance = Double.MAX_VALUE;
                    Iterator var5 = this.chunkQueue.iterator();

                    while(var5.hasNext()) {
                        long candidate = (Long)var5.next();
                        double dx = (double)((chunkKeyX(candidate) << 4) + 8) - this.cameraPosition.x;
                        double dz = (double)((chunkKeyZ(candidate) << 4) + 8) - this.cameraPosition.z;
                        double distance = dx * dx + dz * dz;
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            chunk = candidate;
                        }
                    }
                }

                this.chunkQueue.remove(chunk);
                if (this.sentChunks.contains(chunk) && this.level != null) {
                    int chunkX = chunkKeyX(chunk);
                    int chunkZ = chunkKeyZ(chunk);
                    int minSection = this.level.getMinSectionY();
                    LevelChunk loaded = this.level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                    int surfaceMin = Integer.MIN_VALUE;
                    if (loaded != null) {
                        surfaceMin = Integer.MAX_VALUE;
                        int[][] samples = new int[][]{{0, 0}, {15, 0}, {0, 15}, {15, 15}, {8, 8}};
                        int[][] var9 = samples;
                        int var21 = samples.length;

                        for(int var11 = 0; var11 < var21; ++var11) {
                            int[] sample = var9[var11];
                            surfaceMin = Math.min(surfaceMin, loaded.getHeight(Types.WORLD_SURFACE, sample[0], sample[1]));
                        }
                    }

                    if (loaded != null) {
                        this.sendHeightmap(chunkX, chunkZ, loaded);
                    }

                    for(int i = this.level.getSectionsCount() - 1; i >= 0; --i) {
                        int sectionY = minSection + i;
                        boolean deep = surfaceMin != Integer.MIN_VALUE && (sectionY << 4) + 15 < surfaceMin - 12 && !this.nearCamera(chunkX, sectionY, chunkZ);
                        (deep ? this.deepTasks : this.sectionTasks).add(sectionKey(chunkX, sectionY, chunkZ));
                    }

                    this.queueNeighbourRefresh(chunkX, chunkZ);
                }
            }

            if (!this.sectionTasks.isEmpty()) {
                return (Long)this.sectionTasks.poll();
            } else if (!this.lazy.isEmpty()) {
                it = this.lazy.iterator();
                key = (Long)it.next();
                it.remove();
                return key;
            } else {
                return !this.deepTasks.isEmpty() ? (Long)this.deepTasks.poll() : null;
            }
        }
    }

    private boolean withinUrgentRange(long sectionKey) {
        Vec3 camera = this.cameraPosition;
        if (camera == null) {
            return true;
        } else {
            double dx = (double)((sectionKeyX(sectionKey) << 4) + 8) - camera.x;
            double dy = (double)((sectionKeyY(sectionKey) << 4) + 8) - camera.y;
            double dz = (double)((sectionKeyZ(sectionKey) << 4) + 8) - camera.z;
            return dx * dx + dy * dy + dz * dz < 4096.0;
        }
    }

    private boolean nearCamera(int chunkX, int sectionY, int chunkZ) {
        Vec3 camera = this.cameraPosition;
        if (camera == null) {
            return false;
        } else {
            double dx = (double)((chunkX << 4) + 8) - camera.x;
            double dy = (double)((sectionY << 4) + 8) - camera.y;
            double dz = (double)((chunkZ << 4) + 8) - camera.z;
            return dx * dx + dy * dy + dz * dz < 1600.0;
        }
    }

    private void queueNeighbourRefresh(int chunkX, int chunkZ) {
        int[][] offsets = new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        int[][] var4 = offsets;
        int var5 = offsets.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            int[] offset = var4[var6];
            int neighbourX = chunkX + offset[0];
            int neighbourZ = chunkZ + offset[1];
            BitSet sent = (BitSet)this.sentSections.get(chunkKey(neighbourX, neighbourZ));
            if (sent != null) {
                int minSection = this.level.getMinSectionY();

                for(int i = sent.nextSetBit(0); i >= 0; i = sent.nextSetBit(i + 1)) {
                    this.lazy.add(sectionKey(neighbourX, minSection + i, neighbourZ));
                }
            }
        }

    }

    BlockExport exporter() {
        return this.export;
    }

    int radius() {
        return this.radius;
    }

    private void updateRadius(Minecraft minecraft) {
        Camera camera = minecraft.gameRenderer.mainCamera();
        if (camera != null && camera.isInitialized()) {
            int wanted = this.followRenderDistance ? Math.max(2, Math.min(this.maxRadius, minecraft.options.getEffectiveRenderDistance())) : this.fixedRadius;
            if (wanted != this.radius) {
                TmcgiUeBridgeClient.LOGGER.info("TMCGI UE bridge: sending {} chunks around the camera now (was {})", wanted, this.radius);
                this.radius = wanted;
            }

            Vec3 position = camera.position();
            this.cameraPosition = position;
            int centerX = Mth.floor(position.x) >> 4;
            int centerZ = Mth.floor(position.z) >> 4;
            long key;
            if (this.level != null) {
                for(int dz = -this.radius; dz <= this.radius; ++dz) {
                    for(int dx = -this.radius; dx <= this.radius; ++dx) {
                        key = chunkKey(centerX + dx, centerZ + dz);
                        if (!this.loadedChunks.contains(key) && this.level.getChunkSource().getChunk(centerX + dx, centerZ + dz, ChunkStatus.FULL, false) != null) {
                            this.loadedChunks.add(key);
                        }
                    }
                }
            }

            List<long[]> candidates = new ArrayList();
            Iterator it = this.loadedChunks.iterator();

            int distance;
            while(it.hasNext()) {
                key = (Long)it.next();
                distance = Math.max(Math.abs(chunkKeyX(key) - centerX), Math.abs(chunkKeyZ(key) - centerZ));
                if (distance <= this.radius && !this.sentChunks.contains(key)) {
                    candidates.add(new long[]{(long)distance, key});
                }
            }

            candidates.sort((a, b) -> {
                return Long.compare(a[0], b[0]);
            });
            it = candidates.iterator();

            while(it.hasNext()) {
                long[] candidate = (long[])it.next();
                this.sentChunks.add(candidate[1]);
                this.chunkQueue.add(candidate[1]);
            }

            it = this.sentChunks.iterator();

            while(true) {
                do {
                    if (!it.hasNext()) {
                        return;
                    }

                    key = (Long)it.next();
                    distance = Math.max(Math.abs(chunkKeyX(key) - centerX), Math.abs(chunkKeyZ(key) - centerZ));
                } while(distance <= this.radius + 1 && this.loadedChunks.contains(key));

                it.remove();
                this.sentSections.remove(key);
                this.sentHeights.remove(key);
                this.connection.send(BridgeProtocol.chunkRemove(chunkKeyX(key), chunkKeyZ(key)));
            }
        }
    }

    private void sendHeightmap(int chunkX, int chunkZ, LevelChunk chunk) {
        short[] heights = new short[256];

        for(int z = 0; z < 16; ++z) {
            for(int x = 0; x < 16; ++x) {
                heights[z * 16 + x] = (short)chunk.getHeight(Types.MOTION_BLOCKING, x, z);
            }
        }

        long key = chunkKey(chunkX, chunkZ);
        short[] previous = (short[])this.sentHeights.get(key);
        if (previous == null || !Arrays.equals(previous, heights)) {
            this.sentHeights.put(key, heights);
            this.connection.send(BridgeProtocol.heightmap(chunkX, chunkZ, this.level.getMinSectionY() << 4, heights));
        }
    }

    private void sendSection(int chunkX, int sectionY, int chunkZ, boolean isUrgent) {
        this.deferred.remove(sectionKey(chunkX, sectionY, chunkZ));
        long chunkKey = chunkKey(chunkX, chunkZ);
        if (this.level != null && this.sentChunks.contains(chunkKey)) {
            LevelChunk chunk = this.level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
            int index = this.level.getSectionIndexFromSectionY(sectionY);
            if (chunk != null && index >= 0 && index < this.level.getSectionsCount()) {
                LevelChunkSection section = chunk.getSection(index);
                BitSet sent = (BitSet)this.sentSections.computeIfAbsent(chunkKey, (k) -> {
                    return new BitSet();
                });
                if (section.hasOnlyAir()) {
                    if (sent.get(index)) {
                        sent.clear(index);
                        this.connection.send(BridgeProtocol.sectionRemove(chunkX, sectionY, chunkZ));
                    }

                } else {
                    short[] indices = new short[5832];
                    List<String> palette = new ArrayList();
                    palette.add("minecraft:air");
                    List<BlockState> paletteStates = new ArrayList();
                    paletteStates.add((Object)null);
                    IdentityHashMap<BlockState, Integer> ids = new IdentityHashMap();
                    IdentityHashMap<BlockState, Boolean> opaque = new IdentityHashMap();
                    boolean mayShowFace = false;
                    List<String> emitterNames = new ArrayList();
                    List<int[]> emitterInfos = new ArrayList();
                    List<String> waitingFor = new ArrayList();
                    PalettedContainer<BlockState> states = section.getStates();
                    BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
                    int baseX = chunkX << 4;
                    int baseY = sectionY << 4;
                    int baseZ = chunkZ << 4;

                    for(int y = -1; y <= 16; ++y) {
                        for(int z = -1; z <= 16; ++z) {
                            for(int x = -1; x <= 16; ++x) {
                                BlockState state;
                                if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 && z < 16) {
                                    state = (BlockState)states.get(x, y, z);
                                } else {
                                    state = this.level.getBlockState(position.set(baseX + x, baseY + y, baseZ + z));
                                }

                                if (!mayShowFace) {
                                    boolean innerX = x >= 0 && x < 16;
                                    boolean innerY = y >= 0 && y < 16;
                                    boolean innerZ = z >= 0 && z < 16;
                                    boolean relevant = innerX && innerY && innerZ || !innerX && innerY && innerZ || innerX && !innerY && innerZ || innerX && innerY && !innerZ;
                                    if (relevant) {
                                        Boolean full = (Boolean)opaque.get(state);
                                        if (full == null) {
                                            full = !state.isAir() && state.isSolidRender();
                                            opaque.put(state, full);
                                        }

                                        if (!full) {
                                            mayShowFace = true;
                                        }
                                    }
                                }

                                int id = 0;
                                if (!state.isAir()) {
                                    Integer known = (Integer)ids.get(state);
                                    if (known == null) {
                                        known = palette.size();
                                        String stateName = BlockStateNames.of(state);
                                        palette.add(stateName);
                                        paletteStates.add(state);
                                        if (!this.export.isReady(stateName)) {
                                            this.export.request(stateName, state, isUrgent);
                                            waitingFor.add(stateName);
                                        }

                                        int emission = state.getLightEmission();
                                        if (emission > 0 && !this.emissionSent.contains(stateName)) {
                                            emitterNames.add(stateName);
                                            int color = EmitterColor.of(state);
                                            emitterInfos.add(new int[]{Math.min(emission, 15), (state.isSolidRender() ? 1 : 0) | (color >= 0 ? 2 : 0), Math.max(color, 0)});
                                        }

                                        ids.put(state, known);
                                    }

                                    id = known;
                                }

                                indices[((y + 1) * 18 + z + 1) * 18 + x + 1] = (short)id;
                            }
                        }
                    }

                    if (!mayShowFace) {
                        if (sent.get(index)) {
                            sent.clear(index);
                            this.connection.send(BridgeProtocol.sectionRemove(chunkX, sectionY, chunkZ));
                        }

                    } else if (!waitingFor.isEmpty()) {
                        this.deferred.put(sectionKey(chunkX, sectionY, chunkZ), new Deferred(isUrgent, chunkX, sectionY, chunkZ, index, palette, paletteStates, indices, emitterNames, emitterInfos, waitingFor));
                    } else {
                        this.finishSection(chunkX, sectionY, chunkZ, index, isUrgent, palette, paletteStates, indices, emitterNames, emitterInfos);
                    }
                }
            }
        }
    }

    private void finishSection(int chunkX, int sectionY, int chunkZ, int index, boolean isUrgent, List<String> palette, List<BlockState> paletteStates, short[] indices, List<String> emitterNames, List<int[]> emitterInfos) {
        BitSet sent = (BitSet)this.sentSections.computeIfAbsent(chunkKey(chunkX, chunkZ), (k) -> {
            return new BitSet();
        });
        List<String> newNames = new ArrayList();
        List<int[]> newInfos = new ArrayList();

        for(int i = 0; i < emitterNames.size(); ++i) {
            if (!this.emissionSent.contains(emitterNames.get(i))) {
                newNames.add((String)emitterNames.get(i));
                newInfos.add((int[])emitterInfos.get(i));
            }
        }

        if (!newNames.isEmpty()) {
            this.connection.send(BridgeProtocol.emission(newNames, newInfos));
            this.emissionSent.addAll(newNames);
        }

        this.connection.send(BridgeProtocol.section(chunkX, sectionY, chunkZ, ++this.revision & Integer.MAX_VALUE | (isUrgent ? Integer.MIN_VALUE : 0), palette, indices, this.worldTints(chunkX, sectionY, chunkZ, palette, paletteStates, indices)));
        sent.set(index);
    }

    private int[] worldTints(int chunkX, int sectionY, int chunkZ, List<String> palette, List<BlockState> paletteStates, short[] indices) {
        int[][] tintsOf = new int[palette.size()][];
        boolean any = false;

        for(int id = 1; id < palette.size(); ++id) {
            tintsOf[id] = this.export.worldTints((String)palette.get(id));
            any |= tintsOf[id] != null;
        }

        if (any && this.level != null) {
            BlockColors colors = Minecraft.getInstance().getBlockColors();
            int[] out = new int[256];
            int count = 0;
            BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

            for(int y = 0; y < 16; ++y) {
                for(int z = 0; z < 16; ++z) {
                    for(int x = 0; x < 16; ++x) {
                        int id = indices[((y + 1) * 18 + z + 1) * 18 + x + 1];
                        int[] tints = id > 0 ? tintsOf[id] : null;
                        if (tints != null) {
                            BlockState state = (BlockState)paletteStates.get(id);
                            position.set((chunkX << 4) + x, (sectionY << 4) + y, (chunkZ << 4) + z);
                            int[] var19 = tints;
                            int var20 = tints.length;

                            for(int var21 = 0; var21 < var20; ++var21) {
                                int tint = var19[var21];
                                if (count + 2 > out.length) {
                                    out = Arrays.copyOf(out, out.length * 2);
                                }

                                out[count++] = x + 16 * z + 256 * y << 8 | tint;
                                out[count++] = colors.getTintSource(state, tint).colorInWorld(state, this.level, position) & 16777215;
                            }
                        }
                    }
                }
            }

            return Arrays.copyOf(out, count);
        } else {
            return new int[0];
        }
    }

    private static record Event(int kind, long key) {
        private Event(int kind, long key) {
            this.kind = kind;
            this.key = key;
        }

        public int kind() {
            return this.kind;
        }

        public long key() {
            return this.key;
        }
    }

    private static record Deferred(boolean urgent, int chunkX, int sectionY, int chunkZ, int index, List<String> palette, List<BlockState> paletteStates, short[] indices, List<String> emitterNames, List<int[]> emitterInfos, List<String> waiting) {
        private Deferred(boolean urgent, int chunkX, int sectionY, int chunkZ, int index, List<String> palette, List<BlockState> paletteStates, short[] indices, List<String> emitterNames, List<int[]> emitterInfos, List<String> waiting) {
            this.urgent = urgent;
            this.chunkX = chunkX;
            this.sectionY = sectionY;
            this.chunkZ = chunkZ;
            this.index = index;
            this.palette = palette;
            this.paletteStates = paletteStates;
            this.indices = indices;
            this.emitterNames = emitterNames;
            this.emitterInfos = emitterInfos;
            this.waiting = waiting;
        }

        public boolean urgent() {
            return this.urgent;
        }

        public int chunkX() {
            return this.chunkX;
        }

        public int sectionY() {
            return this.sectionY;
        }

        public int chunkZ() {
            return this.chunkZ;
        }

        public int index() {
            return this.index;
        }

        public List<String> palette() {
            return this.palette;
        }

        public List<BlockState> paletteStates() {
            return this.paletteStates;
        }

        public short[] indices() {
            return this.indices;
        }

        public List<String> emitterNames() {
            return this.emitterNames;
        }

        public List<int[]> emitterInfos() {
            return this.emitterInfos;
        }

        public List<String> waiting() {
            return this.waiting;
        }
    }
}
