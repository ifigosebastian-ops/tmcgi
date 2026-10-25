//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;

final class LodManager {
    private static final int CELLS = 64;
    private static final int MAX_LEVEL = 6;
    private static final int MIN_CELL = 1;
    private static final int STACKED_CELL = 4;
    private static final int MAX_TILES = 220;
    private static final double SPLIT = 2.5;
    private static final int HOLE_MARGIN = 5;
    private static final long JOB_TIMEOUT_MILLIS = 60000L;
    private static final long FAILED_RETRY_MILLIS = 30000L;
    private static final int JOBS = 2;
    private volatile int loadedLogged;
    private final BridgeConnection connection;
    private final SectionTracker tracker;
    private final BridgeConfig config;
    private final DhLodSource source = new DhLodSource();
    private final LodPalette palette = new LodPalette();
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(daemon("TMCGI-LOD"));
    private final ExecutorService jobs = Executors.newFixedThreadPool(2, daemon("TMCGI-LOD-Read"));
    private final Map<Key, Loaded> loaded = new ConcurrentHashMap();
    private final Map<Key, Future<?>> running = new ConcurrentHashMap();
    private final Map<Key, Long> runningSince = new ConcurrentHashMap();
    private final Map<Key, Long> failedAt = new ConcurrentHashMap();
    private volatile Snapshot snapshot;
    private volatile boolean stopped;
    private String dimension;
    private DhLodSource.Level level;
    private long lastFind;
    private int generation = -1;
    private int overflows;
    private List<Key> desired = List.of();
    private double plannedX = Double.NaN;
    private double plannedZ;
    private int plannedRadius = -1;
    private int plannedLod = -1;
    private int sentChunkX = Integer.MIN_VALUE;
    private int sentChunkZ;
    private int sentRadius = -1;
    private int sentLod = -1;
    private boolean viewEnabled;
    private long lastView;
    private boolean announced;
    private volatile int loggedTiles;

    LodManager(BridgeConnection connection, SectionTracker tracker, BridgeConfig config) {
        this.connection = connection;
        this.tracker = tracker;
        this.config = config;
        if (config.lod) {
            this.worker.scheduleWithFixedDelay(this::stepSafely, 1000L, 250L, TimeUnit.MILLISECONDS);
        }

    }

    private static ThreadFactory daemon(String name) {
        return (runnable) -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }

    void stop() {
        this.stopped = true;
        this.worker.shutdownNow();
        this.jobs.shutdownNow();
    }

    void tick(Minecraft client) {
        if (this.config.lod && client.level != null && client.player != null) {
            double x = client.player.getX();
            double z = client.player.getZ();
            this.snapshot = new Snapshot(x, z, Math.floorDiv((int)Math.floor(x), 16), Math.floorDiv((int)Math.floor(z), 16), this.tracker.radius(), client.level.dimension().identifier().toString());
        } else {
            this.snapshot = null;
        }
    }

    private void stepSafely() {
        try {
            this.step();
        } catch (RuntimeException var2) {
            RuntimeException e = var2;
            TmcgiUeBridgeClient.LOGGER.warn("The far terrain failed", e);
        }

    }

    private void step() {
        if (!this.stopped && !this.source.missing()) {
            Snapshot now = this.snapshot;
            if (now != null && this.connection.isConnected()) {
                if (!now.dimension().equals(this.dimension)) {
                    this.forget();
                    this.dimension = now.dimension();
                    this.level = null;
                }

                long time = System.currentTimeMillis();
                if (this.level == null || time - this.lastFind > 2000L) {
                    this.lastFind = time;
                    DhLodSource.Level found = this.source.find(this.dimension);
                    if (found == null && this.level != null) {
                        this.forget();
                    }

                    this.level = found;
                }

                if (this.level != null) {
                    if (!this.announced) {
                        this.announced = true;
                        TmcgiUeBridgeClient.LOGGER.info("Distant Horizons found: the far terrain is shown beyond the chunks that are sent as sections");
                    }

                    int lodRadius = Math.min(this.level.distanceChunks(), this.config.lodMaxChunks);
                    int sectionRadius = now.radius();
                    if (lodRadius <= sectionRadius) {
                        if (this.viewEnabled) {
                            this.forget();
                        }

                    } else {
                        this.resendIfLost();
                        this.sendView(now, sectionRadius, lodRadius, time);
                        if (Double.isNaN(this.plannedX) || Math.hypot(now.x() - this.plannedX, now.z() - this.plannedZ) > 32.0 || this.plannedRadius != sectionRadius || this.plannedLod != lodRadius) {
                            this.desired = this.plan(now, sectionRadius, lodRadius);
                            this.plannedX = now.x();
                            this.plannedZ = now.z();
                            this.plannedRadius = sectionRadius;
                            this.plannedLod = lodRadius;
                        }

                        this.startLoads(now, time);
                        this.removeObsolete();
                    }
                }
            } else {
                if (this.viewEnabled && now == null) {
                    this.forget();
                }

            }
        }
    }

    private void resendIfLost() {
        int currentGeneration = this.connection.generation();
        int currentOverflows = this.connection.overflows();
        if (currentGeneration != this.generation || currentOverflows != this.overflows) {
            this.generation = currentGeneration;
            this.overflows = currentOverflows;
            this.sentRadius = -1;
            this.connection.send(BridgeProtocol.lodView(false, 0, 0, 0, 0));
            this.viewEnabled = false;
            this.palette.resend();
            this.sendPalette();
            Iterator var3 = this.loaded.entrySet().iterator();

            while(var3.hasNext()) {
                Map.Entry<Key, Loaded> entry = (Map.Entry)var3.next();
                byte[] frame = ((Loaded)entry.getValue()).frame;
                if (frame != null) {
                    this.connection.send(frame);
                }
            }

        }
    }

    private void sendView(Snapshot now, int sectionRadius, int lodRadius, long time) {
        boolean changed = !this.viewEnabled || now.chunkX() != this.sentChunkX || now.chunkZ() != this.sentChunkZ || sectionRadius != this.sentRadius || lodRadius != this.sentLod;
        if (changed && (!this.viewEnabled || time - this.lastView >= 400L || sectionRadius != this.sentRadius)) {
            this.lastView = time;
            this.viewEnabled = true;
            this.sentChunkX = now.chunkX();
            this.sentChunkZ = now.chunkZ();
            this.sentRadius = sectionRadius;
            this.sentLod = lodRadius;
            this.connection.send(BridgeProtocol.lodView(true, this.sentChunkX, this.sentChunkZ, sectionRadius, lodRadius));
        }
    }

    private void forget() {
        Iterator var1 = this.running.values().iterator();

        while(var1.hasNext()) {
            Future<?> job = (Future)var1.next();
            job.cancel(true);
        }

        this.running.clear();
        this.runningSince.clear();
        this.loaded.clear();
        this.failedAt.clear();
        this.desired = List.of();
        this.plannedX = Double.NaN;
        this.plannedRadius = -1;
        if (this.viewEnabled) {
            this.viewEnabled = false;
            this.connection.send(BridgeProtocol.lodView(false, 0, 0, 0, 0));
        }

        this.sentRadius = -1;
    }

    private List<Key> plan(Snapshot now, int sectionRadius, int lodRadius) {
        double split = 2.5;
        List<Key> leaves = new ArrayList();

        for(int attempt = 0; attempt < 8; ++attempt) {
            leaves.clear();
            int rootSpan = (new Key(6, 0, 0)).span();
            double reach = (double)lodRadius * 16.0;
            int firstX = (int)Math.floor((now.x() - reach) / (double)rootSpan);
            int lastX = (int)Math.floor((now.x() + reach) / (double)rootSpan);
            int firstZ = (int)Math.floor((now.z() - reach) / (double)rootSpan);
            int lastZ = (int)Math.floor((now.z() + reach) / (double)rootSpan);

            for(int x = firstX; x <= lastX; ++x) {
                for(int z = firstZ; z <= lastZ; ++z) {
                    this.expand(new Key(6, x, z), now, sectionRadius, reach, split, leaves);
                }
            }

            if (leaves.size() <= 220) {
                break;
            }

            split *= 0.8;
        }

        return leaves;
    }

    private void expand(Key tile, Snapshot now, int sectionRadius, double reach, double split, List<Key> leaves) {
        double minX = (double)tile.minX();
        double minZ = (double)tile.minZ();
        double maxX = minX + (double)tile.span();
        double maxZ = minZ + (double)tile.span();
        if (!(minX > now.x() + reach) && !(maxX < now.x() - reach) && !(minZ > now.z() + reach) && !(maxZ < now.z() - reach)) {
            int deep = Math.max(0, sectionRadius - 5);
            double holeMinX = (double)(now.chunkX() - deep) * 16.0;
            double holeMaxX = (double)(now.chunkX() + deep + 1) * 16.0;
            double holeMinZ = (double)(now.chunkZ() - deep) * 16.0;
            double holeMaxZ = (double)(now.chunkZ() + deep + 1) * 16.0;
            if (!(minX >= holeMinX) || !(maxX <= holeMaxX) || !(minZ >= holeMinZ) || !(maxZ <= holeMaxZ)) {
                double dx = Math.max(Math.max(minX - now.x(), now.x() - maxX), 0.0);
                double dz = Math.max(Math.max(minZ - now.z(), now.z() - maxZ), 0.0);
                double reachSplit = tile.level == 1 ? (double)tile.span() * 0.9 : (double)tile.span() * split;
                if (tile.level > 0 && Math.hypot(dx, dz) < reachSplit) {
                    for(int cx = 0; cx < 2; ++cx) {
                        for(int cz = 0; cz < 2; ++cz) {
                            this.expand(new Key(tile.level - 1, tile.x * 2 + cx, tile.z * 2 + cz), now, sectionRadius, reach, split, leaves);
                        }
                    }

                } else {
                    leaves.add(tile);
                }
            }
        }
    }

    private static boolean overlaps(Key a, Key b) {
        return a.minX() < b.minX() + b.span() && b.minX() < a.minX() + a.span() && a.minZ() < b.minZ() + b.span() && b.minZ() < a.minZ() + a.span();
    }

    private void startLoads(Snapshot now, long time) {
        Iterator var4 = this.runningSince.entrySet().iterator();

        while(var4.hasNext()) {
            Map.Entry<Key, Long> entry = (Map.Entry)var4.next();
            if (time - (Long)entry.getValue() > 60000L) {
                Future<?> job = (Future)this.running.remove(entry.getKey());
                this.runningSince.remove(entry.getKey());
                this.failedAt.put((Key)entry.getKey(), time);
                if (job != null) {
                    job.cancel(true);
                }

                TmcgiUeBridgeClient.LOGGER.warn("Distant Horizons did not return the far terrain of {} in time", entry.getKey());
            }
        }

        if (this.running.size() < 2) {
            List<Key> wanted = new ArrayList();
            Iterator var11 = this.desired.iterator();

            while(true) {
                Long failed;
                Key key;
                do {
                    do {
                        do {
                            if (!var11.hasNext()) {
                                if (wanted.isEmpty()) {
                                    var11 = this.desired.iterator();

                                    while(var11.hasNext()) {
                                        key = (Key)var11.next();
                                        Loaded tile = (Loaded)this.loaded.get(key);
                                        long every = key.level <= 1 ? 20000L : 60000L;
                                        if (tile != null && time - tile.at > every && !this.running.containsKey(key)) {
                                            wanted.add(key);
                                        }
                                    }
                                }

                                wanted.sort(Comparator.comparingDouble((keyx) -> {
                                    return distance(now, keyx);
                                }));
                                var11 = wanted.iterator();

                                while(var11.hasNext()) {
                                    key = (Key)var11.next();
                                    if (this.running.size() >= 2) {
                                        return;
                                    }

                                    this.runningSince.put(key, time);
                                    DhLodSource.Level target = this.level;
                                    this.running.put(key, this.jobs.submit(() -> {
                                        try {
                                            this.load(key, target);
                                        } catch (InterruptedException var8) {
                                            Thread.currentThread().interrupt();
                                        } catch (Throwable var9) {
                                            Throwable t = var9;
                                            this.failedAt.put(key, System.currentTimeMillis());
                                            TmcgiUeBridgeClient.LOGGER.warn("The far terrain of {} could not be read", key, t);
                                        } finally {
                                            this.running.remove(key);
                                            this.runningSince.remove(key);
                                        }

                                    }));
                                }

                                return;
                            }

                            key = (Key)var11.next();
                            failed = (Long)this.failedAt.get(key);
                        } while(this.loaded.containsKey(key));
                    } while(this.running.containsKey(key));
                } while(failed != null && time - failed <= 30000L);

                wanted.add(key);
            }
        }
    }

    private static double distance(Snapshot now, Key key) {
        double dx = Math.max(Math.max((double)key.minX() - now.x(), now.x() - (double)(key.minX() + key.span())), 0.0);
        double dz = Math.max(Math.max((double)key.minZ() - now.z(), now.z() - (double)(key.minZ() + key.span())), 0.0);
        return Math.hypot(dx, dz);
    }

    private void removeObsolete() {
        Set<Key> wanted = new HashSet(this.desired);
        Iterator var2 = (new ArrayList(this.loaded.keySet())).iterator();

        while(true) {
            Key key;
            do {
                if (!var2.hasNext()) {
                    return;
                }

                key = (Key)var2.next();
            } while(wanted.contains(key));

            boolean covered = true;
            Iterator var5 = this.desired.iterator();

            while(var5.hasNext()) {
                Key other = (Key)var5.next();
                if (overlaps(key, other) && !this.loaded.containsKey(other)) {
                    covered = false;
                    break;
                }
            }

            if (covered) {
                this.loaded.remove(key);
                this.connection.send(BridgeProtocol.lodTileRemove(key.cell(), key.minX(), key.minZ(), 64));
            }
        }
    }

    private static short clamp(int value) {
        return (short)Math.max(-32767, Math.min(32767, value));
    }

    private void load(Key key, DhLodSource.Level target) throws Exception {
        int cells = 64;
        int cell = key.cell();
        int K = true;
        TileData data = new TileData(cells);
        String[] names = new String[cells * cells * 3];
        Map<String, Object> unknown = new HashMap();
        Object cache = this.source.newCache();
        DhLodSource.Cell sample = new DhLodSource.Cell();
        DhLodSource.Cell best = new DhLodSource.Cell();
        int[] why = new int[6];
        int[] offsets = cell >= 4 ? new int[]{cell / 4, cell * 3 / 4} : new int[]{cell / 2};
        boolean stacked = cell <= 4;
        long started = System.nanoTime();

        int i;
        int missing;
        int cx;
        int cz;
        int i;
        for(i = 0; i < cells; ++i) {
            for(missing = 0; missing < cells; ++missing) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }

                best.reset();
                boolean found = false;
                int[] var20 = offsets;
                cx = offsets.length;

                for(cz = 0; cz < cx; ++cz) {
                    int oz = var20[cz];
                    int[] var24 = offsets;
                    int var25 = offsets.length;

                    for(int var26 = 0; var26 < var25; ++var26) {
                        int ox = var24[var26];
                        if (this.source.read(target, cache, key.minX() + missing * cell + ox, key.minZ() + i * cell + oz, sample) && (!found || sample.groundY > best.groundY)) {
                            found = true;
                            best.copyFrom(sample);
                        }
                    }
                }

                i = i * cells + missing;
                if (!found) {
                    ++why[Math.min(5, sample.why)];
                    data.segs[i] = 0;
                    data.water[i] = Short.MIN_VALUE;
                } else {
                    data.water[i] = best.waterY == -32768 ? Short.MIN_VALUE : clamp(best.waterY);
                    cx = stacked ? best.segments : 1;
                    data.segs[i] = (byte)cx;

                    for(cz = 0; cz < cx; ++cz) {
                        data.top[i * 3 + cz] = clamp(best.segTop[cz]);
                        data.bottom[i * 3 + cz] = cz == cx - 1 ? Short.MIN_VALUE : clamp(best.segBottom[cz]);
                        names[i * 3 + cz] = best.segSerial[cz];
                        if (this.palette.idOf(best.segSerial[cz]) < 0) {
                            unknown.putIfAbsent(best.segSerial[cz], best.segState[cz]);
                        }
                    }
                }
            }
        }

        if (!unknown.isEmpty()) {
            this.resolveColours(unknown);
        }

        int missingOnBorder;
        for(i = 0; i < cells * cells; ++i) {
            int count = data.segs[i];

            for(missingOnBorder = 0; missingOnBorder < count; ++missingOnBorder) {
                i = this.palette.idOf(names[i * 3 + missingOnBorder]);
                if (i < 0) {
                    data.segs[i] = (byte)missingOnBorder;
                    if (missingOnBorder > 0) {
                        data.bottom[i * 3 + missingOnBorder - 1] = Short.MIN_VALUE;
                    }
                    break;
                }

                data.id[i * 3 + missingOnBorder] = (short)i;
            }
        }

        if (this.loadedLogged < 40) {
            ++this.loadedLogged;
            TmcgiUeBridgeClient.LOGGER.info("Far terrain {}: read in {} ms ({} columns)", new Object[]{key, (System.nanoTime() - started) / 1000000L, cells * cells * offsets.length * offsets.length});
        }

        Loaded existing = (Loaded)this.loaded.get(key);
        if (existing != null && existing.data != null) {
            for(missing = 0; missing < cells * cells; ++missing) {
                if (data.segs[missing] == 0 && existing.data.segs[missing] > 0) {
                    data.copyCell(existing.data, missing);
                }
            }
        }

        if (this.loggedTiles < 60) {
            missing = 0;
            missingOnBorder = 0;

            for(i = 0; i < cells * cells; ++i) {
                if (data.segs[i] == 0) {
                    ++missing;
                    cx = i % cells;
                    cz = i / cells;
                    if (cx == 0 || cz == 0 || cx == cells - 1 || cz == cells - 1) {
                        ++missingOnBorder;
                    }
                }
            }

            if (missing > 0) {
                ++this.loggedTiles;
                TmcgiUeBridgeClient.LOGGER.info("Far terrain {}: {} of {} cells have no data ({} of them on the border); reasons: none {}, no result {}, no points {}, empty {}, only air {}, not solid {}", new Object[]{key, missing, cells * cells, missingOnBorder, why[0], why[1], why[2], why[3], why[4], why[5]});
            }
        }

        missing = data.hash();
        long time = System.currentTimeMillis();
        if (existing != null && existing.hash == missing) {
            existing.at = time;
        } else {
            Loaded fresh = new Loaded();
            fresh.hash = missing;
            fresh.at = time;
            fresh.data = data;
            fresh.frame = BridgeProtocol.lodTile(cell, key.minX(), key.minZ(), cells, data.segs, data.water, data.top, data.bottom, data.id, 3);
            this.sendPalette();
            this.connection.send(fresh.frame);
            this.loaded.put(key, fresh);
        }
    }

    private synchronized void sendPalette() {
        List<LodPalette.Entry> fresh = this.palette.takeUnsent();
        if (!fresh.isEmpty()) {
            this.connection.send(BridgeProtocol.lodPalette(fresh));
            BlockExport exporter = this.tracker.exporter();
            List<String[]> names = new ArrayList();
            Iterator var4 = fresh.iterator();

            while(var4.hasNext()) {
                LodPalette.Entry entry = (LodPalette.Entry)var4.next();
                String top = entry.top() == null ? null : exporter.ensureTexture(entry.top());
                String side = entry.side() == null ? null : exporter.ensureTexture(entry.side());
                names.add(new String[]{top, side});
            }

            this.connection.send(BridgeProtocol.lodTextures(names, fresh));
        }
    }

    private void resolveColours(Map<String, Object> unknown) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture();
        Minecraft.getInstance().execute(() -> {
            try {
                Map.Entry entry;
                LodColors.Look look;
                for(Iterator i$ = unknown.entrySet().iterator(); i$.hasNext(); this.palette.add((String)entry.getKey(), look)) {
                    entry = (Map.Entry)i$.next();
                    look = new LodColors.Look(8421504, 0, (TextureAtlasSprite)null, 16777215, (TextureAtlasSprite)null, 16777215);

                    try {
                        Object patt0$temp = this.source.unwrap(entry.getValue());
                        if (patt0$temp instanceof BlockState state) {
                            look = LodColors.look(state);
                        }
                    } catch (RuntimeException | ReflectiveOperationException var8) {
                    }
                }

                done.complete((Object)null);
            } catch (Throwable var9) {
                Throwable t = var9;
                done.completeExceptionally(t);
            }

        });
        done.get(10L, TimeUnit.SECONDS);
    }

    private static record Snapshot(double x, double z, int chunkX, int chunkZ, int radius, String dimension) {
        private Snapshot(double x, double z, int chunkX, int chunkZ, int radius, String dimension) {
            this.x = x;
            this.z = z;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.radius = radius;
            this.dimension = dimension;
        }

        public double x() {
            return this.x;
        }

        public double z() {
            return this.z;
        }

        public int chunkX() {
            return this.chunkX;
        }

        public int chunkZ() {
            return this.chunkZ;
        }

        public int radius() {
            return this.radius;
        }

        public String dimension() {
            return this.dimension;
        }
    }

    private static final class Loaded {
        int hash;
        TileData data;
        byte[] frame;
        long at;

        private Loaded() {
        }
    }

    static record Key(int level, int x, int z) {
        Key(int level, int x, int z) {
            this.level = level;
            this.x = x;
            this.z = z;
        }

        int cell() {
            return 1 << this.level;
        }

        int span() {
            return 64 * this.cell();
        }

        int minX() {
            return this.x * this.span();
        }

        int minZ() {
            return this.z * this.span();
        }

        public int level() {
            return this.level;
        }

        public int x() {
            return this.x;
        }

        public int z() {
            return this.z;
        }
    }

    private static final class TileData {
        static final int K = 3;
        final byte[] segs;
        final short[] water;
        final short[] top;
        final short[] bottom;
        final short[] id;

        TileData(int cells) {
            this.segs = new byte[cells * cells];
            this.water = new short[cells * cells];
            this.top = new short[cells * cells * 3];
            this.bottom = new short[cells * cells * 3];
            this.id = new short[cells * cells * 3];
        }

        int hash() {
            int hash = Arrays.hashCode(this.segs);
            hash = 31 * hash + Arrays.hashCode(this.water);
            hash = 31 * hash + Arrays.hashCode(this.top);
            hash = 31 * hash + Arrays.hashCode(this.bottom);
            return 31 * hash + Arrays.hashCode(this.id);
        }

        void copyCell(TileData from, int i) {
            this.segs[i] = from.segs[i];
            this.water[i] = from.water[i];
            System.arraycopy(from.top, i * 3, this.top, i * 3, 3);
            System.arraycopy(from.bottom, i * 3, this.bottom, i * 3, 3);
            System.arraycopy(from.id, i * 3, this.id, i * 3, 3);
        }
    }
}
