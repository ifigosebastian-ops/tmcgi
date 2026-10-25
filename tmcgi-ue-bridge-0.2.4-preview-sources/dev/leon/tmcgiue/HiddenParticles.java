//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class HiddenParticles {
    private static final Set<String> HIDDEN = ConcurrentHashMap.newKeySet();

    private HiddenParticles() {
    }

    static void set(String commaSeparated) {
        HIDDEN.clear();
        String[] var1 = commaSeparated.split(",");
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            String id = var1[var3];
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) {
                HIDDEN.add(trimmed.contains(":") ? trimmed : "minecraft:" + trimmed);
            }
        }

    }

    public static boolean isHidden(ParticleOptions options) {
        if (!HIDDEN.isEmpty() && options != null) {
            Identifier key = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
            return key != null && HIDDEN.contains(key.toString());
        } else {
            return false;
        }
    }
}
