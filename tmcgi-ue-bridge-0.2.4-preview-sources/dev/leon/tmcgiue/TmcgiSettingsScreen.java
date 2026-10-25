//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class TmcgiSettingsScreen extends OptionsSubScreen {
    public TmcgiSettingsScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, t("screen.main"));
    }

    public static MutableComponent t(String key, Object... args) {
        return Component.translatable("tmcgi_ue_bridge." + key, args);
    }

    static Component[] texts(String... keys) {
        Component[] out = new Component[keys.length];

        for(int i = 0; i < keys.length; ++i) {
            out[i] = t(keys[i]);
        }

        return out;
    }

    static Component[] literals(String... names) {
        Component[] out = new Component[names.length];

        for(int i = 0; i < names.length; ++i) {
            out[i] = Component.literal(names[i]);
        }

        return out;
    }

    static Component onOff(boolean value) {
        return value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
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
            return v < 0 ? t("preset.custom") : t(TmcgiSettings.PRESET_NAMES[v]);
        }, TmcgiSettings.currentPreset()).withValues(presets).create(0, 0, 310, 20, t("option.preset"), (button, value) -> {
            if (value >= 0) {
                TmcgiSettings.applyPreset(value);
            }

        }));
        this.list.addSmall(Button.builder(t("button.weather"), (b) -> {
            this.minecraft.gui.setScreen(new TmcgiWeatherScreen(this));
        }).width(150).build(), Button.builder(t("button.render"), (b) -> {
            this.minecraft.gui.setScreen(new TmcgiRenderScreen(this));
        }).width(150).build());
        this.list.addBig(Button.builder(t("button.unreal"), (b) -> {
            this.minecraft.gui.setScreen(new TmcgiUnrealEngineScreen(this));
        }).build());
        this.list.addBig(Button.builder(t("button.reset"), (b) -> {
            TmcgiSettings.resetToDefaults();
            this.minecraft.gui.setScreen(new TmcgiSettingsScreen(this.lastScreen));
        }).build());
    }

    static CycleButton<Integer> levels(String name, Component[] names, int[] values, int current, IntConsumer set) {
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
                    return names[i];
                }
            }

            return Component.literal(Integer.toString(v));
        }, current).withValues(all).create(0, 0, 150, 20, t(name), (button, valuex) -> {
            set.accept(valuex);
            TmcgiSettings.changed();
        });
    }

    static CycleButton<Boolean> toggle(String name, boolean current, Consumer<Boolean> set) {
        return toggle(name, current, TmcgiSettingsScreen::onOff, set, false);
    }

    static CycleButton<Boolean> toggle(String name, boolean current, Consumer<Boolean> set, boolean live) {
        return toggle(name, current, TmcgiSettingsScreen::onOff, set, live);
    }

    static CycleButton<Boolean> toggle(String name, boolean current, Function<Boolean, Component> label, Consumer<Boolean> set, boolean live) {
        Objects.requireNonNull(label);
        return CycleButton.builder(label::apply, current).withValues(List.of(Boolean.TRUE, Boolean.FALSE)).create(0, 0, 150, 20, t(name), (button, value) -> {
            set.accept(value);
            if (live) {
                TmcgiSettings.changedLive();
            } else {
                TmcgiSettings.changed();
            }

        });
    }
}
