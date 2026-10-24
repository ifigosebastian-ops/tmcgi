//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TmcgiUeBridgeClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("TMCGI-UE-Bridge");
    private static BridgeConnection connection;
    private static SectionTracker tracker;
    private static UiCapture uiCapture;
    private static FrameShare frameShare;
    private static int sentWidth;
    private static int sentHeight;
    private static int resolutionGeneration = -1;
    private static boolean lastGrabbed;
    private static int cursorGeneration = -1;
    private static int frame;
    private static boolean compatWorldRender = System.getenv("TMCGI_COMPAT_WORLD_RENDER") != null || System.getenv("TMCGI_NO_SKIP") != null;
    public static volatile boolean discardDraws;
    private static boolean captureCustomGeometry = System.getenv("TMCGI_CAPTURE_CUSTOM_GEOMETRY") != null;
    private static List<String> selectedPacks;
    private static final long MIDNIGHT_CLOCK = 18000L;
    private static final boolean ENTITIES = System.getenv("TMCGI_NO_ENTITIES") == null;
    private static EntityCapture entityCapture;
    private static ParticleCapture particleCapture;
    private static float sentRain = -1.0F;
    private static boolean sentSnow;
    private static float sentThunder = -1.0F;
    private static int weatherGeneration = -1;
    private static final float[] SENT_TINT = new float[]{-1.0F, -1.0F, -1.0F};
    private static int tintGeneration = -1;

    public TmcgiUeBridgeClient() {
    }

    public static boolean compatWorldRender() {
        return compatWorldRender;
    }

    public static void setCompatWorldRender(boolean value) {
        compatWorldRender = value;
        BridgeConfig.store("compat.worldRender", Boolean.toString(value));
    }

    public static boolean captureCustomGeometry() {
        return captureCustomGeometry;
    }

    public void onInitializeClient() {
        BridgeConfig config = BridgeConfig.load();
        if (!config.enabled) {
            LOGGER.info("TMCGI UE bridge is disabled in the config");
        } else {
            HiddenParticles.set(config.hiddenParticles);
            compatWorldRender |= config.compatWorldRender;
            captureCustomGeometry |= config.captureCustomGeometry;
            if (captureCustomGeometry) {
                LOGGER.info("TMCGI UE bridge: recording the custom geometry that mods submit");
            }

            if (compatWorldRender) {
                LOGGER.info("TMCGI UE bridge: compatibility mode, Minecraft draws its own world and its picture is discarded");
            }

            TmcgiSettings.load();
            RendererLauncher.start(config);
            connection = new BridgeConnection(config.host, config.port);
            tracker = new SectionTracker(connection, config.radiusChunks, config.followRenderDistance, config.maxRadiusChunks);
            connection.start();
            frameShare = new FrameShare(Platform.frameFile());
            ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
                tracker.onChunkLoad(chunk.getPos().x(), chunk.getPos().z());
            });
            ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
                tracker.onChunkUnload(chunk.getPos().x(), chunk.getPos().z());
            });
            Event var10000 = ClientTickEvents.END_CLIENT_TICK;
            SectionTracker var10001 = tracker;
            Objects.requireNonNull(var10001);
            var10000.register(var10001::tick);
            ClientTickEvents.END_CLIENT_TICK.register(TmcgiUeBridgeClient::checkResourcePacks);
            ClientTickEvents.END_CLIENT_TICK.register(DhProbe::tick);
            LodManager lod = new LodManager(connection, tracker, config);
            var10000 = ClientTickEvents.END_CLIENT_TICK;
            Objects.requireNonNull(lod);
            var10000.register(lod::tick);
            ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> {
                lod.stop();
                connection.stop();
                RendererLauncher.stop();
            });
            LOGGER.info("TMCGI UE bridge: sending {} around the camera to {}:{}", new Object[]{config.followRenderDistance ? "the chunks of Minecraft's render distance (at most " + config.maxRadiusChunks + ")" : config.radiusChunks + " chunks", config.host, config.port});
        }
    }

    private static void checkResourcePacks(Minecraft client) {
        List<String> now = List.copyOf(client.options.resourcePacks);
        if (selectedPacks == null) {
            selectedPacks = now;
        } else if (!now.equals(selectedPacks)) {
            selectedPacks = now;
            TmcgiSettings.resourcePacksChanged();
        }

    }

    private static long worldClock(ClientLevel level) {
        return level.dimension() != Level.NETHER && level.dimension() != Level.END ? level.getOverworldClockTime() : 18000L;
    }

    static void sendSettings(byte[] frame) {
        if (connection != null) {
            connection.sendUrgent(frame);
        }

    }

    public static void onBlockChanged(BlockPos pos) {
        if (tracker != null) {
            tracker.onBlockChanged(pos);
        }

    }

    public static boolean overlayActive() {
        return connection != null && connection.isConnected() && connection.rendererWidth() > 0 && Minecraft.getInstance().level != null;
    }

    public static boolean drawSharedFrame(RenderTarget target) {
        if (connection != null && frameShare != null && connection.isConnected() && Minecraft.getInstance().level != null) {
            if (!frameShare.isLive()) {
                return false;
            } else {
                frameShare.draw(target);
                return true;
            }
        } else {
            return false;
        }
    }

    public static void drainInput() {
        if (connection != null) {
            UeInput.drain();
        }

    }

    public static void onFrameRendered(GameRenderer renderer) {
        EntityCensus.frame(renderer.gameRenderState().levelRenderState);
        if (connection != null && connection.isConnected()) {
            RenderTarget mainTarget = renderer.mainRenderTarget();
            if (mainTarget.width != sentWidth || mainTarget.height != sentHeight || connection.generation() != resolutionGeneration) {
                sentWidth = mainTarget.width;
                sentHeight = mainTarget.height;
                resolutionGeneration = connection.generation();
                connection.send(BridgeProtocol.resolution(sentWidth, sentHeight));
            }

            if (overlayActive()) {
                boolean grabbed = Minecraft.getInstance().mouseHandler.isMouseGrabbed();
                if (grabbed != lastGrabbed || connection.generation() != cursorGeneration) {
                    lastGrabbed = grabbed;
                    cursorGeneration = connection.generation();
                    connection.send(BridgeProtocol.cursor(grabbed));
                }

                if (uiCapture == null) {
                    uiCapture = new UiCapture(connection);
                }

                uiCapture.capture(renderer.mainRenderTarget());
            }

            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
                float rain = level.getRainLevel(partialTick);
                float thunder = level.getThunderLevel(partialTick);
                boolean snow = false;
                Camera weatherCamera = renderer.mainCamera();
                if (weatherCamera != null && weatherCamera.isInitialized()) {
                    snow = level.getPrecipitationAt(weatherCamera.blockPosition()) == Precipitation.SNOW;
                }

                if (Math.abs(rain - sentRain) > 0.002F || Math.abs(thunder - sentThunder) > 0.002F || snow != sentSnow || connection.generation() != weatherGeneration) {
                    sentRain = rain;
                    sentThunder = thunder;
                    sentSnow = snow;
                    weatherGeneration = connection.generation();
                    connection.sendUrgent(BridgeProtocol.weather(rain, thunder, snow));
                }
            }

            if (level != null && connection.isConnected()) {
                BlockBreakExport.update(level, connection);
            }

            Camera tintCamera = renderer.mainCamera();
            if (level != null && tintCamera != null && tintCamera.isInitialized()) {
                Vector3fc tint = (Vector3fc)tintCamera.attributeProbe().getValue(EnvironmentAttributes.BLOCK_LIGHT_TINT, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
                if (Math.abs(tint.x() - SENT_TINT[0]) > 0.002F || Math.abs(tint.y() - SENT_TINT[1]) > 0.002F || Math.abs(tint.z() - SENT_TINT[2]) > 0.002F || connection.generation() != tintGeneration) {
                    SENT_TINT[0] = tint.x();
                    SENT_TINT[1] = tint.y();
                    SENT_TINT[2] = tint.z();
                    tintGeneration = connection.generation();
                    connection.sendUrgent(BridgeProtocol.blockLight(tint.x(), tint.y(), tint.z()));
                }
            }

            Camera particleCamera;
            if (ENTITIES) {
                if (entityCapture == null) {
                    entityCapture = new EntityCapture(connection);
                }

                entityCapture.capture(Minecraft.getInstance(), renderer);
                particleCamera = renderer.mainCamera();
                if (particleCamera != null && particleCamera.isInitialized()) {
                    if (particleCapture == null) {
                        particleCapture = new ParticleCapture(connection);
                    }

                    particleCapture.capture(Minecraft.getInstance(), renderer, particleCamera);
                }
            }

            if (tracker != null) {
                tracker.flushUrgent();
            }

            particleCamera = renderer.mainCamera();
            if (level != null && particleCamera != null && particleCamera.isInitialized()) {
                Vec3 position = particleCamera.position();
                Vector3fc forward = particleCamera.forwardVector();
                Vector3fc up = particleCamera.upVector();
                connection.sendPose(BridgeProtocol.pose(position.x, position.y, position.z, forward.x(), forward.y(), forward.z(), up.x(), up.y(), up.z(), particleCamera.getFov(), worldClock(level), frame++));
            }
        }
    }
}
