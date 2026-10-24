//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class BridgeConnection {
    private final String host;
    private final int port;
    private final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue(50000);
    private final AtomicReference<byte[]> latestPose = new AtomicReference();
    private final AtomicBoolean resync = new AtomicBoolean();
    private final AtomicReference<byte[]> latestUi = new AtomicReference();
    private final AtomicReference<byte[]> latestEntities = new AtomicReference();
    private final AtomicReference<byte[]> latestParticles = new AtomicReference();
    private final ConcurrentLinkedQueue<byte[]> urgent = new ConcurrentLinkedQueue();
    private volatile int rendererWidth;
    private volatile int rendererHeight;
    private volatile int generation;
    private volatile int overflows;
    private volatile boolean connected;
    private volatile boolean stopping;
    private Thread thread;

    BridgeConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    void start() {
        this.thread = new Thread(this::run, "TMCGI-UE-Bridge");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    void stop() {
        this.stopping = true;
        if (this.thread != null) {
            this.thread.interrupt();
        }

    }

    boolean isConnected() {
        return this.connected;
    }

    boolean consumeResync() {
        return this.resync.getAndSet(false);
    }

    void send(byte[] frame) {
        if (this.connected) {
            if (!this.queue.offer(frame)) {
                this.queue.clear();
                ++this.overflows;
                this.resync.set(true);
            }

        }
    }

    void sendUi(byte[] frame) {
        if (this.connected) {
            this.latestUi.set(frame);
        }

    }

    int rendererWidth() {
        return this.connected ? this.rendererWidth : 0;
    }

    int rendererHeight() {
        return this.connected ? this.rendererHeight : 0;
    }

    int generation() {
        return this.generation;
    }

    int overflows() {
        return this.overflows;
    }

    private void readLoop(Socket socket) {
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream(), 16384));
            byte[] head = new byte[4];

            while(!this.stopping && !socket.isClosed()) {
                in.readFully(head);
                int length = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN).getInt();
                if (length < 1 || length > 1048576) {
                    return;
                }

                byte[] payload = new byte[length];
                in.readFully(payload);
                ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
                int type = buffer.get() & 255;
                if (type == 128 && buffer.remaining() >= 8) {
                    this.rendererHeight = 0;
                    this.rendererWidth = buffer.getInt();
                    this.rendererHeight = buffer.getInt();
                    TmcgiUeBridgeClient.LOGGER.info("The renderer window is {} x {}", this.rendererWidth, this.rendererHeight);
                } else if (type == 129 && buffer.remaining() >= 4) {
                    TmcgiSettings.onStatus(buffer.getInt());
                } else if (type >= 129 && type <= 133) {
                    try {
                        UeInput.queue(type, buffer);
                    } catch (BufferUnderflowException var9) {
                    }
                }
            }
        } catch (IOException var10) {
        }

    }

    void sendUrgent(byte[] frame) {
        if (this.connected) {
            this.urgent.add(frame);
        }

    }

    void sendEntities(byte[] frame) {
        if (this.connected) {
            this.latestEntities.set(frame);
        }

    }

    void sendParticles(byte[] frame) {
        if (this.connected) {
            this.latestParticles.set(frame);
        }

    }

    void sendPose(byte[] frame) {
        if (this.connected) {
            this.latestPose.set(frame);
        }

    }

    private void writeLatest(OutputStream out) throws IOException {
        byte[] pose = (byte[])this.latestPose.getAndSet((Object)null);
        byte[] ui = (byte[])this.latestUi.getAndSet((Object)null);
        byte[] entities = (byte[])this.latestEntities.getAndSet((Object)null);
        byte[] particles = (byte[])this.latestParticles.getAndSet((Object)null);
        byte[] small = (byte[])this.urgent.peek();
        if (pose != null || ui != null || entities != null || particles != null || small != null) {
            while(true) {
                if ((small = (byte[])this.urgent.poll()) == null) {
                    if (entities != null) {
                        out.write(entities);
                    }

                    if (particles != null) {
                        out.write(particles);
                    }

                    if (pose != null) {
                        out.write(pose);
                    }

                    if (ui != null) {
                        out.write(ui);
                    }

                    out.flush();
                    break;
                }

                out.write(small);
            }
        }

    }

    private void run() {
        while(!this.stopping) {
            label137: {
                try {
                    Socket socket = new Socket();

                    try {
                        socket.connect(new InetSocketAddress(this.host, this.port), 1000);
                        socket.setTcpNoDelay(true);
                        OutputStream out = new BufferedOutputStream(socket.getOutputStream(), 65536);
                        out.write(BridgeProtocol.hello("minecraft 26.3"));
                        out.flush();
                        this.queue.clear();
                        this.latestPose.set((Object)null);
                        this.latestUi.set((Object)null);
                        this.latestEntities.set((Object)null);
                        this.latestParticles.set((Object)null);
                        this.urgent.clear();
                        this.rendererWidth = 0;
                        this.rendererHeight = 0;
                        ++this.generation;
                        Thread reader = new Thread(() -> {
                            this.readLoop(socket);
                        }, "TMCGI-UE-Bridge-Read");
                        reader.setDaemon(true);
                        reader.start();
                        this.connected = true;
                        TmcgiSettings.onConnected();
                        this.urgent.add(TmcgiSettings.frame());
                        this.resync.set(true);
                        TmcgiUeBridgeClient.LOGGER.info("Connected to the TMCGI renderer at {}:{}", this.host, this.port);

                        while(!this.stopping) {
                            for(byte[] frame = (byte[])this.queue.poll(2L, TimeUnit.MILLISECONDS); frame != null; frame = (byte[])this.queue.poll()) {
                                byte[] ahead;
                                while((ahead = (byte[])this.urgent.poll()) != null) {
                                    out.write(ahead);
                                }

                                out.write(frame);
                                this.writeLatest(out);
                            }

                            this.writeLatest(out);
                            out.flush();
                        }
                    } catch (Throwable var15) {
                        try {
                            socket.close();
                        } catch (Throwable var14) {
                            var15.addSuppressed(var14);
                        }

                        throw var15;
                    }

                    socket.close();
                    break label137;
                } catch (IOException var16) {
                    IOException e = var16;
                    if (this.connected) {
                        TmcgiUeBridgeClient.LOGGER.info("The TMCGI renderer connection ended: {}", e.toString());
                    }
                    break label137;
                } catch (InterruptedException var17) {
                } finally {
                    this.connected = false;
                }

                return;
            }

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException var13) {
                return;
            }
        }

    }
}
