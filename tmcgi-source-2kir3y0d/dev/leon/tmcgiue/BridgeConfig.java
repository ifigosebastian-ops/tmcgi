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
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

final class BridgeConfig {
    boolean enabled = true;
    String host = "127.0.0.1";
    int port = 9477;
    int radiusChunks = 6;
    boolean followRenderDistance = true;
    int maxRadiusChunks = 32;
    boolean launch = true;
    String rendererPath = "";
    String rendererArguments = "";
    boolean lod = true;
    int lodMaxChunks = 256;
    boolean compatWorldRender = true;
    boolean captureCustomGeometry = true;
    String hiddenParticles = "minecraft:rain";

    BridgeConfig() {
    }

    static BridgeConfig load() {
        BridgeConfig config = new BridgeConfig();
        Path file = FabricLoader.getInstance().getConfigDir().resolve("tmcgi-ue-bridge.properties");
        Properties properties = new Properties();
        IOException e;
        if (Files.exists(file, new LinkOption[0])) {
            try {
                InputStream in = Files.newInputStream(file);

                try {
                    properties.load(in);
                } catch (Throwable var10) {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Throwable var7) {
                            var10.addSuppressed(var7);
                        }
                    }

                    throw var10;
                }

                if (in != null) {
                    in.close();
                }
            } catch (IOException var11) {
                e = var11;
                TmcgiUeBridgeClient.LOGGER.warn("Cannot read {}: {}", file, e.toString());
            }
        }

        config.enabled = Boolean.parseBoolean(properties.getProperty("enabled", Boolean.toString(config.enabled)));
        config.host = properties.getProperty("host", config.host).trim();
        config.port = parseInt(properties.getProperty("port"), config.port);
        config.radiusChunks = Math.max(1, Math.min(32, parseInt(properties.getProperty("radiusChunks"), config.radiusChunks)));
        config.followRenderDistance = Boolean.parseBoolean(properties.getProperty("radius.follow", Boolean.toString(config.followRenderDistance)));
        config.maxRadiusChunks = Math.max(2, Math.min(64, parseInt(properties.getProperty("radius.max"), config.maxRadiusChunks)));
        config.launch = Boolean.parseBoolean(properties.getProperty("launch", Boolean.toString(config.launch)));
        config.rendererPath = properties.getProperty("renderer.path", config.rendererPath).trim();
        config.rendererArguments = properties.getProperty("renderer.arguments", config.rendererArguments).trim();
        config.hiddenParticles = properties.getProperty("particles.hidden", config.hiddenParticles).trim();
        config.lod = Boolean.parseBoolean(properties.getProperty("lod", Boolean.toString(config.lod)));
        config.compatWorldRender = Boolean.parseBoolean(properties.getProperty("compat.worldRender", Boolean.toString(config.compatWorldRender)));
        config.captureCustomGeometry = Boolean.parseBoolean(properties.getProperty("capture.customGeometry", Boolean.toString(config.captureCustomGeometry)));
        config.lodMaxChunks = Math.max(32, Math.min(512, parseInt(properties.getProperty("lod.maxChunks"), config.lodMaxChunks)));
        properties.setProperty("enabled", Boolean.toString(config.enabled));
        properties.setProperty("host", config.host);
        properties.setProperty("port", Integer.toString(config.port));
        properties.setProperty("radiusChunks", Integer.toString(config.radiusChunks));
        properties.setProperty("radius.follow", Boolean.toString(config.followRenderDistance));
        properties.setProperty("radius.max", Integer.toString(config.maxRadiusChunks));
        properties.setProperty("launch", Boolean.toString(config.launch));
        properties.setProperty("renderer.path", config.rendererPath);
        properties.setProperty("renderer.arguments", config.rendererArguments);
        properties.setProperty("particles.hidden", config.hiddenParticles);
        properties.setProperty("lod", Boolean.toString(config.lod));
        properties.setProperty("compat.worldRender", Boolean.toString(config.compatWorldRender));
        properties.setProperty("capture.customGeometry", Boolean.toString(config.captureCustomGeometry));
        properties.setProperty("lod.maxChunks", Integer.toString(config.lodMaxChunks));

        try {
            OutputStream out = Files.newOutputStream(file);

            try {
                properties.store(out, "TMCGI UE bridge: where the Unreal Engine renderer listens, how many chunks around the camera are sent, and whether the renderer next to the jar is started (launch, renderer.path, renderer.arguments)");
            } catch (Throwable var8) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Throwable var6) {
                        var8.addSuppressed(var6);
                    }
                }

                throw var8;
            }

            if (out != null) {
                out.close();
            }
        } catch (IOException var9) {
            e = var9;
            TmcgiUeBridgeClient.LOGGER.warn("Cannot write {}: {}", file, e.toString());
        }

        return config;
    }

    static void store(String key, String value) {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("tmcgi-ue-bridge.properties");
        Properties properties = new Properties();

        try {
            if (Files.exists(file, new LinkOption[0])) {
                InputStream in = Files.newInputStream(file);

                try {
                    properties.load(in);
                } catch (Throwable var10) {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Throwable var8) {
                            var10.addSuppressed(var8);
                        }
                    }

                    throw var10;
                }

                if (in != null) {
                    in.close();
                }
            }

            properties.setProperty(key, value);
            OutputStream out = Files.newOutputStream(file);

            try {
                properties.store(out, "TMCGI UE bridge");
            } catch (Throwable var9) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Throwable var7) {
                        var9.addSuppressed(var7);
                    }
                }

                throw var9;
            }

            if (out != null) {
                out.close();
            }
        } catch (IOException var11) {
            IOException e = var11;
            TmcgiUeBridgeClient.LOGGER.warn("Cannot write {}: {}", file, e.toString());
        }

    }

    private static int parseInt(String text, int fallback) {
        try {
            return text == null ? fallback : Integer.parseInt(text.trim());
        } catch (NumberFormatException var3) {
            return fallback;
        }
    }
}
