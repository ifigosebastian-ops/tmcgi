//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public class TmcgiSettingsScreen extends OptionsSubScreen {
    public TmcgiSettingsScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, Component.literal("TMCGI Renderer Settings"));
    }

    TmcgiSettingsScreen fresh() {
        return new TmcgiSettingsScreen(this.lastScreen);
    }

    public void onClose() {
        TmcgiSettings.apply();
        super.onClose();
    }

    protected void addOptions() {
        List<Integer> presets = new ArrayList();
        presets.add(-1);

        for(int i = 0; i < TmcgiSettings.PRESET_NAMES.length; ++i) {
            presets.add(i);
        }

        this.list.addBig(CycleButton.builder((v) -> {
            return Component.literal(v < 0 ? "Custom" : TmcgiSettings.PRESET_NAMES[v]);
        }, TmcgiSettings.currentPreset()).withValues(presets).create(0, 0, 310, 20, Component.literal("Global Preset"), (button, value) -> {
            if (value >= 0) {
                TmcgiSettings.applyPreset(value);
            }

        }));
        this.list.addSmall(Button.builder(Component.literal("Weather Settings..."), (b) -> {
            this.minecraft.gui.setScreen(new TmcgiWeatherScreen(this));
        }).width(150).build(), Button.builder(Component.literal("Render Settings..."), (b) -> {
            this.minecraft.gui.setScreen(new TmcgiRenderScreen(this));
        }).width(150).build());
        this.list.addBig(Button.builder(Component.literal("Unreal Engine/Bridge settings..."), (b) -> {
            this.minecraft.gui.setScreen(new TmcgiUnrealEngineScreen(this));
        }).build());
        this.list.addBig(Button.builder(Component.literal("Reset All to Defaults"), (b) -> {
            TmcgiSettings.resetToDefaults();
            this.minecraft.gui.setScreen(new TmcgiSettingsScreen(this.lastScreen));
        }).build());
    }

    static CycleButton<Integer> levels(String name, String[] names, int[] values, int current, IntConsumer set) {
        List<Integer> all = new ArrayList();
        int[] var6 = values;
        int var7 = values.length;

        for(int var8 = 0; var8 < var7; ++var8) {
            int value = var6[var8];
            all.add(value);
        }

        if (!all.contains(current)) {
            all.add(current);
        }

        return CycleButton.builder((v) -> {
            for(int i = 0; i < values.length; ++i) {
                if (values[i] == v) {
                    return Component.literal(names[i]);
                }
            }

            return Component.literal(Integer.toString(v));
        }, current).withValues(all).create(0, 0, 150, 20, Component.literal(name), (button, valuex) -> {
            set.accept(valuex);
            TmcgiSettings.changed();
        });
    }

    static CycleButton<Boolean> toggle(String name, boolean current, Function<Boolean, String> label, Consumer<Boolean> set) {
        return toggle(name, current, label, set, false);
    }

    static CycleButton<Boolean> toggle(String name, boolean current, Function<Boolean, String> label, Consumer<Boolean> set, boolean live) {
        return CycleButton.builder((v) -> {
            return Component.literal((String)label.apply(v));
        }, current).withValues(List.of(Boolean.TRUE, Boolean.FALSE)).create(0, 0, 150, 20, Component.literal(name), (button, value) -> {
            set.accept(value);
            if (live) {
                TmcgiSettings.changedLive();
            } else {
                TmcgiSettings.changed();
            }

        });
    }
}
