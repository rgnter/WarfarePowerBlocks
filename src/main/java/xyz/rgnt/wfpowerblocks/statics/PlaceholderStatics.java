package xyz.rgnt.wfpowerblocks.statics;


import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Placeholder API Statics
 */
public class PlaceholderStatics {

    /**
     * Processes message with PlaceholderAPI if possible
     *
     * @param message Message
     * @param player  Player
     * @return Processed message
     */
    public static @NotNull String askPapiForPlaceholders(@NotNull String message, @NotNull Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return message;
        }
        return message;
    }


}
