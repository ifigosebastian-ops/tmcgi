//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue.core;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class MeshCache {
    public static final int MAX_SHAPES = 2048;
    public static final int MAX_UPDATES_PER_FRAME = 64;
    public static final int MAX_VERTICES = 2048;
    private final Host host;
    private final Map<Long, Source> sources = new HashMap();
    private final ArrayDeque<Integer> free = new ArrayDeque();
    private int shapeCount;
    private long frame;
    private int updatesThisFrame;
    private boolean full;
    private boolean reportedFull;

    public MeshCache(Host host) {
        this.host = host;
    }

    public void beginFrame() {
        ++this.frame;
        this.updatesThisFrame = 0;
    }

    public void endFrame() {
        Iterator<Source> it = this.sources.values().iterator();

        while(it.hasNext()) {
            Source source = (Source)it.next();
            if (source.seen != this.frame) {
                this.free.add(source.id);
                it.remove();
            }
        }

    }

    public boolean takeFullReport() {
        boolean first = this.full && !this.reportedFull;
        if (first) {
            this.reportedFull = true;
        }

        return first;
    }

    public Placed place(long sourceKey, NeutralMesh mesh) {
        float ox = mesh.positions[0];
        float oy = mesh.positions[1];
        float oz = mesh.positions[2];
        long hash = hash(mesh, ox, oy, oz);
        Source source = (Source)this.sources.get(sourceKey);
        if (source == null) {
            Integer reused = (Integer)this.free.poll();
            if (reused == null && this.shapeCount >= 2048) {
                this.full = true;
                return null;
            }

            if (this.updatesThisFrame >= 64) {
                if (reused != null) {
                    this.free.addFirst(reused);
                }

                return null;
            }

            source = new Source();
            source.id = this.define(reused == null ? -1 : reused, mesh, ox, oy, oz);
            if (reused == null) {
                ++this.shapeCount;
            }

            source.hash = hash;
            this.sources.put(sourceKey, source);
        } else if (source.hash != hash && this.updatesThisFrame < 64) {
            this.define(source.id, mesh, ox, oy, oz);
            source.hash = hash;
        }

        source.seen = this.frame;
        return new Placed(source.id, ox, oy, oz, average(mesh.colors));
    }

    private int define(int id, NeutralMesh mesh, float ox, float oy, float oz) {
        ++this.updatesThisFrame;
        int quads = mesh.triangles ? mesh.vertexCount / 3 : mesh.vertexCount / 4;
        float[] data = new float[quads * 23];
        float cx = 0.0F;
        float cy = 0.0F;
        float cz = 0.0F;
        int out = 0;

        for(int quad = 0; quad < quads; ++quad) {
            int[] corner = new int[4];

            for(int i = 0; i < 4; ++i) {
                corner[i] = mesh.triangles ? quad * 3 + Math.min(i, 2) : quad * 4 + i;
            }

            float ax = mesh.positions[3 * corner[1]] - mesh.positions[3 * corner[0]];
            float ay = mesh.positions[3 * corner[1] + 1] - mesh.positions[3 * corner[0] + 1];
            float az = mesh.positions[3 * corner[1] + 2] - mesh.positions[3 * corner[0] + 2];
            float bx = mesh.positions[3 * corner[2]] - mesh.positions[3 * corner[0]];
            float by = mesh.positions[3 * corner[2] + 1] - mesh.positions[3 * corner[0] + 1];
            float bz = mesh.positions[3 * corner[2] + 2] - mesh.positions[3 * corner[0] + 2];
            float nx = ay * bz - az * by;
            float ny = az * bx - ax * bz;
            float nz = ax * by - ay * bx;
            float length = (float)Math.sqrt((double)(nx * nx + ny * ny + nz * nz));
            if (length < 1.0E-6F) {
                nx = 0.0F;
                ny = 1.0F;
                nz = 0.0F;
            } else {
                nx /= length;
                ny /= length;
                nz /= length;
            }

            data[out++] = nx;
            data[out++] = ny;
            data[out++] = nz;

            for(int i = 0; i < 4; ++i) {
                float x = mesh.positions[3 * corner[i]] - ox;
                float y = mesh.positions[3 * corner[i] + 1] - oy;
                float z = mesh.positions[3 * corner[i] + 2] - oz;
                data[out++] = x;
                data[out++] = y;
                data[out++] = z;
                data[out++] = mesh.uvs[2 * corner[i]];
                data[out++] = mesh.uvs[2 * corner[i] + 1];
                cx += x;
                cy += y;
                cz += z;
            }
        }

        float n = (float)quads * 4.0F;
        return this.host.defineShape(id, quads, data, cx / n, cy / n, cz / n);
    }

    private static long hash(NeutralMesh mesh, float ox, float oy, float oz) {
        long h = -3750763034362895579L ^ (long)(mesh.triangles ? 1 : 0);
        h = (h ^ (long)mesh.vertexCount) * 1099511628211L;

        for(int i = 0; i < mesh.vertexCount; ++i) {
            h = (h ^ (long)Math.round((mesh.positions[3 * i] - ox) * 1024.0F)) * 1099511628211L;
            h = (h ^ (long)Math.round((mesh.positions[3 * i + 1] - oy) * 1024.0F)) * 1099511628211L;
            h = (h ^ (long)Math.round((mesh.positions[3 * i + 2] - oz) * 1024.0F)) * 1099511628211L;
            h = (h ^ (long)Math.round(mesh.uvs[2 * i] * 4096.0F)) * 1099511628211L;
            h = (h ^ (long)Math.round(mesh.uvs[2 * i + 1] * 4096.0F)) * 1099511628211L;
        }

        return h;
    }

    private static int average(int[] colors) {
        long a = 0L;
        long r = 0L;
        long g = 0L;
        long b = 0L;
        int[] var9 = colors;
        int var10 = colors.length;

        for(int var11 = 0; var11 < var10; ++var11) {
            int c = var9[var11];
            a += (long)(c >>> 24);
            r += (long)(c >> 16 & 255);
            g += (long)(c >> 8 & 255);
            b += (long)(c & 255);
        }

        int n = colors.length;
        return (int)(a / (long)n) << 24 | (int)(r / (long)n) << 16 | (int)(g / (long)n) << 8 | (int)(b / (long)n);
    }

    public interface Host {
        int defineShape(int var1, int var2, float[] var3, float var4, float var5, float var6);
    }

    private static final class Source {
        int id;
        long hash;
        long seen;

        private Source() {
        }
    }

    public static record Placed(int shape, float ox, float oy, float oz, int color) {
        public Placed(int shape, float ox, float oy, float oz, int color) {
            this.shape = shape;
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
            this.color = color;
        }

        public int shape() {
            return this.shape;
        }

        public float ox() {
            return this.ox;
        }

        public float oy() {
            return this.oy;
        }

        public float oz() {
            return this.oz;
        }

        public int color() {
            return this.color;
        }
    }
}
