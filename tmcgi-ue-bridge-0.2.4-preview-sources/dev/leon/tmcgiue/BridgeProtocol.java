//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;

final class BridgeProtocol {
    static final int VERSION = 1;
    static final int GRID = 18;
    static final int HELLO = 1;
    static final int SECTION = 2;
    static final int SECTION_REMOVE = 3;
    static final int POSE = 4;
    static final int CHUNK_REMOVE = 5;
    static final int CLEAR = 6;
    static final int UI = 7;
    static final int CURSOR = 8;
    static final int RESOLUTION = 9;
    static final int CUBE = 10;
    static final int TEXTURE = 11;
    static final int ENTITIES = 12;
    static final int WEATHER = 13;
    static final int LIGHTNING = 14;
    static final int EMISSION = 15;
    static final int BLOCK_LIGHT = 16;
    static final int HEIGHTMAP = 17;
    static final int BLOCK_TEXTURE = 18;
    static final int BLOCK_MODEL = 19;
    static final int PARTICLES = 20;
    static final int ATLAS = 21;
    static final int BLOCK_BREAK = 23;
    static final int SETTINGS = 22;
    static final int LOD_PALETTE = 24;
    static final int LOD_TILE = 25;
    static final int LOD_VIEW = 26;
    static final int LOD_TEXTURES = 27;
    static final int DYNAMIC_LIGHTS = 28;
    static final int WINDOW = 128;
    static final int STATUS = 129;

    private BridgeProtocol() {
    }

    private static ByteBuffer frame(int type, int payloadSize) {
        ByteBuffer buffer = ByteBuffer.allocate(5 + payloadSize).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(1 + payloadSize);
        buffer.put((byte)type);
        return buffer;
    }

    static byte[] hello(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = frame(1, 6 + bytes.length);
        buffer.putInt(1);
        buffer.putShort((short)bytes.length);
        buffer.put(bytes);
        return buffer.array();
    }

    static byte[] resolution(int width, int height) {
        ByteBuffer buffer = frame(9, 8);
        buffer.putInt(width).putInt(height);
        return buffer.array();
    }

    static byte[] cursor(boolean grabbed) {
        ByteBuffer buffer = frame(8, 4);
        buffer.putInt(grabbed ? 1 : 0);
        return buffer.array();
    }

    static byte[] ui(int width, int height, byte[] rgba) {
        ByteBuffer buffer = frame(7, 8 + rgba.length);
        buffer.putInt(width).putInt(height);
        buffer.put(rgba);
        return buffer.array();
    }

    static byte[] settings(Map<String, String> values) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        ByteBuffer count = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        count.putShort((short)values.size());
        body.writeBytes(count.array());
        Iterator var3 = values.entrySet().iterator();

        while(var3.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry)var3.next();
            String[] var5 = new String[]{(String)entry.getKey(), (String)entry.getValue()};
            int var6 = var5.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                String text = var5[var7];
                byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                ByteBuffer length = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
                length.putShort((short)bytes.length);
                body.writeBytes(length.array());
                body.writeBytes(bytes);
            }
        }

        byte[] payload = body.toByteArray();
        ByteBuffer buffer = frame(22, payload.length);
        buffer.put(payload);
        return buffer.array();
    }

    static byte[] dynamicLights(int[] ids, double[] positions, int[] levels, int[] colors, int count) {
        ByteBuffer buffer = frame(28, 2 + count * 33);
        buffer.putShort((short)count);

        for(int i = 0; i < count; ++i) {
            int color = colors[i];
            buffer.putInt(ids[i]).putDouble(positions[i * 3]).putDouble(positions[i * 3 + 1]).putDouble(positions[i * 3 + 2]).put((byte)levels[i]).put((byte)(color >= 0 ? 1 : 0)).put((byte)(color >> 16)).put((byte)(color >> 8)).put((byte)color);
        }

        return buffer.array();
    }

    static byte[] blockBreak(int[] blocks, int count) {
        ByteBuffer buffer = frame(23, 2 + count * 13);
        buffer.putShort((short)count);

        for(int i = 0; i < count; ++i) {
            buffer.putInt(blocks[i * 4]).putInt(blocks[i * 4 + 1]).putInt(blocks[i * 4 + 2]).put((byte)blocks[i * 4 + 3]);
        }

        return buffer.array();
    }

    static byte[] weather(float rain, float thunder, boolean snow) {
        ByteBuffer buffer = frame(13, 9);
        buffer.putFloat(rain).putFloat(thunder);
        buffer.put((byte)(snow ? 1 : 0));
        return buffer.array();
    }

    static byte[] lightning(double x, double y, double z, long seed) {
        ByteBuffer buffer = frame(14, 32);
        buffer.putDouble(x).putDouble(y).putDouble(z).putLong(seed);
        return buffer.array();
    }

    static byte[] emission(List<String> names, List<int[]> infos) {
        byte[][] encoded = new byte[names.size()][];
        int size = 2;

        for(int i = 0; i < encoded.length; ++i) {
            encoded[i] = ((String)names.get(i)).getBytes(StandardCharsets.UTF_8);
            size += 2 + encoded[i].length + 5;
        }

        ByteBuffer buffer = frame(15, size);
        buffer.putShort((short)encoded.length);

        for(int i = 0; i < encoded.length; ++i) {
            buffer.putShort((short)encoded[i].length);
            buffer.put(encoded[i]);
            int[] info = (int[])infos.get(i);
            buffer.put((byte)info[0]).put((byte)info[1]).put((byte)(info[2] >> 16)).put((byte)(info[2] >> 8)).put((byte)info[2]);
        }

        return buffer.array();
    }

    static byte[] blockTexture(String name, byte[] png, byte[] meta, byte[] normal, byte[] specular) {
        byte[] encoded = name.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = frame(18, 2 + encoded.length + 4 + png.length + 4 + meta.length + 4 + normal.length + 4 + specular.length);
        buffer.putShort((short)encoded.length).put(encoded);
        buffer.putInt(png.length).put(png);
        buffer.putInt(meta.length).put(meta);
        buffer.putInt(normal.length).put(normal);
        buffer.putInt(specular.length).put(specular);
        return buffer.array();
    }

    static byte[] blockModel(String state, int occlusion, List<float[]> boxes, List<String> textures, List<float[]> quads) {
        byte[] name = state.getBytes(StandardCharsets.UTF_8);
        byte[][] encoded = new byte[textures.size()][];
        int size = 2 + name.length + 1 + 2 + boxes.size() * 24 + 2;

        int quadCount;
        for(quadCount = 0; quadCount < encoded.length; ++quadCount) {
            encoded[quadCount] = ((String)textures.get(quadCount)).getBytes(StandardCharsets.UTF_8);
            size += 2 + encoded[quadCount].length;
        }

        quadCount = Math.min(quads.size(), 65535);
        size += 2 + quadCount * 89;
        ByteBuffer buffer = frame(19, size);
        buffer.putShort((short)name.length).put(name);
        buffer.put((byte)occlusion);
        buffer.putShort((short)boxes.size());
        Iterator var10 = boxes.iterator();

        float[] quad;
        while(var10.hasNext()) {
            quad = (float[])var10.next();
            float[] var12 = quad;
            int var13 = quad.length;

            for(int var14 = 0; var14 < var13; ++var14) {
                float value = var12[var14];
                buffer.putFloat(value);
            }
        }

        buffer.putShort((short)encoded.length);
        byte[][] var16 = encoded;
        int var18 = encoded.length;

        int j;
        for(j = 0; j < var18; ++j) {
            byte[] texture = var16[j];
            buffer.putShort((short)texture.length).put(texture);
        }

        buffer.putShort((short)quadCount);

        for(int i = 0; i < quadCount; ++i) {
            quad = (float[])quads.get(i);
            buffer.put((byte)((int)quad[0])).put((byte)((int)quad[1])).put((byte)((int)quad[2])).putShort((short)((int)quad[3])).putInt(quad[24] != 0.0F ? -16777216 | (int)quad[25] : 0);

            for(j = 4; j < 24; ++j) {
                buffer.putFloat(quad[j]);
            }
        }

        return buffer.array();
    }

    static byte[] blockLight(float red, float green, float blue) {
        ByteBuffer buffer = frame(16, 12);
        buffer.putFloat(red).putFloat(green).putFloat(blue);
        return buffer.array();
    }

    static byte[] heightmap(int chunkX, int chunkZ, int minY, short[] heights) {
        ByteBuffer buffer = frame(17, 12 + heights.length * 2);
        buffer.putInt(chunkX).putInt(chunkZ).putInt(minY);
        short[] var5 = heights;
        int var6 = heights.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            short height = var5[var7];
            buffer.putShort(height);
        }

        return buffer.array();
    }

    static byte[] lodPalette(List<LodPalette.Entry> entries) {
        ByteBuffer buffer = frame(24, 2 + entries.size() * 6);
        buffer.putShort((short)entries.size());
        Iterator var2 = entries.iterator();

        while(var2.hasNext()) {
            LodPalette.Entry entry = (LodPalette.Entry)var2.next();
            buffer.putShort((short)entry.id()).put((byte)entry.flags()).put((byte)(entry.rgb() >> 16)).put((byte)(entry.rgb() >> 8)).put((byte)entry.rgb());
        }

        return buffer.array();
    }

    static byte[] lodTextures(List<String[]> names, List<LodPalette.Entry> entries) {
        int size = 2;
        List<byte[][]> encoded = new ArrayList();

        byte[] top;
        byte[] side;
        for(Iterator var4 = names.iterator(); var4.hasNext(); size += 10 + top.length + 2 + side.length) {
            String[] pair = (String[])var4.next();
            top = pair[0] == null ? new byte[0] : pair[0].getBytes(StandardCharsets.UTF_8);
            side = pair[1] == null ? new byte[0] : pair[1].getBytes(StandardCharsets.UTF_8);
            encoded.add(new byte[][]{top, side});
        }

        ByteBuffer buffer = frame(27, size);
        buffer.putShort((short)entries.size());

        for(int i = 0; i < entries.size(); ++i) {
            LodPalette.Entry entry = (LodPalette.Entry)entries.get(i);
            byte[][] pair = (byte[][])encoded.get(i);
            buffer.putShort((short)entry.id());
            buffer.put((byte)(entry.topTint() >> 16)).put((byte)(entry.topTint() >> 8)).put((byte)entry.topTint());
            buffer.put((byte)(entry.sideTint() >> 16)).put((byte)(entry.sideTint() >> 8)).put((byte)entry.sideTint());
            buffer.putShort((short)pair[0].length).put(pair[0]);
            buffer.putShort((short)pair[1].length).put(pair[1]);
        }

        return buffer.array();
    }

    static byte[] lodTile(int cell, int x, int z, int cells, byte[] segs, short[] water, short[] top, short[] bottom, short[] id, int stride) {
        int rawSize = 0;

        for(int i = 0; i < cells * cells; ++i) {
            rawSize += 3 + segs[i] * 6;
        }

        ByteBuffer raw = ByteBuffer.allocate(rawSize).order(ByteOrder.LITTLE_ENDIAN);

        for(int i = 0; i < cells * cells; ++i) {
            raw.put(segs[i]).putShort(water[i]);

            for(int k = 0; k < segs[i]; ++k) {
                raw.putShort(top[i * stride + k]).putShort(bottom[i * stride + k]).putShort(id[i * stride + k]);
            }
        }

        Deflater deflater = new Deflater(1);
        deflater.setInput(raw.array());
        deflater.finish();
        byte[] compressed = new byte[rawSize + 64];
        int compressedSize = deflater.deflate(compressed);
        deflater.end();
        ByteBuffer buffer = frame(25, 22 + compressedSize);
        buffer.putInt(cell).putInt(x).putInt(z).put((byte)cells).put((byte)2).putInt(compressedSize).putInt(rawSize);
        buffer.put(compressed, 0, compressedSize);
        return buffer.array();
    }

    static byte[] lodTileRemove(int cell, int x, int z, int cells) {
        ByteBuffer buffer = frame(25, 18);
        buffer.putInt(cell).putInt(x).putInt(z).put((byte)cells).put((byte)1).putInt(0);
        return buffer.array();
    }

    static byte[] lodView(boolean enabled, int chunkX, int chunkZ, int sectionRadius, int lodRadius) {
        ByteBuffer buffer = frame(26, 17);
        buffer.put((byte)(enabled ? 1 : 0)).putInt(chunkX).putInt(chunkZ).putInt(sectionRadius).putInt(lodRadius);
        return buffer.array();
    }

    static byte[] clear() {
        return frame(6, 0).array();
    }

    static byte[] sectionRemove(int chunkX, int sectionY, int chunkZ) {
        ByteBuffer buffer = frame(3, 12);
        buffer.putInt(chunkX).putInt(sectionY).putInt(chunkZ);
        return buffer.array();
    }

    static byte[] chunkRemove(int chunkX, int chunkZ) {
        ByteBuffer buffer = frame(5, 8);
        buffer.putInt(chunkX).putInt(chunkZ);
        return buffer.array();
    }

    static byte[] pose(double x, double y, double z, float forwardX, float forwardY, float forwardZ, float upX, float upY, float upZ, float fov, long clock, int frame, float[] bobbing) {
        ByteBuffer buffer = frame(4, 100);
        buffer.putDouble(x).putDouble(y).putDouble(z);
        buffer.putFloat(forwardX).putFloat(forwardY).putFloat(forwardZ);
        buffer.putFloat(upX).putFloat(upY).putFloat(upZ);
        buffer.putFloat(fov);
        buffer.putLong(clock);
        buffer.putInt(frame);
        float[] var18 = bobbing;
        int var19 = bobbing.length;

        for(int var20 = 0; var20 < var19; ++var20) {
            float value = var18[var20];
            buffer.putFloat(value);
        }

        return buffer.array();
    }

    static byte[] section(int chunkX, int sectionY, int chunkZ, int revision, List<String> palette, short[] indices, int[] worldTints) {
        ByteBuffer raw = ByteBuffer.allocate(indices.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        short[] var8 = indices;
        int var9 = indices.length;

        int compressedSize;
        for(compressedSize = 0; compressedSize < var9; ++compressedSize) {
            short index = var8[compressedSize];
            raw.putShort(index);
        }

        Deflater deflater = new Deflater(1);
        deflater.setInput(raw.array());
        deflater.finish();
        byte[] compressed = new byte[raw.capacity() + 64];
        compressedSize = deflater.deflate(compressed);
        deflater.end();
        byte[][] names = new byte[palette.size()][];
        int paletteBytes = 0;

        int tintCount;
        for(tintCount = 0; tintCount < names.length; ++tintCount) {
            names[tintCount] = ((String)palette.get(tintCount)).getBytes(StandardCharsets.UTF_8);
            paletteBytes += 2 + names[tintCount].length;
        }

        tintCount = worldTints.length / 2;
        byte[] tintsCompressed = new byte[0];
        int tintsSize = 0;
        ByteBuffer buffer;
        int key;
        int color;
        if (tintCount > 0) {
            buffer = ByteBuffer.allocate(tintCount * 6).order(ByteOrder.LITTLE_ENDIAN);

            for(int i = 0; i < tintCount; ++i) {
                key = worldTints[i * 2];
                color = worldTints[i * 2 + 1];
                buffer.putShort((short)(key >>> 8)).put((byte)key).put((byte)(color >> 16)).put((byte)(color >> 8)).put((byte)color);
            }

            Deflater tintDeflater = new Deflater(1);
            tintDeflater.setInput(buffer.array());
            tintDeflater.finish();
            tintsCompressed = new byte[buffer.capacity() + 64];
            tintsSize = tintDeflater.deflate(tintsCompressed);
            tintDeflater.end();
        }

        buffer = frame(2, 18 + paletteBytes + 4 + compressedSize + 8 + tintsSize);
        buffer.putInt(chunkX).putInt(sectionY).putInt(chunkZ).putInt(revision);
        buffer.putShort((short)names.length);
        byte[][] var25 = names;
        key = names.length;

        for(color = 0; color < key; ++color) {
            byte[] name = var25[color];
            buffer.putShort((short)name.length);
            buffer.put(name);
        }

        buffer.putInt(compressedSize);
        buffer.put(compressed, 0, compressedSize);
        buffer.putInt(tintCount).putInt(tintsSize);
        buffer.put(tintsCompressed, 0, tintsSize);
        return buffer.array();
    }
}
