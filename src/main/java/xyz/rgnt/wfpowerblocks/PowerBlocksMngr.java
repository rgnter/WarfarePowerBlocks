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
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.rgnt.revoken.common.providers.storage.data.codec.ICodec;
import xyz.rgnt.revoken.common.providers.storage.data.codec.meta.CodecKey;
import xyz.rgnt.revoken.common.providers.storage.flatfile.store.AStore;
import xyz.rgnt.wfpowerblocks.block.PowerBlock;
import xyz.rgnt.wfpowerblocks.providers.data.codecs.ParticleCodec;
import xyz.rgnt.wfpowerblocks.providers.data.codecs.SoundCodec;

import java.util.*;

/**
 * Manages power blocks
 */
@Log4j2(topic = "WarfarePowerBlocks - Mngr")
public class PowerBlocksMngr implements Listener {

    private final BukkitPlugin pluginInstance;

    private final Table<UUID, Long, PowerBlock> powerBlocks = HashBasedTable.create();
    private final Map<UUID, Map.Entry<String, Integer>> queuedRewards = new HashMap<>();

    @Getter
    private @NotNull Optional<AStore> configurationStore = Optional.empty();
    @Getter
    private final Configuration configuration = new Configuration();

    @Getter
    private final EventListener eventHandler = new EventListener();

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
        this.load();

        final long saveInterval = this.configuration.dataSaveInterval * 60 * 20;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(pluginInstance, this::save, saveInterval, saveInterval);
        Bukkit.getPluginManager().registerEvents(this.eventHandler, pluginInstance);
    }

    /**
     * Terminates manager
     */
    public void terminate() {
        this.save();
    }


    public void load() {
        log.info("Loading data...");
        this.configuration.loadConfiguration();

        try {
            AStore queuedRewardsStore = pluginInstance.getStorageProvider().provideJson("", "data/reward_queue.json", false);
            final JsonObject data = (JsonObject)queuedRewardsStore.getUnderlyingDataSource();
            if(data!=null) {
                data.entrySet().forEach((entry) -> {
                    final UUID player = UUID.fromString(entry.getKey());
                    final JsonObject playerDataJson = entry.getValue().getAsJsonObject();

                    this.queuedRewards.put(player, Map.entry(playerDataJson.get("pwb_id").getAsString(), playerDataJson.get("position").getAsInt()));
                });
                log.info("Queued {} rewards.", this.queuedRewards.size());
            }

        } catch (Exception e) {
            log.error("Couldn't create queue rewards file", e);
        }

        {
            getConfiguration().getPowerBlockCodecs().forEach((powerBlockId, codec) -> {
                PowerBlock powerBlock = null;
                final AStore blockMemoryStore;
                try {
                    blockMemoryStore = pluginInstance.getStorageProvider()
                            .provideJson("", "data/powerblocks/" + powerBlockId + ".json", false);
                    // load memory if possible
                    if (blockMemoryStore.getFile().exists())
                        powerBlock = codec.constructPowerBlock(powerBlockId, PowerBlock.decodeBlockMemory((JsonObject) blockMemoryStore.getUnderlyingDataSource()));
                } catch (Exception x) {
                    log.error("Failed to decode memory file of power block '{}'.", powerBlockId, x);
                } finally {
                    // use null memory
                    if (powerBlock == null)
                        powerBlock = codec.constructPowerBlock(powerBlockId, null);
                }

                if(powerBlock == null) {
                    log.error("Invalid power block with id '{}'", powerBlockId);
                    return;
                }

                registerPowerBlock(powerBlock);
            });
        }

        log.info("Data loaded!");
    }

    public void save() {
        log.info("Saving data...");
        this.powerBlocks.values().forEach(powerBlock -> {
            try {
                AStore store = pluginInstance.getStorageProvider()
                        .provideJson("", "data/powerblocks/" + powerBlock.getId() + ".json", false);

                store.setUnderlyingDataSource(PowerBlock.encodeBlockMemory(powerBlock.getBlockMemory()));
                store.save();
            } catch (Exception e) {
                log.error("Couldn't create memory file for power block '{}'", powerBlock.getId(), e);
            }
        });

        try {
            AStore queuedRewardsStore = pluginInstance.getStorageProvider()
                    .provideJson("", "data/reward_queue.json", false);
            final var data = new JsonObject();
            this.queuedRewards.forEach((playerUuid, playerData) -> {
                final JsonObject playerDataJson = new JsonObject();
                playerDataJson.addProperty("pwb_id", playerData.getKey());
                playerDataJson.addProperty("position", playerData.getValue());
                data.add(playerUuid.toString(), playerDataJson);
            });
            queuedRewardsStore.setUnderlyingDataSource(data);
            queuedRewardsStore.save();

        } catch (Exception e) {
            log.error("Couldn't create queue rewards file", e);
        }
        log.info("Data saved!");
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
        log.info("§fPlugin created §ePower block§7({})§f at {} {} {}({}).",
                powerBlock.getId(),
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

        final var powerBlock = this.powerBlocks.remove(worldUID, locKey);
        if (powerBlock != null) {
            final Location loc = block.getLocation();
            log.info("§fPlugin destroyed §ePower block§7({})§f at {} {} {}({}).",
                    powerBlock.getId(),
                    loc.getBlockX(),
                    loc.getBlockY(),
                    loc.getBlockZ(),
                    loc.getWorld().getName()
            );
        }
        return powerBlock;
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

    public @Nullable PowerBlock getPowerBlock(@NotNull String id) {
        return this.powerBlocks.values()
                .stream().filter(powerBlock -> powerBlock.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    class EventListener implements Listener {

        @EventHandler
        public void handleOnPlayerJoin(final PlayerJoinEvent event) {
            final var uuid = event.getPlayer().getUniqueId();
            final var data = PowerBlocksMngr.this.queuedRewards.remove(uuid);
            if (data == null)
                return;

            final String pwbId = data.getKey();
            final int position = data.getValue();

            PowerBlock.Codec pwbCodec = PowerBlocksMngr.this.configuration.getPowerBlockCodecs().get(pwbId);
            if (pwbCodec == null) {
                log.warn("Player '{}' had queued reward(#{}) for power block '{}'. But it does not exist!", event.getPlayer().getName(), position, pwbId);
                return;
            }
            List<String> commands = pwbCodec.getRewards().get(position);
            if (commands == null) {
                log.warn("Player '{}' had queued reward(#{}) for power block '{}'. But rewards are not specified!", event.getPlayer().getName(), position, pwbId);
                return;
            }
            PowerBlocksMngr.this.processCommands(commands, event.getPlayer());
        }

        @EventHandler
        public void handleOnDestroyBlock(final BlockBreakEvent event) {
            final Block block = event.getBlock();
            final long locKey = block.getBlockKey();
            final UUID worldUID = block.getWorld().getUID();

            final PowerBlock powerBlock = PowerBlocksMngr.this.powerBlocks.get(worldUID, locKey);
            if (powerBlock == null)
                return;
            // If player is in creative mode, event will be cancelled
            if (event.getPlayer().getGameMode() == GameMode.CREATIVE && !event.getPlayer().hasPermission("warfarepowerblocks.admin")) {
                event.getPlayer().sendMessage(Component.text("You can't break this while in CREATIVE"));
                event.setCancelled(true);
                return;
            }
            event.setCancelled(true);
            final Player player = event.getPlayer();
            final UUID vandal = player.getUniqueId();

            if (configuration.getBreakSound() != null)
                configuration.getBreakSound().playTo(player);

            if (configuration.getBreakParticle() != null)
                configuration.getBreakParticle().showTo(player, block.getLocation());

            if (powerBlock.getBlockMemory().damage(vandal, 1) > 0)
                return;

            handlePowerBlockDefeat(powerBlock);
        }

        private void handlePowerBlockDefeat(@NotNull PowerBlock powerBlock) {
            final Location loc = powerBlock.getBlockLocation();

            log.info("§fDefeated §ePower block§7({})§f at {} {} {}({}) with key {}. Total participants count: {}. Total health points: {}",
                    powerBlock.getId(),
                    loc.getBlockX(),
                    loc.getBlockY(),
                    loc.getBlockZ(),
                    loc.getWorld().getName(),
                    loc.toBlockKey(),
                    powerBlock.getBlockMemory().getAttackers().size(),
                    powerBlock.getMaximalHealthPoints()
            );

            Bukkit.broadcast(PowerBlocksMngr.this.configuration.getMessage_PowerBlockDefeated(powerBlock));

            final var positionRewards = powerBlock.getPositionRewards();
            final var attackersSorted = powerBlock.getBlockMemory().getAttackersSorted();

            // rewards
            for (int i = 0;
                 (attackersSorted.size() > positionRewards.size()
                         ? i < positionRewards.size()
                         : i < attackersSorted.size());
                 i++) {

                final var attackerData = attackersSorted.get(i);
                final var uuid = attackerData.getKey();
                final var damage = attackerData.getValue();

                final var player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline())
                    PowerBlocksMngr.this.queuedRewards.put(uuid, Map.entry(powerBlock.getId(), i));
                else {
                    final var commands = positionRewards.get(i + 1);
                    processCommands(commands, player);

                    if (powerBlock.canRespawn()) {
                        if (configuration.getRespawnSound() != null)
                            configuration.getRespawnSound().playTo(player);
                    } else {
                        if (configuration.getDefeatSound() != null)
                            configuration.getDefeatSound().playTo(player);
                    }
                }
            }
            powerBlock.respawn();
        }
    }


    private void processCommands(@NotNull Iterable<String> commands, @NotNull Player player) {
        commands.forEach(command -> {
            boolean self = command.startsWith("@");
            command = command.replaceAll("%player%", player.getName());

            if (self)
                player.performCommand(command.substring(1));
            else
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        });
    }

    class Configuration implements ICodec {
        @Getter
        private final Map<String, PowerBlock.Codec> powerBlockCodecs = new HashMap<>();

        @CodecKey("messages.pwb-defeated")
        private final List<String> pwbDefeated = new ArrayList<>();

        @CodecKey("data.save-interval-minutes")
        @Getter
        private final int dataSaveInterval = 180;


        @CodecKey("settings.sounds.break-sound")
        @Getter
        private SoundCodec breakSound = new SoundCodec();

        @CodecKey("settings.sounds.respawn-sound")
        @Getter
        private SoundCodec respawnSound = new SoundCodec();

        @CodecKey("settings.sounds.defeat-sound")
        @Getter
        private SoundCodec defeatSound = new SoundCodec();

        @CodecKey("settings.particles.break-particle")
        @Getter
        private ParticleCodec breakParticle = new ParticleCodec();

        @CodecKey("settings.particles.respawn-particle")
        @Getter
        private ParticleCodec respawnParticle = new ParticleCodec();

        @CodecKey("settings.particles.defeat-particle")
        @Getter
        private ParticleCodec defeatParticle = new ParticleCodec();


        public @NotNull Component getMessage_PowerBlockDefeated(@NotNull PowerBlock block) {
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

        public void loadConfiguration() {
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

                // resolve Configuration codec
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

                // resolve all PowerBlock codecs
                powerBlocksCodecsSector.getKeys().forEach(powerBlockId -> {
                    final var powerBlockCodec = powerBlocksCodecsSector.getSector(powerBlockId);
                    if (powerBlockCodec == null)
                        return;

                    final PowerBlock.Codec codec = new PowerBlock.Codec();
                    try {
                        powerBlockCodec.decode(codec);
                        this.powerBlockCodecs.put(powerBlockId, codec);
                    } catch (Exception e) {
                        log.error("Couldn't decode powerblock with id '{}'", powerBlockId, e);
                    }
                });
            });
        }
    }

}
