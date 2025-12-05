package io.github.loliiiico.landSpeak.lands;

import io.github.loliiiico.landSpeak.LandPlasmoVoice;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.List;

// Lands API imports aligned with 7.15.x
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.enums.FlagTarget;
import me.angeschossen.lands.api.flags.enums.RoleFlagCategory;
import me.angeschossen.lands.api.flags.type.RoleFlag;
import me.angeschossen.lands.api.land.LandWorld;

public class LandsHook {

    private final LandPlasmoVoice plugin;
    private RoleFlag speakFlag;
    private LandsIntegration landsApi;

    public static final String BYPASS_PERMISSION = "landspeak.bypass";

    public LandsHook(LandPlasmoVoice plugin) {
        this.plugin = plugin;
    }

    /**
     * Must be called during plugin onLoad, after Lands loaded but before it enables.
     */
    public void registerSpeakFlag() {
        this.landsApi = LandsIntegration.of(plugin);
        // Create role flag for players under ACTION category
        this.speakFlag = RoleFlag.of(landsApi, FlagTarget.PLAYER, RoleFlagCategory.ACTION, "speak")
                .setDisplayName("Speak")
                .setIcon(new ItemStack(Material.NOTE_BLOCK))
                .setDescription(List.of("§f允许使用 Simple Voice Chat 说话吗？"))
                .setDisplay(true);

        plugin.getLogger().info("Registered Lands role flag 'speak'.");
    }

    public boolean canSpeak(Player player) {
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return true;
        }
        if (speakFlag == null || landsApi == null) {
            // If flag not available, do not block
            return true;
        }
        Location loc = player.getLocation();
        if (loc.getWorld() == null) {
            return true;
        }

        try {
            LandWorld lWorld = landsApi.getWorld(loc.getWorld());
            if (lWorld == null) return true;
            return lWorld.hasRoleFlag(player.getUniqueId(), loc, this.speakFlag);
        } catch (Throwable t) {
            plugin.getLogger().warning("Lands role flag check failed: " + t.getMessage());
            return true;
        }
    }

}
