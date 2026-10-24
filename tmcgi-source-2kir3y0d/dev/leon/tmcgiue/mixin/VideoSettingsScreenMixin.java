//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue.mixin;

import dev.leon.tmcgiue.TmcgiSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({VideoSettingsScreen.class})
public abstract class VideoSettingsScreenMixin {
    public VideoSettingsScreenMixin() {
    }

    @Inject(
        method = {"addOptions()V"},
        at = {@At("TAIL")}
    )
    private void tmcgiue$addPage(CallbackInfo ci) {
        Screen self = (Screen)this;
        OptionsList list = ((OptionsSubScreenAccessor)self).tmcgiue$getList();
        list.addHeader(Component.literal("TMCGI"));
        list.addBig(Button.builder(Component.literal("TMCGI Renderer Settings..."), (b) -> {
            Minecraft.getInstance().gui.setScreen(new TmcgiSettingsScreen(self));
        }).build());
    }
}
