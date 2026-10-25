//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

final class LodPalette {
    static final int FLAG_WATER = 1;
    private final Map<String, Integer> ids = new HashMap();
    private final List<Entry> entries = new ArrayList();
    private int sent;

    LodPalette() {
    }

    synchronized int idOf(String serial) {
        Integer id = (Integer)this.ids.get(serial);
        return id == null ? -1 : id;
    }

    synchronized Entry add(String serial, LodColors.Look look) {
        Integer existing = (Integer)this.ids.get(serial);
        if (existing != null) {
            return (Entry)this.entries.get(existing);
        } else {
            Entry entry = new Entry(this.entries.size(), serial, look.rgb(), look.flags(), look.top(), look.topTint(), look.side(), look.sideTint());
            if (this.entries.size() < 120) {
                TmcgiUeBridgeClient.LOGGER.info("Far terrain colour of {}: #{}{}", new Object[]{serial, String.format("%06X", look.rgb()), (look.flags() & 1) != 0 ? " (water)" : ""});
            }

            this.ids.put(serial, entry.id());
            this.entries.add(entry);
            return entry;
        }
    }

    synchronized List<Entry> takeUnsent() {
        List<Entry> fresh = new ArrayList(this.entries.subList(this.sent, this.entries.size()));
        this.sent = this.entries.size();
        return fresh;
    }

    synchronized void resend() {
        this.sent = 0;
    }

    synchronized int size() {
        return this.entries.size();
    }

    static record Entry(int id, String name, int rgb, int flags, TextureAtlasSprite top, int topTint, TextureAtlasSprite side, int sideTint) {
        Entry(int id, String name, int rgb, int flags, TextureAtlasSprite top, int topTint, TextureAtlasSprite side, int sideTint) {
            this.id = id;
            this.name = name;
            this.rgb = rgb;
            this.flags = flags;
            this.top = top;
            this.topTint = topTint;
            this.side = side;
            this.sideTint = sideTint;
        }

        public int id() {
            return this.id;
        }

        public String name() {
            return this.name;
        }

        public int rgb() {
            return this.rgb;
        }

        public int flags() {
            return this.flags;
        }

        public TextureAtlasSprite top() {
            return this.top;
        }

        public int topTint() {
            return this.topTint;
        }

        public TextureAtlasSprite side() {
            return this.side;
        }

        public int sideTint() {
            return this.sideTint;
        }
    }
}
