//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTexture;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

final class FrameShare {
    private static final int HEADER = 64;
    private static final long STALE_NANOS = 10000000000L;
    private final Path path;
    private MappedByteBuffer map;
    private ByteBuffer frame;
    private int width;
    private int height;
    private long lastSequence = -1L;
    private long lastChange;
    private long lastOpenAttempt;
    private boolean uploadFailed;
    private TextureTarget scaleTarget;

    FrameShare(Path path) {
        this.path = path;
    }

    private boolean open() {
        if (this.map != null) {
            return true;
        } else {
            long now = System.nanoTime();
            if (now - this.lastOpenAttempt < 1000000000L) {
                return false;
            } else {
                this.lastOpenAttempt = now;

                try {
                    FileChannel channel = FileChannel.open(this.path, StandardOpenOption.READ);

                    boolean var4;
                    try {
                        this.map = channel.map(MapMode.READ_ONLY, 0L, channel.size());
                        this.map.order(ByteOrder.LITTLE_ENDIAN);
                        var4 = true;
                    } catch (Throwable var7) {
                        if (channel != null) {
                            try {
                                channel.close();
                            } catch (Throwable var6) {
                                var7.addSuppressed(var6);
                            }
                        }

                        throw var7;
                    }

                    if (channel != null) {
                        channel.close();
                    }

                    return var4;
                } catch (RuntimeException | IOException var8) {
                    this.map = null;
                    return false;
                }
            }
        }
    }

    private void poll() {
        if (this.open()) {
            if (this.map.getInt(0) == 1196246612) {
                long before = this.map.getLong(16);
                if ((before & 1L) == 0L) {
                    if (before != this.lastSequence) {
                        int w = this.map.getInt(8);
                        int h = this.map.getInt(12);
                        if (w > 0 && h > 0) {
                            if ((long)w * (long)h * 4L > (long)(this.map.capacity() - 64)) {
                                this.map = null;
                            } else {
                                int size = w * h * 4;
                                if (this.frame == null || this.frame.capacity() != size) {
                                    this.frame = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
                                }

                                this.width = w;
                                this.height = h;
                                this.frame.clear();
                                this.frame.put(0, this.map, 64, size);
                                if (this.map.getLong(16) == before) {
                                    this.lastSequence = before;
                                    this.lastChange = System.nanoTime();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    boolean isLive() {
        this.poll();
        return this.frame != null && System.nanoTime() - this.lastChange < 10000000000L;
    }

    boolean draw(RenderTarget target) {
        if (!this.isLive()) {
            return false;
        } else {
            GpuTexture texture = target.getColorTexture();
            if (texture != null && !texture.isClosed()) {
                if (this.frame.capacity() != this.width * this.height * 4) {
                    return false;
                } else {
                    try {
                        return this.upload(target, texture);
                    } catch (RuntimeException var4) {
                        RuntimeException e = var4;
                        if (!this.uploadFailed) {
                            this.uploadFailed = true;
                            TmcgiUeBridgeClient.LOGGER.warn("The renderer's frame ({} x {}) could not be drawn into the {} x {} target", new Object[]{this.width, this.height, target.width, target.height, e});
                        }

                        return false;
                    }
                }
            } else {
                return false;
            }
        }
    }

    private boolean upload(RenderTarget target, GpuTexture texture) {
        this.frame.clear();
        if (this.width == target.width && this.height == target.height) {
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, this.frame, 0, 0, 0, 0, this.width, this.height);
            return true;
        } else {
            if (this.scaleTarget == null || this.scaleTarget.width != this.width || this.scaleTarget.height != this.height) {
                if (this.scaleTarget != null) {
                    this.scaleTarget.destroyBuffers();
                }

                this.scaleTarget = new TextureTarget("TMCGI renderer frame", this.width, this.height, texture.getFormat(), target.getDepthTexture().getFormat());
            }

            RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.scaleTarget.getColorTexture(), this.frame, 0, 0, 0, 0, this.width, this.height);
            this.scaleTarget.blitAndBlendToTexture(target.getColorTextureView(), target.getDepthTextureView());
            return true;
        }
    }
}
