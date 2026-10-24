//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.leon.tmcgiue.TmcgiUeBridgeClient;
import dev.leon.tmcgiue.UeInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({InputConstants.class})
public abstract class InputConstantsMixin {
    public InputConstantsMixin() {
    }

    @Inject(
        method = {"grabMouse(Lcom/mojang/blaze3d/platform/Window;DD)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void tmcgiue$grab(Window window, double x, double y, CallbackInfo ci) {
        if (TmcgiUeBridgeClient.overlayActive()) {
            ci.cancel();
        }

    }

    @Inject(
        method = {"releaseMouse(Lcom/mojang/blaze3d/platform/Window;DD)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void tmcgiue$release(Window window, double x, double y, CallbackInfo ci) {
        if (TmcgiUeBridgeClient.overlayActive()) {
            ci.cancel();
        }

    }

    @Inject(
        method = {"isKeyDown(I)Z"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void tmcgiue$keyDown(int key, CallbackInfoReturnable<Boolean> cir) {
        if (TmcgiUeBridgeClient.overlayActive()) {
            cir.setReturnValue(UeInput.isKeyDown(key));
        }

    }
}
