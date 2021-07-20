package xyz.rgnt.wfpowerblocks.statics;


import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Placeholder API static functionality
 */
public class PlaceholderStatics {

    /**
     * @return Returns true value only when PlaceholderAPI plugin is present. Otherwise returns false.
     */
    public static boolean hasPlaceholderAPISupport() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    /**
     * Processes message with PlaceholderAPI if possible
     *
     * @param message Message
     * @param player  Player
     * @return Processed message
     */
    public static @NotNull String askPapiForPlaceholders(@NotNull String message, @NotNull Player player) {
        return hasPlaceholderAPISupport() ? PlaceholderAPI.setPlaceholders(player, message) : message;
    }


}
