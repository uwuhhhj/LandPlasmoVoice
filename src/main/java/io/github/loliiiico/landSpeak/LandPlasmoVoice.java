package io.github.loliiiico.landSpeak;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.loliiiico.landSpeak.lands.LandsHook;
import su.plo.voice.api.server.PlasmoVoiceServer;

public final class LandPlasmoVoice extends JavaPlugin {

    public static final String VOICE_PLUGIN_ID = "landspeak";

    private boolean landsPresent;
    private LandsHook landsHook;
    private LandSpeakAddon addon;
    
    @Override
    public void onLoad() {
        // Register Lands flags at the correct lifecycle phase
        Plugin lands = Bukkit.getPluginManager().getPlugin("Lands");
        landsPresent = lands != null; // at onLoad, plugin might not be enabled yet
        if (!landsPresent) return;
        
        Plugin plasmoVoice = Bukkit.getPluginManager().getPlugin("PlasmoVoice");
        if (plasmoVoice == null) {
            getLogger().severe("PlasmoVoice not found. plugin disabled.");
            return;
        }

        try {
            landsHook = new LandsHook(this);
            landsHook.registerSpeakFlag();

            addon = new LandSpeakAddon(this, landsHook);
            PlasmoVoiceServer.getAddonsLoader().load(addon);
        } catch (Throwable inner) {
            getLogger().severe("Failed to register Lands flags or load addon onLoad: " + inner.getMessage());
        }
    }

    @Override
    public void onEnable() {
        detectDependencies();
        if (!landsPresent) {
            getLogger().warning("Lands not found. Flag registration and checks are disabled.");
        }

    }

    @Override
    public void onDisable() {
        if (addon != null) {
            try {
                PlasmoVoiceServer.getAddonsLoader().unload(addon);
            } catch (Throwable t) {
                getLogger().warning("Failed to unload Plasmo Voice addon: " + t.getMessage());
            }
        }
    }

    private void detectDependencies() {
        Plugin lands = Bukkit.getPluginManager().getPlugin("Lands");
        landsPresent = lands != null && lands.isEnabled();
    }
}
