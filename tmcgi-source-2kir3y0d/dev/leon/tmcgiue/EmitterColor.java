//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

final class EmitterColor {
    private static final Field ORIGINAL_IMAGE = originalImageField();
    private static final IdentityHashMap<BlockState, Integer> CACHE = new IdentityHashMap();
    private static final Map<String, List<Override>> OVERRIDES = new HashMap();
    private static boolean overridesLoaded;

    private EmitterColor() {
    }

    private static Field originalImageField() {
        try {
            Field field = SpriteContents.class.getDeclaredField("originalImage");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException var1) {
            return null;
        }
    }

    static int of(BlockState state) {
        Integer cached = (Integer)CACHE.get(state);
        if (cached != null) {
            return cached;
        } else {
            int color = compute(state);
            CACHE.put(state, color);
            if (CACHE.size() <= 120) {
                TmcgiUeBridgeClient.LOGGER.info("Light colour of {}: {}", BlockStateNames.of(state), color < 0 ? "none (dimension tint)" : String.format("#%06X", color));
            }

            return color;
        }
    }

    private static int compute(BlockState state) {
        loadOverrides();
        Identifier key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        List<Override> overrides = (List)OVERRIDES.get(key.toString());
        if (overrides != null) {
            Iterator var3 = overrides.iterator();

            while(var3.hasNext()) {
                Override override = (Override)var3.next();
                if (matches(state, override.conditions)) {
                    return override.color;
                }
            }
        }

        if (key.getNamespace().equals("minecraft")) {
            int vanilla = vanilla(key.getPath());
            if (vanilla >= 0) {
                return vanilla;
            }
        }

        return fromTexture(state);
    }

    private static boolean matches(BlockState state, Map<String, String> conditions) {
        Iterator var2 = conditions.entrySet().iterator();

        boolean found;
        do {
            if (!var2.hasNext()) {
                return true;
            }

            Map.Entry<String, String> condition = (Map.Entry)var2.next();
            found = false;
            Iterator var5 = state.getProperties().iterator();

            while(var5.hasNext()) {
                Property<?> property = (Property)var5.next();
                if (property.getName().equals(condition.getKey())) {
                    found = valueName(state, property).equals(condition.getValue());
                    break;
                }
            }
        } while(found);

        return false;
    }

    private static String valueName(BlockState state, Property<?> property) {
        return property.getName(state.getValue(property));
    }

    private static int rgb(float red, float green, float blue) {
        return Math.round(red * 255.0F) << 16 | Math.round(green * 255.0F) << 8 | Math.round(blue * 255.0F);
    }

    private static int vanilla(String path) {
        boolean soul = path.contains("soul_");
        if (!soul || !path.contains("torch") && !path.contains("lantern") && !path.contains("fire") && !path.contains("campfire")) {
            if (!path.contains("sea_lantern") && !path.contains("end_rod")) {
                if (path.contains("pearlescent_froglight")) {
                    return rgb(1.0F, 0.64F, 0.88F);
                } else if (path.contains("verdant_froglight")) {
                    return rgb(0.64F, 1.0F, 0.62F);
                } else if (!path.contains("ochre_froglight") && !path.contains("shroomlight")) {
                    if (path.contains("redstone") && path.contains("torch")) {
                        return rgb(1.0F, 0.12F, 0.05F);
                    } else if (path.contains("redstone_lamp")) {
                        return rgb(1.0F, 0.46F, 0.18F);
                    } else {
                        return !path.contains("glowstone") && !path.contains("jack_o_lantern") && !path.contains("lantern") && !path.contains("torch") && !path.contains("campfire") && !path.contains("fire") ? -1 : rgb(1.0F, 0.64F, 0.3F);
                    }
                } else {
                    return rgb(1.0F, 0.62F, 0.28F);
                }
            } else {
                return rgb(0.58F, 0.86F, 1.0F);
            }
        } else {
            return rgb(0.22F, 0.66F, 1.0F);
        }
    }

    private static int fromTexture(BlockState state) {
        if (ORIGINAL_IMAGE == null) {
            return -1;
        } else {
            try {
                TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite();
                NativeImage image = (NativeImage)ORIGINAL_IMAGE.get(sprite.contents());
                int width = image.getWidth();
                int height = Math.min(image.getHeight(), width);
                double sumR = 0.0;
                double sumG = 0.0;
                double sumB = 0.0;
                double sumWeight = 0.0;

                for(int y = 0; y < height; ++y) {
                    for(int x = 0; x < width; ++x) {
                        int argb = image.getPixel(x, y);
                        if (argb >>> 24 >= 128) {
                            double r = (double)(argb >> 16 & 255) / 255.0;
                            double g = (double)(argb >> 8 & 255) / 255.0;
                            double b = (double)(argb & 255) / 255.0;
                            double brightness = Math.max(r, Math.max(g, b));
                            double weight = brightness * brightness * brightness;
                            sumR += r * weight;
                            sumG += g * weight;
                            sumB += b * weight;
                            sumWeight += weight;
                        }
                    }
                }

                if (sumWeight <= 0.0) {
                    return -1;
                } else {
                    double r = sumR / sumWeight;
                    double g = sumG / sumWeight;
                    double b = sumB / sumWeight;
                    double max = Math.max(r, Math.max(g, b));
                    if (max < 0.05) {
                        return -1;
                    } else {
                        int red = (int)Math.round((0.25 + 0.75 * r / max) * 255.0);
                        int green = (int)Math.round((0.25 + 0.75 * g / max) * 255.0);
                        int blue = (int)Math.round((0.25 + 0.75 * b / max) * 255.0);
                        return red << 16 | green << 8 | blue;
                    }
                }
            } catch (RuntimeException | ReflectiveOperationException var26) {
                return -1;
            }
        }
    }

    private static void loadOverrides() {
        if (!overridesLoaded) {
            overridesLoaded = true;
            Path file = FabricLoader.getInstance().getConfigDir().resolve("tmcgi-ue-bridge-lights.properties");
            if (Files.exists(file, new LinkOption[0])) {
                Properties properties = new Properties();

                try {
                    InputStream in = Files.newInputStream(file);

                    try {
                        properties.load(in);
                    } catch (Throwable var17) {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (Throwable var16) {
                                var17.addSuppressed(var16);
                            }
                        }

                        throw var17;
                    }

                    if (in != null) {
                        in.close();
                    }
                } catch (IOException var19) {
                    IOException e = var19;
                    TmcgiUeBridgeClient.LOGGER.warn("Cannot read {}: {}", file, e.toString());
                    return;
                }

                int entries = 0;
                Iterator var3 = properties.stringPropertyNames().iterator();

                while(true) {
                    String name;
                    int color;
                    while(true) {
                        do {
                            if (!var3.hasNext()) {
                                TmcgiUeBridgeClient.LOGGER.info("{} light colours for {} blocks read from {}", new Object[]{entries, OVERRIDES.size(), file});
                                return;
                            }

                            name = (String)var3.next();
                        } while(!name.startsWith("light."));

                        try {
                            color = Integer.parseInt(name.substring("light.".length()).trim(), 16) & 16777215;
                            break;
                        } catch (NumberFormatException var18) {
                            TmcgiUeBridgeClient.LOGGER.warn("{}: '{}' is not a light.RRGGBB colour", file, name);
                        }
                    }

                    String[] var6 = properties.getProperty(name).trim().split("\\s+");
                    int var7 = var6.length;

                    for(int var8 = 0; var8 < var7; ++var8) {
                        String entry = var6[var8];
                        if (!entry.isEmpty()) {
                            String[] parts = entry.split(":");
                            byte next;
                            String id;
                            if (parts.length >= 2 && !parts[1].contains("=")) {
                                id = parts[0] + ":" + parts[1];
                                next = 2;
                            } else {
                                id = "minecraft:" + parts[0];
                                next = 1;
                            }

                            Map<String, String> conditions = new HashMap();

                            for(int i = next; i < parts.length; ++i) {
                                int equals = parts[i].indexOf(61);
                                if (equals > 0) {
                                    conditions.put(parts[i].substring(0, equals), parts[i].substring(equals + 1));
                                }
                            }

                            ((List)OVERRIDES.computeIfAbsent(id, (k) -> {
                                return new ArrayList();
                            })).add(new Override(conditions, color));
                            ++entries;
                        }
                    }
                }
            }
        }
    }

    private static record Override(Map<String, String> conditions, int color) {
        private Override(Map<String, String> conditions, int color) {
            this.conditions = conditions;
            this.color = color;
        }

        public Map<String, String> conditions() {
            return this.conditions;
        }

        public int color() {
            return this.color;
        }
    }
}
