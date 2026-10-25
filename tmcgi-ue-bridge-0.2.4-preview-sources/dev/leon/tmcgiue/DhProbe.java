//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;

final class DhProbe {
    private static final boolean ENABLED = System.getenv("TMCGI_DH_PROBE") != null;
    private static final int ROUNDS = parseRounds(System.getenv("TMCGI_DH_PROBE"));
    private static final long INTERVAL_NANOS = 10000000000L;
    private static long next;
    private static int rounds;
    private static boolean missing;
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor((runnable) -> {
        Thread thread = new Thread(runnable, "TMCGI-DH-Probe");
        thread.setDaemon(true);
        return thread;
    });
    private static final ExecutorService READS = Executors.newCachedThreadPool((runnable) -> {
        Thread thread = new Thread(runnable, "TMCGI-DH-Read");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicInteger ABANDONED = new AtomicInteger();
    private static final long READ_TIMEOUT_MILLIS = 4000L;
    private static volatile long jobStarted;
    private static volatile boolean jobRunning;

    private DhProbe() {
    }

    private static int parseRounds(String text) {
        try {
            return text == null ? 0 : Math.max(1, Integer.parseInt(text.trim()));
        } catch (NumberFormatException var2) {
            return 12;
        }
    }

    static void tick(Minecraft client) {
        if (ENABLED && !missing && rounds < ROUNDS && client.level != null && client.player != null) {
            long now = System.nanoTime();
            if (jobRunning) {
                if (now - jobStarted > 15000000000L && now >= next) {
                    next = now + 10000000000L;
                    long var10000 = now - jobStarted;
                    log("the last read has been waiting for " + var10000 / 1000000000L + " s, the client thread goes on");
                }

            } else if (now >= next) {
                next = now + 10000000000L;
                double x = client.player.getX();
                double z = client.player.getZ();
                String dimension = client.level.dimension().identifier().toString();
                jobRunning = true;
                jobStarted = now;
                WORKER.execute(() -> {
                    try {
                        run(x, z, dimension);
                        ++rounds;
                    } catch (ClassNotFoundException var10) {
                        missing = true;
                        log("Distant Horizons is not installed");
                    } catch (Throwable var11) {
                        Throwable t = var11;
                        ++rounds;
                        TmcgiUeBridgeClient.LOGGER.warn("DH probe failed", t);
                    } finally {
                        jobRunning = false;
                    }

                });
            }
        }
    }

    private static void run(double playerX, double playerZ, String dimension) throws Exception {
        Class<?> api = Class.forName("com.seibel.distanthorizons.api.DhApi");
        Class<?> delayed = Class.forName("com.seibel.distanthorizons.api.DhApi$Delayed");
        int var10000 = rounds + 1;
        log("round " + var10000 + ": DH mod " + String.valueOf(callStatic(api, "getModVersion")) + ", API " + String.valueOf(callStatic(api, "getApiMajorVersion")) + "." + String.valueOf(callStatic(api, "getApiMinorVersion")) + "." + String.valueOf(callStatic(api, "getApiPatchVersion")));
        Object repo = delayed.getField("terrainRepo").get((Object)null);
        Object world = delayed.getField("worldProxy").get((Object)null);
        Object configs = delayed.getField("configs").get((Object)null);
        if (repo != null && world != null) {
            boolean loaded = (Boolean)call(world, "worldLoaded");
            log("  world loaded: " + loaded);
            if (loaded) {
                String var27;
                if (configs != null) {
                    Object graphics = call(configs, "graphics");
                    var27 = String.valueOf(call(call(graphics, "chunkRenderDistance"), "getValue"));
                    log("  DH render distance (chunks): " + var27 + ", rendering enabled: " + String.valueOf(call(call(graphics, "renderingEnabled"), "getValue")) + ", vertical quality: " + String.valueOf(call(call(graphics, "verticalQuality"), "getValue")) + ", horizontal quality: " + String.valueOf(call(call(graphics, "horizontalQuality"), "getValue")));
                }

                int bx = (int)Math.floor(playerX);
                int bz = (int)Math.floor(playerZ);
                List<Object> candidates = new ArrayList();
                Object single = null;

                try {
                    single = call(world, "getSinglePlayerLevel");
                    if (single != null) {
                        log("  single player level: " + describeLevel(single));
                    }
                } catch (Exception var23) {
                    Exception e = var23;
                    Object var29 = e.getCause() != null ? e.getCause() : e;
                    log("  getSinglePlayerLevel: " + String.valueOf(var29));
                }

                Iterator var25 = ((Iterable)call(world, "getAllLoadedLevelWrappers")).iterator();

                Object level;
                while(var25.hasNext()) {
                    level = var25.next();
                    boolean here = String.valueOf(call(level, "getDimensionName")).equals(dimension);
                    var27 = describeLevel(level);
                    log("  loaded level: " + var27 + (here ? " (the player's)" : ""));
                    if (here && String.valueOf(call(level, "getLevelType")).contains("SERVER")) {
                        candidates.add(level);
                    }
                }

                if (candidates.isEmpty() && single != null) {
                    candidates.add(single);
                }

                Object cache = null;
                String[] var28 = new String[]{"createSoftCache", "getSoftCache"};
                int var30 = var28.length;
                int var18 = 0;

                while(var18 < var30) {
                    String name = var28[var18];

                    try {
                        cache = call(repo, name);
                        log("  cache from " + name);
                        break;
                    } catch (NoSuchMethodException var22) {
                        ++var18;
                    }
                }

                level = null;
                Iterator var31 = candidates.iterator();

                while(var31.hasNext()) {
                    Object candidate = var31.next();
                    Object result = timedRead(repo, "getColumnDataAtBlockPos", candidate, bx, bz, cache);
                    String message = messageOf(result);
                    boolean answered = !message.contains("before the world has loaded");
                    log("  level " + describeLevel(candidate) + " answers: " + answered + " (" + message + ")");
                    if (answered && level == null) {
                        level = candidate;
                    }
                }

                if (level == null) {
                    log("  no level answers yet");
                } else {
                    log("  player at block " + bx + ", " + bz);
                    int[] var32 = rounds < 2 ? new int[]{0, 2, 4, 6, 8} : new int[]{4, 6};
                    var18 = var32.length;

                    int var35;
                    int distance;
                    for(var35 = 0; var35 < var18; ++var35) {
                        distance = var32[var35];
                        describeTile(repo, level, cache, distance, Math.floorDiv(bx, 1 << distance), Math.floorDiv(bz, 1 << distance));
                    }

                    var32 = new int[]{300, 500, 800, 1200, 2000};
                    var18 = var32.length;

                    for(var35 = 0; var35 < var18; ++var35) {
                        distance = var32[var35];
                        describeTile(repo, level, cache, 6, Math.floorDiv(bx + distance, 64), Math.floorDiv(bz, 64));
                    }

                    var32 = new int[]{0, 100, 200, 300, 400, 600, 800, 1200, 1600, 2400, 3200, 4000};
                    var18 = var32.length;

                    for(var35 = 0; var35 < var18; ++var35) {
                        distance = var32[var35];
                        describeColumn(repo, level, cache, bx + distance, bz);
                    }

                }
            }
        } else {
            log("  not ready yet (terrainRepo " + (repo != null) + ", worldProxy " + (world != null) + ")");
        }
    }

    private static void describeTile(Object repo, Object level, Object cache, int detail, int tileX, int tileZ) {
        try {
            long start = System.nanoTime();
            Object result = timedRead(repo, "getAllTerrainDataAtDetailLevelAndPos", level, (byte)detail, tileX, tileZ, cache);
            long millis = (System.nanoTime() - start) / 1000000L;
            Object payload = payloadOf(result);
            if (payload == null) {
                log("  detail " + detail + " tile (" + tileX + ", " + tileZ + "): no data (" + messageOf(result) + "), " + millis + " ms");
                return;
            }

            int width = Array.getLength(payload);
            int depth = width > 0 ? Array.getLength(Array.get(payload, 0)) : 0;
            long points = 0L;
            long solid = 0L;
            long filled = 0L;
            int most = 0;
            long[] byDetail = new long[16];
            Object sample = null;

            for(int x = 0; x < width; ++x) {
                Object row = Array.get(payload, x);

                for(int z = 0; z < Array.getLength(row); ++z) {
                    Object column = Array.get(row, z);
                    int length = column == null ? 0 : Array.getLength(column);
                    points += (long)length;
                    if (length > 0) {
                        ++filled;
                        most = Math.max(most, length);
                    }

                    for(int i = 0; i < length; ++i) {
                        Object point = Array.get(column, i);
                        ++byDetail[Math.min(15, (Byte)point.getClass().getField("detailLevel").get(point))];
                        if (!isAir(point)) {
                            ++solid;
                            if (sample == null) {
                                sample = point;
                            }
                        }
                    }
                }
            }

            StringBuilder details = new StringBuilder();

            for(int i = 0; i < byDetail.length; ++i) {
                if (byDetail[i] > 0L) {
                    details.append(" d").append(i).append('=').append(byDetail[i]);
                }
            }

            log("  detail " + detail + " tile (" + tileX + ", " + tileZ + "): " + width + " x " + depth + " columns, " + filled + " with data, " + points + " points (" + solid + " not air), at most " + most + " per column, " + millis + " ms, points by detail:" + String.valueOf(details) + (sample != null ? ", first solid: " + describePoint(sample) : ""));
        } catch (Exception var30) {
            Exception e = var30;
            log("  detail " + detail + " tile failed: " + String.valueOf(e));
        }

    }

    private static void describeColumn(Object repo, Object level, Object cache, int x, int z) {
        try {
            Object result = timedRead(repo, "getColumnDataAtBlockPos", level, x, z, cache);
            Object payload = payloadOf(result);
            if (payload == null || Array.getLength(payload) == 0) {
                log("  column at " + x + ", " + z + ": none (" + messageOf(result) + ")");
                return;
            }

            Object solid = null;
            int solidPoints = 0;

            for(int i = 0; i < Array.getLength(payload); ++i) {
                Object point = Array.get(payload, i);
                if (!isAir(point)) {
                    ++solidPoints;
                    if (solid == null) {
                        solid = point;
                    }
                }
            }

            log("  column at " + x + ", " + z + ": " + Array.getLength(payload) + " points, " + solidPoints + " not air, top solid " + (solid == null ? "none" : describePoint(solid)));
        } catch (Exception var11) {
            Exception e = var11;
            log("  column at " + x + ", " + z + " failed: " + String.valueOf(e));
        }

    }

    private static String describePoint(Object point) throws Exception {
        if (point == null) {
            return "null";
        } else {
            Class<?> type = point.getClass();
            Object state = type.getField("blockStateWrapper").get(point);
            String name = state == null ? "none" : String.valueOf(call(state, "getSerialString"));
            return String.format(Locale.ROOT, "{detail %s, y %s..%s, %s, block light %s, sky light %s}", type.getField("detailLevel").get(point), type.getField("bottomYBlockPos").get(point), type.getField("topYBlockPos").get(point), name, type.getField("blockLightLevel").get(point), type.getField("skyLightLevel").get(point));
        }
    }

    private static boolean isAir(Object point) throws Exception {
        Object state = point.getClass().getField("blockStateWrapper").get(point);
        return state == null || (Boolean)call(state, "isAir");
    }

    private static String describeLevel(Object level) throws Exception {
        String var10000 = level.getClass().getSimpleName();
        return var10000 + " " + String.valueOf(call(level, "getDimensionName")) + " (" + String.valueOf(call(level, "getLevelType")) + ")";
    }

    private static Object payloadOf(Object result) throws Exception {
        return result == null ? null : result.getClass().getField("payload").get(result);
    }

    private static String messageOf(Object result) throws Exception {
        return result == null ? "null" : String.valueOf(result.getClass().getField("message").get(result));
    }

    private static Object call(Object target, String name, Object... arguments) throws Exception {
        return invoke(target.getClass(), target, name, arguments);
    }

    private static Object timedRead(Object target, String name, Object... arguments) throws Exception {
        Future<Object> read = READS.submit(() -> {
            return call(target, name, arguments);
        });

        try {
            return read.get(4000L, TimeUnit.MILLISECONDS);
        } catch (TimeoutException var7) {
            read.cancel(true);
            ABANDONED.incrementAndGet();
            throw new TimeoutException(name + " did not return in 4000 ms (" + ABANDONED.get() + " left behind so far)");
        } catch (ExecutionException var8) {
            ExecutionException e = var8;
            Throwable var6 = e.getCause();
            Object var10000;
            if (var6 instanceof Exception cause) {
                var10000 = cause;
            } else {
                var10000 = e;
            }

            throw (Exception)var10000;
        }
    }

    private static Object callStatic(Class<?> type, String name, Object... arguments) throws Exception {
        return invoke(type, (Object)null, name, arguments);
    }

    private static Object invoke(Class<?> type, Object target, String name, Object[] arguments) throws Exception {
        Method[] var4 = type.getMethods();
        int var5 = var4.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            Method method = var4[var6];
            if (method.getName().equals(name) && method.getParameterCount() == arguments.length) {
                Class<?>[] parameters = method.getParameterTypes();
                boolean fits = true;

                for(int i = 0; i < parameters.length; ++i) {
                    Object argument = arguments[i];
                    fits &= argument == null ? !parameters[i].isPrimitive() : box(parameters[i]).isInstance(argument);
                }

                if (fits) {
                    method.setAccessible(true);
                    return method.invoke(target, arguments);
                }
            }
        }

        throw new NoSuchMethodException(type.getName() + "." + name + " with " + arguments.length + " argument(s)");
    }

    private static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        } else {
            Class var10000;
            switch (type.getName()) {
                case "int" -> var10000 = Integer.class;
                case "byte" -> var10000 = Byte.class;
                case "boolean" -> var10000 = Boolean.class;
                case "long" -> var10000 = Long.class;
                case "float" -> var10000 = Float.class;
                case "double" -> var10000 = Double.class;
                case "short" -> var10000 = Short.class;
                default -> var10000 = Character.class;
            }

            return var10000;
        }
    }

    private static void log(String text) {
        TmcgiUeBridgeClient.LOGGER.info("[DH probe] {}", text);
    }
}
