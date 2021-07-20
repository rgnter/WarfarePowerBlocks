package xyz.rgnt.wfpowerblocks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderExt extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "wfp";
    }

    @Override
    public @NotNull String getAuthor() {
        return "warfaremc.eu developers";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        try {
            var id = params.split("_")[0];
            var tp = params.split("_")[1];
            var powerBlock = BukkitPlugin.getPlugin(BukkitPlugin.class).getPowerBlocksMngr()
                    .getPowerBlock(id);
            if (powerBlock == null)
                return "NaN";

            return switch (tp) {
                case "maxHealth" -> powerBlock.getMaximalHealthPoints() + "";
                case "currentHealth" -> powerBlock.getCurrentHealthPoints() + "";
                default -> "NaN";
            };
        }
        catch (final Exception exception) {
            return "NaN";
        }
    }
}