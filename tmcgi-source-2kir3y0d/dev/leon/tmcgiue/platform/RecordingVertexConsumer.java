//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue.platform;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.leon.tmcgiue.core.MeshBuilder;

final class RecordingVertexConsumer implements VertexConsumer {
    private final MeshBuilder builder;

    RecordingVertexConsumer(MeshBuilder builder) {
        this.builder = builder;
    }

    public VertexConsumer addVertex(float x, float y, float z) {
        this.builder.vertex(x, y, z);
        return this;
    }

    public VertexConsumer setColor(int r, int g, int b, int a) {
        this.builder.color((a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255);
        return this;
    }

    public VertexConsumer setColor(int argb) {
        this.builder.color(argb);
        return this;
    }

    public VertexConsumer setUv(float u, float v) {
        this.builder.uv(u, v);
        return this;
    }

    public VertexConsumer setUv1(int u, int v) {
        return this;
    }

    public VertexConsumer setUv2(int u, int v) {
        return this;
    }

    public VertexConsumer setUv3(float u, float v) {
        return this;
    }

    public VertexConsumer setNormal(float x, float y, float z) {
        return this;
    }

    public VertexConsumer setLineWidth(float width) {
        return this;
    }
}
