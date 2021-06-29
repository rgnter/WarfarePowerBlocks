package xyz.rgnt.wfpowerblocks.providers.data.codecs;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.rgnt.revoken.common.providers.storage.data.AuxData;
import xyz.rgnt.revoken.common.providers.storage.data.codec.ICodec;
import xyz.rgnt.revoken.common.providers.storage.data.codec.meta.CodecKey;

public class ParticleCodec implements ICodec {

    @CodecKey("particle-name")
    @Getter
    private String particleName;

    @CodecKey("count")
    @Getter
    private int count = 10;

    @CodecKey("offset-x")
    @Getter
    private double offsetX = 1;
    @CodecKey("offset-y")
    @Getter
    private int offsetY = 1;
    @CodecKey("offset-z")
    @Getter
    private int offsetZ = 1;

    @Getter
    private Particle bukkitParticle;

    public void showTo(@NotNull Player player, @NotNull Location location) {
        if(getBukkitParticle() != null)
            player.getWorld().spawnParticle(getBukkitParticle(), location, getCount(), getOffsetX(), getOffsetY(), getOffsetZ());
    }

    @Override
    public void onDecode(@NotNull AuxData source) throws Exception {
        try {
            if(!this.particleName.isBlank())
                this.bukkitParticle = Particle.valueOf(this.particleName.toUpperCase());
        } catch (Exception x) {
            throw new Exception("Invalid particle name: " + this.getParticleName());
        }
    }

}
