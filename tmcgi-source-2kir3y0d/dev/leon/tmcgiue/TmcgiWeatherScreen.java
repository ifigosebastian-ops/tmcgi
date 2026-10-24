//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

final class TmcgiWeatherScreen extends OptionsSubScreen {
    TmcgiWeatherScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, Component.literal("Weather Settings"));
    }

    public void onClose() {
        Screen var2 = this.lastScreen;
        if (var2 instanceof TmcgiSettingsScreen main) {
            this.minecraft.gui.setScreen(main.fresh());
        } else {
            super.onClose();
        }

    }

    protected void addOptions() {
        this.list.addBig(TmcgiSettingsScreen.levels("Volumetric Cloud Quality", TmcgiSettings.CLOUD_NAMES, TmcgiSettings.CLOUD_MODES, TmcgiSettings.cloudMode, (v) -> {
            TmcgiSettings.cloudMode = v;
        }));
        this.list.addBig(TmcgiSettingsScreen.toggle("Volumetric Fog", TmcgiSettings.volumetricFog, (v) -> {
            return v ? "On" : "Off";
        }, (v) -> {
            TmcgiSettings.volumetricFog = v;
        }));
    }
}
