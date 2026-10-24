//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue.mixin;

import dev.leon.tmcgiue.TmcgiUeBridgeClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LevelChunk.class})
public abstract class LevelChunkMixin {
    public LevelChunkMixin() {
    }

    @Inject(
        method = {"setBlockState"},
        at = {@At("RETURN")}
    )
    private void tmcgiue$onSetBlockState(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        if (cir.getReturnValue() != null) {
            if (((LevelChunk)this).getLevel().isClientSide()) {
                TmcgiUeBridgeClient.onBlockChanged(pos);
            }

        }
    }
}
