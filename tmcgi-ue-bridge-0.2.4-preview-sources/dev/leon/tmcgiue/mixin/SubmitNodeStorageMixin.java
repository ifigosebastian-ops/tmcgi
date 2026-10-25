//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.leon.tmcgiue.EntityCensus;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SubmitNodeStorage.class})
public abstract class SubmitNodeStorageMixin {
    public SubmitNodeStorageMixin() {
    }

    @Inject(
        method = {"submitModel"},
        at = {@At("HEAD")}
    )
    private void tmcgiue$model(Model<?> model, Object state, PoseStack stack, RenderType type, int light, int overlay, int color, UvMapping uv, int outline, CallbackInfo ci) {
        EntityCensus.model(model, type);
    }

    @Inject(
        method = {"submitItem"},
        at = {@At("HEAD")}
    )
    private void tmcgiue$item(CallbackInfo ci) {
        EntityCensus.call("submitItem");
    }

    @Inject(
        method = {"submitBlockModel"},
        at = {@At("HEAD")}
    )
    private void tmcgiue$blockModel(CallbackInfo ci) {
        EntityCensus.call("submitBlockModel");
    }

    @Inject(
        method = {"submitCustomGeometry"},
        at = {@At("HEAD")}
    )
    private void tmcgiue$custom(CallbackInfo ci) {
        EntityCensus.call("submitCustomGeometry");
    }

    @Inject(
        method = {"submitShadow"},
        at = {@At("HEAD")}
    )
    private void tmcgiue$shadow(CallbackInfo ci) {
        EntityCensus.call("submitShadow");
    }

    @Inject(
        method = {"submitNameTag"},
        at = {@At("HEAD")}
    )
    private void tmcgiue$nameTag(CallbackInfo ci) {
        EntityCensus.call("submitNameTag");
    }

    @Inject(
        method = {"submitFlame"},
        at = {@At("HEAD")}
    )
    private void tmcgiue$flame(CallbackInfo ci) {
        EntityCensus.call("submitFlame");
    }

    @Inject(
        method = {"submitLeash"},
        at = {@At("HEAD")}
    )
    private void tmcgiue$leash(CallbackInfo ci) {
        EntityCensus.call("submitLeash");
    }

    @Inject(
        method = {"submitMovingBlock"},
        at = {@At("HEAD")}
    )
    private void tmcgiue$moving(CallbackInfo ci) {
        EntityCensus.call("submitMovingBlock");
    }

    @Inject(
        method = {"submitQuadParticleGroup"},
        at = {@At("HEAD")}
    )
    private void tmcgiue$particles(CallbackInfo ci) {
        EntityCensus.call("submitQuadParticleGroup");
    }

    @Inject(
        method = {"submitText"},
        at = {@At("HEAD")}
    )
    private void tmcgiue$text(CallbackInfo ci) {
        EntityCensus.call("submitText");
    }
}
