//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import com.mojang.blaze3d.platform.Window;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class UeInput {
    static final int KEY = 129;
    static final int CHAR = 130;
    static final int BUTTON = 131;
    static final int MOVE = 132;
    static final int SCROLL = 133;
    private static final ConcurrentLinkedQueue<Runnable> QUEUE = new ConcurrentLinkedQueue();
    private static final ConcurrentLinkedQueue<Long> QUEUED_AT = new ConcurrentLinkedQueue();
    private static int eventsInWindow;
    private static long maxWaitNanos;
    private static int framesInWindow;
    private static long maxFrameNanos;
    private static long lastFrame;
    private static long windowStart = System.nanoTime();
    private static final boolean[] KEY_DOWN = new boolean[512];
    private static double cursorX;
    private static double cursorY;

    private UeInput() {
    }

    private static void enqueue(Runnable task) {
        QUEUED_AT.add(System.nanoTime());
        QUEUE.add(task);
    }

    public static boolean isKeyDown(int scancode) {
        return scancode >= 0 && scancode < KEY_DOWN.length && KEY_DOWN[scancode];
    }

    static void queue(int type, ByteBuffer in) {
        int scancode;
        int action;
        int modifiers;
        switch (type) {
            case 129:
                scancode = in.getInt();
                action = in.getInt();
                modifiers = in.getInt();
                if (scancode >= 0 && scancode < KEY_DOWN.length) {
                    KEY_DOWN[scancode] = action != 0;
                }

                enqueue(() -> {
                    Minecraft.getInstance().keyboardHandler.keyPress(handle(), action, new KeyEvent(scancode, scancode, modifiers));
                });
                break;
            case 130:
                scancode = in.getInt();
                enqueue(() -> {
                    Minecraft.getInstance().keyboardHandler.charTyped(handle(), new CharacterEvent(scancode));
                });
                break;
            case 131:
                scancode = in.getInt();
                action = in.getInt();
                modifiers = in.getInt();
                enqueue(() -> {
                    Minecraft.getInstance().mouseHandler.onButton(handle(), new MouseButtonInfo(scancode, modifiers), action);
                });
                break;
            case 132:
                scancode = in.getInt();
                double a = in.getDouble();
                double b = in.getDouble();
                enqueue(() -> {
                    move(scancode, a, b);
                });
                break;
            case 133:
                double x = in.getDouble();
                double y = in.getDouble();
                enqueue(() -> {
                    Minecraft.getInstance().mouseHandler.onScroll(handle(), x, y);
                });
        }

    }

    static void drain() {
        long now = System.nanoTime();
        if (lastFrame != 0L) {
            maxFrameNanos = Math.max(maxFrameNanos, now - lastFrame);
        }

        lastFrame = now;
        ++framesInWindow;

        Runnable task;
        while((task = (Runnable)QUEUE.poll()) != null) {
            Long queuedAt = (Long)QUEUED_AT.poll();
            if (queuedAt != null) {
                maxWaitNanos = Math.max(maxWaitNanos, now - queuedAt);
            }

            ++eventsInWindow;

            try {
                task.run();
            } catch (RuntimeException var5) {
                RuntimeException e = var5;
                TmcgiUeBridgeClient.LOGGER.warn("Forwarding input failed", e);
            }
        }

        logWindow(now);
    }

    private static void logWindow(long now) {
        if (now - windowStart >= 2000000000L) {
            StringBuilder keys = new StringBuilder();

            for(int i = 0; i < KEY_DOWN.length; ++i) {
                if (KEY_DOWN[i]) {
                    keys.append(i).append(' ');
                }
            }

            TmcgiUeBridgeClient.LOGGER.info("Input: {} events, longest wait {} ms, {} frames, longest frame {} ms, keys down [{}]", new Object[]{eventsInWindow, maxWaitNanos / 1000000L, framesInWindow, maxFrameNanos / 1000000L, keys.toString().trim()});
            eventsInWindow = 0;
            maxWaitNanos = 0L;
            framesInWindow = 0;
            maxFrameNanos = 0L;
            windowStart = now;
        }
    }

    private static long handle() {
        return Minecraft.getInstance().getWindow().handle();
    }

    private static void move(int mode, double a, double b) {
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        double x;
        double y;
        double dx;
        double dy;
        if (mode == 0) {
            x = a * (double)window.getScreenWidth();
            y = b * (double)window.getScreenHeight();
            dx = x - cursorX;
            dy = y - cursorY;
        } else {
            dx = a;
            dy = b;
            x = cursorX + dx;
            y = cursorY + dy;
        }

        cursorX = x;
        cursorY = y;
        minecraft.mouseHandler.onMove(window.handle(), x, y, dx, dy);
    }
}
