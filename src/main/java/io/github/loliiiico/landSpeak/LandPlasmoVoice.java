package io.github.loliiiico.landSpeak;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.loliiiico.landSpeak.lands.LandsHook;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEvent;

public final class LandPlasmoVoice extends JavaPlugin {

    public static final String VOICE_PLUGIN_ID = "landspeak";
    // Addon class annotated with @Addon.
    private final LandPlasmoVoice addon = new LandPlasmoVoice();
    private boolean landsPresent;
    private LandsHook landsHook;
    private static final class CacheEntry {
        final boolean allowed;
        final long expiry;

        CacheEntry(boolean allowed, long expiry) {
            this.allowed = allowed;
            this.expiry = expiry;
        }
    }
    private final java.util.concurrent.ConcurrentHashMap<java.util.UUID, CacheEntry> speakCache = new java.util.concurrent.ConcurrentHashMap<>();

    private static final long CACHE_MILLIS = 3000L;
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
        PlasmoVoiceServer.getAddonsLoader().load(addon);
        try {
            landsHook = new LandsHook(this);
            landsHook.registerSpeakFlag();
        } catch (Throwable inner) {
            getLogger().severe("Failed to register Lands flags onLoad: " + inner.getMessage());
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

    }

    private void detectDependencies() {
        Plugin lands = Bukkit.getPluginManager().getPlugin("Lands");
        landsPresent = lands != null && lands.isEnabled();
    }

    public void onPlayerSpeak(PlayerSpeakEvent event) {
        try {
            UUID uuid = event.getPlayer().getInstance().getUuid();
            Player player = Bukkit.getPlayer(uuid);
            if (uuid == null) {
                return;
            }
            LandsHook lands = landsHook;
            if (lands == null) {
                return; // no lands, do not block
            }
            long now = System.currentTimeMillis();
            CacheEntry entry = speakCache.get(uuid);
            boolean cacheHit = entry != null && entry.expiry > now;
            boolean allowed = cacheHit ? entry.allowed : lands.canSpeak(player);
            if (!cacheHit) {
                speakCache.put(player.getUniqueId(), new CacheEntry(allowed, now + CACHE_MILLIS));
            }

            if (!allowed) {
                event.setCancelled(true);
                // Only send denial message when we refresh the cache (i.e., once per 3s)
                if (!cacheHit) {
                    player.sendMessage("§c你在当前领地的子区域中已禁止发言");
                }
            }
        } catch (Throwable t) {
            getLogger().warning("Voice event handling error: " + t.getMessage());
        }
    }
}
