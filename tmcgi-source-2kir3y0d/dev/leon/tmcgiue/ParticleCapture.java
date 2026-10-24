//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

final class ParticleCapture {
    private static final int MAX_QUADS = 8000;
    private static final Field ORIGINAL_IMAGE = field(SpriteContents.class, "originalImage");
    private static final Field SPRITES = field(TextureAtlas.class, "sprites");
    private static final Field ATLAS_WIDTH = field(TextureAtlas.class, "width");
    private static final Field ATLAS_HEIGHT = field(TextureAtlas.class, "height");
    private final BridgeConnection connection;
    private final SubmitNodeCollector collector = this.recorder();
    private final Map<Identifier, AbstractTexture> exported = new ConcurrentHashMap();
    private final Set<Identifier> pending = ConcurrentHashMap.newKeySet();
    private final Map<Identifier, Long> retryAt = new ConcurrentHashMap();
    private final ExecutorService worker = Executors.newSingleThreadExecutor((runnable) -> {
        Thread thread = new Thread(runnable, "TMCGI-Particle-Atlas");
        thread.setDaemon(true);
        return thread;
    });
    private final QuadRecorder quads = new QuadRecorder();
    private int generation = -1;
    private boolean sentEmpty;
    private boolean warned;
    private long logged;
    private ByteArrayOutputStream groups;
    private int groupCount;
    private int quadTotal;
    private double cameraX;
    private double cameraY;
    private double cameraZ;
    private int skyDarken;

    ParticleCapture(BridgeConnection connection) {
        this.connection = connection;
    }

    private static Field field(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException var3) {
            ReflectiveOperationException e = var3;
            throw new IllegalStateException(e);
        }
    }

    void capture(Minecraft minecraft, GameRenderer renderer, Camera camera) {
        if (this.generation != this.connection.generation()) {
            this.generation = this.connection.generation();
            this.exported.clear();
            this.pending.clear();
            this.retryAt.clear();
            this.sentEmpty = false;
        }

        ClientLevel level = minecraft.level;
        if (level != null) {
            Vec3 position = camera.position();
            this.cameraX = position.x;
            this.cameraY = position.y;
            this.cameraZ = position.z;
            this.skyDarken = level.getSkyDarken();
            this.groups = new ByteArrayOutputStream(65536);
            this.groupCount = 0;
            this.quadTotal = 0;

            try {
                LevelRenderState levelState = renderer.gameRenderState().levelRenderState;
                CameraRenderState cameraState = levelState.cameraRenderState;
                ParticlesRenderState state = levelState.particlesRenderState;
                if (state.particles.isEmpty() && cameraState.cullFrustum != null) {
                    state = new ParticlesRenderState();
                    minecraft.particleEngine.extract(state, cameraState.cullFrustum, camera, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
                }

                state.submit(this.collector, cameraState);
            } catch (RuntimeException var10) {
                RuntimeException e = var10;
                if (!this.warned) {
                    this.warned = true;
                    TmcgiUeBridgeClient.LOGGER.warn("The particles could not be recorded", e);
                }
            }

            long now = System.nanoTime();
            if (now - this.logged > 10000000000L && this.quadTotal > 0) {
                this.logged = now;
                TmcgiUeBridgeClient.LOGGER.info("Particles: {} quads in {} groups sent in this frame", this.quadTotal, this.groupCount);
            }

            if (this.groupCount == 0) {
                if (this.sentEmpty) {
                    return;
                }

                this.sentEmpty = true;
            } else {
                this.sentEmpty = false;
            }

            byte[] body = this.groups.toByteArray();
            ByteBuffer frame = ByteBuffer.allocate(33 + body.length).order(ByteOrder.LITTLE_ENDIAN);
            frame.putInt(29 + body.length).put((byte)20);
            frame.putDouble(this.cameraX).putDouble(this.cameraY).putDouble(this.cameraZ);
            frame.putInt(this.groupCount);
            frame.put(body);
            this.connection.sendParticles(frame.array());
            this.groups = null;
        }
    }

    private SubmitNodeCollector recorder() {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if (name.equals("order")) {
                return proxy;
            } else if (name.equals("submitQuadParticleGroup") && args != null && args.length == 1) {
                this.group((QuadParticleRenderState)args[0]);
                return null;
            } else {
                return method.isDefault() ? InvocationHandler.invokeDefault(proxy, method, args) : null;
            }
        };
        return (SubmitNodeCollector)Proxy.newProxyInstance(ParticleCapture.class.getClassLoader(), new Class[]{SubmitNodeCollector.class}, handler);
    }

    private void group(QuadParticleRenderState state) {
        if (this.groups != null) {
            Iterator var2 = state.layers().iterator();

            while(true) {
                SingleQuadParticle.Layer layer;
                Identifier atlas;
                int count;
                do {
                    do {
                        if (!var2.hasNext()) {
                            return;
                        }

                        layer = (SingleQuadParticle.Layer)var2.next();
                        if (this.quadTotal >= 8000) {
                            return;
                        }

                        atlas = layer.textureAtlasLocation();
                    } while(!this.atlasReady(atlas));

                    this.quads.clear();
                    state.buildLayer(layer, this.quads);
                    count = Math.min(this.quads.vertices / 4, 8000 - this.quadTotal);
                } while(count <= 0);

                byte[] name = atlas.toString().getBytes(StandardCharsets.UTF_8);
                ByteBuffer buffer = ByteBuffer.allocate(2 + name.length + 1 + 4 + count * 88).order(ByteOrder.LITTLE_ENDIAN);
                buffer.putShort((short)name.length).put(name).put((byte)(layer.translucent() ? 1 : 0)).putInt(count);
                boolean absolute = false;
                if ((double)(this.quads.x[0] * this.quads.x[0] + this.quads.y[0] * this.quads.y[0] + this.quads.z[0] * this.quads.z[0]) > square((double)this.quads.x[0] - this.cameraX) + square((double)this.quads.y[0] - this.cameraY) + square((double)this.quads.z[0] - this.cameraZ)) {
                    absolute = true;
                }

                for(int quad = 0; quad < count; ++quad) {
                    int light;
                    for(light = 0; light < 4; ++light) {
                        int i = quad * 4 + light;
                        buffer.putFloat(absolute ? (float)((double)this.quads.x[i] - this.cameraX) : this.quads.x[i]);
                        buffer.putFloat(absolute ? (float)((double)this.quads.y[i] - this.cameraY) : this.quads.y[i]);
                        buffer.putFloat(absolute ? (float)((double)this.quads.z[i] - this.cameraZ) : this.quads.z[i]);
                        buffer.putFloat(this.quads.u[i]).putFloat(this.quads.v[i]);
                    }

                    light = this.quads.light[quad * 4];
                    float block = (float)LightCoordsUtil.block(light) / 15.0F;
                    float sky = (float)Math.max(0, LightCoordsUtil.sky(light) - this.skyDarken) / 15.0F;
                    buffer.putInt(this.quads.color[quad * 4]).putFloat(Math.min(1.0F, Math.max(block, sky)));
                }

                this.groups.write(buffer.array(), 0, buffer.position());
                ++this.groupCount;
                this.quadTotal += count;
            }
        }
    }

    private static double square(double value) {
        return value * value;
    }

    private boolean atlasReady(Identifier location) {
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(location);
        if (texture == null) {
            return false;
        } else if (this.exported.get(location) == texture) {
            return true;
        } else {
            Long later = (Long)this.retryAt.get(location);
            if (later != null && System.nanoTime() - later < 0L) {
                return false;
            } else {
                if (this.pending.add(location) && texture instanceof TextureAtlas) {
                    TextureAtlas atlas = (TextureAtlas)texture;
                    this.worker.execute(() -> {
                        try {
                            if (this.exportAtlas(location, atlas)) {
                                this.exported.put(location, texture);
                                this.retryAt.remove(location);
                            } else {
                                this.retryAt.put(location, System.nanoTime() + 2000000000L);
                            }
                        } catch (LinkageError | Exception var8) {
                            Throwable e = var8;
                            if (this.retryAt.put(location, System.nanoTime() + 2000000000L) == null) {
                                TmcgiUeBridgeClient.LOGGER.warn("The particle atlas {} could not be put together (it is tried again)", location, e);
                            }
                        } finally {
                            this.pending.remove(location);
                        }

                    });
                }

                return false;
            }
        }
    }

    private boolean exportAtlas(Identifier location, TextureAtlas atlas) throws Exception {
        int width = ATLAS_WIDTH.getInt(atlas);
        int height = ATLAS_HEIGHT.getInt(atlas);
        List<TextureAtlasSprite> sprites = (List)SPRITES.get(atlas);
        if (width > 0 && height > 0 && sprites != null && !sprites.isEmpty()) {
            int copied = 0;
            NativeImage image = new NativeImage(Format.RGBA, width, height, true);

            byte[] png;
            label114: {
                boolean var24;
                try {
                    Iterator var9 = List.copyOf(sprites).iterator();

                    while(var9.hasNext()) {
                        TextureAtlasSprite sprite = (TextureAtlasSprite)var9.next();
                        SpriteContents contents = sprite.contents();
                        Object source = ORIGINAL_IMAGE.get(contents);
                        if (source instanceof NativeImage) {
                            NativeImage original = (NativeImage)source;
                            if (!original.isClosed()) {
                                int x = Math.round(sprite.getU0() * (float)width);
                                int y = Math.round(sprite.getV0() * (float)height);
                                original.copyRect(image, 0, 0, x, y, contents.width(), contents.height(), false, false);
                                ++copied;
                            }
                        }
                    }

                    if (copied != 0) {
                        Path temp = Files.createTempFile("tmcgi-atlas", ".png");

                        try {
                            image.writeToFile(temp);
                            png = Files.readAllBytes(temp);
                            break label114;
                        } finally {
                            Files.deleteIfExists(temp);
                        }
                    }

                    var24 = false;
                } catch (Throwable var22) {
                    try {
                        image.close();
                    } catch (Throwable var20) {
                        var22.addSuppressed(var20);
                    }

                    throw var22;
                }

                image.close();
                return var24;
            }

            image.close();
            byte[] name = location.toString().getBytes(StandardCharsets.UTF_8);
            ByteBuffer frame = ByteBuffer.allocate(7 + name.length + 8 + 4 + png.length).order(ByteOrder.LITTLE_ENDIAN);
            frame.putInt(frame.capacity() - 4).put((byte)21);
            frame.putShort((short)name.length).put(name).putInt(width).putInt(height).putInt(png.length).put(png);
            this.connection.send(frame.array());
            TmcgiUeBridgeClient.LOGGER.info("The particle atlas {} was sent ({} x {}, {} KB)", new Object[]{location, width, height, png.length / 1024});
            return true;
        } else {
            return false;
        }
    }

    private static final class QuadRecorder implements VertexConsumer {
        float[] x = new float[4096];
        float[] y = new float[4096];
        float[] z = new float[4096];
        float[] u = new float[4096];
        float[] v = new float[4096];
        int[] color = new int[4096];
        int[] light = new int[4096];
        int vertices;

        private QuadRecorder() {
        }

        void clear() {
            this.vertices = 0;
        }

        public VertexConsumer addVertex(float px, float py, float pz) {
            if (this.vertices == this.x.length) {
                int size = this.vertices * 2;
                this.x = Arrays.copyOf(this.x, size);
                this.y = Arrays.copyOf(this.y, size);
                this.z = Arrays.copyOf(this.z, size);
                this.u = Arrays.copyOf(this.u, size);
                this.v = Arrays.copyOf(this.v, size);
                this.color = Arrays.copyOf(this.color, size);
                this.light = Arrays.copyOf(this.light, size);
            }

            this.x[this.vertices] = px;
            this.y[this.vertices] = py;
            this.z[this.vertices] = pz;
            this.u[this.vertices] = 0.0F;
            this.v[this.vertices] = 0.0F;
            this.color[this.vertices] = -1;
            this.light[this.vertices] = 15728880;
            ++this.vertices;
            return this;
        }

        public VertexConsumer setColor(int r, int g, int b, int a) {
            return this.setColor((a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255);
        }

        public VertexConsumer setColor(int argb) {
            if (this.vertices > 0) {
                this.color[this.vertices - 1] = argb;
            }

            return this;
        }

        public VertexConsumer setUv(float pu, float pv) {
            if (this.vertices > 0) {
                this.u[this.vertices - 1] = pu;
                this.v[this.vertices - 1] = pv;
            }

            return this;
        }

        public VertexConsumer setLight(int packed) {
            if (this.vertices > 0) {
                this.light[this.vertices - 1] = packed;
            }

            return this;
        }

        public VertexConsumer setUv1(int a, int b) {
            return this;
        }

        public VertexConsumer setUv2(int a, int b) {
            if (this.vertices > 0) {
                this.light[this.vertices - 1] = (b & '\uffff') << 16 | a & '\uffff';
            }

            return this;
        }

        public VertexConsumer setUv3(float a, float b) {
            return this;
        }

        public VertexConsumer setNormal(float a, float b, float c) {
            return this;
        }

        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }
}
