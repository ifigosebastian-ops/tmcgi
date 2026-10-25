//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTexture;
import dev.leon.tmcgiue.EntityCensus;
import dev.leon.tmcgiue.TmcgiUeBridgeClient;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public abstract class GameRendererMixin {
    public GameRendererMixin() {
    }

    @Inject(
        method = {"render()V"},
        at = {@At("HEAD")}
    )
    private void tmcgiue$beforeRender(CallbackInfo ci) {
        TmcgiUeBridgeClient.discardDraws = false;
        EntityCensus.head(((GameRenderer)this).gameRenderState().levelRenderState);
        if (TmcgiUeBridgeClient.overlayActive()) {
            ((GameRenderer)this).gameRenderState().guiRenderState.clearColorOverride = new Vector4f(0.0F, 0.0F, 0.0F, 0.0F);
        }

    }

    @Inject(
        method = {"renderLevel()V"},
        at = {@At("HEAD")}
    )
    private void tmcgiue$discardWorld(CallbackInfo ci) {
        TmcgiUeBridgeClient.discardDraws = true;
    }

    @ModifyArg(
        method = {"renderLevel()V"},
        at = {@At(
    value = "INVOKE",
    target = "Lorg/joml/Matrix4f;mul(Lorg/joml/Matrix4fc;)Lorg/joml/Matrix4f;",
    ordinal = 0
)},
        require = 0
    )
    private Matrix4fc tmcgiue$captureBobbing(Matrix4fc bobbing) {
        TmcgiUeBridgeClient.captureBobbing(bobbing);
        return bobbing;
    }

    @Inject(
        method = {"renderLevel()V"},
        at = {@At("TAIL")}
    )
    private void tmcgiue$replaceWorldPicture(CallbackInfo ci) {
        TmcgiUeBridgeClient.discardDraws = false;
        RenderTarget target = ((GameRenderer)this).mainRenderTarget();
        if (TmcgiUeBridgeClient.overlayActive()) {
            GpuTexture texture = target.getColorTexture();
            if (texture != null && !texture.isClosed()) {
                RenderSystem.getDevice().createCommandEncoder().clearColorTexture(texture, new Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
            }
        } else {
            TmcgiUeBridgeClient.drawSharedFrame(target);
        }

    }

    @Inject(
        method = {"render()V"},
        at = {@At("TAIL")}
    )
    private void tmcgiue$afterRender(CallbackInfo ci) {
        TmcgiUeBridgeClient.drainInput();
        TmcgiUeBridgeClient.onFrameRendered((GameRenderer)this);
    }
}
