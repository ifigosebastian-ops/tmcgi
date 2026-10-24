//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue;

import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

final class TmcgiRenderScreen extends OptionsSubScreen {
    private final Screen parent;

    TmcgiRenderScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, Component.literal("Render Settings"));
        this.parent = lastScreen;
    }

    private static String[] percentNames() {
        String[] names = new String[TmcgiSettings.RESOLUTION_LEVELS.length];

        for(int i = 0; i < names.length; ++i) {
            int var10002 = TmcgiSettings.RESOLUTION_LEVELS[i];
            names[i] = "" + var10002 + "%";
        }

        return names;
    }

    private static CycleButton<Integer> quality(String name, int current, IntConsumer set) {
        return TmcgiSettingsScreen.levels(name, TmcgiSettings.QUALITY_NAMES, TmcgiSettings.QUALITY_LEVELS, current, set);
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
        this.list.addHeader(Component.literal("Quality Levels"));
        this.list.addSmall(TmcgiSettingsScreen.levels("Resolution", percentNames(), TmcgiSettings.RESOLUTION_LEVELS, TmcgiSettings.resolutionQuality, (v) -> {
            TmcgiSettings.resolutionQuality = v;
        }), quality("View Distance", TmcgiSettings.viewDistanceQuality, (v) -> {
            TmcgiSettings.viewDistanceQuality = v;
        }));
        this.list.addSmall(quality("Anti-Aliasing Quality", TmcgiSettings.antiAliasingQuality, (v) -> {
            TmcgiSettings.antiAliasingQuality = v;
        }), quality("Shadows", TmcgiSettings.shadowQuality, (v) -> {
            TmcgiSettings.shadowQuality = v;
        }));
        Object illumination;
        if (TmcgiSettings.lumen) {
            illumination = quality("Global Illumination", TmcgiSettings.globalIlluminationQuality, (v) -> {
                TmcgiSettings.globalIlluminationQuality = v;
            });
        } else {
            illumination = Button.builder(Component.literal("Global Illumination: N/A"), (b) -> {
            }).width(150).build();
            ((AbstractWidget)illumination).active = false;
        }

        this.list.addSmall((AbstractWidget)illumination, quality("Reflections", TmcgiSettings.reflectionQuality, (v) -> {
            TmcgiSettings.reflectionQuality = v;
        }));
        this.list.addSmall(quality("Post Processing", TmcgiSettings.postProcessQuality, (v) -> {
            TmcgiSettings.postProcessQuality = v;
        }), quality("Textures", TmcgiSettings.textureQuality, (v) -> {
            TmcgiSettings.textureQuality = v;
        }));
        this.list.addSmall(quality("Effects", TmcgiSettings.effectsQuality, (v) -> {
            TmcgiSettings.effectsQuality = v;
        }), quality("Shading", TmcgiSettings.shadingQuality, (v) -> {
            TmcgiSettings.shadingQuality = v;
        }));
        this.list.addHeader(Component.literal("Anti-Aliasing and Lighting"));
        this.list.addSmall(TmcgiSettingsScreen.levels("Anti-Aliasing Mode", TmcgiSettings.ANTI_ALIASING_NAMES, TmcgiSettings.ANTI_ALIASING_METHODS, TmcgiSettings.antiAliasingMethod, (v) -> {
            TmcgiSettings.antiAliasingMethod = v;
        }), TmcgiSettingsScreen.toggle("Lumen GI", TmcgiSettings.lumen, (v) -> {
            return v ? "On" : "Off";
        }, (v) -> {
            TmcgiSettings.lumen = v;
            this.minecraft.gui.setScreen(new TmcgiRenderScreen(this.parent));
        }));
        this.list.addSmall(TmcgiSettingsScreen.toggle("MegaLights", TmcgiSettings.megaLights, (v) -> {
            return v ? "On" : "Off";
        }, (v) -> {
            TmcgiSettings.megaLights = v;
        }), TmcgiSettingsScreen.toggle("Hardware Ray Tracing", TmcgiSettings.hardwareRayTracing, (v) -> {
            String var10000 = v ? "On" : "Off";
            return var10000 + (v != TmcgiSettings.launchedWithRayTracing ? " (restart to apply)" : "");
        }, (v) -> {
            TmcgiSettings.hardwareRayTracing = v;
        }));
        this.list.addBig(TmcgiSettingsScreen.levels("Ray Tracing Distance", TmcgiSettings.RAY_TRACING_DISTANCE_NAMES, TmcgiSettings.RAY_TRACING_DISTANCES, TmcgiSettings.rayTracingDistance, (v) -> {
            TmcgiSettings.rayTracingDistance = v;
        }));
        this.list.addBig(Button.builder(Component.literal("Restart Renderer (needed for Hardware Ray Tracing)"), (b) -> {
            if (RendererLauncher.restart()) {
                this.minecraft.gui.hud.getChat().addClientSystemMessage(Component.literal("[TMCGI] Restarting the renderer..."));
            } else {
                this.minecraft.gui.hud.getChat().addClientSystemMessage(Component.literal("[TMCGI] The renderer was not started by Minecraft, please restart it by hand."));
            }

        }).build());
    }
}
