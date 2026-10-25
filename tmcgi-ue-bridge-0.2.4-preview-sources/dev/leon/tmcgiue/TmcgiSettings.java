//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

public final class TmcgiSettings {
    static final int[] QUALITY_LEVELS = new int[]{0, 1, 2, 3, 4};
    static final String[] QUALITY_NAMES = new String[]{"quality.low", "quality.medium", "quality.high", "quality.epic", "quality.cinematic"};
    static final int[] RESOLUTION_LEVELS = new int[]{50, 60, 70, 75, 80, 85, 90, 100};
    static final int[] ANTI_ALIASING_METHODS = new int[]{1, 2, 4};
    static final String[] ANTI_ALIASING_NAMES = new String[]{"FXAA", "TAA", "TSR"};
    static final int BLOCK_LIGHT_MIN = 2;
    static final int BLOCK_LIGHT_MAX = 200;
    static final int[] RAY_TRACING_DISTANCES = new int[]{2, 3, 4, 6, 8, 12, 0};
    static final int[] CLOUD_MODES = new int[]{0, 1, 2, 3};
    static final String[] CLOUD_NAMES = new String[]{"cloud.background", "cloud.performance", "cloud.quality", "cloud.cinematic"};
    static final String[] PRESET_NAMES = new String[]{"preset.low", "preset.medium", "preset.high", "preset.ultra"};
    static int cloudMode;
    static boolean volumetricFog;
    static int resolutionQuality;
    static int viewDistanceQuality;
    static int antiAliasingQuality;
    static int shadowQuality;
    static int globalIlluminationQuality;
    static int reflectionQuality;
    static int postProcessQuality;
    static int textureQuality;
    static int effectsQuality;
    static int shadingQuality;
    static int antiAliasingMethod;
    static boolean lumen;
    static boolean megaLights;
    static boolean lightLimit;
    static boolean motionBlur;
    static boolean dynamicLights;
    static int blockLightIntensity;
    static boolean hardwareRayTracing;
    static int rayTracingDistance;
    static boolean statUnit;
    static boolean engineWarnings;
    static boolean bakingDebug;
    static boolean launchedWithRayTracing = true;
    private static volatile int status = -1;
    private static String lastWarning = "";
    static final int STATUS_RAY_TRACING_SUPPORTED = 1;
    static final int STATUS_RAY_TRACING_ALLOWED = 2;
    static final int STATUS_RAY_TRACING_ENABLED = 4;
    static final int STATUS_LUMEN = 8;
    static final int STATUS_MEGA_LIGHTS_ALLOWED = 16;
    private static boolean pending;
    private static final int[][] PRESETS;

    private TmcgiSettings() {
    }

    private static void setDefaults() {
        cloudMode = 1;
        volumetricFog = true;
        resolutionQuality = 50;
        viewDistanceQuality = 1;
        antiAliasingQuality = 1;
        shadowQuality = 1;
        globalIlluminationQuality = 1;
        reflectionQuality = 2;
        postProcessQuality = 2;
        textureQuality = 2;
        effectsQuality = 2;
        shadingQuality = 2;
        antiAliasingMethod = 4;
        lumen = true;
        megaLights = true;
        lightLimit = false;
        motionBlur = true;
        dynamicLights = true;
        blockLightIntensity = 100;
        hardwareRayTracing = true;
        rayTracingDistance = 2;
        statUnit = true;
        engineWarnings = true;
        bakingDebug = true;
    }

    static void resetToDefaults() {
        setDefaults();
        changed();
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("tmcgi-ue-settings.properties");
    }

    static void load() {
        Properties p = new Properties();
        Path file = file();
        if (Files.exists(file, new LinkOption[0])) {
            try {
                InputStream in = Files.newInputStream(file);

                try {
                    p.load(in);
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
            } catch (IOException var7) {
                IOException e = var7;
                TmcgiUeBridgeClient.LOGGER.warn("Cannot read {}: {}", file, e.toString());
            }
        }

        cloudMode = pick(p, "weather.cloudMode", cloudMode, CLOUD_MODES);
        volumetricFog = bool(p, "weather.volumetricFog", volumetricFog);
        resolutionQuality = Math.max(10, Math.min(100, integer(p, "render.resolutionQuality", resolutionQuality)));
        viewDistanceQuality = pick(p, "render.viewDistanceQuality", viewDistanceQuality, QUALITY_LEVELS);
        antiAliasingQuality = pick(p, "render.antiAliasingQuality", antiAliasingQuality, QUALITY_LEVELS);
        shadowQuality = pick(p, "render.shadowQuality", shadowQuality, QUALITY_LEVELS);
        globalIlluminationQuality = pick(p, "render.globalIlluminationQuality", globalIlluminationQuality, QUALITY_LEVELS);
        reflectionQuality = pick(p, "render.reflectionQuality", reflectionQuality, QUALITY_LEVELS);
        postProcessQuality = pick(p, "render.postProcessQuality", postProcessQuality, QUALITY_LEVELS);
        textureQuality = pick(p, "render.textureQuality", textureQuality, QUALITY_LEVELS);
        effectsQuality = pick(p, "render.effectsQuality", effectsQuality, QUALITY_LEVELS);
        shadingQuality = pick(p, "render.shadingQuality", shadingQuality, QUALITY_LEVELS);
        antiAliasingMethod = pick(p, "render.antiAliasingMethod", antiAliasingMethod, ANTI_ALIASING_METHODS);
        lumen = bool(p, "render.lumen", lumen);
        megaLights = bool(p, "render.megaLights", megaLights);
        lightLimit = bool(p, "render.lightLimit", lightLimit);
        motionBlur = bool(p, "render.motionBlur", motionBlur);
        dynamicLights = bool(p, "render.dynamicLights", dynamicLights);
        blockLightIntensity = Math.max(2, Math.min(200, integer(p, "render.blockLightIntensity", blockLightIntensity)));
        hardwareRayTracing = bool(p, "render.hardwareRayTracing", hardwareRayTracing);
        rayTracingDistance = pick(p, "render.rayTracingDistance", rayTracingDistance, RAY_TRACING_DISTANCES);
        statUnit = bool(p, "ue.statUnit", statUnit);
        engineWarnings = bool(p, "ue.engineWarnings", engineWarnings);
        bakingDebug = bool(p, "ue.bakingDebug", bakingDebug);
        launchedWithRayTracing = hardwareRayTracing;
        save();
    }

    private static int integer(Properties p, String key, int fallback) {
        try {
            return Integer.parseInt(p.getProperty(key, Integer.toString(fallback)).trim());
        } catch (NumberFormatException var4) {
            return fallback;
        }
    }

    private static int pick(Properties p, String key, int fallback, int[] allowed) {
        int value = integer(p, key, fallback);
        int[] var5 = allowed;
        int var6 = allowed.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            int candidate = var5[var7];
            if (candidate == value) {
                return value;
            }
        }

        return fallback;
    }

    private static boolean bool(Properties p, String key, boolean fallback) {
        return Boolean.parseBoolean(p.getProperty(key, Boolean.toString(fallback)).trim());
    }

    private static Map<String, String> values() {
        Map<String, String> m = new LinkedHashMap();
        m.put("weather.cloudMode", Integer.toString(cloudMode));
        m.put("weather.volumetricFog", Boolean.toString(volumetricFog));
        m.put("render.resolutionQuality", Integer.toString(resolutionQuality));
        m.put("render.viewDistanceQuality", Integer.toString(viewDistanceQuality));
        m.put("render.antiAliasingQuality", Integer.toString(antiAliasingQuality));
        m.put("render.shadowQuality", Integer.toString(shadowQuality));
        m.put("render.globalIlluminationQuality", Integer.toString(globalIlluminationQuality));
        m.put("render.reflectionQuality", Integer.toString(reflectionQuality));
        m.put("render.postProcessQuality", Integer.toString(postProcessQuality));
        m.put("render.textureQuality", Integer.toString(textureQuality));
        m.put("render.effectsQuality", Integer.toString(effectsQuality));
        m.put("render.shadingQuality", Integer.toString(shadingQuality));
        m.put("render.antiAliasingMethod", Integer.toString(antiAliasingMethod));
        m.put("render.lumen", Boolean.toString(lumen));
        m.put("render.megaLights", Boolean.toString(megaLights));
        m.put("render.lightLimit", Boolean.toString(lightLimit));
        m.put("render.motionBlur", Boolean.toString(motionBlur));
        m.put("render.dynamicLights", Boolean.toString(dynamicLights));
        m.put("render.blockLightIntensity", Integer.toString(blockLightIntensity));
        m.put("render.hardwareRayTracing", Boolean.toString(hardwareRayTracing));
        m.put("render.rayTracingDistance", Integer.toString(rayTracingDistance));
        m.put("ue.statUnit", Boolean.toString(statUnit));
        m.put("ue.engineWarnings", Boolean.toString(engineWarnings));
        m.put("ue.bakingDebug", Boolean.toString(bakingDebug));
        return m;
    }

    private static void save() {
        Properties p = new Properties();
        Map var10000 = values();
        Objects.requireNonNull(p);
        var10000.forEach(p::setProperty);

        try {
            OutputStream out = Files.newOutputStream(file());

            try {
                p.store(out, "TMCGI renderer settings (the TMCGI page in Minecraft's video settings)");
            } catch (Throwable var5) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Throwable var4) {
                        var5.addSuppressed(var4);
                    }
                }

                throw var5;
            }

            if (out != null) {
                out.close();
            }
        } catch (IOException var6) {
            IOException e = var6;
            TmcgiUeBridgeClient.LOGGER.warn("Cannot write {}: {}", file(), e.toString());
        }

    }

    static byte[] frame() {
        Map<String, String> m = values();
        m.remove("render.hardwareRayTracing");
        return BridgeProtocol.settings(m);
    }

    static void changed() {
        save();
        pending = true;
    }

    static void changedLive() {
        save();
        TmcgiUeBridgeClient.sendSettings(frame());
    }

    static void apply() {
        if (pending) {
            pending = false;
            Minecraft minecraft = Minecraft.getInstance();
            boolean restarted = RendererLauncher.restart();
            if (minecraft.gui != null) {
                minecraft.gui.hud.getChat().addClientSystemMessage(TmcgiSettingsScreen.t(restarted ? "chat.settingsRestart" : "chat.settingsManual"));
            }

            if (restarted) {
                return;
            }
        }

        checkWarnings();
    }

    static void resourcePacksChanged() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean restarted = RendererLauncher.restart();
        if (minecraft.gui != null) {
            minecraft.gui.hud.getChat().addClientSystemMessage(TmcgiSettingsScreen.t(restarted ? "chat.packsRestart" : "chat.packsManual"));
        }

    }

    static void onConnected() {
        status = -1;
        lastWarning = "";
        pending = false;
    }

    static int status() {
        return status;
    }

    static void onStatus(int flags) {
        status = flags;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(TmcgiSettings::checkWarnings);
    }

    static void checkWarnings() {
        int flags = status;
        if (flags >= 0) {
            boolean rayTracing = (flags & 4) != 0;
            List<String> lines = new ArrayList();
            if (hardwareRayTracing && launchedWithRayTracing && !rayTracing) {
                lines.add("chat.noRayTracing");
            }

            if (megaLights && !rayTracing) {
                lines.add("chat.noMegaLights");
            }

            String text = String.join("\n", lines);
            if (!text.equals(lastWarning)) {
                lastWarning = text;
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.gui != null) {
                    Iterator var5 = lines.iterator();

                    while(var5.hasNext()) {
                        String line = (String)var5.next();
                        minecraft.gui.hud.getChat().addClientSystemMessage(TmcgiSettingsScreen.t(line).withStyle(ChatFormatting.RED));
                    }

                }
            }
        }
    }

    static void applyPreset(int index) {
        int[] preset = PRESETS[index];
        viewDistanceQuality = antiAliasingQuality = shadowQuality = globalIlluminationQuality = reflectionQuality = preset[0];
        postProcessQuality = textureQuality = effectsQuality = shadingQuality = preset[0];
        resolutionQuality = preset[1];
        antiAliasingMethod = preset[2];
        cloudMode = preset[3];
        volumetricFog = preset[4] != 0;
        lumen = preset[5] != 0;
        megaLights = preset[6] != 0;
        changed();
    }

    static int currentPreset() {
        for(int i = 0; i < PRESETS.length; ++i) {
            int[] preset = PRESETS[i];
            int level = preset[0];
            if (viewDistanceQuality == level && antiAliasingQuality == level && shadowQuality == level && globalIlluminationQuality == level && reflectionQuality == level && postProcessQuality == level && textureQuality == level && effectsQuality == level && shadingQuality == level && resolutionQuality == preset[1] && antiAliasingMethod == preset[2] && cloudMode == preset[3] && volumetricFog == (preset[4] != 0) && lumen == (preset[5] != 0) && megaLights == (preset[6] != 0)) {
                return i;
            }
        }

        return -1;
    }

    static {
        setDefaults();
        PRESETS = new int[][]{{0, 70, 4, 0, 0, 0, 0}, {1, 85, 4, 1, 1, 1, 0}, {2, 100, 4, 1, 1, 1, 1}, {3, 100, 4, 2, 1, 1, 1}};
    }
}
