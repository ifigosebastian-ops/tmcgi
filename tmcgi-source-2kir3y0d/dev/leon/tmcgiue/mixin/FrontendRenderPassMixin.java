//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue.mixin;

import com.mojang.renderpearl.frontend.FrontendRenderPass;
import dev.leon.tmcgiue.TmcgiUeBridgeClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({FrontendRenderPass.class})
public abstract class FrontendRenderPassMixin {
    public FrontendRenderPassMixin() {
    }

    @Inject(
        method = {"draw", "drawIndexed", "multiDraw", "multiDrawIndexed", "drawIndirect", "drawIndexedIndirect", "drawMultipleIndexed"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void tmcgiue$dropDraw(CallbackInfo ci) {
        if (TmcgiUeBridgeClient.discardDraws) {
            ci.cancel();
        }

    }
}
