//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue.mixin;

import dev.leon.tmcgiue.TmcgiUeBridgeClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Minecraft.class})
public abstract class MinecraftMixin {
    public MinecraftMixin() {
    }

    @Inject(
        method = {"isWindowActive()Z"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void tmcgiue$windowActive(CallbackInfoReturnable<Boolean> cir) {
        if (TmcgiUeBridgeClient.overlayActive()) {
            cir.setReturnValue(true);
        }

    }
}
