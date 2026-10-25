//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

final class TmcgiRenderScreen extends OptionsSubScreen {
    private final Screen parent;

    TmcgiRenderScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, TmcgiSettingsScreen.t("screen.render"));
        this.parent = lastScreen;
    }

    private static Component[] percentNames() {
        Component[] names = new Component[TmcgiSettings.RESOLUTION_LEVELS.length];

        for(int i = 0; i < names.length; ++i) {
            int var10002 = TmcgiSettings.RESOLUTION_LEVELS[i];
            names[i] = Component.literal("" + var10002 + "%");
        }

        return names;
    }

    private static Component[] distanceNames() {
        Component[] names = new Component[TmcgiSettings.RAY_TRACING_DISTANCES.length];

        for(int i = 0; i < names.length; ++i) {
            int chunks = TmcgiSettings.RAY_TRACING_DISTANCES[i];
            names[i] = chunks > 0 ? TmcgiSettingsScreen.t("distance.chunks", chunks) : TmcgiSettingsScreen.t("distance.unlimited");
        }

        return names;
    }

    private static CycleButton<Integer> quality(String name, int current, IntConsumer set) {
        return TmcgiSettingsScreen.levels(name, TmcgiSettingsScreen.texts(TmcgiSettings.QUALITY_NAMES), TmcgiSettings.QUALITY_LEVELS, current, set);
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
        this.list.addHeader(TmcgiSettingsScreen.t("header.quality"));
        this.list.addSmall(TmcgiSettingsScreen.levels("option.resolution", percentNames(), TmcgiSettings.RESOLUTION_LEVELS, TmcgiSettings.resolutionQuality, (v) -> {
            TmcgiSettings.resolutionQuality = v;
        }), quality("option.viewDistance", TmcgiSettings.viewDistanceQuality, (v) -> {
            TmcgiSettings.viewDistanceQuality = v;
        }));
        this.list.addSmall(quality("option.antiAliasingQuality", TmcgiSettings.antiAliasingQuality, (v) -> {
            TmcgiSettings.antiAliasingQuality = v;
        }), quality("option.shadows", TmcgiSettings.shadowQuality, (v) -> {
            TmcgiSettings.shadowQuality = v;
        }));
        Object illumination;
        if (TmcgiSettings.lumen) {
            illumination = quality("option.globalIllumination", TmcgiSettings.globalIlluminationQuality, (v) -> {
                TmcgiSettings.globalIlluminationQuality = v;
            });
        } else {
            illumination = Button.builder(TmcgiSettingsScreen.t("option.globalIllumination.na"), (b) -> {
            }).width(150).build();
            ((AbstractWidget)illumination).active = false;
        }

        this.list.addSmall((AbstractWidget)illumination, quality("option.reflections", TmcgiSettings.reflectionQuality, (v) -> {
            TmcgiSettings.reflectionQuality = v;
        }));
        this.list.addSmall(quality("option.postProcessing", TmcgiSettings.postProcessQuality, (v) -> {
            TmcgiSettings.postProcessQuality = v;
        }), quality("option.textures", TmcgiSettings.textureQuality, (v) -> {
            TmcgiSettings.textureQuality = v;
        }));
        this.list.addSmall(quality("option.effects", TmcgiSettings.effectsQuality, (v) -> {
            TmcgiSettings.effectsQuality = v;
        }), quality("option.shading", TmcgiSettings.shadingQuality, (v) -> {
            TmcgiSettings.shadingQuality = v;
        }));
        this.list.addHeader(TmcgiSettingsScreen.t("header.lighting"));
        this.list.addSmall(TmcgiSettingsScreen.levels("option.antiAliasingMode", TmcgiSettingsScreen.literals(TmcgiSettings.ANTI_ALIASING_NAMES), TmcgiSettings.ANTI_ALIASING_METHODS, TmcgiSettings.antiAliasingMethod, (v) -> {
            TmcgiSettings.antiAliasingMethod = v;
        }), TmcgiSettingsScreen.toggle("option.lumen", TmcgiSettings.lumen, (v) -> {
            TmcgiSettings.lumen = v;
            this.minecraft.gui.setScreen(new TmcgiRenderScreen(this.parent));
        }));
        this.list.addSmall(TmcgiSettingsScreen.toggle("option.megaLights", TmcgiSettings.megaLights, (v) -> {
            TmcgiSettings.megaLights = v;
        }), TmcgiSettingsScreen.toggle("option.hardwareRayTracing", TmcgiSettings.hardwareRayTracing, (v) -> {
            return (Component)(v != TmcgiSettings.launchedWithRayTracing ? TmcgiSettingsScreen.t("option.restartToApply", TmcgiSettingsScreen.onOff(v)) : TmcgiSettingsScreen.onOff(v));
        }, (v) -> {
            TmcgiSettings.hardwareRayTracing = v;
        }, false));
        this.list.addSmall(TmcgiSettingsScreen.toggle("option.motionBlur", TmcgiSettings.motionBlur, (v) -> {
            TmcgiSettings.motionBlur = v;
        }), TmcgiSettingsScreen.toggle("option.dynamicLights", TmcgiSettings.dynamicLights, (v) -> {
            TmcgiSettings.dynamicLights = v;
        }, true));
        this.list.addBig(new BlockLightSlider());
        this.list.addBig(CycleButton.builder(TmcgiSettingsScreen::onOff, TmcgiSettings.lightLimit).withValues(List.of(Boolean.TRUE, Boolean.FALSE)).withTooltip((v) -> {
            return Tooltip.create(TmcgiSettingsScreen.t("option.lightLimit.tooltip"));
        }).create(0, 0, 310, 20, TmcgiSettingsScreen.t("option.lightLimit"), (button, value) -> {
            TmcgiSettings.lightLimit = value;
            TmcgiSettings.changed();
        }));
        this.list.addBig(TmcgiSettingsScreen.levels("option.rayTracingDistance", distanceNames(), TmcgiSettings.RAY_TRACING_DISTANCES, TmcgiSettings.rayTracingDistance, (v) -> {
            TmcgiSettings.rayTracingDistance = v;
        }));
        this.list.addBig(Button.builder(TmcgiSettingsScreen.t("button.restartRenderer"), (b) -> {
            this.minecraft.gui.hud.getChat().addClientSystemMessage(TmcgiSettingsScreen.t(RendererLauncher.restart() ? "chat.restarting" : "chat.restartManual"));
        }).build());
    }

    private static final class BlockLightSlider extends AbstractSliderButton {
        private static final int MIN = 2;
        private static final int MAX = 200;

        BlockLightSlider() {
            super(0, 0, 310, 20, Component.empty(), (double)(TmcgiSettings.blockLightIntensity - 2) / 198.0);
            this.updateMessage();
        }

        private int percent() {
            return 2 + (int)Math.round(this.value * 198.0);
        }

        protected void updateMessage() {
            this.setMessage(TmcgiSettingsScreen.t("option.blockLightIntensity", String.format(Locale.ROOT, "%.2fx", (double)this.percent() / 100.0)));
        }

        protected void applyValue() {
            int percent = this.percent();
            if (percent != TmcgiSettings.blockLightIntensity) {
                TmcgiSettings.blockLightIntensity = percent;
                TmcgiSettings.changed();
            }

        }
    }
}
