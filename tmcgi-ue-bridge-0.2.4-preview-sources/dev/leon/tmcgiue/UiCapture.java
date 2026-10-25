//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.GpuTexture;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

final class UiCapture {
    private static final int MAX_IN_FLIGHT = 2;
    private final BridgeConnection connection;
    private final AtomicInteger inFlight = new AtomicInteger();
    private byte[] previous;
    private int previousGeneration = -1;
    private byte[] raw = new byte[0];
    private byte[] current = new byte[0];
    private int captured;
    private int sent;
    private long windowStart = System.nanoTime();

    UiCapture(BridgeConnection connection) {
        this.connection = connection;
    }

    void capture(RenderTarget target) {
        if (this.inFlight.get() < 2) {
            GpuTexture texture = target.getColorTexture();
            if (texture != null && !texture.isClosed()) {
                int width = texture.getWidth(0);
                int height = texture.getHeight(0);
                int pixelSize = texture.getFormat().blockSize();
                if (pixelSize == 4 && width > 0 && height > 0) {
                    long size = (long)width * (long)height * (long)pixelSize;
                    GpuDevice device = RenderSystem.getDevice();
                    GpuBuffer buffer = device.createBuffer(() -> {
                        return "TMCGI interface readback";
                    }, 9, size);
                    this.inFlight.incrementAndGet();
                    device.createCommandEncoder().copyTextureToBuffer(texture, buffer, 0L, () -> {
                        this.finish(buffer, width, height, size);
                    }, 0);
                }
            }
        }
    }

    private void finish(GpuBuffer buffer, int width, int height, long size) {
        try {
            GpuBufferSlice.MappedView view = buffer.map(true, false);

            try {
                ByteBuffer data = view.data();
                if ((long)this.raw.length != size) {
                    this.raw = new byte[(int)size];
                    this.current = new byte[(int)size];
                    this.previous = null;
                }

                data.get(0, this.raw);
                int rowBytes = width * 4;

                int generation;
                for(generation = 0; generation < height; ++generation) {
                    System.arraycopy(this.raw, (height - 1 - generation) * rowBytes, this.current, generation * rowBytes, rowBytes);
                }

                ++this.captured;
                generation = this.connection.generation();
                if (generation != this.previousGeneration || this.previous == null || Arrays.mismatch(this.previous, this.current) >= 0) {
                    if (this.previous == null) {
                        this.previous = new byte[(int)size];
                    }

                    System.arraycopy(this.current, 0, this.previous, 0, (int)size);
                    this.previousGeneration = generation;
                    this.connection.sendUi(BridgeProtocol.ui(width, height, this.current));
                    ++this.sent;
                }

                long now = System.nanoTime();
                if (now - this.windowStart > 2000000000L) {
                    TmcgiUeBridgeClient.LOGGER.info("Interface: {} frames read back, {} sent in the last two seconds", this.captured, this.sent);
                    this.captured = 0;
                    this.sent = 0;
                    this.windowStart = now;
                }
            } catch (Throwable var18) {
                if (view != null) {
                    try {
                        view.close();
                    } catch (Throwable var17) {
                        var18.addSuppressed(var17);
                    }
                }

                throw var18;
            }

            if (view != null) {
                view.close();
            }
        } catch (RuntimeException var19) {
            RuntimeException e = var19;
            TmcgiUeBridgeClient.LOGGER.warn("Reading back the interface failed", e);
        } finally {
            buffer.close();
            this.inFlight.decrementAndGet();
        }

    }
}
