package xyz.rgnt.wfpowerblocks;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import xyz.rgnt.revoken.common.Revoken;
import xyz.rgnt.revoken.common.providers.storage.flatfile.StorageProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Log4j2(topic = "WarfarePowerBlocks - Plugin")
public class BukkitPlugin extends JavaPlugin implements Revoken<BukkitPlugin> {

    @Getter
    private final StorageProvider storageProvider;
    private PowerBlocksMngr powerBlocksMngr;

    public BukkitPlugin() {
        this.storageProvider = new StorageProvider(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        log.info("Constructing plugin.");

        this.powerBlocksMngr = new PowerBlocksMngr(this);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        log.info("Initializing plugin.");

        this.powerBlocksMngr.initialize();
        Bukkit.getPluginManager().addPermission(new Permission("warfarepowerblocks.admin", PermissionDefault.OP));
        Bukkit.getPluginManager().registerEvents(this.powerBlocksMngr, this);
        Bukkit.getServer().getCommandMap().register("warfaremc", new Command("powerblocks") {
            {
                setAliases(Arrays.asList("pwrb", "pwrblocks", "pwrbl"));
            }

            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
                if (!sender.hasPermission("warfarepowerblocks.admin"))
                    return true;
                if(args.length == 0) {
                    sender.sendMessage("§cUsage: /pwrb <reload, save>");
                    return true;
                }

                if(args[0].equalsIgnoreCase("reload")) {
                    powerBlocksMngr.getConfiguration().reload();
                    sender.sendMessage("§aReloaded!");
                } else if(args[0].equalsIgnoreCase("save")) {
                    sender.sendMessage("§aSaving...");
                    Bukkit.getScheduler().runTaskAsynchronously(BukkitPlugin.this, () -> {
                        BukkitPlugin.this.powerBlocksMngr.save();
                        sender.sendMessage("§aSaved!");
                    });
                }
                return true;
            }

            @Override
            public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
                if (!sender.hasPermission("warfarepowerblocks.admin"))
                    return Collections.emptyList();
                if(args.length == 1)
                    return Arrays.asList("reload", "save");
                return Collections.emptyList();
            }
        });
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.powerBlocksMngr.terminate();

        log.info("Terminating plugin.");
    }

    @Override
    public BukkitPlugin instance() {
        return this;
    }
}
