//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.LevelRenderState;

public final class EntityCensus {
    private static final boolean ENABLED = System.getenv("TMCGI_CENSUS") != null;
    private static final Map<String, Integer> CALLS = new TreeMap();
    private static final Map<String, Integer> MODELS = new TreeMap();
    private static final Map<String, Integer> TYPES = new TreeMap();
    private static int frames;
    private static long windowStart = System.nanoTime();

    private EntityCensus() {
    }

    public static void call(String kind) {
        if (ENABLED) {
            CALLS.merge(kind, 1, Integer::sum);
        }

    }

    public static void model(Model<?> model, RenderType renderType) {
        if (ENABLED) {
            CALLS.merge("submitModel", 1, Integer::sum);
            String var10000 = model.getClass().getName();
            String key = var10000 + " parts=" + model.allParts().size() + " type=" + String.valueOf(renderType);
            MODELS.merge(key, 1, Integer::sum);
        }
    }

    public static void head(LevelRenderState state) {
        if (ENABLED) {
            CALLS.merge("entity states at render start", state.entityRenderStates.size(), Integer::sum);
            CALLS.merge("block entity states at render start", state.blockEntityRenderStates.size(), Integer::sum);
            CALLS.merge("lastEntityRenderStateCount", state.lastEntityRenderStateCount, Integer::sum);
        }

    }

    public static void frame(LevelRenderState state) {
        if (ENABLED) {
            ++frames;
            Iterator var1 = state.entityRenderStates.iterator();

            while(var1.hasNext()) {
                EntityRenderState entity = (EntityRenderState)var1.next();
                TYPES.merge(String.valueOf(entity.entityType), 1, Integer::sum);
            }

            TYPES.merge("(block entities)", state.blockEntityRenderStates.size(), Integer::sum);
            long now = System.nanoTime();
            if (now - windowStart >= 5000000000L && frames != 0) {
                TmcgiUeBridgeClient.LOGGER.info("Census over {} frames (per frame):", frames);
                CALLS.forEach((k, v) -> {
                    TmcgiUeBridgeClient.LOGGER.info("  call {}: {}", k, String.format("%.2f", (double)v / (double)frames));
                });
                MODELS.forEach((k, v) -> {
                    TmcgiUeBridgeClient.LOGGER.info("  model {}: {}", k, String.format("%.2f", (double)v / (double)frames));
                });
                TYPES.forEach((k, v) -> {
                    TmcgiUeBridgeClient.LOGGER.info("  entity {}: {}", k, String.format("%.2f", (double)v / (double)frames));
                });
                CALLS.clear();
                MODELS.clear();
                TYPES.clear();
                frames = 0;
                windowStart = now;
            }
        }
    }
}
