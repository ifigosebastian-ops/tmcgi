//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue.core;

public final class NeutralMesh {
    public final float[] positions;
    public final float[] uvs;
    public final int[] colors;
    public final int vertexCount;
    public final boolean triangles;

    NeutralMesh(float[] positions, float[] uvs, int[] colors, int vertexCount, boolean triangles) {
        this.positions = positions;
        this.uvs = uvs;
        this.colors = colors;
        this.vertexCount = vertexCount;
        this.triangles = triangles;
    }
}
