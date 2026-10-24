//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.lang.reflect.Method;

public final class DhSupport {
    private static final String HANDLER = "com.seibel.distanthorizons.core.render.RenderThreadTaskHandler";
    private static boolean resolved;
    private static Object handler;
    private static Method run;
    private static boolean logged;
    private static long windowStart = System.currentTimeMillis();
    private static long worst;
    private static int calls;

    private DhSupport() {
    }

    public static void runRenderThreadTasks() {
        ReflectiveOperationException e;
        if (!resolved) {
            resolved = true;

            try {
                Class<?> type = Class.forName("com.seibel.distanthorizons.core.render.RenderThreadTaskHandler");
                handler = type.getField("INSTANCE").get((Object)null);
                run = type.getMethod("runRenderThreadTasks");
            } catch (ClassNotFoundException var7) {
            } catch (RuntimeException | ReflectiveOperationException var8) {
                e = var8;
                TmcgiUeBridgeClient.LOGGER.warn("Distant Horizons is there, but its render thread tasks cannot be reached", e);
            }
        }

        if (run != null) {
            try {
                long started = System.nanoTime();
                run.invoke(handler);
                long took = System.nanoTime() - started;
                ++calls;
                if (took > worst) {
                    worst = took;
                }

                long now = System.currentTimeMillis();
                if (now - windowStart > 5000L) {
                    if (worst > 20000000L) {
                        TmcgiUeBridgeClient.LOGGER.info("Distant Horizons render thread tasks: slowest {} ms in {} calls over the last {} ms", new Object[]{worst / 1000000L, calls, now - windowStart});
                    }

                    windowStart = now;
                    calls = 0;
                    worst = 0L;
                }

                if (!logged) {
                    logged = true;
                    TmcgiUeBridgeClient.LOGGER.info("Distant Horizons: its render thread tasks are run while Minecraft's world drawing is skipped");
                }
            } catch (RuntimeException | ReflectiveOperationException var6) {
                e = var6;
                TmcgiUeBridgeClient.LOGGER.warn("The render thread tasks of Distant Horizons failed, they are not run again", e);
                run = null;
            }

        }
    }
}
