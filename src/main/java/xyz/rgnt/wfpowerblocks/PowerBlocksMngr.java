package xyz.rgnt.wfpowerblocks;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.logging.log4j.util.Strings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.rgnt.revoken.common.providers.storage.data.codec.ICodec;
import xyz.rgnt.revoken.common.providers.storage.data.codec.meta.CodecKey;
import xyz.rgnt.revoken.common.providers.storage.flatfile.store.AStore;
import xyz.rgnt.wfpowerblocks.block.PowerBlock;

import java.util.*;

/**
 * Manages power blocks
 */
@Log4j2(topic = "WarfarePowerBlocks - Mngr")
public class PowerBlocksMngr implements Listener {

    private final BukkitPlugin pluginInstance;

    private final Table<UUID, Long, PowerBlock> powerBlocks = HashBasedTable.create();

    @Getter
    private @NotNull Optional<AStore> configurationStore = Optional.empty();
    @Getter
    private final Configuration configuration = new Configuration();

    /**
     * Default constructor
     *
     * @param owningPlugin Plugin instance
     */
    public PowerBlocksMngr(@NotNull BukkitPlugin owningPlugin) {
        this.pluginInstance = owningPlugin;
    }

    /**
     * Initializes manager
     */
    public void initialize() {
        this.configuration.reload();

        final long saveInterval = this.configuration.dataSaveInterval * 60 * 20;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(pluginInstance, this::save, saveInterval, saveInterval);
    }

    /**
     * Terminates manager
     */
    public void terminate() {
        this.save();
    }


    public void save() {
        log.info("Saving data...");
        this.powerBlocks.values().forEach(powerBlock -> {
            try {
                AStore store = pluginInstance.getStorageProvider().provideJson("", "data/" + powerBlock.getId() + ".json", false);
                store.setUnderlyingDataSource(PowerBlock.encodeBlockMemory(powerBlock.getBlockMemory()));
                store.save();
            } catch (Exception e) {
                log.error("Couldn't create memory file for power block '{}'", powerBlock.getId(), e);
            }
        });
        log.info("Saved data.");
    }


    private void registerPowerBlock(@NotNull PowerBlock powerBlock) {
        final Location loc = powerBlock.getBlockLocation();
        final UUID worldUID = powerBlock.getBlockLocation().getWorld().getUID();
        final Long locKey = loc.toBlockKey();

        this.powerBlocks.put(
                worldUID,
                locKey,
                powerBlock
        );
        log.info("§fPlugin created §ePower block§f at {} {} {}({}).",
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ(),
                loc.getWorld().getName()
        );
    }

    public @NotNull PowerBlock createPowerBlock(@NotNull String id, @NotNull Block block, @NotNull Integer healthPoints) {
        final var powerBlock = PowerBlock.builder(id)
                .fromBukkitBlock(block)
                .withMaximalHealthPoints(healthPoints)
                .build();
        registerPowerBlock(powerBlock);
        return powerBlock;
    }


    public @Nullable PowerBlock destroyPowerBlock(@NotNull Block block) {
        final UUID worldUID = block.getWorld().getUID();
        final Long locKey = block.getBlockKey();

        final Location loc = block.getLocation();
        log.info("§fPlugin destroyed §ePower block§f at {} {} {}({}).",
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ(),
                loc.getWorld().getName()
        );
        return this.powerBlocks.remove(worldUID, locKey);
    }


    public @Nullable PowerBlock getPowerBlock(@NotNull Location location) {
        return this.getPowerBlock(location.toBlockKey(), location.getWorld());
    }

    public @Nullable PowerBlock getPowerBlock(@NotNull Long location, @NotNull World world) {
        return this.getPowerBlock(location, world.getUID());
    }

    public @Nullable PowerBlock getPowerBlock(@NotNull Long location, @NotNull UUID worldUID) {
        return this.powerBlocks.get(worldUID, location);
    }


    @EventHandler
    public void handleOnDestroyBlock(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        final long locKey = block.getBlockKey();
        final UUID worldUID = block.getWorld().getUID();

        final PowerBlock powerBlock = this.powerBlocks.get(worldUID, locKey);
        if (powerBlock == null)
            return;

        event.setCancelled(true);
        final UUID vandal = event.getPlayer().getUniqueId();
        final Location loc = block.getLocation();

        if (powerBlock.getBlockMemory().damage(vandal, 1) > 0)
            return;

        log.info("§fDestroyed §ePower block§f at {} {} {}({}) with key {}. Total participants count: {}.",
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ(),
                loc.getWorld().getName(),
                locKey,
                powerBlock.getBlockMemory().getAttackers().size()
        );

        Bukkit.broadcast(configuration.msgPowerBlockDefeated(powerBlock));

        powerBlock.reset();
    }

    class Configuration implements ICodec {
        @Getter
        private final List<PowerBlock.Codec> powerBlockCodecs = new ArrayList<>();

        @CodecKey("messages.pwb-defeated")
        private final List<String> pwbDefeated = new ArrayList<>();

        @CodecKey("data.save-interval-minutes")
        @Getter
        private final int dataSaveInterval = 180;

        public @NotNull Component msgPowerBlockDefeated(@NotNull PowerBlock block) {
            final Map<String, String> placeholders = new HashMap<>();
            final var attackers = block.getBlockMemory().getAttackersSorted();

            placeholders.put("pwb-max-health", block.getMaximalHealthPoints() + "");
            for (int i = 0; i < (Math.min(attackers.size(), 3)); i++) {
                final String attackerName = Bukkit.getOfflinePlayer(attackers.get(i).getKey()).getName();
                final Integer attackerScore = attackers.get(i).getValue();
                placeholders.put((i + 1) + "-attacker", attackerName);
                placeholders.put((i + 1) + "-attacker-damage", attackerScore + "");
                placeholders.put((i + 1) + "-attacker-damage-%", Math.round((block.getMaximalHealthPoints() / attackerScore) * 100f) + "");
            }

            Component component = MiniMessage.markdown().parse(Strings.join(pwbDefeated, '\n'), placeholders);
            component = component.replaceText(TextReplacementConfig.builder()
                    .match("<pwb-name>")
                    .replacement(block.getName())
                    .build());
            return component;
        }

        public void reload() {
            configurationStore.or(() -> {
                try {
                    return Optional.of(
                            pluginInstance.getStorageProvider()
                                    .provideYaml("resources", "configuration.yaml", true)
                    );
                } catch (Exception x) {
                    log.error("Couldn't provide default configuration", x);
                }
                return Optional.empty();
            }).ifPresent((config) -> {
                final var data = config.getData();

                try {
                    data.decode(this);
                } catch (Exception e) {
                    log.error("Couldn't decode settings.", e);
                }

                final var powerBlocksCodecsSector = data.getSector("power-blocks");
                if (powerBlocksCodecsSector == null) {
                    log.error("No power blocks specified!");
                    return;
                }
                powerBlocksCodecsSector.getKeys().forEach(powerBlockId -> {
                    final var powerBlockCodec = powerBlocksCodecsSector.getSector(powerBlockId);
                    if (powerBlockCodec == null)
                        return;

                    final PowerBlock.Codec codec = new PowerBlock.Codec();
                    try {
                        powerBlockCodec.decode(codec);
                        this.powerBlockCodecs.add(codec);
                    } catch (Exception e) {
                        log.error("Couldn't decode powerblock with id '{}'", powerBlockId, e);
                        return;
                    }

                    PowerBlock powerBlock = null;
                    final AStore blockMemoryStore;
                    try {
                        blockMemoryStore = pluginInstance.getStorageProvider().provideJson("", "data/" + powerBlockId + ".json", false);
                        if (blockMemoryStore.getFile().exists())
                            powerBlock = codec.makePowerBlock(powerBlockId, PowerBlock.decodeBlockMemory((JsonObject) blockMemoryStore.getUnderlyingDataSource()));
                    } catch (Exception x) {
                        log.error("Failed to decode memory file of power block '{}'.", powerBlockId, x);
                    } finally {
                        if (powerBlock == null)
                            powerBlock = codec.makePowerBlock(powerBlockId, null);
                    }
                    registerPowerBlock(powerBlock);
                });
            });
        }
    }

}
