package io.github.loliiiico.landSpeak;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import io.github.loliiiico.landSpeak.lands.LandsHook;
import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.event.EventPriority;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.capture.ServerActivation;
import su.plo.voice.api.server.event.audio.capture.PlayerServerActivationEvent;

@Addon(
        id = LandPlasmoVoice.VOICE_PLUGIN_ID,
        name = "LandPlasmoVoice",
        version = "1.0.0",
        authors = {"Loliiiico"}
)
public final class LandSpeakAddon implements AddonInitializer {

    private static final long CACHE_MILLIS = 3000L;

    @InjectPlasmoVoice
    private PlasmoVoiceServer voiceServer;

    private final LandPlasmoVoice plugin;
    private final LandsHook landsHook;

    private static final class CacheEntry {
        final boolean allowed;
        final long expiry;

        CacheEntry(boolean allowed, long expiry) {
            this.allowed = allowed;
            this.expiry = expiry;
        }
    }

    private final ConcurrentHashMap<UUID, CacheEntry> speakCache = new ConcurrentHashMap<>();
    private final Set<UUID> pendingChecks = ConcurrentHashMap.newKeySet();

    public LandSpeakAddon(LandPlasmoVoice plugin, LandsHook landsHook) {
        this.plugin = plugin;
        this.landsHook = landsHook;
    }

    @Override
    public void onAddonInitialize() {
        if (voiceServer == null) {
            plugin.getLogger().severe("PlasmoVoiceServer injection failed; voice events will not be registered.");
            return;
        }

        plugin.getLogger().info("LandSpeak Plasmo Voice addon initialized, registering PlayerSpeakEvent handler");
        voiceServer.getEventBus().register(
                this,
                PlayerServerActivationEvent.class,
                EventPriority.NORMAL,
                this::handlePlayerSpeak
        );
    }

    @Override
    public void onAddonShutdown() {
        plugin.getLogger().info("LandSpeak Plasmo Voice addon shut down");
    }

    private void handlePlayerSpeak(PlayerServerActivationEvent event) {
        try {
            UUID uuid = event.getPlayer().getInstance().getUuid();
            if (uuid == null) {
                return;
            }
            if (landsHook == null) {
                return;
            }

            long now = System.currentTimeMillis();
            CacheEntry entry = speakCache.get(uuid);
            boolean cacheHit = entry != null && entry.expiry > now;
            if (cacheHit) {
                if (!entry.allowed) {
                    // Mark this audio packet as handled so default processing is skipped
                    event.setResult(ServerActivation.Result.HANDLED);
                }
                return;
            }

            // No cache yet: pessimistically mark this packet as handled
            event.setResult(ServerActivation.Result.HANDLED);

            // Only schedule one permission check per player at a time
            if (!pendingChecks.add(uuid)) {
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) {
                        return;
                    }

                    boolean allowed = landsHook.canSpeak(player);
                    long expiry = System.currentTimeMillis() + CACHE_MILLIS;
                    speakCache.put(uuid, new CacheEntry(allowed, expiry));

                    if (!allowed) {
                        player.sendMessage("§c你在当前领地的子区域中已禁止发言");
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning("Voice permission check error: " + t.getMessage());
                } finally {
                    pendingChecks.remove(uuid);
                }
            });
        } catch (Throwable t) {
            plugin.getLogger().warning("Voice event handling error: " + t.getMessage());
        }
    }
}
