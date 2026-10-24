//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTexture;
import dev.leon.tmcgiue.DhSupport;
import dev.leon.tmcgiue.EntityCensus;
import dev.leon.tmcgiue.TmcgiUeBridgeClient;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
        TmcgiUeBridgeClient.drainInput();
        EntityCensus.head(((GameRenderer)this).gameRenderState().levelRenderState);
        if (TmcgiUeBridgeClient.overlayActive()) {
            ((GameRenderer)this).gameRenderState().guiRenderState.clearColorOverride = new Vector4f(0.0F, 0.0F, 0.0F, 0.0F);
        }

    }

    @Inject(
        method = {"renderLevel()V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void tmcgiue$skipWorld(CallbackInfo ci) {
        if (TmcgiUeBridgeClient.compatWorldRender()) {
            TmcgiUeBridgeClient.discardDraws = true;
        } else {
            if (TmcgiUeBridgeClient.overlayActive() || TmcgiUeBridgeClient.drawSharedFrame(((GameRenderer)this).mainRenderTarget())) {
                DhSupport.runRenderThreadTasks();
                LevelRenderState levelRenderState = ((GameRenderer)this).gameRenderState().levelRenderState;
                Runnable playerSectionReady = levelRenderState.playerCompiledSectionCallback;
                if (playerSectionReady != null) {
                    levelRenderState.playerCompiledSectionCallback = null;
                    playerSectionReady.run();
                }

                ci.cancel();
            }

        }
    }

    @Inject(
        method = {"renderLevel()V"},
        at = {@At("TAIL")}
    )
    private void tmcgiue$replaceWorldPicture(CallbackInfo ci) {
        TmcgiUeBridgeClient.discardDraws = false;
        if (TmcgiUeBridgeClient.compatWorldRender()) {
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
    }

    @Inject(
        method = {"render()V"},
        at = {@At("TAIL")}
    )
    private void tmcgiue$afterRender(CallbackInfo ci) {
        TmcgiUeBridgeClient.onFrameRendered((GameRenderer)this);
    }
}
