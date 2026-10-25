//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.util.Iterator;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

final class DynamicLights {
    static final int MAX = 16;
    private static final double RANGE = 64.0;
    private static final int[] IDS = new int[16];
    private static final double[] POSITIONS = new double[48];
    private static final int[] LEVELS = new int[16];
    private static final int[] COLORS = new int[16];
    private static final double[] DISTANCES = new double[16];
    private static int sentCount;
    private static int sentEpoch = -1;

    private DynamicLights() {
    }

    static void update(Minecraft minecraft, Camera camera, BridgeConnection connection) {
        ClientLevel level = minecraft.level;
        int count = 0;
        if (TmcgiSettings.dynamicLights && level != null && camera != null && camera.isInitialized()) {
            float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            Vec3 eye = camera.position();
            Iterator var7 = level.entitiesForRendering().iterator();

            label107:
            while(true) {
                Entity entity;
                int light;
                BlockState state;
                int slot;
                Vec3 at;
                double distance;
                do {
                    label86:
                    do {
                        do {
                            do {
                                if (!var7.hasNext()) {
                                    break label107;
                                }

                                entity = (Entity)var7.next();
                            } while(entity.distanceToSqr(eye) > 4096.0);

                            if (entity instanceof ItemEntity) {
                                ItemEntity item = (ItemEntity)entity;
                                state = blockOf(item.getItem());
                                light = state != null ? state.getLightEmission() : 0;
                                continue label86;
                            }
                        } while(!(entity instanceof LivingEntity));

                        LivingEntity living = (LivingEntity)entity;
                        BlockState main = blockOf(living.getMainHandItem());
                        BlockState off = blockOf(living.getOffhandItem());
                        slot = main != null ? main.getLightEmission() : 0;
                        int offLight = off != null ? off.getLightEmission() : 0;
                        state = slot >= offLight ? main : off;
                        light = Math.max(slot, offLight);
                    } while(light <= 0);

                    if (entity == camera.entity() && !camera.isDetached()) {
                        Vector3fc forward = camera.forwardVector();
                        at = eye.add((double)forward.x() * 0.5, (double)forward.y() * 0.5 - 0.35, (double)forward.z() * 0.5);
                    } else if (entity instanceof ItemEntity) {
                        at = entity.getPosition(partialTick).add(0.0, 0.3, 0.0);
                    } else {
                        at = entity.getPosition(partialTick).add(0.0, (double)entity.getBbHeight() * 0.6, 0.0);
                    }

                    distance = at.distanceToSqr(eye);
                    slot = count < 16 ? count : 15;
                } while(count >= 16 && distance >= DISTANCES[15]);

                while(slot > 0 && DISTANCES[slot - 1] > distance) {
                    copy(slot - 1, slot);
                    --slot;
                }

                IDS[slot] = entity.getId();
                POSITIONS[slot * 3] = at.x;
                POSITIONS[slot * 3 + 1] = at.y;
                POSITIONS[slot * 3 + 2] = at.z;
                LEVELS[slot] = Math.min(light, 15);
                COLORS[slot] = EmitterColor.of(state);
                DISTANCES[slot] = distance;
                if (count < 16) {
                    ++count;
                }
            }
        }

        if (count > 0 || sentCount > 0 || connection.epoch() != sentEpoch) {
            connection.send(BridgeProtocol.dynamicLights(IDS, POSITIONS, LEVELS, COLORS, count));
            sentCount = count;
            sentEpoch = connection.epoch();
        }

    }

    private static void copy(int from, int to) {
        IDS[to] = IDS[from];
        POSITIONS[to * 3] = POSITIONS[from * 3];
        POSITIONS[to * 3 + 1] = POSITIONS[from * 3 + 1];
        POSITIONS[to * 3 + 2] = POSITIONS[from * 3 + 2];
        LEVELS[to] = LEVELS[from];
        COLORS[to] = COLORS[from];
        DISTANCES[to] = DISTANCES[from];
    }

    private static BlockState blockOf(ItemStack stack) {
        BlockState var10000;
        if (!stack.isEmpty()) {
            Item var2 = stack.getItem();
            if (var2 instanceof BlockItem) {
                BlockItem block = (BlockItem)var2;
                var10000 = block.getBlock().defaultBlockState();
                return var10000;
            }
        }

        var10000 = null;
        return var10000;
    }
}
