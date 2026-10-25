//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import com.mojang.blaze3d.platform.NativeImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;

final class LodColors {
    private static final Field ORIGINAL_IMAGE = originalImageField();
    private static final int WATER = 4159204;
    private static final int FOLIAGE = 7842607;
    private static final int GRASS = 9551193;

    private LodColors() {
    }

    private static Field originalImageField() {
        try {
            Field field = SpriteContents.class.getDeclaredField("originalImage");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException var1) {
            return null;
        }
    }

    static int[] of(BlockState state) {
        Look look = look(state);
        return new int[]{look.rgb(), look.flags()};
    }

    static Look look(BlockState state) {
        int[] flat = flatOf(state);

        try {
            String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
            if (flat[1] != 0) {
                return new Look(flat[0], flat[1], (TextureAtlasSprite)null, 16777215, (TextureAtlasSprite)null, 16777215);
            } else {
                List<BlockStateModelPart> parts = new ArrayList();
                Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state).collectParts(RandomSource.create(42L), parts);
                BakedQuad up = null;
                BakedQuad side = null;
                Iterator var6 = parts.iterator();

                while(true) {
                    while(true) {
                        BlockStateModelPart part;
                        int sideTint;
                        do {
                            if (!var6.hasNext()) {
                                if (up == null && side == null) {
                                    return new Look(flat[0], flat[1], (TextureAtlasSprite)null, 16777215, (TextureAtlasSprite)null, 16777215);
                                }

                                BakedQuad topQuad = up != null ? up : side;
                                BakedQuad sideQuad = side != null ? side : up;
                                int topTint = topQuad.materialInfo().tintIndex() >= 0 ? tint(path) : 16777215;
                                sideTint = sideQuad.materialInfo().tintIndex() >= 0 ? tint(path) : 16777215;
                                return new Look(flat[0], flat[1], topQuad.materialInfo().sprite(), topTint, sideQuad.materialInfo().sprite(), sideTint);
                            }

                            part = (BlockStateModelPart)var6.next();
                            if (up == null) {
                                List<BakedQuad> quads = part.getQuads(Direction.UP);
                                if (!quads.isEmpty()) {
                                    up = (BakedQuad)quads.get(0);
                                }
                            }
                        } while(side != null);

                        Direction[] var16 = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
                        sideTint = var16.length;

                        for(int var10 = 0; var10 < sideTint; ++var10) {
                            Direction direction = var16[var10];
                            List<BakedQuad> quads = part.getQuads(direction);
                            if (!quads.isEmpty()) {
                                side = (BakedQuad)quads.get(0);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException var13) {
            return new Look(flat[0], flat[1], (TextureAtlasSprite)null, 16777215, (TextureAtlasSprite)null, 16777215);
        }
    }

    private static int[] flatOf(BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (!path.equals("water") && !path.equals("bubble_column")) {
            int fallback = state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col;

            try {
                List<BlockStateModelPart> parts = new ArrayList();
                Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state).collectParts(RandomSource.create(42L), parts);
                BakedQuad best = null;
                Iterator var5 = parts.iterator();

                while(var5.hasNext()) {
                    BlockStateModelPart part = (BlockStateModelPart)var5.next();
                    List<BakedQuad> up = part.getQuads(Direction.UP);
                    if (!up.isEmpty()) {
                        best = (BakedQuad)up.get(0);
                        break;
                    }

                    if (best == null) {
                        Direction[] var8 = Direction.values();
                        int var9 = var8.length;

                        for(int var10 = 0; var10 < var9; ++var10) {
                            Direction side = var8[var10];
                            List<BakedQuad> quads = part.getQuads(side);
                            if (!quads.isEmpty()) {
                                best = (BakedQuad)quads.get(0);
                                break;
                            }
                        }
                    }

                    if (best == null) {
                        List<BakedQuad> loose = part.getQuads((Direction)null);
                        if (!loose.isEmpty()) {
                            best = (BakedQuad)loose.get(0);
                        }
                    }
                }

                if (best == null) {
                    return new int[]{fallback, 0};
                } else {
                    BakedQuad.MaterialInfo info = best.materialInfo();
                    int average = average(info.sprite());
                    if (average < 0) {
                        return new int[]{fallback, 0};
                    } else {
                        if (info.tintIndex() >= 0) {
                            average = multiply(average, tint(path));
                        }

                        return new int[]{average, 0};
                    }
                }
            } catch (RuntimeException var13) {
                return new int[]{fallback, 0};
            }
        } else {
            return new int[]{4159204, 1};
        }
    }

    private static int tint(String path) {
        if (path.contains("spruce_leaves")) {
            return 6396257;
        } else if (path.contains("birch_leaves")) {
            return 8431445;
        } else {
            return !path.contains("leaves") && !path.contains("vine") ? 9551193 : 7842607;
        }
    }

    private static int multiply(int color, int tint) {
        int r = (color >> 16 & 255) * (tint >> 16 & 255) / 255;
        int g = (color >> 8 & 255) * (tint >> 8 & 255) / 255;
        int b = (color & 255) * (tint & 255) / 255;
        return r << 16 | g << 8 | b;
    }

    private static int average(TextureAtlasSprite sprite) {
        if (ORIGINAL_IMAGE == null) {
            return -1;
        } else {
            try {
                NativeImage image = (NativeImage)ORIGINAL_IMAGE.get(sprite.contents());
                if (image == null) {
                    return -1;
                } else {
                    int width = image.getWidth();
                    int height = Math.min(image.getHeight(), width);
                    long red = 0L;
                    long green = 0L;
                    long blue = 0L;
                    long count = 0L;

                    for(int y = 0; y < height; ++y) {
                        for(int x = 0; x < width; ++x) {
                            int argb = image.getPixel(x, y);
                            if (argb >>> 24 > 16) {
                                red += (long)(argb >> 16 & 255);
                                green += (long)(argb >> 8 & 255);
                                blue += (long)(argb & 255);
                                ++count;
                            }
                        }
                    }

                    if (count == 0L) {
                        return -1;
                    } else {
                        return (int)(red / count) << 16 | (int)(green / count) << 8 | (int)(blue / count);
                    }
                }
            } catch (RuntimeException | ReflectiveOperationException var15) {
                return -1;
            }
        }
    }

    static record Look(int rgb, int flags, TextureAtlasSprite top, int topTint, TextureAtlasSprite side, int sideTint) {
        Look(int rgb, int flags, TextureAtlasSprite top, int topTint, TextureAtlasSprite side, int sideTint) {
            this.rgb = rgb;
            this.flags = flags;
            this.top = top;
            this.topTint = topTint;
            this.side = side;
            this.sideTint = sideTint;
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
