package xyz.rgnt.wfpowerblocks;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import xyz.rgnt.revoken.common.Revoken;
import xyz.rgnt.revoken.common.providers.storage.flatfile.StorageProvider;
import xyz.rgnt.wfpowerblocks.block.PowerBlock;
import xyz.rgnt.wfpowerblocks.statics.PlaceholderStatics;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Log4j2(topic = "WarfarePowerBlocks - Plugin")
public class BukkitPlugin extends JavaPlugin implements Revoken<BukkitPlugin> {

    @Getter
    private final StorageProvider storageProvider;

    @Getter
    private PowerBlocksMngr powerBlocksMngr;

    static {
        if (Bukkit.getPluginManager().getPermission("warfarepowerblocks.admin") == null)
            Bukkit.getPluginManager().addPermission(new Permission("warfarepowerblocks.admin", PermissionDefault.OP));
    }


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
        if (PlaceholderStatics.hasPlaceholderAPISupport())
            new PlaceholderExt().register();

        Bukkit.getServer().getCommandMap().register("warfaremc", new Command("powerblocks") {
            {
                setAliases(Arrays.asList("pwrb", "pwrblocks", "pwrbl"));
            }

            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
                if (!sender.hasPermission("warfarepowerblocks.admin"))
                    return true;
                if(args.length == 0) {
                    sender.sendMessage("§cUsage: /pwrb <reload, save, info, add>");
                    return true;
                }

                if(args[0].equalsIgnoreCase("reload")) {
                    powerBlocksMngr.getConfiguration().loadConfiguration();
                    sender.sendMessage("§aReloaded!");
                } else if(args[0].equalsIgnoreCase("save")) {
                    sender.sendMessage("§aSaving...");
                    Bukkit.getScheduler().runTaskAsynchronously(BukkitPlugin.this, () -> {
                        BukkitPlugin.this.powerBlocksMngr.save();
                        sender.sendMessage("§aSaved!");
                    });
                } else if(args[0].equalsIgnoreCase("info")) {
                    if(sender instanceof Player) {
                        final Block block = ((Player) sender).getTargetBlock(10);
                        if (block == null) {
                            sender.sendMessage("§cYou have to look at a block.");
                            return true;
                        }
                        final PowerBlock pwrBlock = powerBlocksMngr.getPowerBlock(block.getLocation());
                        if (pwrBlock == null) {
                            sender.sendMessage("§cThat block is not a power block.");
                            return true;
                        }

                        sender.sendMessage("§a" + pwrBlock.getId() + ", Current Health: " + pwrBlock.getCurrentHealthPoints() + ", Maximal Health: " + pwrBlock.getMaximalHealthPoints());
                    } else {

                    }
                } else if(args[0].equalsIgnoreCase("add")) {
                    if(sender instanceof Player) {
                        final Block block = ((Player) sender).getTargetBlock(10);
                        if (block == null) {
                            sender.sendMessage("§cYou have to look at a block.");
                            return true;
                        }
                        PowerBlock pwrBlock = powerBlocksMngr.getPowerBlock(block.getLocation());
                        if (pwrBlock != null) {
                            sender.sendMessage("§cThat block is a power block, choose other.");
                            return true;
                        }
                        // Registering new PowerBlock
                        if (args.length < 3) {
                            sender.sendMessage("§cNot enough arguments provided.");
                            return true;
                        }
                        var id = args[1];
                        if (!args[2].chars().allMatch(Character::isDigit)) {
                            sender.sendMessage("§cArgument must be a valid number.");
                            return true;
                        }
                        var hp = Integer.parseInt(args[2]);
                        pwrBlock = powerBlocksMngr.createPowerBlock(id, block, hp);
                        sender.sendMessage("§aCreated: " + pwrBlock.getId() + ", Current Health: " + pwrBlock.getCurrentHealthPoints() + ", Maximal Health: " + pwrBlock.getMaximalHealthPoints());
                    } else {

                    }
                }
                return true;
            }

            @Override
            public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
                if (!sender.hasPermission("warfarepowerblocks.admin"))
                    return Collections.emptyList();
                if(args.length == 1)
                    return Arrays.asList("reload", "save", "info", "add");
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
