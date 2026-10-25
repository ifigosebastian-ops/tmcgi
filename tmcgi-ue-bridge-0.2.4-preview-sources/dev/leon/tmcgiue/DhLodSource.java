//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class DhLodSource {
    static final int NONE = -32768;
    static final int MAX_SEGMENTS = 3;
    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();
    private Object repo;
    private Object world;
    private Object configs;
    private Method column;
    private boolean columnWithCache;
    private Method cacheFactory;
    private Field payload;
    private Field pointState;
    private Field pointTop;
    private Field pointBottom;
    private Method isAir;
    private Method isSolid;
    private Method isLiquid;
    private Method serial;
    private Method wrapped;
    private boolean missing;

    DhLodSource() {
    }

    boolean missing() {
        return this.missing;
    }

    Level find(String dimension) {
        try {
            if (!this.bind()) {
                return null;
            } else if (!(Boolean)call(this.world, "worldLoaded")) {
                return null;
            } else {
                Object graphics = call(this.configs, "graphics");
                if (!(Boolean)call(call(graphics, "renderingEnabled"), "getValue")) {
                    return null;
                } else {
                    int distance = (Integer)call(call(graphics, "chunkRenderDistance"), "getValue");
                    Object found = null;
                    Object fallback = null;
                    Iterator var6 = ((Iterable)call(this.world, "getAllLoadedLevelWrappers")).iterator();

                    while(var6.hasNext()) {
                        Object candidate = var6.next();
                        if (dimension.equals(String.valueOf(call(candidate, "getDimensionName")))) {
                            if (String.valueOf(call(candidate, "getLevelType")).contains("SERVER")) {
                                found = candidate;
                                break;
                            }

                            fallback = candidate;
                        }
                    }

                    Object level = found != null ? found : fallback;
                    return level != null && !(Boolean)call(level, "hasCeiling") ? new Level(level, distance) : null;
                }
            }
        } catch (RuntimeException | ReflectiveOperationException var8) {
            return null;
        }
    }

    Object newCache() {
        try {
            return this.cacheFactory == null ? null : this.cacheFactory.invoke(this.repo);
        } catch (RuntimeException | ReflectiveOperationException var2) {
            return null;
        }
    }

    boolean read(Level level, Object cache, int x, int z, Cell out) throws ReflectiveOperationException {
        out.reset();
        Object result = this.columnWithCache ? this.column.invoke(this.repo, level.wrapper(), x, z, cache) : this.column.invoke(this.repo, level.wrapper(), x, z);
        if (result == null) {
            out.why = 1;
            return false;
        } else {
            if (this.payload == null) {
                this.payload = result.getClass().getField("payload");
            }

            Object points = this.payload.get(result);
            if (points == null) {
                out.why = 2;
                return false;
            } else {
                int count = Array.getLength(points);
                if (count == 0) {
                    out.why = 3;
                }

                String waterSerial = null;
                Object loose = null;
                int looseTop = -32768;
                String looseSerial = null;
                Object waterState = null;

                for(int i = 0; i < count; ++i) {
                    Object point = Array.get(points, i);
                    if (this.pointState == null) {
                        this.pointState = point.getClass().getField("blockStateWrapper");
                        this.pointTop = point.getClass().getField("topYBlockPos");
                        this.pointBottom = point.getClass().getField("bottomYBlockPos");
                    }

                    Object state = this.pointState.get(point);
                    if (state != null) {
                        if (this.isAir == null) {
                            this.isAir = method(state, "isAir");
                            this.isSolid = method(state, "isSolid");
                            this.isLiquid = method(state, "isLiquid");
                            this.serial = method(state, "getSerialString");
                            this.wrapped = method(state, "getWrappedMcObject");
                        }

                        if ((Boolean)this.isAir.invoke(state)) {
                            if (out.why == 0 || out.why == 3) {
                                out.why = 4;
                            }
                        } else {
                            int top = (Integer)this.pointTop.get(point);
                            String name = (String)this.serial.invoke(state);
                            boolean lava = name.contains("lava");
                            if ((Boolean)this.isLiquid.invoke(state) && !lava) {
                                if (out.waterY == -32768 && out.segments == 0) {
                                    out.waterY = top;
                                    waterSerial = name;
                                    waterState = state;
                                }
                            } else if (!(Boolean)this.isSolid.invoke(state) && !lava) {
                                out.why = 5;
                                if (loose == null) {
                                    loose = state;
                                    looseTop = (Integer)this.pointTop.get(point);
                                    looseSerial = name;
                                }

                                if (SEEN.size() < 40 && SEEN.add(name)) {
                                    TmcgiUeBridgeClient.LOGGER.info("Far terrain: not solid: {} (liquid {})", name, this.isLiquid.invoke(state));
                                }
                            } else {
                                int bottom = (Integer)this.pointBottom.get(point);
                                int var10000;
                                if (out.segments < 3) {
                                    int var10002 = out.segments;
                                    var10000 = var10002;
                                    out.segments = var10002 + 1;
                                } else {
                                    var10000 = 2;
                                }

                                int slot = var10000;
                                out.segTop[slot] = top;
                                out.segBottom[slot] = bottom;
                                out.segSerial[slot] = name;
                                out.segState[slot] = state;
                                if (slot == 0) {
                                    out.groundY = top;
                                    out.groundSerial = name;
                                    out.groundState = state;
                                }
                            }
                        }
                    }
                }

                if (out.segments > 0) {
                    return true;
                } else if (out.waterY != -32768) {
                    out.groundY = out.waterY;
                    out.groundSerial = waterSerial;
                    out.groundState = waterState;
                    out.waterY = -32768;
                    out.segments = 1;
                    out.segTop[0] = out.groundY;
                    out.segBottom[0] = out.groundY - 1;
                    out.segSerial[0] = out.groundSerial;
                    out.segState[0] = out.groundState;
                    return true;
                } else if (loose != null) {
                    out.groundY = looseTop;
                    out.groundSerial = looseSerial;
                    out.groundState = loose;
                    out.segments = 1;
                    out.segTop[0] = looseTop;
                    out.segBottom[0] = looseTop - 1;
                    out.segSerial[0] = looseSerial;
                    out.segState[0] = loose;
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    Object unwrap(Object state) throws ReflectiveOperationException {
        return this.wrapped.invoke(state);
    }

    private boolean bind() throws ReflectiveOperationException {
        if (this.repo != null && this.world != null && this.configs != null) {
            return true;
        } else if (this.missing) {
            return false;
        } else {
            Class delayed;
            try {
                Class.forName("com.seibel.distanthorizons.api.DhApi");
                delayed = Class.forName("com.seibel.distanthorizons.api.DhApi$Delayed");
            } catch (ClassNotFoundException var9) {
                this.missing = true;
                return false;
            }

            Object terrain = delayed.getField("terrainRepo").get((Object)null);
            Object worldProxy = delayed.getField("worldProxy").get((Object)null);
            Object config = delayed.getField("configs").get((Object)null);
            if (terrain != null && worldProxy != null && config != null) {
                Method[] var5 = terrain.getClass().getMethods();
                int var6 = var5.length;

                for(int var7 = 0; var7 < var6; ++var7) {
                    Method method = var5[var7];
                    if (method.getName().equals("getColumnDataAtBlockPos")) {
                        if (method.getParameterCount() == 4) {
                            this.column = method;
                            this.columnWithCache = true;
                        } else if (method.getParameterCount() == 3 && this.column == null) {
                            this.column = method;
                        }
                    } else if (method.getParameterCount() == 0 && (method.getName().equals("createSoftCache") || method.getName().equals("getSoftCache"))) {
                        this.cacheFactory = method;
                    }
                }

                if (this.column == null) {
                    this.missing = true;
                    TmcgiUeBridgeClient.LOGGER.warn("Distant Horizons is installed, but its terrain data API has no column read: the far terrain is not shown");
                    return false;
                } else {
                    this.column.setAccessible(true);
                    if (this.cacheFactory != null) {
                        this.cacheFactory.setAccessible(true);
                    }

                    this.repo = terrain;
                    this.world = worldProxy;
                    this.configs = config;
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    private static Method method(Object target, String name) throws NoSuchMethodException {
        Method[] var2 = target.getClass().getMethods();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            Method method = var2[var4];
            if (method.getName().equals(name) && method.getParameterCount() == 0) {
                method.setAccessible(true);
                return method;
            }
        }

        String var10002 = target.getClass().getName();
        throw new NoSuchMethodException(var10002 + "." + name);
    }

    private static Object call(Object target, String name) throws ReflectiveOperationException {
        return method(target, name).invoke(target);
    }

    static record Level(Object wrapper, int distanceChunks) {
        Level(Object wrapper, int distanceChunks) {
            this.wrapper = wrapper;
            this.distanceChunks = distanceChunks;
        }

        public Object wrapper() {
            return this.wrapper;
        }

        public int distanceChunks() {
            return this.distanceChunks;
        }
    }

    static final class Cell {
        int groundY = -32768;
        int waterY = -32768;
        String groundSerial;
        Object groundState;
        int segments;
        final int[] segTop = new int[3];
        final int[] segBottom = new int[3];
        final String[] segSerial = new String[3];
        final Object[] segState = new Object[3];
        int why;

        Cell() {
        }

        void reset() {
            this.why = 0;
            this.groundY = -32768;
            this.waterY = -32768;
            this.groundSerial = null;
            this.groundState = null;
            this.segments = 0;
        }

        void copyFrom(Cell other) {
            this.groundY = other.groundY;
            this.waterY = other.waterY;
            this.groundSerial = other.groundSerial;
            this.groundState = other.groundState;
            this.segments = other.segments;

            for(int i = 0; i < other.segments; ++i) {
                this.segTop[i] = other.segTop[i];
                this.segBottom[i] = other.segBottom[i];
                this.segSerial[i] = other.segSerial[i];
                this.segState[i] = other.segState[i];
            }

        }
    }
}
