//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue.mixin;

import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.frontend.FrontendCommandEncoder;
import dev.leon.tmcgiue.TmcgiUeBridgeClient;
import java.lang.reflect.Proxy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FrontendCommandEncoder.class})
public abstract class FrontendCommandEncoderMixin {
    private static final RenderPass NOTHING = (RenderPass)Proxy.newProxyInstance(FrontendCommandEncoderMixin.class.getClassLoader(), new Class[]{RenderPass.class}, (proxy, method, args) -> {
        Object var10000;
        switch (method.getName()) {
            case "hashCode" -> var10000 = System.identityHashCode(proxy);
            case "equals" -> var10000 = proxy == args[0];
            case "toString" -> var10000 = "TMCGI no-op render pass";
            default -> var10000 = null;
        }

        return var10000;
    });

    public FrontendCommandEncoderMixin() {
    }

    @Inject(
        method = {"createRenderPass(Lcom/mojang/renderpearl/api/commands/RenderPassDescriptor;)Lcom/mojang/renderpearl/api/commands/RenderPass;"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void tmcgiue$noPass(RenderPassDescriptor descriptor, CallbackInfoReturnable<RenderPass> cir) {
        if (TmcgiUeBridgeClient.discardDraws) {
            cir.setReturnValue(NOTHING);
        }

    }
}
