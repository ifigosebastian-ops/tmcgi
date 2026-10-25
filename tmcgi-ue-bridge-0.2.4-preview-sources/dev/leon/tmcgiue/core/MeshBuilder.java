//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue.core;

import java.util.Arrays;

public final class MeshBuilder {
    private final int maxVertices;
    private float[] positions = new float[192];
    private float[] uvs = new float[128];
    private int[] colors = new int[64];
    private int count;
    private boolean triangles;
    private boolean overflow;

    public MeshBuilder(int maxVertices) {
        this.maxVertices = maxVertices;
    }

    public void begin(boolean triangles) {
        this.triangles = triangles;
        this.count = 0;
        this.overflow = false;
    }

    public void vertex(float x, float y, float z) {
        if (this.count >= this.maxVertices) {
            this.overflow = true;
        } else {
            if (this.count == this.colors.length) {
                this.positions = Arrays.copyOf(this.positions, this.positions.length * 2);
                this.uvs = Arrays.copyOf(this.uvs, this.uvs.length * 2);
                this.colors = Arrays.copyOf(this.colors, this.colors.length * 2);
            }

            this.positions[3 * this.count] = x;
            this.positions[3 * this.count + 1] = y;
            this.positions[3 * this.count + 2] = z;
            this.uvs[2 * this.count] = 0.0F;
            this.uvs[2 * this.count + 1] = 0.0F;
            this.colors[this.count] = -1;
            ++this.count;
        }
    }

    public void color(int argb) {
        if (this.count > 0 && !this.overflow) {
            this.colors[this.count - 1] = argb;
        }

    }

    public void uv(float u, float v) {
        if (this.count > 0 && !this.overflow) {
            this.uvs[2 * this.count - 2] = u;
            this.uvs[2 * this.count - 1] = v;
        }

    }

    public NeutralMesh build() {
        int per = this.triangles ? 3 : 4;
        return !this.overflow && this.count != 0 && this.count % per == 0 ? new NeutralMesh(Arrays.copyOf(this.positions, 3 * this.count), Arrays.copyOf(this.uvs, 2 * this.count), Arrays.copyOf(this.colors, this.count), this.count, this.triangles) : null;
    }
}
