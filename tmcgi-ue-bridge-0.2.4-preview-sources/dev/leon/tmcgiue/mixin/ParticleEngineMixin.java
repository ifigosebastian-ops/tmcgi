//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue.mixin;

import dev.leon.tmcgiue.HiddenParticles;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ParticleEngine.class})
public abstract class ParticleEngineMixin {
    public ParticleEngineMixin() {
    }

    @Inject(
        method = {"createParticle"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void tmcgiue$hide(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        if (HiddenParticles.isHidden(options)) {
            cir.setReturnValue((Object)null);
        }

    }
}
