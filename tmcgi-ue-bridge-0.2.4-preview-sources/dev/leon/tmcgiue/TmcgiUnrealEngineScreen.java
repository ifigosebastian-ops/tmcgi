//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;

final class TmcgiUnrealEngineScreen extends OptionsSubScreen {
    TmcgiUnrealEngineScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, TmcgiSettingsScreen.t("screen.unreal"));
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
        this.list.addHeader(TmcgiSettingsScreen.t("header.onScreen"));
        this.list.addSmall(TmcgiSettingsScreen.toggle("option.statUnit", TmcgiSettings.statUnit, (v) -> {
            TmcgiSettings.statUnit = v;
        }, true), TmcgiSettingsScreen.toggle("option.engineWarnings", TmcgiSettings.engineWarnings, (v) -> {
            TmcgiSettings.engineWarnings = v;
        }, true));
        this.list.addSmall(TmcgiSettingsScreen.toggle("option.bakingDebug", TmcgiSettings.bakingDebug, (v) -> {
            TmcgiSettings.bakingDebug = v;
        }, true), Button.builder(TmcgiSettingsScreen.t("button.hideAll"), (b) -> {
            TmcgiSettings.statUnit = false;
            TmcgiSettings.engineWarnings = false;
            TmcgiSettings.bakingDebug = false;
            TmcgiSettings.changedLive();
            this.minecraft.gui.setScreen(new TmcgiUnrealEngineScreen(this.lastScreen));
        }).width(150).build());
    }
}
