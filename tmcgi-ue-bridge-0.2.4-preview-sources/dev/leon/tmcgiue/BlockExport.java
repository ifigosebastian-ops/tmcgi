//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3fc;

final class BlockExport {
    private static final Direction[] CULLS;
    private static final Direction[] FACES;
    private static final Field ORIGINAL_IMAGE;
    private static final Set<String> FAILED;
    private final BridgeConnection connection;
    private final ExecutorService urgentLane = Executors.newSingleThreadExecutor((runnable) -> {
        return thread(runnable, "TMCGI-Export-Urgent", 5);
    });
    private final ExecutorService bulkLane = Executors.newSingleThreadExecutor((runnable) -> {
        return thread(runnable, "TMCGI-Export", 4);
    });
    private final Set<String> ready = ConcurrentHashMap.newKeySet();
    private final Map<String, int[]> worldTints = new ConcurrentHashMap();
    private final Map<String, Task> pending = new ConcurrentHashMap();
    private final Set<String> texturesSent = new HashSet();
    private final Object textureLock = new Object();
    private final Object readyLock = new Object();
    private final AtomicInteger finished = new AtomicInteger();
    private volatile int generation;
    private int models;
    private int quadsTotal;
    private int textures;
    private volatile int fluidGeneration = -1;

    private static Thread thread(Runnable runnable, String name, int priority) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        thread.setPriority(priority);
        return thread;
    }

    BlockExport(BridgeConnection connection) {
        this.connection = connection;
    }

    private void sendFluidTextures() throws ReflectiveOperationException, IOException {
        int current = this.generation;
        if (this.fluidGeneration != current) {
            synchronized(this.textureLock) {
                if (this.fluidGeneration == current) {
                    return;
                }

                this.fluidGeneration = current;
            }

            String[] var2 = new String[]{"block/lava_still"};
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                String name = var2[var4];
                this.sendTexture(name, Identifier.fromNamespaceAndPath("minecraft", name), (SpriteContents)null);
            }

        }
    }

    void reset() {
        synchronized(this.readyLock) {
            ++this.generation;
            this.ready.clear();
            this.pending.clear();
            this.worldTints.clear();
        }

        synchronized(this.textureLock) {
            this.texturesSent.clear();
        }
    }

    boolean isReady(String name) {
        return this.ready.contains(name);
    }

    int[] worldTints(String name) {
        return (int[])this.worldTints.get(name);
    }

    int finishedCount() {
        return this.finished.get();
    }

    void request(String name, BlockState state, boolean urgent) {
        if (!this.ready.contains(name)) {
            Task task = (Task)this.pending.get(name);
            Task promoted;
            if (task == null) {
                promoted = new Task(name, state, this.generation);
                Task existing = (Task)this.pending.putIfAbsent(name, promoted);
                if (existing == null) {
                    promoted.onUrgentLane = urgent;
                    (urgent ? this.urgentLane : this.bulkLane).execute(() -> {
                        promoted.run(urgent);
                    });
                    return;
                }

                task = existing;
            }

            if (urgent && !task.onUrgentLane) {
                task.onUrgentLane = true;
                promoted = task;
                this.urgentLane.execute(() -> {
                    promoted.run(true);
                });
            }

        }
    }

    private static Field originalImage() {
        try {
            Field field = SpriteContents.class.getDeclaredField("originalImage");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException var1) {
            return null;
        }
    }

    String ensureTexture(TextureAtlasSprite sprite) {
        Identifier spriteName = sprite.contents().name();
        String name = key(spriteName);

        try {
            this.sendTexture(name, spriteName, sprite.contents());
        } catch (IOException | ReflectiveOperationException var5) {
            Exception e = var5;
            TmcgiUeBridgeClient.LOGGER.warn("The texture {} of the far terrain could not be sent", name, e);
        }

        return name;
    }

    private static String key(Identifier name) {
        return name.getNamespace().equals("minecraft") ? name.getPath() : name.getNamespace() + ":" + name.getPath();
    }

    private void export(String name, BlockState state, boolean urgent) throws ReflectiveOperationException, IOException {
        if (state.getRenderShape() == RenderShape.MODEL) {
            BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
            List<BlockStateModelPart> parts = new ArrayList();
            model.collectParts(RandomSource.create(42L), parts);
            List<float[]> quads = new ArrayList();
            Map<String, Integer> textureIndex = new LinkedHashMap();
            List<BlockTintSource> tintSources = Minecraft.getInstance().getBlockColors().getTintSources(state);
            TreeSet<Integer> inWorld = new TreeSet();
            Iterator var10 = parts.iterator();

            while(var10.hasNext()) {
                BlockStateModelPart part = (BlockStateModelPart)var10.next();

                for(int cull = 0; cull < CULLS.length; ++cull) {
                    Iterator var13 = part.getQuads(CULLS[cull]).iterator();

                    while(var13.hasNext()) {
                        BakedQuad quad = (BakedQuad)var13.next();
                        BakedQuad.MaterialInfo info = quad.materialInfo();
                        TextureAtlasSprite sprite = info.sprite();
                        Identifier spriteName = sprite.contents().name();
                        String texture = key(spriteName);
                        Integer index = (Integer)textureIndex.get(texture);
                        if (index == null) {
                            index = textureIndex.size();
                            textureIndex.put(texture, index);
                            this.sendTexture(texture, spriteName, sprite.contents());
                        }

                        float u0 = sprite.getU0();
                        float v0 = sprite.getV0();
                        float du = Math.max(sprite.getU1() - u0, 1.0E-9F);
                        float dv = Math.max(sprite.getV1() - v0, 1.0E-9F);
                        float[] data = new float[26];
                        data[0] = (float)(cull - 1);
                        data[1] = (float)info.tintIndex();
                        boolean hasTintColor = false;
                        int tintColor = 0;
                        if (info.tintIndex() >= 0 && info.tintIndex() < tintSources.size()) {
                            BlockTintSource source = (BlockTintSource)tintSources.get(info.tintIndex());
                            if (!source.relevantProperties().isEmpty()) {
                                hasTintColor = true;
                                tintColor = source.color(state) & 16777215;
                            } else {
                                inWorld.add(info.tintIndex());
                            }
                        }

                        data[24] = hasTintColor ? 1.0F : 0.0F;
                        data[25] = (float)tintColor;
                        data[2] = info.layer().translucent() ? 1.0F : 0.0F;
                        data[3] = (float)index;

                        for(int v = 0; v < 4; ++v) {
                            Vector3fc position = quad.position(v);
                            long packed = quad.packedUV(v);
                            data[4 + v * 5] = position.x();
                            data[5 + v * 5] = position.y();
                            data[6 + v * 5] = position.z();
                            data[7 + v * 5] = (UVPair.unpackU(packed) - u0) / du;
                            data[8 + v * 5] = (UVPair.unpackV(packed) - v0) / dv;
                        }

                        quads.add(data);
                    }
                }
            }

            if (!quads.isEmpty()) {
                if (!inWorld.isEmpty()) {
                    this.worldTints.put(name, inWorld.stream().mapToInt(Integer::intValue).toArray());
                }

                int occlusion = 0;
                if (state.canOcclude()) {
                    for(int face = 0; face < 6; ++face) {
                        if (Block.isFaceFull(state.getOcclusionShape(), FACES[face])) {
                            occlusion |= 1 << face;
                        }
                    }
                }

                List<float[]> boxes = new ArrayList();

                try {
                    Iterator var37 = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs().iterator();

                    while(var37.hasNext()) {
                        AABB box = (AABB)var37.next();
                        if (boxes.size() < 64) {
                            boxes.add(new float[]{(float)box.minX, (float)box.minY, (float)box.minZ, (float)box.maxX, (float)box.maxY, (float)box.maxZ});
                        }
                    }
                } catch (RuntimeException var33) {
                    boxes.clear();
                }

                byte[] frame = BridgeProtocol.blockModel(name, occlusion, boxes, new ArrayList(textureIndex.keySet()), quads);
                if (urgent) {
                    this.connection.sendUrgent(frame);
                } else {
                    this.connection.send(frame);
                }

                synchronized(this.textureLock) {
                    ++this.models;
                    this.quadsTotal += quads.size();
                    if (this.models <= 3 || this.models % 200 == 0) {
                        TmcgiUeBridgeClient.LOGGER.info("Exported {} block models so far ({} quads, {} textures); last: {}", new Object[]{this.models, this.quadsTotal, this.textures, name});
                    }

                }
            }
        }
    }

    private void sendTexture(String key, Identifier spriteName, SpriteContents contents) throws ReflectiveOperationException, IOException {
        synchronized(this.textureLock) {
            if (this.texturesSent.add(key)) {
                Identifier file = Identifier.fromNamespaceAndPath(spriteName.getNamespace(), "textures/" + spriteName.getPath() + ".png");
                byte[] png = EntityCapture.readResource(file);
                if (png.length == 0 && ORIGINAL_IMAGE != null && contents != null) {
                    Object image = ORIGINAL_IMAGE.get(contents);
                    if (image instanceof NativeImage) {
                        NativeImage nativeImage = (NativeImage)image;
                        png = EntityCapture.encodePng(nativeImage);
                    }
                }

                byte[] meta = EntityCapture.readResource(Identifier.fromNamespaceAndPath(spriteName.getNamespace(), "textures/" + spriteName.getPath() + ".png.mcmeta"));
                if (png.length == 0) {
                    TmcgiUeBridgeClient.LOGGER.warn("No image for the texture {}", key);
                } else {
                    byte[] normal = EntityCapture.readResource(Identifier.fromNamespaceAndPath(spriteName.getNamespace(), "textures/" + spriteName.getPath() + "_n.png"));
                    byte[] specular = EntityCapture.readResource(Identifier.fromNamespaceAndPath(spriteName.getNamespace(), "textures/" + spriteName.getPath() + "_s.png"));
                    this.connection.sendUrgent(BridgeProtocol.blockTexture(key, png, meta, normal, specular));
                    ++this.textures;
                }
            }
        }
    }

    static {
        CULLS = new Direction[]{null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        FACES = new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        ORIGINAL_IMAGE = originalImage();
        FAILED = ConcurrentHashMap.newKeySet();
    }

    private final class Task {
        final String name;
        final BlockState state;
        final int generation;
        final AtomicBoolean claimed;
        volatile boolean onUrgentLane;

        Task(String name, BlockState state, int generation) {
            Objects.requireNonNull(BlockExport.this);
            super();
            this.claimed = new AtomicBoolean();
            this.name = name;
            this.state = state;
            this.generation = generation;
        }

        void run(boolean urgent) {
            if (this.claimed.compareAndSet(false, true)) {
                boolean var13 = false;

                label168: {
                    try {
                        var13 = true;
                        if (this.generation == BlockExport.this.generation) {
                            BlockExport.this.sendFluidTextures();
                            BlockExport.this.export(this.name, this.state, urgent);
                            var13 = false;
                        } else {
                            var13 = false;
                        }
                        break label168;
                    } catch (ReflectiveOperationException | IOException | RuntimeException var17) {
                        Exception e = var17;
                        if (BlockExport.FAILED.add(this.name)) {
                            TmcgiUeBridgeClient.LOGGER.warn("The model of {} could not be exported", this.name, e);
                            var13 = false;
                        } else {
                            var13 = false;
                        }
                    } finally {
                        if (var13) {
                            synchronized(BlockExport.this.readyLock) {
                                if (this.generation == BlockExport.this.generation) {
                                    BlockExport.this.ready.add(this.name);
                                }
                            }

                            BlockExport.this.pending.remove(this.name, this);
                            BlockExport.this.finished.incrementAndGet();
                        }
                    }

                    synchronized(BlockExport.this.readyLock) {
                        if (this.generation == BlockExport.this.generation) {
                            BlockExport.this.ready.add(this.name);
                        }
                    }

                    BlockExport.this.pending.remove(this.name, this);
                    BlockExport.this.finished.incrementAndGet();
                    return;
                }

                synchronized(BlockExport.this.readyLock) {
                    if (this.generation == BlockExport.this.generation) {
                        BlockExport.this.ready.add(this.name);
                    }
                }

                BlockExport.this.pending.remove(this.name, this);
                BlockExport.this.finished.incrementAndGet();
            }
        }
    }
}
