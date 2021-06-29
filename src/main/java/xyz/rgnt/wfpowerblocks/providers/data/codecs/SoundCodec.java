package xyz.rgnt.wfpowerblocks.providers.data.codecs;

import lombok.Getter;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.rgnt.revoken.common.providers.storage.data.AuxData;
import xyz.rgnt.revoken.common.providers.storage.data.codec.ICodec;
import xyz.rgnt.revoken.common.providers.storage.data.codec.meta.CodecKey;

public class SoundCodec implements ICodec {

    @CodecKey("sound-name")
    @Getter
    private String soundName;
    @CodecKey("pitch")
    @Getter
    private float pitch = 1.0f;
    @CodecKey("volume")
    @Getter
    private float volume = 1.0f;
    @Getter
    private Sound bukkitSound;

    public void playTo(@NotNull Player player) {
        if(getBukkitSound() != null)
            player.playSound(player.getLocation(), getBukkitSound(), getVolume(), getPitch());
    }

    @Override
    public void onDecode(@NotNull AuxData source) throws Exception {
        try {
            if(!this.soundName.isBlank())
                this.bukkitSound = Sound.valueOf(this.soundName.toUpperCase());
        } catch (Exception x) {
            throw new Exception("Invalid sound name: " + this.soundName);
        }
    }
}
