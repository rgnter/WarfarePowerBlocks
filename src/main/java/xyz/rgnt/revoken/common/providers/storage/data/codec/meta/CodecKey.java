package xyz.rgnt.revoken.common.providers.storage.data.codec.meta;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents a codec key for any {@link xyz.rgnt.revoken.common.providers.storage.data.codec.ICodec} class
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CodecKey {
    @NotNull String value();
}
