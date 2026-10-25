//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

final class BlockStateNames {
    private static final IdentityHashMap<BlockState, String> CACHE = new IdentityHashMap();

    private BlockStateNames() {
    }

    static String of(BlockState state) {
        String cached = (String)CACHE.get(state);
        if (cached != null) {
            return cached;
        } else {
            StringBuilder text = new StringBuilder(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            List<Property<?>> properties = new ArrayList(state.getProperties());
            if (!properties.isEmpty()) {
                properties.sort(Comparator.comparing(Property::getName));
                text.append('[');

                for(int i = 0; i < properties.size(); ++i) {
                    if (i > 0) {
                        text.append(',');
                    }

                    Property<?> property = (Property)properties.get(i);
                    text.append(property.getName()).append('=').append(valueName(state, property));
                }

                text.append(']');
            }

            String result = text.toString();
            CACHE.put(state, result);
            return result;
        }
    }

    private static String valueName(BlockState state, Property<?> property) {
        return property.getName(state.getValue(property));
    }
}
