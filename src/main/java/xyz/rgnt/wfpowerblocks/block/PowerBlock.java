package xyz.rgnt.wfpowerblocks.block;

import com.google.gson.JsonObject;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.rgnt.revoken.common.providers.storage.data.AuxData;
import xyz.rgnt.revoken.common.providers.storage.data.codec.ICodec;
import xyz.rgnt.revoken.common.providers.storage.data.codec.meta.CodecKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Power block class represents world block with special abilities
 */
public class PowerBlock {

    @Getter
    private @NotNull String id;
    @Getter
    private @NotNull Component name;
    @Getter
    private @NotNull Location blockLocation;
    @Getter
    private @NotNull Block blockInstance;

    @Getter
    private BlockMemory blockMemory = new BlockMemory();

    @Getter
    private final Map<Integer, List<String>> positionRewards = new HashMap<>();

    /**
     * Decrements block's health points value
     *
     * @param attacker Player UUID
     * @param damage   Damage dealt
     * @return Decremented health points value
     */
    public Integer damageBlock(@NotNull UUID attacker, @NotNull Integer damage) {
        return blockMemory.damage(attacker, damage);
    }

    /**
     * @return Maximal health points value
     */
    public @NotNull Integer getMaximalHealthPoints() {
        return this.blockMemory.maximalHealthPoints;
    }

    /**
     * @return Current health points value
     */
    public @NotNull Integer getCurrentHealthPoints() {
        return this.blockMemory.currentHealthPoints.get();
    }

    /**
     * Resets power block
     */
    public boolean respawn() {
        return this.getBlockMemory().respawn(false);
    }

    public boolean canRespawn() {
        return this.blockMemory.canRespawn();
    }


    public static @NotNull JsonObject encodeBlockMemory(final @NotNull BlockMemory memory) {
        JsonObject attackers = new JsonObject();
        memory.attackers.forEach((vandalUUID, damage) -> {
            attackers.addProperty(vandalUUID.toString(), damage);
        });
        JsonObject root = new JsonObject();
        root.add("attackers", attackers);
        root.addProperty("health_points", memory.getCurrentHealthPoints());
        return root;
    }

    public static @NotNull BlockMemory decodeBlockMemory(final @NotNull JsonObject data) {
        final BlockMemory memory = new BlockMemory();
        JsonObject attackers = data.get("attackers").getAsJsonObject();

        attackers.entrySet().forEach(entry -> {
            final String vandal = entry.getKey();
            final Integer damage = entry.getValue().getAsInt();

            memory.damage(UUID.fromString(vandal), damage);
        });

        memory.currentHealthPoints = new AtomicInteger(data.get("health_points").getAsInt());
        return memory;
    }

    public static @NotNull PowerBlock.Builder builder(@NotNull String id) {
        return Builder.builder(id);
    }

    /**
     * Stores all dynamic data of power block
     */
    public static class BlockMemory {
        @Getter
        private Integer maximalHealthPoints = 0;
        @Getter
        private AtomicInteger currentHealthPoints = new AtomicInteger(1);

        @Getter
        private Integer maximalRespawnCount = 0;
        @Getter
        private AtomicInteger currentRespawnCount = new AtomicInteger(0);

        @Getter
        private final Map<UUID, Integer> attackers = new ConcurrentHashMap<>();

        /**
         * Decreases current heath points value and registers attacker and his damage
         *
         * @param uuid   Attacker
         * @param damage Damage dealt by attacker
         * @return Current health points value
         */
        public int damage(UUID uuid, Integer damage) {
            this.attackers.compute(uuid, (oldUuid, originDamage) ->
                    (originDamage != null ? originDamage : 0) + damage
            );
            return decreaseHealth(damage);
        }

        /**
         * Increases current health points
         *
         * @param value Value
         * @return Increased health points value
         */
        public int increaseHealth(int value) {
            return this.currentHealthPoints.addAndGet(value);
        }

        /**
         * Decreases current health points
         *
         * @param value Value
         * @return Decreased health points value
         */
        public int decreaseHealth(int value) {
            return this.currentHealthPoints.addAndGet(-value);
        }

        /**
         * Clears all attacker data and restores maximal health points if can be respawned.
         */
        public boolean respawn(boolean force) {
            if (canRespawn() && !force)
                return false;
            this.attackers.clear();
            this.currentHealthPoints = new AtomicInteger(this.maximalHealthPoints);
            return true;
        }

        /**
         * @return Whether this power block will respawn after it's destroyed
         */
        public boolean canRespawn() {
            return this.currentRespawnCount.get() > this.maximalRespawnCount;
        }

        public List<Map.Entry<UUID, Integer>> getAttackersSorted() {
            final List<Map.Entry<UUID, Integer>> attackers = new LinkedList<>(this.getAttackers().entrySet());
            attackers.sort(Map.Entry.comparingByValue());
            Collections.reverse(attackers);
            return attackers;
        }
    }

    /**
     * Codec
     */
    public static class Codec implements ICodec {
        @CodecKey("material")
        private @Nullable String material = null;
        @CodecKey("name")
        private String name;
        @CodecKey("health-points")
        private int healthPoints;
        @CodecKey("respawn-count")
        private int respawnCount;

        @CodecKey("location.world")
        private String worldName;
        @CodecKey("location.location.x")
        private int x;
        @CodecKey("location.location.y")
        private int y;
        @CodecKey("location.location.z")
        private int z;

        @Getter
        private final Map<Integer, List<String>> rewards = new HashMap<>();

        /**
         * Constructs power block from codec data
         *
         * @param id     Id of powerblock
         * @param memory Nullable memory of powerblock
         * @return Powerblock
         */
        public @NotNull PowerBlock constructPowerBlock(@NotNull String id, @Nullable PowerBlock.BlockMemory memory) {
            final var loc = new Location(Bukkit.getWorld(worldName), x, y, z);
            if (material != null)
                loc.getWorld().getBlockAt(loc).setType(Material.valueOf(this.material));

            final var builder = PowerBlock.builder(id);

            rewards.forEach(builder::withRewardCommand);

            if (memory == null)
                builder.withCurrentHealthPoints(healthPoints);
            return builder.withBlockMemory(memory).withKyoriName(name).withMaximalHealthPoints(healthPoints).fromBukkitLocation(loc).build();
        }

        @Override
        public void onDecode(@NotNull AuxData source) throws Exception {
            source.getKeys("rewards").forEach((key) -> {
                List<String> rewardCommands = source.getStringList("rewards." + key);
                this.rewards.put(Integer.valueOf(key), rewardCommands);
            });

        }
    }


    public static class Builder {
        private final PowerBlock powerBlock;

        private Builder(@NotNull String id) {
            this.powerBlock = new PowerBlock();
            this.powerBlock.id = id;
        }

        public @NotNull Builder withKyoriName(@NotNull String minimessage) {
            this.powerBlock.name = MiniMessage.markdown().parse(minimessage);
            return this;
        }

        public @NotNull Builder fromBukkitBlock(@NotNull Block block) {
            this.powerBlock.blockInstance = block;
            this.powerBlock.blockLocation = block.getLocation();
            return this;
        }

        public @NotNull Builder fromBukkitLocation(@NotNull Location location) {
            this.powerBlock.blockLocation = location;
            this.powerBlock.blockInstance = location.getBlock();
            return this;
        }

        public @NotNull Builder withHealthPoints(@NotNull Integer healthPoints) {
            this.withMaximalHealthPoints(healthPoints);
            return this.withCurrentHealthPoints(healthPoints);
        }

        public @NotNull Builder withMaximalHealthPoints(@NotNull Integer healthPoints) {
            this.powerBlock.blockMemory.maximalHealthPoints = healthPoints;
            return this;
        }

        public @NotNull Builder withCurrentHealthPoints(@NotNull Integer healthPoints) {
            this.powerBlock.blockMemory.currentHealthPoints = new AtomicInteger(healthPoints);
            return this;
        }

        public @NotNull Builder withBlockMemory(@Nullable BlockMemory memory) {
            if (memory != null)
                this.powerBlock.blockMemory = memory;
            return this;
        }


        public @NotNull Builder withRewardCommand(int index, List<String> commands) {
            this.powerBlock.positionRewards.put(index, commands);
            return this;
        }

        public @NotNull PowerBlock build() {
            return this.powerBlock;
        }

        public static @NotNull PowerBlock.Builder builder(@NotNull String id) {
            return new Builder(id);
        }
    }

}
