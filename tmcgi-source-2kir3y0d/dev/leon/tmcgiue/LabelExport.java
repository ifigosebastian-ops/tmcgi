//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

final class LabelExport {
    static final int SCALE = 4;
    private static final int MARGIN = 6;
    private static final int MAX_LABELS = 1500;
    private final ExecutorService worker = Executors.newSingleThreadExecutor((runnable) -> {
        Thread thread = new Thread(runnable, "TMCGI-Label");
        thread.setDaemon(true);
        return thread;
    });
    private final Set<String> started = ConcurrentHashMap.newKeySet();
    private final Queue<Label> finished = new ConcurrentLinkedQueue();
    private final Map<String, Label> ready = new ConcurrentHashMap();

    LabelExport() {
    }

    static List<Segment> segments(Component text) {
        List<Segment> out = new ArrayList();
        text.visit((style, string) -> {
            if (!string.isEmpty()) {
                TextColor color = style.getColor();
                out.add(new Segment(string, color != null ? color.getValue() : 16777215, style.isBold(), style.isItalic(), style.isUnderlined(), style.isStrikethrough()));
            }

            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    static String keyOf(List<Segment> segments) {
        StringBuilder key = new StringBuilder();
        Iterator var2 = segments.iterator();

        while(var2.hasNext()) {
            Segment segment = (Segment)var2.next();
            key.append(segment.color).append((char)(segment.bold ? 'b' : '-')).append((char)(segment.italic ? 'i' : '-')).append((char)(segment.underline ? 'u' : '-')).append((char)(segment.strike ? 's' : '-')).append(segment.text).append('\u0001');
        }

        return key.toString();
    }

    Label get(String key, List<Segment> segments) {
        Label label = (Label)this.ready.get(key);
        if (label != null) {
            return label;
        } else {
            if (this.ready.size() + this.started.size() < 1500 && this.started.add(key)) {
                this.worker.execute(() -> {
                    try {
                        Label drawn = draw(key, segments);
                        if (drawn != null) {
                            this.finished.add(drawn);
                        }
                    } catch (LinkageError | Exception var4) {
                        Throwable e = var4;
                        TmcgiUeBridgeClient.LOGGER.warn("A name tag could not be drawn", e);
                    }

                });
            }

            return null;
        }
    }

    List<Label> drain() {
        List<Label> out = new ArrayList();

        Label label;
        while((label = (Label)this.finished.poll()) != null) {
            this.ready.put(label.key, label);
            out.add(label);
        }

        return out;
    }

    Label ready(String key) {
        return (Label)this.ready.get(key);
    }

    private static Label draw(String key, List<Segment> segments) throws IOException {
        Font base = new Font("SansSerif", 0, 32);
        BufferedImage probe = new BufferedImage(1, 1, 2);
        Graphics2D measure = probe.createGraphics();
        applyHints(measure);
        int width = 0;
        int ascent = 0;
        int descent = 0;
        List<Font> fonts = new ArrayList();
        List<Integer> widths = new ArrayList();

        FontMetrics metrics;
        int x;
        for(Iterator var10 = segments.iterator(); var10.hasNext(); descent = Math.max(descent, metrics.getDescent())) {
            Segment segment = (Segment)var10.next();
            Font font = base.deriveFont((segment.bold ? 1 : 0) | (segment.italic ? 2 : 0));
            metrics = measure.getFontMetrics(font);
            fonts.add(font);
            x = metrics.stringWidth(segment.text);
            widths.add(x);
            width += x;
            ascent = Math.max(ascent, metrics.getAscent());
        }

        measure.dispose();
        if (width <= 0) {
            return null;
        } else {
            int imageWidth = width + 12;
            int imageHeight = ascent + descent + 12;
            BufferedImage image = new BufferedImage(imageWidth, imageHeight, 2);
            Graphics2D g = image.createGraphics();
            applyHints(g);
            x = 6;
            int baseline = 6 + ascent;

            for(int i = 0; i < segments.size(); ++i) {
                Segment segment = (Segment)segments.get(i);
                g.setFont((Font)fonts.get(i));
                g.setColor(new Color(0, 0, 0, 200));
                int step = Math.max(1, 2);

                int w;
                for(w = -step; w <= step; w += step) {
                    for(int dy = -step; dy <= step; dy += step) {
                        if (w != 0 || dy != 0) {
                            g.drawString(segment.text, x + w, baseline + dy);
                        }
                    }
                }

                g.setColor(new Color(segment.color | -16777216, true));
                g.drawString(segment.text, x, baseline);
                w = (Integer)widths.get(i);
                if (segment.underline) {
                    g.fillRect(x, baseline + 2, w, Math.max(1, 2));
                }

                if (segment.strike) {
                    g.fillRect(x, baseline - ascent / 3, w, Math.max(1, 2));
                }

                x += w;
            }

            g.dispose();
            ByteArrayOutputStream png = new ByteArrayOutputStream(8192);
            if (!ImageIO.write(image, "png", png)) {
                return null;
            } else {
                return new Label(key, png.toByteArray(), imageWidth, imageHeight);
            }
        }
    }

    private static void applyHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    static {
        System.setProperty("java.awt.headless", "true");
    }

    static record Segment(String text, int color, boolean bold, boolean italic, boolean underline, boolean strike) {
        Segment(String text, int color, boolean bold, boolean italic, boolean underline, boolean strike) {
            this.text = text;
            this.color = color;
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
            this.strike = strike;
        }

        public String text() {
            return this.text;
        }

        public int color() {
            return this.color;
        }

        public boolean bold() {
            return this.bold;
        }

        public boolean italic() {
            return this.italic;
        }

        public boolean underline() {
            return this.underline;
        }

        public boolean strike() {
            return this.strike;
        }
    }

    static record Label(String key, byte[] png, int width, int height) {
        Label(String key, byte[] png, int width, int height) {
            this.key = key;
            this.png = png;
            this.width = width;
            this.height = height;
        }

        public String key() {
            return this.key;
        }

        public byte[] png() {
            return this.png;
        }

        public int width() {
            return this.width;
        }

        public int height() {
            return this.height;
        }
    }
}
