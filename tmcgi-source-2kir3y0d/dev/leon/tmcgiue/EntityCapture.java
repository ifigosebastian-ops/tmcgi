//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.leon.tmcgiue.core.MeshCache;
import dev.leon.tmcgiue.core.NeutralMesh;
import dev.leon.tmcgiue.platform.CustomGeometryRecorder;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.FabricTextureAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.SpriteFinder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class EntityCapture {
    private static final double RANGE = 96.0;
    private static final Field CUBES = field(ModelPart.class, "cubes");
    private static final Field CHILDREN = field(ModelPart.class, "children");
    private static final Field RENDER_TYPE_STATE = field(RenderType.class, "state");
    private static final Field RENDER_TYPE_NAME = field(RenderType.class, "name");
    private static final Set<Object> FAILED = new HashSet();
    private static final Set<String> REPORTED = new HashSet();
    private final BridgeConnection connection;
    private final Map<ModelPart.Cube, Integer> cubeIds = new IdentityHashMap();
    private final List<byte[]> cubeFrames = new ArrayList();
    private final Map<Identifier, Integer> textureIds = new HashMap();
    private final List<byte[]> textureFrames = new ArrayList();
    private final CustomGeometryRecorder geometry = new CustomGeometryRecorder();
    private final MeshCache meshes = new MeshCache(this::defineShape);
    private final BitSet customCubes = new BitSet();
    private final List<Integer> replacedCubes = new ArrayList();
    private final Map<Long, Integer> customOrdinals = new HashMap();
    private final SubmitNodeCollector collector = this.recorder();
    private static final Identifier[] FIRE = new Identifier[]{Identifier.fromNamespaceAndPath("minecraft", "textures/block/fire_0.png"), Identifier.fromNamespaceAndPath("minecraft", "textures/block/fire_1.png")};
    private static final int NO_OVERLAY = 655360;
    private final FlameStrip[] flameStrips = new FlameStrip[2];
    private final Map<Long, Integer> flameCubes = new HashMap();
    private long gameTicks;
    private static final int HAND_ENTITY = -2147483641;
    private static final Method BOB_HURT = bob("bobHurt");
    private static final Method BOB_VIEW = bob("bobView");
    private final Map<ItemQuads, List<ItemPart>> itemParts = new IdentityHashMap();
    private final Map<Identifier, Integer> stripFrames = new HashMap();
    private final LabelExport labels = new LabelExport();
    private final Map<String, Integer> labelTextures = new HashMap();
    private int labelCube = -1;
    private static final int[] COLUMN_MAJOR_4X3 = new int[]{0, 1, 2, 4, 5, 6, 8, 9, 10, 12, 13, 14};
    private final float[] matrix = new float[16];
    private final Map<BlockKey, List<ItemPart>> blockParts = new HashMap();
    private static final Direction[] CULLS;
    private final ByteBuffer scratch;
    private int sentGeneration;
    private int sentCubes;
    private int sentTextures;
    private ByteArrayOutputStream records;
    private int recordCount;
    private int currentEntity;
    private int currentLayer;
    private final List<float[]> cubeCenters;
    private final Set<Integer> seenBolts;
    private final Matrix4f layerMatrix;
    private static final boolean DEBUG;
    private int debugEntity;
    private final StringBuilder debugLine;
    private long debugLast;
    private long debugPick;
    private int debugCube;
    private final float[] debugMin;
    private final float[] debugMax;
    private int debugCubeCount;
    private long debugSwing;
    private float debugShear;
    private String debugType;
    private final Map<List<BakedQuad>, List<ItemPart>> meshParts;

    public EntityCapture(BridgeConnection connection) {
        this.scratch = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        this.sentGeneration = -1;
        this.cubeCenters = new ArrayList();
        this.seenBolts = new HashSet();
        this.layerMatrix = new Matrix4f();
        this.debugEntity = -1;
        this.debugLine = new StringBuilder();
        this.debugMin = new float[64];
        this.debugMax = new float[64];
        Arrays.fill(this.debugMin, 9.0F);
        Arrays.fill(this.debugMax, -9.0F);
        this.debugType = "";
        this.meshParts = new HashMap();
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

    public void capture(Minecraft minecraft, GameRenderer renderer) {
        ClientLevel level = minecraft.level;
        Camera camera = renderer.mainCamera();
        if (level != null && camera != null && camera.isInitialized()) {
            this.gameTicks = level.getGameTime();
            EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
            float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            Vec3 cameraPosition = camera.position();
            dispatcher.prepare(camera, dispatcher.crosshairPickEntity);
            this.records = new ByteArrayOutputStream(65536);
            this.debugCubeCount = Math.max(this.debugCubeCount, this.debugCube);
            this.debugCube = 0;
            if (DEBUG && this.debugEntity >= 0 && System.nanoTime() - this.debugSwing > 3000000000L) {
                this.debugSwing = System.nanoTime();
                StringBuilder swing = new StringBuilder();

                for(int i = 0; i < Math.min(this.debugCubeCount, 64); ++i) {
                    swing.append(String.format(" %d:%.2f", i, this.debugMax[i] - this.debugMin[i]));
                    this.debugMin[i] = 9.0F;
                    this.debugMax[i] = -9.0F;
                }

                TmcgiUeBridgeClient.LOGGER.info("Entity {} {} swing per cube (range of m12):{} | max column dot (shear) {}", new Object[]{this.debugEntity, this.debugType, swing, this.debugShear});
                this.debugShear = 0.0F;
            }

            this.debugCubeCount = Math.max(this.debugCubeCount, this.debugCube);
            if (DEBUG && this.debugLine.length() > 0) {
                if (System.nanoTime() - this.debugLast > 1000000000L) {
                    this.debugLast = System.nanoTime();
                    TmcgiUeBridgeClient.LOGGER.info("Entity {} {} cubes:{}", new Object[]{this.debugEntity, this.debugType, this.debugLine});
                }

                this.debugLine.setLength(0);
            }

            Iterator var18;
            if (DEBUG && (this.debugEntity < 0 || System.nanoTime() - this.debugPick > 6000000000L)) {
                this.debugPick = System.nanoTime();
                this.debugEntity = -1;
                var18 = level.entitiesForRendering().iterator();

                while(var18.hasNext()) {
                    Entity e = (Entity)var18.next();
                    if (e != camera.entity() && e instanceof LivingEntity) {
                        LivingEntity living = (LivingEntity)e;
                        if (living.walkAnimation.speed() > 0.3F && e.distanceToSqr(cameraPosition) < 4096.0) {
                            this.debugEntity = e.getId();
                            this.debugType = e.getType().toString();
                            break;
                        }
                    }
                }
            }

            this.recordCount = 0;
            this.meshes.beginFrame();
            this.customOrdinals.clear();
            var18 = this.labels.drain().iterator();

            while(var18.hasNext()) {
                LabelExport.Label label = (LabelExport.Label)var18.next();
                int id = this.textureFrames.size();
                this.textureFrames.add(pngTextureFrame(id, "tmcgi:label/" + Integer.toHexString(label.key().hashCode()) + "_" + id, label.png()));
                this.labelTextures.put(label.key(), id);
            }

            double rangeSquared = 9216.0;
            Iterator var23 = level.entitiesForRendering().iterator();

            while(true) {
                while(var23.hasNext()) {
                    Entity entity = (Entity)var23.next();
                    if (entity instanceof LightningBolt) {
                        if (this.seenBolts.size() > 512) {
                            this.seenBolts.clear();
                        }

                        if (this.seenBolts.add(entity.getId())) {
                            this.connection.sendUrgent(BridgeProtocol.lightning(entity.getX(), entity.getY(), entity.getZ(), (long)entity.getId()));
                        }
                    } else if (!(entity.distanceToSqr(cameraPosition) > rangeSquared) && (entity != camera.entity() || camera.isDetached())) {
                        try {
                            EntityRenderState state = dispatcher.extractEntity(entity, partialTick);
                            this.currentEntity = entity.getId();
                            this.currentLayer = 0;
                            PoseStack poseStack = new PoseStack();
                            int recordsBefore = this.recordCount;
                            double var10003 = state.x - cameraPosition.x;
                            double var10004 = state.y - cameraPosition.y;
                            double var10005 = state.z - cameraPosition.z;
                            dispatcher.submit(state, renderer.gameRenderState().levelRenderState.cameraRenderState, var10003, var10004, var10005, poseStack, this.collector);
                            int made = this.recordCount - recordsBefore;
                            Set var10000 = REPORTED;
                            String var10001 = entity.getType().toString();
                            if (var10000.add(var10001 + (made > 0 ? "+" : "-"))) {
                                TmcgiUeBridgeClient.LOGGER.info("Entity {} recorded {} part(s)", entity.getType(), made);
                            }
                        } catch (RuntimeException var17) {
                            RuntimeException e = var17;
                            if (FAILED.add(entity.getType())) {
                                TmcgiUeBridgeClient.LOGGER.warn("Entity {} could not be recorded", entity.getType(), e);
                            }
                        }
                    }
                }

                try {
                    this.recordHand(minecraft, renderer);
                } catch (RuntimeException var16) {
                    if (FAILED.add("hand")) {
                        TmcgiUeBridgeClient.LOGGER.warn("The first person hand could not be recorded", var16);
                    }
                }

                this.meshes.endFrame();
                if (this.connection.generation() != this.sentGeneration) {
                    this.sentGeneration = this.connection.generation();
                    this.sentCubes = 0;
                    this.sentTextures = 0;
                }

                while(this.sentTextures < this.textureFrames.size()) {
                    this.connection.send((byte[])this.textureFrames.get(this.sentTextures));
                    ++this.sentTextures;
                }

                var23 = this.replacedCubes.iterator();

                while(var23.hasNext()) {
                    int id = (Integer)var23.next();
                    if (id < this.sentCubes) {
                        this.connection.sendUrgent((byte[])this.cubeFrames.get(id));
                    }
                }

                this.replacedCubes.clear();

                for(; this.sentCubes < this.cubeFrames.size(); ++this.sentCubes) {
                    if (this.customCubes.get(this.sentCubes)) {
                        this.connection.sendUrgent((byte[])this.cubeFrames.get(this.sentCubes));
                    } else {
                        this.connection.send((byte[])this.cubeFrames.get(this.sentCubes));
                    }
                }

                byte[] body = this.records.toByteArray();
                ByteBuffer frame = ByteBuffer.allocate(33 + body.length).order(ByteOrder.LITTLE_ENDIAN);
                frame.putInt(29 + body.length).put((byte)12);
                frame.putDouble(cameraPosition.x).putDouble(cameraPosition.y).putDouble(cameraPosition.z);
                frame.putInt(this.recordCount);
                frame.put(body);
                this.connection.sendEntities(frame.array());
                this.records = null;
                return;
            }
        }
    }

    private SubmitNodeCollector recorder() {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if (name.equals("order")) {
                return proxy;
            } else if (name.equals("submitModel") && args != null && args.length == 9 && args[3] instanceof RenderType) {
                this.record((Model)args[0], args[1], (PoseStack)args[2], (RenderType)args[3], (Integer)args[5], (Integer)args[6]);
                return null;
            } else if (name.equals("submitItem") && args != null && args.length == 8) {
                this.recordItem((PoseStack)args[0], (int[])args[5], (ItemQuads)args[6]);
                return null;
            } else if (name.equals("submitBlockModel") && args != null && args.length == 7) {
                this.recordBlock((PoseStack)args[0], (List)args[2], (int[])args[3], (Integer)args[5]);
                return null;
            } else if (name.equals("submitBlockModel") && args != null && args.length == 9 && args[3] instanceof List) {
                this.recordBlock((PoseStack)args[0], (List)args[3], (int[])args[5], (Integer)args[7]);
                this.recordMesh((PoseStack)args[0], (MeshView)args[4], (int[])args[5], (Integer)args[7]);
                return null;
            } else if (name.equals("submitMovingBlock") && args != null && args.length == 3) {
                this.recordMovingBlock((PoseStack)args[0], (MovingBlockRenderState)args[1]);
                return null;
            } else if (name.equals("submitNameTag") && args != null && args.length == 7) {
                this.recordNameTag((PoseStack)args[0], (Vec3)args[1], (Integer)args[2], (Component)args[3], (CameraRenderState)args[6]);
                return null;
            } else if (name.equals("submitFlame") && args != null && args.length == 3) {
                this.recordFlame((PoseStack)args[0], (EntityRenderState)args[1], (Quaternionf)args[2]);
                return null;
            } else if (name.equals("submitCustomGeometry") && args != null && args.length == 3 && TmcgiUeBridgeClient.captureCustomGeometry()) {
                this.recordCustomGeometry((PoseStack)args[0], (RenderType)args[1], (SubmitNodeCollector.CustomGeometryRenderer)args[2]);
                return null;
            } else {
                if (REPORTED.add("call:" + method.toGenericString())) {
                    TmcgiUeBridgeClient.LOGGER.info("Entity drawing calls {} ({} arguments), not recorded", method.toGenericString(), args == null ? 0 : args.length);
                }

                return method.isDefault() ? InvocationHandler.invokeDefault(proxy, method, args) : null;
            }
        };
        return (SubmitNodeCollector)Proxy.newProxyInstance(EntityCapture.class.getClassLoader(), new Class[]{SubmitNodeCollector.class}, handler);
    }

    private void recordCustomGeometry(PoseStack stack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
        if (this.records != null) {
            try {
                NeutralMesh mesh = this.geometry.capture(stack, renderType, renderer);
                if (mesh == null) {
                    return;
                }

                int texture = this.textureOf(renderType);
                int flags = flagsOf(renderType);
                long signature = (long)this.currentEntity << 32 ^ (long)texture * -7046029254386353131L ^ (long)flags << 8 ^ (long)mesh.vertexCount * -4417276706812531889L;
                int ordinal = (Integer)this.customOrdinals.merge(signature, 1, Integer::sum) - 1;
                MeshCache.Placed placed = this.meshes.place(signature * 31L + (long)ordinal, mesh);
                if (placed == null) {
                    if (this.meshes.takeFullReport()) {
                        TmcgiUeBridgeClient.LOGGER.warn("Custom geometry: the limit of {} shapes is reached, meshes that are new are dropped", 2048);
                    }

                    return;
                }

                this.scratch.clear();
                this.scratch.putInt(this.currentEntity).putInt(placed.shape()).putInt(texture).putInt(flags).putInt(placed.color()).putInt(655360);
                (new Matrix4f()).translation(placed.ox(), placed.oy(), placed.oz()).get(this.matrix);
                int[] var11 = COLUMN_MAJOR_4X3;
                int var12 = var11.length;

                for(int var13 = 0; var13 < var12; ++var13) {
                    int index = var11[var13];
                    this.scratch.putFloat(this.matrix[index]);
                }

                this.records.write(this.scratch.array(), 0, this.scratch.position());
                ++this.recordCount;
            } catch (RuntimeException var15) {
                RuntimeException e = var15;
                if (FAILED.add("custom:" + String.valueOf(renderType))) {
                    TmcgiUeBridgeClient.LOGGER.warn("Custom geometry of {} could not be recorded", renderType, e);
                }
            }

        }
    }

    private int defineShape(int existing, int quads, float[] data, float cx, float cy, float cz) {
        ByteBuffer buffer = ByteBuffer.allocate(11 + quads * 92).order(ByteOrder.LITTLE_ENDIAN);
        int id = existing >= 0 ? existing : this.cubeFrames.size();
        buffer.putInt(buffer.capacity() - 4).put((byte)10);
        buffer.putInt(id).putShort((short)quads);
        float[] var9 = data;
        int var10 = data.length;

        for(int var11 = 0; var11 < var10; ++var11) {
            float value = var9[var11];
            buffer.putFloat(value);
        }

        if (existing >= 0) {
            this.cubeCenters.set(id, new float[]{cx, cy, cz});
            this.cubeFrames.set(id, buffer.array());
            this.replacedCubes.add(id);
        } else {
            this.cubeCenters.add(new float[]{cx, cy, cz});
            this.cubeFrames.add(buffer.array());
            this.customCubes.set(id);
        }

        return id;
    }

    private void record(Model<?> model, Object state, PoseStack stack, RenderType renderType, int overlay, int color) {
        if (this.records != null) {
            try {
                model.setupAnim(state);
            } catch (RuntimeException var13) {
                RuntimeException e = var13;
                if (FAILED.add(model.getClass())) {
                    TmcgiUeBridgeClient.LOGGER.warn("Model {} could not be posed", model.getClass().getName(), e);
                }
            }

            int texture = this.textureOf(renderType);
            int flags = flagsOf(renderType);
            stack.pushPose();

            try {
                this.walk(model.root(), stack, texture, flags, overlay, color);
            } finally {
                ++this.currentLayer;
                stack.popPose();
            }

        }
    }

    private void walk(ModelPart part, PoseStack stack, int texture, int flags, int overlay, int color) {
        if (part.visible) {
            IllegalAccessException e;
            Map children;
            List cubes;
            try {
                cubes = (List)CUBES.get(part);
                children = (Map)CHILDREN.get(part);
                if (cubes.isEmpty() && children.isEmpty()) {
                    return;
                }
            } catch (IllegalAccessException var11) {
                e = var11;
                throw new IllegalStateException(e);
            }

            try {
                stack.pushPose();
                part.translateAndRotate(stack);
                Iterator var9;
                if (!part.skipDraw) {
                    var9 = cubes.iterator();

                    while(var9.hasNext()) {
                        ModelPart.Cube cube = (ModelPart.Cube)var9.next();
                        this.emit(cube, stack, texture, flags, overlay, color);
                    }
                }

                var9 = children.values().iterator();

                while(var9.hasNext()) {
                    ModelPart child = (ModelPart)var9.next();
                    this.walk(child, stack, texture, flags, overlay, color);
                }

                stack.popPose();
            } catch (IllegalAccessException var12) {
                e = var12;
                throw new IllegalStateException(e);
            }
        }
    }

    private void emit(ModelPart.Cube cube, PoseStack stack, int texture, int flags, int overlay, int color) {
        Integer known = (Integer)this.cubeIds.get(cube);
        int id = known != null ? known : this.defineCube(cube);
        this.scratch.clear();
        this.scratch.putInt(this.currentEntity).putInt(id).putInt(texture).putInt(flags).putInt(color).putInt(overlay);
        float d02;
        if (this.currentLayer > 0) {
            float[] center = (float[])this.cubeCenters.get(id);
            d02 = 1.0F + 0.006F * (float)Math.min(this.currentLayer, 4);
            this.layerMatrix.set(stack.last().pose()).translate(center[0], center[1], center[2]).scale(d02).translate(-center[0], -center[1], -center[2]).get(this.matrix);
        } else {
            stack.last().pose().get(this.matrix);
        }

        if (DEBUG && this.currentEntity == this.debugEntity) {
            if (this.debugCube < 64) {
                this.debugMin[this.debugCube] = Math.min(this.debugMin[this.debugCube], this.matrix[6]);
                this.debugMax[this.debugCube] = Math.max(this.debugMax[this.debugCube], this.matrix[6]);
            }

            float d01 = this.matrix[0] * this.matrix[4] + this.matrix[1] * this.matrix[5] + this.matrix[2] * this.matrix[6];
            d02 = this.matrix[0] * this.matrix[8] + this.matrix[1] * this.matrix[9] + this.matrix[2] * this.matrix[10];
            float d12 = this.matrix[4] * this.matrix[8] + this.matrix[5] * this.matrix[9] + this.matrix[6] * this.matrix[10];
            this.debugShear = Math.max(this.debugShear, Math.max(Math.abs(d01), Math.max(Math.abs(d02), Math.abs(d12))));
            ++this.debugCube;
        }

        int[] var14 = COLUMN_MAJOR_4X3;
        int var15 = var14.length;

        for(int var16 = 0; var16 < var15; ++var16) {
            int index = var14[var16];
            this.scratch.putFloat(this.matrix[index]);
        }

        this.records.write(this.scratch.array(), 0, this.scratch.position());
        ++this.recordCount;
    }

    private static Method bob(String name) {
        try {
            Method method = GameRenderer.class.getDeclaredMethod(name, CameraRenderState.class, PoseStack.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException var2) {
            return null;
        }
    }

    private void recordHand(Minecraft minecraft, GameRenderer renderer) {
        LevelRenderState levelState = renderer.gameRenderState().levelRenderState;
        PlayerRenderState playerState = levelState.playerRenderState;
        CameraRenderState cameraState = levelState.cameraRenderState;
        OptionsRenderState options = renderer.gameRenderState().optionsRenderState;
        if (playerState.hasPlayer && playerState.firstPersonHandsAndItems != null && !cameraState.isPanoramicMode && options.cameraType.isFirstPerson() && !cameraState.entityRenderState.isSleeping && !renderer.gameRenderState().guiRenderState.isHudHidden && minecraft.gameMode != null && minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR) {
            PoseStack poseStack = new PoseStack();
            poseStack.pushPose();
            poseStack.mulPose(cameraState.viewRotationMatrix.invert(new Matrix4f()));

            try {
                if (BOB_HURT != null) {
                    BOB_HURT.invoke(renderer, cameraState, poseStack);
                }

                if (options.bobView && BOB_VIEW != null) {
                    BOB_VIEW.invoke(renderer, cameraState, poseStack);
                }
            } catch (ReflectiveOperationException var9) {
            }

            this.currentEntity = -2147483641;
            this.currentLayer = 0;
            renderer.firstPersonHandsAndItemsRenderer.submitHandsWithItems(cameraState.cameraEntityPartialTicks, poseStack, this.collector, playerState, playerState.firstPersonHandsAndItems);
            poseStack.popPose();
        }
    }

    private void recordItem(PoseStack stack, int[] tints, ItemQuads quads) {
        if (this.records != null && quads != null && !quads.isEmpty()) {
            List<ItemPart> parts = (List)this.itemParts.get(quads);
            if (parts == null) {
                parts = this.defineItem(quads);
                this.itemParts.put(quads, parts);
            }

            this.writeParts(stack, parts, tints, 655360);
        }
    }

    private void writeParts(PoseStack stack, List<ItemPart> parts, int[] tints, int overlay) {
        for(Iterator var5 = parts.iterator(); var5.hasNext(); ++this.recordCount) {
            ItemPart part = (ItemPart)var5.next();
            int color = part.tint() >= 0 && tints != null && part.tint() < tints.length ? tints[part.tint()] : -1;
            this.scratch.clear();
            this.scratch.putInt(this.currentEntity).putInt(part.cube()).putInt(part.texture()).putInt(part.flags()).putInt(color).putInt(overlay);
            stack.last().pose().get(this.matrix);
            int[] var8 = COLUMN_MAJOR_4X3;
            int var9 = var8.length;

            for(int var10 = 0; var10 < var9; ++var10) {
                int index = var8[var10];
                this.scratch.putFloat(this.matrix[index]);
            }

            this.records.write(this.scratch.array(), 0, this.scratch.position());
        }

    }

    private void recordBlock(PoseStack stack, List<?> modelParts, int[] tints, int overlay) {
        if (this.records != null && modelParts != null && !modelParts.isEmpty()) {
            BlockKey key = new BlockKey(modelParts);
            List<ItemPart> parts = (List)this.blockParts.get(key);
            if (parts == null) {
                List<BakedQuad> quads = new ArrayList();
                Iterator var8 = modelParts.iterator();

                while(var8.hasNext()) {
                    Object object = var8.next();
                    BlockStateModelPart modelPart = (BlockStateModelPart)object;
                    Direction[] var11 = CULLS;
                    int var12 = var11.length;

                    for(int var13 = 0; var13 < var12; ++var13) {
                        Direction cull = var11[var13];
                        quads.addAll(modelPart.getQuads(cull));
                    }
                }

                parts = this.defineQuads(quads);
                this.blockParts.put(key, parts);
                if (REPORTED.size() < 400) {
                    TmcgiUeBridgeClient.LOGGER.info("A block model of {} part(s) with {} quad(s) became {} cube part(s)", new Object[]{modelParts.size(), quads.size(), parts.size()});
                }
            }

            this.writeParts(stack, parts, tints, overlay);
        } else {
            if (REPORTED.add("block-empty")) {
                TmcgiUeBridgeClient.LOGGER.info("A block model was submitted with no parts (records {}, parts {})", this.records != null, modelParts);
            }

        }
    }

    private void recordMesh(PoseStack stack, MeshView mesh, int[] tints, int overlay) {
        if (this.records != null && mesh != null && mesh.size() != 0) {
            SpriteFinder finder = blockSpriteFinder();
            if (finder != null) {
                List<BakedQuad> quads = new ArrayList();
                mesh.forEach((quad) -> {
                    quads.add(quad.toBakedQuad(finder.find(quad)));
                });
                List<ItemPart> parts = (List)this.meshParts.get(quads);
                if (parts == null) {
                    if (this.meshParts.size() > 512) {
                        this.meshParts.clear();
                    }

                    parts = this.defineQuads(quads);
                    this.meshParts.put(quads, parts);
                    if (REPORTED.size() < 400) {
                        TmcgiUeBridgeClient.LOGGER.info("A mesh of {} quad(s) became {} cube part(s)", quads.size(), parts.size());
                    }
                }

                this.writeParts(stack, parts, tints, overlay);
            }
        }
    }

    private static SpriteFinder blockSpriteFinder() {
        Object atlas = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        SpriteFinder var10000;
        if (atlas instanceof FabricTextureAtlas fabric) {
            var10000 = fabric.spriteFinder();
        } else {
            var10000 = null;
        }

        return var10000;
    }

    private void recordMovingBlock(PoseStack stack, MovingBlockRenderState moving) {
        BlockState state = moving.blockState;
        if (this.records != null && state != null) {
            Minecraft minecraft = Minecraft.getInstance();
            List<BlockStateModelPart> modelParts = new ArrayList();
            long seed = moving.randomSeedPos != null ? state.getSeed(moving.randomSeedPos) : 42L;
            minecraft.getModelManager().getBlockStateModelSet().get(state).collectParts(RandomSource.create(seed), modelParts);
            List<BlockTintSource> sources = minecraft.getBlockColors().getTintSources(state);
            int[] tints = new int[sources.size()];

            for(int i = 0; i < tints.length; ++i) {
                tints[i] = ((BlockTintSource)sources.get(i)).colorInWorld(state, moving, moving.blockPos) | -16777216;
            }

            this.recordBlock(stack, modelParts, tints, 655360);
        }
    }

    private List<ItemPart> defineItem(ItemQuads quads) {
        return this.defineQuads(quads.all());
    }

    private List<ItemPart> defineQuads(Iterable<BakedQuad> quads) {
        Map<String, List<BakedQuad>> groups = new LinkedHashMap();
        Iterator var3 = quads.iterator();

        while(var3.hasNext()) {
            BakedQuad quad = (BakedQuad)var3.next();
            BakedQuad.MaterialInfo info = quad.materialInfo();
            String var10000 = String.valueOf(info.sprite().contents().name());
            String key = var10000 + "|" + info.tintIndex() + "|" + (info.layer().translucent() ? 1 : 0);
            ((List)groups.computeIfAbsent(key, (k) -> {
                return new ArrayList();
            })).add(quad);
        }

        List<ItemPart> parts = new ArrayList();
        Iterator var29 = groups.values().iterator();

        while(var29.hasNext()) {
            List<BakedQuad> group = (List)var29.next();
            BakedQuad.MaterialInfo info = ((BakedQuad)group.get(0)).materialInfo();
            TextureAtlasSprite sprite = info.sprite();
            Identifier name = sprite.contents().name();
            Identifier location = Identifier.fromNamespaceAndPath(name.getNamespace(), "textures/" + name.getPath() + ".png");
            int texture = this.textureOf(location);
            int frames = (Integer)this.stripFrames.computeIfAbsent(location, EntityCapture::framesOf);
            float u0 = sprite.getU0();
            float v0 = sprite.getV0();
            float du = Math.max(sprite.getU1() - u0, 1.0E-9F);
            float dv = Math.max(sprite.getV1() - v0, 1.0E-9F);
            ByteBuffer buffer = ByteBuffer.allocate(11 + group.size() * 92).order(ByteOrder.LITTLE_ENDIAN);
            int id = this.cubeFrames.size();
            buffer.putInt(buffer.capacity() - 4).put((byte)10);
            buffer.putInt(id).putShort((short)group.size());
            float[] centre = new float[3];
            Iterator var19 = group.iterator();

            while(var19.hasNext()) {
                BakedQuad quad = (BakedQuad)var19.next();
                Vector3f normal = (new Vector3f(quad.position1())).sub(quad.position0()).cross((new Vector3f(quad.position2())).sub(quad.position0()));
                if (normal.lengthSquared() < 1.0E-12F) {
                    normal.set(0.0F, 1.0F, 0.0F);
                } else {
                    normal.normalize();
                }

                buffer.putFloat(normal.x).putFloat(normal.y).putFloat(normal.z);

                for(int vertex = 0; vertex < 4; ++vertex) {
                    Vector3fc position = quad.position(vertex);
                    long packed = quad.packedUV(vertex);
                    float u = (UVPair.unpackU(packed) - u0) / du;
                    float v = (UVPair.unpackV(packed) - v0) / dv / (float)frames;
                    buffer.putFloat(position.x()).putFloat(position.y()).putFloat(position.z()).putFloat(u).putFloat(v);
                    centre[0] += position.x();
                    centre[1] += position.y();
                    centre[2] += position.z();
                }
            }

            float count = (float)group.size() * 4.0F;
            this.cubeCenters.add(new float[]{centre[0] / count, centre[1] / count, centre[2] / count});
            this.cubeFrames.add(buffer.array());
            parts.add(new ItemPart(id, texture, info.tintIndex(), info.layer().translucent() ? 1 : 0));
        }

        return parts;
    }

    private static int framesOf(Identifier location) {
        byte[] png = readResource(location);
        if (png.length > 24) {
            ByteBuffer header = ByteBuffer.wrap(png);
            int width = header.getInt(16);
            int height = header.getInt(20);
            if (width > 0 && height > width) {
                return height / width;
            }
        }

        return 1;
    }

    private void recordFlame(PoseStack stack, EntityRenderState state, Quaternionf rotation) {
        if (this.records != null) {
            float s = state.boundingBoxWidth * 1.4F;
            Matrix4f m = (new Matrix4f(stack.last().pose())).scale(s, s, s);
            float h = state.boundingBoxHeight / s;
            m.rotate(rotation);
            m.translate(0.0F, 0.0F, 0.3F - (float)((int)h) * 0.02F);
            float r = 0.5F;
            float yo = 0.0F;
            float zo = 0.0F;

            for(int step = 0; h > 0.0F; ++step) {
                int which = step % 2;
                boolean flip = step / 2 % 2 == 0;
                FlameStrip strip = this.flameStrip(which);
                int cube = this.flameCube(which, strip.frameAt(this.gameTicks), strip.frames, flip);
                Matrix4f placed = (new Matrix4f(m)).translate(0.0F, -yo, zo).scale(r * 2.0F, 1.0F, 1.0F);
                this.scratch.clear();
                this.scratch.putInt(this.currentEntity).putInt(cube).putInt(strip.texture).putInt(2).putInt(-1).putInt(655360);
                placed.get(this.matrix);
                int[] var16 = COLUMN_MAJOR_4X3;
                int var17 = var16.length;

                for(int var18 = 0; var18 < var17; ++var18) {
                    int index = var16[var18];
                    this.scratch.putFloat(this.matrix[index]);
                }

                this.records.write(this.scratch.array(), 0, this.scratch.position());
                ++this.recordCount;
                h -= 0.45F;
                yo -= 0.45F;
                r *= 0.9F;
                zo -= 0.03F;
            }

        }
    }

    private FlameStrip flameStrip(int which) {
        if (this.flameStrips[which] == null) {
            this.flameStrips[which] = new FlameStrip(this.textureOf(FIRE[which]), FIRE[which]);
        }

        return this.flameStrips[which];
    }

    private int flameCube(int which, int frame, int frames, boolean flip) {
        long key = (long)which << 32 | (long)frame << 1 | (long)(flip ? 1 : 0);
        Integer known = (Integer)this.flameCubes.get(key);
        if (known != null) {
            return known;
        } else {
            int id = this.cubeFrames.size();
            float inset = 0.01F / (float)frames;
            float top = ((float)frame + inset * (float)frames) / (float)frames;
            float bottom = ((float)(frame + 1) - inset * (float)frames) / (float)frames;
            float left = flip ? 0.0F : 1.0F;
            float right = 1.0F - left;
            float[][] corners = new float[][]{{-0.5F, 0.0F, left, bottom}, {0.5F, 0.0F, right, bottom}, {0.5F, 1.4F, right, top}, {-0.5F, 1.4F, left, top}};
            ByteBuffer buffer = ByteBuffer.allocate(103).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(buffer.capacity() - 4).put((byte)10);
            buffer.putInt(id).putShort((short)1);
            buffer.putFloat(0.0F).putFloat(0.0F).putFloat(1.0F);

            for(int i = 0; i < 4; ++i) {
                buffer.putFloat(corners[i][0]).putFloat(corners[i][1]).putFloat(0.0F).putFloat(corners[i][2]).putFloat(corners[i][3]);
            }

            this.cubeCenters.add(new float[]{0.0F, 0.7F, 0.0F});
            this.cubeFrames.add(buffer.array());
            this.flameCubes.put(key, id);
            return id;
        }
    }

    private int defineCube(ModelPart.Cube cube) {
        ModelPart.Polygon[] polygons = cube.polygons;
        ByteBuffer buffer = ByteBuffer.allocate(11 + polygons.length * 92).order(ByteOrder.LITTLE_ENDIAN);
        int id = this.cubeFrames.size();
        buffer.putInt(buffer.capacity() - 4).put((byte)10);
        buffer.putInt(id).putShort((short)polygons.length);
        float[] low = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] high = new float[]{-3.4028235E38F, -3.4028235E38F, -3.4028235E38F};
        ModelPart.Polygon[] var7 = polygons;
        int var8 = polygons.length;

        int var9;
        ModelPart.Polygon polygon;
        ModelPart.Vertex[] var11;
        int var12;
        int var13;
        ModelPart.Vertex vertex;
        for(var9 = 0; var9 < var8; ++var9) {
            polygon = var7[var9];
            var11 = polygon.vertices();
            var12 = var11.length;

            for(var13 = 0; var13 < var12; ++var13) {
                vertex = var11[var13];
                low[0] = Math.min(low[0], vertex.worldX());
                low[1] = Math.min(low[1], vertex.worldY());
                low[2] = Math.min(low[2], vertex.worldZ());
                high[0] = Math.max(high[0], vertex.worldX());
                high[1] = Math.max(high[1], vertex.worldY());
                high[2] = Math.max(high[2], vertex.worldZ());
            }
        }

        this.cubeCenters.add(new float[]{(low[0] + high[0]) / 2.0F, (low[1] + high[1]) / 2.0F, (low[2] + high[2]) / 2.0F});
        var7 = polygons;
        var8 = polygons.length;

        for(var9 = 0; var9 < var8; ++var9) {
            polygon = var7[var9];
            buffer.putFloat(polygon.normal().x()).putFloat(polygon.normal().y()).putFloat(polygon.normal().z());
            var11 = polygon.vertices();
            var12 = var11.length;

            for(var13 = 0; var13 < var12; ++var13) {
                vertex = var11[var13];
                buffer.putFloat(vertex.worldX()).putFloat(vertex.worldY()).putFloat(vertex.worldZ());
                buffer.putFloat(vertex.u()).putFloat(vertex.v());
            }
        }

        this.cubeFrames.add(buffer.array());
        this.cubeIds.put(cube, id);
        return id;
    }

    private int textureOf(RenderType renderType) {
        Identifier location = textureLocation(renderType);
        return location == null ? -1 : this.textureOf(location);
    }

    private int textureOf(Identifier location) {
        Integer existing = (Integer)this.textureIds.get(location);
        if (existing != null) {
            return existing;
        } else {
            int id = this.textureFrames.size();
            this.textureIds.put(location, id);
            this.textureFrames.add(textureFrame(id, location));
            return id;
        }
    }

    private void recordNameTag(PoseStack stack, Vec3 attachment, int offset, Component text, CameraRenderState camera) {
        if (this.records != null) {
            List<LabelExport.Segment> segments = LabelExport.segments(text);
            if (!segments.isEmpty()) {
                String key = LabelExport.keyOf(segments);
                LabelExport.Label label = this.labels.get(key, segments);
                Integer texture = (Integer)this.labelTextures.get(key);
                if (label != null && texture != null) {
                    if (this.labelCube < 0) {
                        this.labelCube = this.unitQuadCube();
                    }

                    float width = (float)label.width() / 4.0F;
                    float height = (float)label.height() / 4.0F;
                    stack.pushPose();

                    try {
                        stack.translate(attachment.x, attachment.y + 0.5, attachment.z);
                        stack.rotate(camera.orientation);
                        stack.scale(0.025F, -0.025F, 0.025F);
                        this.layerMatrix.set(stack.last().pose()).translate(-width / 2.0F, (float)offset + 4.0F - height / 2.0F, 0.0F).scale(width, height, 1.0F).get(this.matrix);
                    } finally {
                        stack.popPose();
                    }

                    this.scratch.clear();
                    this.scratch.putInt(this.currentEntity).putInt(this.labelCube).putInt(texture).putInt(2).putInt(-1).putInt(655360);
                    int[] var12 = COLUMN_MAJOR_4X3;
                    int var13 = var12.length;

                    for(int var14 = 0; var14 < var13; ++var14) {
                        int index = var12[var14];
                        this.scratch.putFloat(this.matrix[index]);
                    }

                    this.records.write(this.scratch.array(), 0, this.scratch.position());
                    ++this.recordCount;
                }
            }
        }
    }

    private int unitQuadCube() {
        int id = this.cubeFrames.size();
        float[][] corners = new float[][]{{0.0F, 1.0F}, {1.0F, 1.0F}, {1.0F, 0.0F}, {0.0F, 0.0F}};
        float[][] uvs = new float[][]{{0.0F, 1.0F}, {1.0F, 1.0F}, {1.0F, 0.0F}, {0.0F, 0.0F}};
        ByteBuffer buffer = ByteBuffer.allocate(103).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(buffer.capacity() - 4).put((byte)10);
        buffer.putInt(id).putShort((short)1);
        buffer.putFloat(0.0F).putFloat(0.0F).putFloat(1.0F);

        for(int i = 0; i < 4; ++i) {
            buffer.putFloat(corners[i][0]).putFloat(corners[i][1]).putFloat(0.0F).putFloat(uvs[i][0]).putFloat(uvs[i][1]);
        }

        this.cubeCenters.add(new float[]{0.5F, 0.5F, 0.0F});
        this.cubeFrames.add(buffer.array());
        return id;
    }

    private static byte[] pngTextureFrame(int id, String name, byte[] png) {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(11 + nameBytes.length + 4 + png.length + 8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(buffer.capacity() - 4).put((byte)11);
        buffer.putInt(id).putShort((short)nameBytes.length).put(nameBytes);
        buffer.putInt(png.length).put(png).putInt(0).putInt(0);
        return buffer.array();
    }

    private static byte[] textureFrame(int id, Identifier location) {
        byte[] name = location.toString().getBytes(StandardCharsets.UTF_8);
        byte[][] images = new byte[3][];
        String path = location.getPath();
        String base = path.endsWith(".png") ? path.substring(0, path.length() - 4) : path;
        String[] suffixes = new String[]{"", "_n", "_s"};
        int total = 0;

        for(int i = 0; i < 3; ++i) {
            images[i] = readResource(Identifier.fromNamespaceAndPath(location.getNamespace(), base + suffixes[i] + ".png"));
            if (i == 0 && images[i].length == 0) {
                images[i] = readDynamicTexture(location);
            }

            total += 4 + images[i].length;
        }

        ByteBuffer buffer = ByteBuffer.allocate(11 + name.length + total).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(buffer.capacity() - 4).put((byte)11);
        buffer.putInt(id).putShort((short)name.length).put(name);
        byte[][] var9 = images;
        int var10 = images.length;

        for(int var11 = 0; var11 < var10; ++var11) {
            byte[] image = var9[var11];
            buffer.putInt(image.length).put(image);
        }

        return buffer.array();
    }

    private static byte[] readDynamicTexture(Identifier location) {
        try {
            Object texture = Minecraft.getInstance().getTextureManager().getTexture(location);
            if (texture instanceof DynamicTexture dynamic) {
                if (dynamic.getPixels() != null) {
                    Path temp = Files.createTempFile("tmcgi-skin", ".png");

                    byte[] var4;
                    try {
                        dynamic.getPixels().writeToFile(temp);
                        var4 = Files.readAllBytes(temp);
                    } finally {
                        Files.deleteIfExists(temp);
                    }

                    return var4;
                }
            }
        } catch (Exception var9) {
            Exception e = var9;
            TmcgiUeBridgeClient.LOGGER.warn("The run-time texture {} could not be read", location, e);
        }

        return new byte[0];
    }

    static byte[] readResource(Identifier location) {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isEmpty()) {
                return new byte[0];
            } else {
                InputStream in = ((Resource)resource.get()).open();

                byte[] var3;
                try {
                    var3 = in.readAllBytes();
                } catch (Throwable var6) {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Throwable var5) {
                            var6.addSuppressed(var5);
                        }
                    }

                    throw var6;
                }

                if (in != null) {
                    in.close();
                }

                return var3;
            }
        } catch (Exception var7) {
            return new byte[0];
        }
    }

    private static Identifier textureLocation(RenderType renderType) {
        try {
            Object setup = RENDER_TYPE_STATE.get(renderType);
            Field texturesField = setup.getClass().getDeclaredField("textures");
            texturesField.setAccessible(true);
            Map<?, ?> textures = (Map)texturesField.get(setup);
            Object binding = textures.get("Sampler0");
            if (binding == null) {
                return null;
            } else {
                Method location = binding.getClass().getDeclaredMethod("location");
                location.setAccessible(true);
                return (Identifier)location.invoke(binding);
            }
        } catch (ReflectiveOperationException var6) {
            return null;
        }
    }

    private static int flagsOf(RenderType renderType) {
        String name;
        try {
            name = String.valueOf(RENDER_TYPE_NAME.get(renderType));
        } catch (IllegalAccessException var3) {
            return 0;
        }

        int flags = 0;
        if (name.contains("trans")) {
            flags |= 1;
        }

        if (name.contains("eyes") || name.contains("emissive") || name.contains("energy_swirl") || name.contains("beacon")) {
            flags |= 2;
        }

        return flags;
    }

    static {
        CULLS = new Direction[]{null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        DEBUG = System.getenv("TMCGI_DEBUG_ENTITY") != null;
    }

    private static final class FlameStrip {
        final int texture;
        final int frames;
        final int[] order;
        final int[] times;
        final int total;

        FlameStrip(int texture, Identifier location) {
            this.texture = texture;
            byte[] png = EntityCapture.readResource(location);
            int count = 1;
            if (png.length > 24) {
                ByteBuffer header = ByteBuffer.wrap(png);
                int width = header.getInt(16);
                int height = header.getInt(20);
                count = Math.max(1, width > 0 ? height / width : 1);
            }

            this.frames = count;
            int frameTime = 1;
            List<int[]> steps = new ArrayList();
            byte[] meta = EntityCapture.readResource(Identifier.fromNamespaceAndPath(location.getNamespace(), location.getPath() + ".mcmeta"));
            if (meta.length > 0) {
                try {
                    JsonObject animation = JsonParser.parseString(new String(meta, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animation");
                    if (animation != null) {
                        if (animation.has("frametime")) {
                            frameTime = Math.max(1, animation.get("frametime").getAsInt());
                        }

                        if (animation.has("frames")) {
                            Iterator var9 = animation.getAsJsonArray("frames").iterator();

                            while(var9.hasNext()) {
                                JsonElement entry = (JsonElement)var9.next();
                                if (entry.isJsonObject()) {
                                    JsonObject object = entry.getAsJsonObject();
                                    steps.add(new int[]{object.get("index").getAsInt(), object.has("time") ? Math.max(1, object.get("time").getAsInt()) : frameTime});
                                } else {
                                    steps.add(new int[]{entry.getAsInt(), frameTime});
                                }
                            }
                        }
                    }
                } catch (RuntimeException var12) {
                    steps.clear();
                }
            }

            int sum;
            if (steps.isEmpty()) {
                for(sum = 0; sum < count; ++sum) {
                    steps.add(new int[]{sum, frameTime});
                }
            }

            this.order = new int[steps.size()];
            this.times = new int[steps.size()];
            sum = 0;

            for(int i = 0; i < this.order.length; ++i) {
                this.order[i] = Math.min(Math.max(((int[])steps.get(i))[0], 0), count - 1);
                this.times[i] = ((int[])steps.get(i))[1];
                sum += this.times[i];
            }

            this.total = Math.max(sum, 1);
        }

        int frameAt(long ticks) {
            int inSequence = (int)(ticks % (long)this.total);
            int step = 0;

            for(int start = 0; step + 1 < this.order.length && start + this.times[step] <= inSequence; ++step) {
                start += this.times[step];
            }

            return this.order[step];
        }
    }

    private static record ItemPart(int cube, int texture, int tint, int flags) {
        private ItemPart(int cube, int texture, int tint, int flags) {
            this.cube = cube;
            this.texture = texture;
            this.tint = tint;
            this.flags = flags;
        }

        public int cube() {
            return this.cube;
        }

        public int texture() {
            return this.texture;
        }

        public int tint() {
            return this.tint;
        }

        public int flags() {
            return this.flags;
        }
    }

    private static final class BlockKey {
        private final Object[] parts;

        BlockKey(List<?> list) {
            this.parts = list.toArray();
        }

        public boolean equals(Object other) {
            if (other instanceof BlockKey key) {
                if (key.parts.length == this.parts.length) {
                    for(int i = 0; i < this.parts.length; ++i) {
                        if (key.parts[i] != this.parts[i]) {
                            return false;
                        }
                    }

                    return true;
                }
            }

            return false;
        }

        public int hashCode() {
            int hash = this.parts.length;
            Object[] var2 = this.parts;
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                Object part = var2[var4];
                hash = hash * 31 + System.identityHashCode(part);
            }

            return hash;
        }
    }
}
