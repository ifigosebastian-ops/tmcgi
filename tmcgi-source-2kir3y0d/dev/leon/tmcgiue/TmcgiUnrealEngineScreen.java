//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

final class TmcgiUnrealEngineScreen extends OptionsSubScreen {
    TmcgiUnrealEngineScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, Component.literal("Unreal Engine/Bridge settings"));
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
        this.list.addHeader(Component.literal("On-Screen Information"));
        this.list.addSmall(TmcgiSettingsScreen.toggle("Stat Unit (top right)", TmcgiSettings.statUnit, (v) -> {
            return v ? "On" : "Off";
        }, (v) -> {
            TmcgiSettings.statUnit = v;
        }, true), TmcgiSettingsScreen.toggle("Engine Warnings (top left)", TmcgiSettings.engineWarnings, (v) -> {
            return v ? "On" : "Off";
        }, (v) -> {
            TmcgiSettings.engineWarnings = v;
        }, true));
        this.list.addSmall(TmcgiSettingsScreen.toggle("Baking Debug (top left)", TmcgiSettings.bakingDebug, (v) -> {
            return v ? "On" : "Off";
        }, (v) -> {
            TmcgiSettings.bakingDebug = v;
        }, true), Button.builder(Component.literal("Hide All"), (b) -> {
            TmcgiSettings.statUnit = false;
            TmcgiSettings.engineWarnings = false;
            TmcgiSettings.bakingDebug = false;
            TmcgiSettings.changedLive();
            this.minecraft.gui.setScreen(new TmcgiUnrealEngineScreen(this.lastScreen));
        }).width(150).build());
        this.list.addHeader(Component.literal("Bridge"));
        this.list.addBig(CycleButton.builder((v) -> {
            return Component.literal(v ? "On" : "Off");
        }, TmcgiUeBridgeClient.compatWorldRender()).withValues(List.of(Boolean.TRUE, Boolean.FALSE)).withTooltip((v) -> {
            return Tooltip.create(Component.literal("Minecraft draws its own world too, so that the rendering code of other mods (Sodium, Iris, Flywheel, Create...) runs; its picture is discarded. Costs frame rate. Changing it restarts the renderer."));
        }).create(0, 0, 310, 20, Component.literal("Compatibility Mode"), (button, value) -> {
            TmcgiUeBridgeClient.setCompatWorldRender(value);
            TmcgiSettings.changed();
        }));
    }
}
